package dev.kuyo.autopsy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuyo.domain.Lesson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShishoTest {

    private static final String SARIF = """
            {"runs": [{
              "tool": {"driver": {"rules": [
                {"id": "field-injection", "shortDescription": {"text": "Field injection"}},
                {"id": "system-out", "shortDescription": {"text": "Printing to the console"}},
                {"id": "missing-actuator", "shortDescription": {"text": "No health checks"}}
              ]}},
              "results": [
                {"ruleId": "missing-actuator", "properties": {"severity": "LEVE"},
                 "locations": [{"physicalLocation": {"artifactLocation": {"uri": "pom.xml"}}}]},
                {"ruleId": "field-injection", "properties": {"severity": "GRAVE"},
                 "locations": [{"physicalLocation": {"artifactLocation": {"uri": "src/x/Foo.java"}, "region": {"startLine": 6}}}]},
                {"ruleId": "system-out", "properties": {"severity": "MEDIA"},
                 "locations": [{"physicalLocation": {"artifactLocation": {"uri": "src/x/Foo.java"}, "region": {"startLine": 8}}}]},
                {"ruleId": "system-out", "properties": {"severity": "MEDIA"},
                 "locations": [{"physicalLocation": {"artifactLocation": {"uri": "src/x/Foo.java"}, "region": {"startLine": 8}}}]}
              ]
            }]}
            """;

    @Test
    void onlyTheDeceasedsOwnLessonsCountWithoutRepeats() throws Exception {
        List<Lesson> lessons = Shisho.lessons(new ObjectMapper().readTree(SARIF), "src/x/Foo.java");

        assertEquals(List.of(
                new Lesson("field-injection", "GRAVE", "Field injection", 6),
                new Lesson("system-out", "MEDIA", "Printing to the console", 8)), lessons);
    }
}
