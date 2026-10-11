package com.kate.bank.question.fce.api;

import java.util.List;
import java.util.Map;
import com.kate.bank.question.fce.api.dto.CatalogResponse;
import com.kate.bank.question.fce.api.dto.PaperResponse;
import com.kate.bank.question.fce.api.dto.SectionResponse;
import com.kate.bank.question.fce.api.dto.SectionResponse.QuestionResponse;
import com.kate.bank.question.fce.service.CatalogService;
import com.kate.bank.question.fce.service.ExamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExamControllerTests {
    private final CatalogService catalogService = mock(CatalogService.class);
    private final ExamService examService = mock(ExamService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ExamController(catalogService, examService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    private SectionResponse section() {
        var question = new QuestionResponse("9007199254740993", "25", 25, "Stem\n{{25}}", Map.of(),
                List.of("had not expected", "hadn't expected"), "had not expected / hadn't expected", "official",
                "答案页", "关键词转换", "解析", "说明", "策略", "易错点", List.of("定位"), Map.of(),
                null, List.of(), List.of(new SectionResponse.EvidenceResponse("P4-p1", "{{25}}")));
        return new SectionResponse("10", "P4", "Part 4", 4, "grammar", "key_word_transformation", "Part 4",
                "原卷", null, List.of(), Map.of(), List.of(), List.of(question));
    }

    @Test
    // 测试目的：验证目录 GET 路由、JSON 字段名、状态和可用标志符合首页数据需求。
    void catalogReturnsJson() throws Exception {
        when(catalogService.getCatalog()).thenReturn(new CatalogResponse(List.of(
                new CatalogResponse.EditionItem("1", "standard", "标准版", List.of(
                        new CatalogResponse.BookItem("2", 1, "标准版 1", List.of(
                                new CatalogResponse.PaperItem("3", 1, "standard-1-test-1", "标题", "副标题",
                                        "published", 75, true))))))));
        mvc.perform(get("/api/v1/catalog")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.editions[0].code").value("standard"))
                .andExpect(jsonPath("$.editions[0].books[0].book_no").value(1))
                .andExpect(jsonPath("$.editions[0].books[0].tests[0].test_no").value(1))
                .andExpect(jsonPath("$.editions[0].books[0].tests[0].available").value(true))
                .andExpect(jsonPath("$.editions[0].books[0].tests[0].duration_minutes").value(75));
    }

    @Test
    // 测试目的：验证整卷序列化兼容静态字段名，大 ID 为字符串，答案数组保持原始类型，并省略未入库教学模块。
    void paperUsesStaticFieldNamesAndStringDatabaseIds() throws Exception {
        when(examService.getPaper("standard-1-test-1")).thenReturn(new PaperResponse("1", "standard-1-test-1",
                "试卷", "阅读与语用", 75, "更新版", Map.of("exam_page", 2),
                new PaperResponse.Summary(1, 1), List.of(section())));
        mvc.perform(get("/api/v1/papers/standard-1-test-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.database_id").isString())
                .andExpect(jsonPath("$.exam_summary.sections").value(1))
                .andExpect(jsonPath("$.exam_summary.questions").value(1))
                .andExpect(jsonPath("$.sections[0].question_format").value("key_word_transformation"))
                .andExpect(jsonPath("$.sections[0].questions[0].database_id").value("9007199254740993"))
                .andExpect(jsonPath("$.sections[0].questions[0].database_id").isString())
                .andExpect(jsonPath("$.sections[0].questions[0].id").value("25"))
                .andExpect(jsonPath("$.sections[0].questions[0].answer").isArray())
                .andExpect(jsonPath("$.sections[0].questions[0].answer[1]").value("hadn't expected"))
                .andExpect(jsonPath("$.sections[0].questions[0].answer_source").value("答案页"))
                .andExpect(jsonPath("$.sections[0].questions[0].solve_steps[0]").value("定位"))
                .andExpect(jsonPath("$.sections[0].questions[0].evidence[0].paragraph_id").value("P4-p1"))
                .andExpect(jsonPath("$.dictionary").doesNotExist())
                .andExpect(jsonPath("$.sections[0].vocabulary").doesNotExist())
                .andExpect(jsonPath("$.sections[0].writing_bank").doesNotExist());
        verify(examService).getPaper("standard-1-test-1");
    }

    @Test
    // 测试目的：验证 Part 路由正确传递 slug 和 P 编号，并只返回单个 Part。
    void sectionRouteForwardsBothIdentifiers() throws Exception {
        when(examService.getSection("standard-1-test-1", "P4")).thenReturn(section());
        mvc.perform(get("/api/v1/papers/standard-1-test-1/sections/P4")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("P4"))
                .andExpect(jsonPath("$.part_no").value(4))
                .andExpect(jsonPath("$.questions[0].question_no").value(25));
        verify(examService).getSection("standard-1-test-1", "P4");
    }

    @Test
    // 测试目的：验证未知或未发布试卷返回 RFC 9457 的 404 JSON 错误。
    void missingPaperReturnsProblemDetail() throws Exception {
        when(examService.getPaper("missing"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Published paper not found"));
        mvc.perform(get("/api/v1/papers/missing")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Published paper not found"));
    }

    @Test
    // 测试目的：验证不存在的 Part 返回 404，错误请求不伪装成空 Part。
    void missingSectionReturnsProblemDetail() throws Exception {
        when(examService.getSection("standard-1-test-1", "P8"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Part not found"));
        mvc.perform(get("/api/v1/papers/standard-1-test-1/sections/P8")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    // 测试目的：验证内容 API 仅提供 GET，不接受写入请求。
    void postIsNotSupported() throws Exception {
        mvc.perform(post("/api/v1/catalog")).andExpect(status().isMethodNotAllowed());
    }
}
