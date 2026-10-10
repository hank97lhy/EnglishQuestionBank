#!/usr/bin/env python3
"""Read selected FCE paper/answer PDFs and optional HTML; never execute SQL.

PDF text: pypdf. Optional rendering: pypdfium2. No OCR engine is bundled here.
"""
import argparse
import hashlib
import json
import math
from html.parser import HTMLParser
from pathlib import Path


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f'Duplicate JSON object key: {key}')
        result[key] = value
    return result


def select_pages(spec, total):
    if spec is None:
        return list(range(1, total + 1))
    pages = set()
    for term in spec.split(','):
        values = term.strip().split('-')
        if len(values) == 1:
            start = end = int(values[0])
        elif len(values) == 2:
            start, end = map(int, values)
        else:
            raise ValueError('Invalid page range')
        if start < 1 or end < start or end > total:
            raise ValueError(f'Page range {term} outside 1-{total}')
        pages.update(range(start, end + 1))
    if not pages:
        raise ValueError('Empty page selection')
    return sorted(pages)


def extract_pdf(path, out, role, page_spec=None, render=False, scale=2.0):
    try:
        from pypdf import PdfReader
    except ImportError as exc:
        raise RuntimeError('PDF extraction requires pypdf; use the bundled Python runtime.') from exc
    if not math.isfinite(scale) or scale <= 0:
        raise ValueError('Render scale must be finite and positive')
    reader = PdfReader(path)
    selected = select_pages(page_spec, len(reader.pages))
    renderer = None
    if render:
        try:
            import pypdfium2
        except ImportError as exc:
            raise RuntimeError('Rendering requires pypdfium2; use bundled Python or render with available Poppler.') from exc
        renderer = pypdfium2.PdfDocument(str(path))
    records = []
    image_dir = out / (role + '-pages')
    try:
        for number in selected:
            page = reader.pages[number - 1]
            text = page.extract_text() or ''
            record = {'pdf_page': number, 'printed_page': None, 'text': text,
                      'width_points': float(page.mediabox.width),
                      'height_points': float(page.mediabox.height),
                      'needs_visual_or_ocr': len(text.strip()) < 40,
                      'visually_verified': False}
            if renderer is not None:
                image_dir.mkdir(parents=True, exist_ok=True)
                rendered_page = renderer[number - 1]
                bitmap = rendered_page.render(scale=scale)
                try:
                    target = image_dir / f'page-{number:03d}.png'
                    bitmap.to_pil().save(target)
                    record['rendered_image'] = target.relative_to(out).as_posix()
                finally:
                    bitmap.close()
                    rendered_page.close()
            records.append(record)
    finally:
        if renderer is not None:
            renderer.close()
    result = {'source': str(path.resolve()), 'sha256': sha256(path.read_bytes()),
              'role': role, 'total_pages': len(reader.pages), 'selected_pages': selected,
              'pages': records, 'notes': [
                  'Page numbers are 1-based PDF indices, not printed page numbers.',
                  'Short/empty text must be read visually or with OCR; it does not mean no questions.',
                  'The scan heuristic is not OCR quality validation; all imported text needs verification.',
                  'Rendering does not mark a page verified or transcribe answers.',
                  'Only explicitly selected files are read; no sibling PDFs are opened.']}
    write_json(out / (role + '-extracted.json'), result)
    return {'total_pages': len(reader.pages), 'selected_pages': selected,
            'needs_visual_or_ocr': sum(p['needs_visual_or_ocr'] for p in records),
            'rendered': render}


class ExamDataParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=False)
        self.active = False
        self.current = []
        self.scripts = []

    def handle_starttag(self, tag, attrs):
        if tag.lower() == 'script' and dict(attrs).get('id') == 'examData':
            self.active = True
            self.current = []

    def handle_data(self, data):
        if self.active:
            self.current.append(data)

    def handle_endtag(self, tag):
        if tag.lower() == 'script' and self.active:
            self.scripts.append(''.join(self.current))
            self.active = False


