import java.nio.file.*;
import java.util.*;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.resource.StringResource;
import org.flywaydb.database.postgresql.PostgreSQLParser;

/** Flyway statement-boundary check only: no database or datasource configuration. */
class CheckSql {
 public static void main(String[] args) throws Exception {
  var config = new FluentConfiguration().placeholderReplacement(false);
  for (String file : args) {
   var parser = new PostgreSQLParser(config, new ParsingContext());
   int count=0;
   try (var statements=parser.parse(new StringResource(Files.readString(Path.of(file))))) {
    while(statements.hasNext()) { statements.next(); count++; }
   }
   System.out.println(file+": "+count+" statements parsed (not PostgreSQL execution)");
  }
 }
}
