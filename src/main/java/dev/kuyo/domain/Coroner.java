package dev.kuyo.domain;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Examines the deceased and decides how restless the soul is and what it died of.
 * All deterministic: the model only receives the result.
 */
@ApplicationScoped
public class Coroner {

    static final double VIOLENT_THRESHOLD = 0.6;

    private static final int MAX_FILES = 50;
    private static final int MAX_LINES = 2_000;
    private static final Duration PREMATURE = Duration.ofHours(24);
    private static final Duration SHORT_LIFE_END = Duration.ofDays(30);
    private static final Duration OBLIVION = Duration.ofDays(365);

    /** English and Portuguese: commit messages come in whatever language the author speaks. */
    private static final Pattern SUSPICIOUS_MESSAGE = Pattern.compile(
            "\\b(remov(e[ds]?|ing|al|er|endo|i|id[oa]s?)|remo[çc](ão|ao|ões|oes)"
                    + "|delet(e[ds]?|ing|ion|ar|ad[oa]s?|ei|ando)"
                    + "|apag(a|ar|ad[oa]s?|uei|ando)"
                    + "|exclu(i|ir|íd[oa]s?|id[oa]s?|são|sao|indo)"
                    + "|lixo|wip|hotfix|clean\\s?up|oops)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    /** "No tests" only counts for code: nobody deletes a .md or a .png together with tests. */
    private static final Pattern CODE = Pattern.compile(
            "\\.(java|kt|kts|scala|groovy|ts|tsx|js|jsx|mjs|cjs|vue|svelte|py|go|rs|rb|php|cs|fs"
                    + "|c|cc|cpp|h|hpp|swift|m|dart|ex|exs|clj|sql|sh)$",
            Pattern.CASE_INSENSITIVE);

    /** Empty-folder markers: they die when the first real file arrives. Mission accomplished. */
    private static final Pattern MARKER = Pattern.compile("(^|/)\\.(gitkeep|keep)$");

    public Soul examine(Deceased d) {
        if (MARKER.matcher(d.path()).find()) {
            return new Soul(d, CauseOfDeath.NATURAL, new Findings(0, 0, 0, 0, 0), null);
        }
        Findings findings = findings(d);
        return new Soul(d, cause(d, findings), findings, null);
    }

    Findings findings(Deceased d) {
        DeathCommit death = d.deathCommit();
        return new Findings(
                size(death),
                hour(death),
                message(death.message()),
                noTests(d),
                shortLife(life(d)));
    }

    CauseOfDeath cause(Deceased d, Findings findings) {
        if (findings.restlessness() >= VIOLENT_THRESHOLD) {
            return CauseOfDeath.VIOLENT;
        }
        if (life(d).compareTo(PREMATURE) < 0) {
            return CauseOfDeath.PREMATURE;
        }
        if (Duration.between(d.lastTouched(), d.died()).compareTo(OBLIVION) > 0) {
            return CauseOfDeath.FORGOTTEN;
        }
        return CauseOfDeath.NATURAL;
    }

    private static Duration life(Deceased d) {
        return Duration.between(d.born(), d.died());
    }

    /**
     * Logarithmic, reaching 1 at 50 files or 2,000 removed lines. Linear saturated too early:
     * 25-file cleanups are common and should not weigh the same as a mass grave of 200.
     */
    private static double size(DeathCommit death) {
        double byFiles = log(death.filesChanged(), MAX_FILES);
        double byLines = log(death.linesRemoved(), MAX_LINES);
        return Math.min(1, Math.max(byFiles, byLines));
    }

    private static double log(int value, int max) {
        return Math.log1p(value) / Math.log1p(max);
    }

    /** The small hours (0h–5h) weigh fully; late night (22h–0h) weighs half. */
    private static double hour(DeathCommit death) {
        int h = death.when().getHour();
        if (h < 5) {
            return 1;
        }
        return h >= 22 ? 0.5 : 0;
    }

    private static double noTests(Deceased d) {
        boolean code = CODE.matcher(d.path()).find();
        return code && !d.deathCommit().touchedTests() ? 1 : 0;
    }

    private static double message(String message) {
        if (message.isBlank()) {
            return 1;
        }
        return SUSPICIOUS_MESSAGE.matcher(message).find() ? 1 : 0;
    }

    /**
     * Logarithmic in hours: one hour of life weighs almost fully, one day weighs half, and it reaches zero at 30 days.
     * A fixed 7-day cutoff marked almost every deleted file as short-lived.
     */
    private static double shortLife(Duration life) {
        double hours = Math.max(0, life.toMinutes() / 60.0);
        double end = SHORT_LIFE_END.toHours();
        return Math.max(0, 1 - Math.log1p(hours) / Math.log1p(end));
    }
}