def extract_html(path, out):
    payload = path.read_bytes()
    parser = ExamDataParser()
    parser.feed(payload.decode('utf-8-sig'))
    parser.close()
    if parser.active or len(parser.scripts) != 1:
        raise ValueError('Expected exactly one complete script#examData')
    data = json.loads(parser.scripts[0], object_pairs_hook=unique_object)
    if not isinstance(data, dict) or not isinstance(data.get('sections'), list):
        raise ValueError('examData must be an object containing sections array')
    write_json(out / 'exam-data.json', data)
    sections = []
    issues = []
    question_ids = []
    totals = {'sections': 0, 'questions': 0, 'paragraphs': 0, 'evidence': 0}
    for section in data['sections']:
        code = section['id']
        paragraphs = section['paragraphs']
        para_map = {p['id']: p['text'] for p in paragraphs}
        questions = section['questions']
        if len(para_map) != len(paragraphs):
            issues.append({'kind': 'duplicate_paragraph_id', 'section': code})
        option_counts = []
        for question in questions:
            qid = str(question['id'])
            question_ids.append(qid)
            options = question.get('options', {})
            option_counts.append(len(options))
            for index, evidence in enumerate(question.get('evidence', []), 1):
                totals['evidence'] += 1
                key, quote = evidence['paragraph_id'], evidence['quote']
                kind = ('missing_evidence_paragraph' if key not in para_map
                        else 'evidence_quote_mismatch' if quote not in para_map[key] else None)
                if kind:
                    issues.append({'kind': kind, 'section': code, 'question': qid,
                                   'evidence_order': index, 'paragraph_id': key, 'quote': quote})
        declared = section.get('expected_option_count')
        if declared is not None and any(n != declared for n in option_counts):
            issues.append({'kind': 'declared_option_count_mismatch', 'section': code,
                           'declared': declared, 'actual_per_question': option_counts})
        if code in ['P6', 'P7'] and questions:
            expected = section.get('shared_options', questions[0].get('options', {}))
            if any(q.get('options', {}) != expected for q in questions):
                issues.append({'kind': 'shared_options_mismatch', 'section': code})
        totals['sections'] += 1
        totals['questions'] += len(questions)
        totals['paragraphs'] += len(paragraphs)
        sections.append({'code': code, 'questions': len(questions),
                         'paragraphs': len(paragraphs), 'source_fields': list(section)})
    if len(set(question_ids)) != len(question_ids):
        issues.append({'kind': 'duplicate_paper_question_id'})
    expected_ids = data.get('expected_question_ids')
    if expected_ids is not None and sorted(map(str, expected_ids)) != sorted(question_ids):
        issues.append({'kind': 'expected_question_ids_mismatch'})
    report = {'source': str(path.resolve()), 'sha256': sha256(payload),
              'source_fields': list(data), 'totals': totals, 'question_ids': question_ids,
              'sections': sections, 'issues': issues,
              'notes': ['Extraction diagnostics only; not a complete import validator.',
                        'Issues are preserved for review; exit success does not imply publishable data.']}
    write_json(out / 'html-report.json', report)
    return {'totals': totals, 'issues': len(issues)}


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument('--pdf', type=Path, help='Selected exam PDF only')
    cli.add_argument('--answers', type=Path, help='Matching answer PDF only')
    cli.add_argument('--html', type=Path, help='Optional matching static website')
    cli.add_argument('--pages', help='1-based exam pages, e.g. 2-13')
    cli.add_argument('--answer-pages', help='1-based answer pages, e.g. 1')
    cli.add_argument('--render', action='store_true')
    cli.add_argument('--scale', type=float, default=2.0)
    cli.add_argument('--out', required=True, type=Path)
    args = cli.parse_args()
    if not any([args.pdf, args.answers, args.html]):
        cli.error('At least one of --pdf, --answers or --html is required')
    if (args.pages and not args.pdf) or (args.answer_pages and not args.answers):
        cli.error('Page selection requires the corresponding PDF input')
    for source in [args.pdf, args.answers]:
        if source and source.suffix.lower() != '.pdf':
            cli.error('Exam and answer inputs must be PDF files')
    args.out.mkdir(parents=True, exist_ok=True)
    outputs = [args.out / name for name in ['paper-extracted.json', 'answers-extracted.json',
                                          'exam-data.json', 'html-report.json']]
    for source in [args.pdf, args.answers, args.html]:
        if source and source.resolve() in [p.resolve() for p in outputs]:
            cli.error('Output path collides with an input file')
    result = {}
    if args.pdf:
        result['pdf'] = extract_pdf(args.pdf, args.out, 'paper', args.pages, args.render, args.scale)
    if args.answers:
        result['answers'] = extract_pdf(args.answers, args.out, 'answers', args.answer_pages, args.render, args.scale)
    if args.html:
        result['html'] = extract_html(args.html, args.out)
    print(json.dumps(result, ensure_ascii=True))


if __name__ == '__main__':
    main()
