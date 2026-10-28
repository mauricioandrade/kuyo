package dev.kuyo.seance;

import dev.kuyo.domain.CauseOfDeath;
import dev.kuyo.domain.DeathCommit;
import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Findings;
import dev.kuyo.domain.Soul;
import dev.kuyo.epitaph.Language;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediumTest {

    @Test
    void theGhostRemembersItsContentsAndLastWords() {
        String invocation = Medium.invocation(soul("class EmailService { void enviar() {} }", false), Language.pt);

        assertTrue(invocation.contains("class EmailService { void enviar() {} }"));
        assertTrue(invocation.contains("\"chore: remover EmailService\""));
        assertTrue(invocation.contains("Brazilian Portuguese"));
        assertTrue(invocation.contains("you lived 11 days"));
    }

    @Test
    void aBinaryFileRemembersNoWords() {
        String invocation = Medium.invocation(soul(null, true), Language.en);

        assertTrue(invocation.contains("You were a binary file"));
    }

    @Test
    void aHugeFileIsTrimmed() {
        String huge = "x".repeat(Medium.MAX_MEMORY + 500);
        String trimmed = Medium.trim(huge);

        assertTrue(trimmed.endsWith("(the rest has faded)"));
        assertFalse(trimmed.length() > Medium.MAX_MEMORY + 40);
    }

    private static Soul soul(String contents, boolean binary) {
        ZonedDateTime when = ZonedDateTime.of(2026, 6, 1, 17, 1, 0, 0, ZoneOffset.ofHours(-3));
        DeathCommit death = new DeathCommit("acb09f24", "chore: remover EmailService", "Ana", when, 2, 40, false);
        Deceased d = new Deceased("src/EmailService.java", when.toInstant().minus(Duration.ofDays(11)),
                when.toInstant(), when.toInstant().minus(Duration.ofDays(3)), Set.of("Ana"), 4, 40, binary,
                death, contents);
        return new Soul(d, CauseOfDeath.NATURAL, new Findings(0, 0, 1, 1, 0), null);
    }
}
