package dev.kuyo.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoronerTest {

    private static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);
    private final Coroner coroner = new Coroner();

    @Test
    void calmDeathIsNatural() {
        Soul soul = coroner.examine(deceased("A.java", Duration.ofDays(200), Duration.ofDays(10),
                death("refactor: troca por KataGuide", 14, 2, 30, true)));

        assertEquals(CauseOfDeath.NATURAL, soul.cause());
        assertEquals(0.30 * Math.log1p(30) / Math.log1p(2000), soul.restlessness(), 1e-9);
    }

    @Test
    void lessThanADayIsPremature() {
        Soul soul = coroner.examine(deceased("A.java", Duration.ofHours(5), Duration.ZERO,
                death("feat: troca componente", 14, 2, 10, true)));

        assertEquals(CauseOfDeath.PREMATURE, soul.cause());
    }

    @Test
    void violentBeatsPremature() {
        Soul soul = coroner.examine(deceased("A.java", Duration.ofHours(2), Duration.ZERO,
                death("wip", 3, 60, 5_000, false)));

        assertEquals(CauseOfDeath.VIOLENT, soul.cause());
        assertEquals(0.80 + 0.20 * (1 - Math.log1p(2) / Math.log1p(720)), soul.restlessness(), 1e-9);
    }

    @Test
    void massGraveInTheSmallHoursWithoutTestsIsViolent() {
        Soul soul = coroner.examine(deceased("A.java", Duration.ofDays(400), Duration.ofDays(1),
                death("cleanup", 2, 50, 0, false)));

        assertEquals(CauseOfDeath.VIOLENT, soul.cause());
        assertEquals(0.30 + 0.15 + 0.20 + 0.15, soul.restlessness(), 1e-9);
    }

    @Test
    void overAYearUntouchedIsForgotten() {
        Soul soul = coroner.examine(deceased("A.java", Duration.ofDays(900), Duration.ofDays(500),
                death("chore: tira modulo antigo", 15, 1, 10, true)));

        assertEquals(CauseOfDeath.FORGOTTEN, soul.cause());
    }

    @Test
    void gitkeepFulfilledItsMission() {
        Soul soul = coroner.examine(deceased("src/domain/.gitkeep", Duration.ofMinutes(10), Duration.ZERO,
                death("wip", 3, 60, 5_000, false)));

        assertEquals(CauseOfDeath.NATURAL, soul.cause());
        assertEquals(0.0, soul.restlessness());
    }

    @Test
    void hourOfDeathUsesTheAuthorsTimeZone() {
        // 3 a.m. in Brasília is 6 a.m. UTC: still the small hours for whoever deleted it
        DeathCommit death = death("feat: x", 3, 1, 1, true);
        assertEquals(1.0, coroner.findings(deceased("A.java", Duration.ofDays(100), Duration.ZERO, death)).hourOfDeath());
    }

    @Test
    void shortLifeFallsOnALogScale() {
        DeathCommit death = death("feat: x", 14, 1, 1, true);
        assertEquals(0.0, shortLife(Duration.ofDays(30), death), 1e-9);
        assertEquals(0.0, shortLife(Duration.ofDays(400), death), 1e-9);
        assertEquals(1 - Math.log1p(24) / Math.log1p(720), shortLife(Duration.ofDays(1), death), 1e-9);
    }

    @Test
    void noTestsOnlyCountsForCode() {
        DeathCommit death = death("feat: x", 14, 1, 1, false);
        assertEquals(1.0, coroner.findings(deceased("A.java", Duration.ofDays(100), Duration.ZERO, death)).noTests());
        assertEquals(0.0, coroner.findings(deceased("PLAN.md", Duration.ofDays(100), Duration.ZERO, death)).noTests());
    }

    @Test
    void emptyOrPortugueseMessageIsSuspicious() {
        assertEquals(1.0, message(""));
        assertEquals(1.0, message("Remoção de telas antigas"));
        assertEquals(1.0, message("chore: apagar specs"));
        assertEquals(0.0, message("refactor: removable flag"));
    }

    private double shortLife(Duration life, DeathCommit death) {
        return coroner.findings(deceased("A.java", life, Duration.ZERO, death)).shortLife();
    }

    private double message(String message) {
        return coroner.findings(deceased("A.java", Duration.ofDays(100), Duration.ZERO, death(message, 14, 1, 1, true)))
                .suspiciousMessage();
    }

    private static DeathCommit death(String message, int hour, int files, int lines, boolean tests) {
        ZonedDateTime when = ZonedDateTime.of(2026, 10, 1, hour, 0, 0, 0, BRASILIA);
        return new DeathCommit("abc12345", message, "Ana", when, files, lines, tests);
    }

    private static Deceased deceased(String path, Duration life, Duration idleBeforeDeath, DeathCommit death) {
        Instant end = death.when().toInstant();
        return new Deceased(path, end.minus(life), end, end.minus(idleBeforeDeath), Set.of("Ana"),
                3, 10, false, death, "class A {}");
    }
}
