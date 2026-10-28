package dev.kuyo.cemetery;

import dev.kuyo.domain.CauseOfDeath;
import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Findings;
import dev.kuyo.domain.Lesson;
import dev.kuyo.domain.Soul;
import io.quarkus.qute.TemplateData;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** What the template needs to know about each soul, already formatted. */
@TemplateData
public record Grave(
        String id,
        String path,
        String name,
        String cause,
        String causeLabel,
        String causeKanji,
        String restlessness,
        int flames,
        boolean violent,
        boolean forgotten,
        boolean premature,
        String born,
        String died,
        String life,
        String hourOfDeath,
        String lastWords,
        String hash,
        String authors,
        String epitaph,
        List<String> reasons,
        boolean autopsied,
        List<String> lessons,
        int pillarHeight,
        double nameSize,
        boolean twoColumns) {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final int PILLAR_MIN = 96;
    private static final int PILLAR_MAX = 188;

    static Grave of(Soul soul, int index, Texts t) {
        Deceased d = soul.deceased();
        var death = d.deathCommit();
        String path = d.path();
        String name = path.substring(path.lastIndexOf('/') + 1);
        Duration life = Duration.between(d.born(), d.died());
        String lifeText = t.life(life);
        String hour = HOUR.format(death.when());
        int height = pillarHeight(life);
        double restlessness = soul.restlessness();
        ZoneId zone = death.when().getZone();

        return new Grave(
                "t" + index,
                path,
                name,
                soul.cause().name().toLowerCase(Locale.ROOT),
                t.cause(soul.cause()),
                kanji(soul.cause()),
                String.format(Locale.ROOT, "%.2f", restlessness),
                restlessness < 0.3 ? 1 : restlessness < 0.6 ? 2 : 3,
                soul.cause() == CauseOfDeath.VIOLENT,
                soul.cause() == CauseOfDeath.FORGOTTEN,
                soul.cause() == CauseOfDeath.PREMATURE,
                t.date(d.born().atZone(zone).toLocalDate()),
                t.date(death.when().toLocalDate()),
                lifeText,
                hour,
                firstLine(death.message()),
                death.hash(),
                String.join(", ", d.authors()),
                soul.epitaph(),
                reasons(soul.findings(), death.filesChanged() - 1, hour, lifeText, t),
                soul.autopsy() != null,
                soul.autopsy() == null ? List.of() : soul.autopsy().stream().map(l -> lesson(l, t)).toList(),
                height,
                nameSize(name, height),
                nameSize(name, height) < LEGIBLE);
    }

    /** 横死 violent death · 夭折 premature death · 無縁 no one left to tend the grave · 天寿 the life one was given. */
    private static String kanji(CauseOfDeath cause) {
        return switch (cause) {
            case VIOLENT -> "横死";
            case PREMATURE -> "夭折";
            case FORGOTTEN -> "無縁";
            case NATURAL -> "天寿";
        };
    }

    /** The pillar grows with the log of the life: from minutes to a year fits between 96 and 188 pixels. */
    static int pillarHeight(Duration life) {
        double hours = Math.max(0, life.toMinutes() / 60.0);
        double fraction = Math.min(1, Math.log1p(hours) / Math.log1p(365 * 24));
        return (int) Math.round(PILLAR_MIN + fraction * (PILLAR_MAX - PILLAR_MIN));
    }

    /** Below this the carving becomes illegible: the name moves to two columns on the pillar. */
    static final double LEGIBLE = 11;

    /** The name is carved vertically; long names shrink and, if needed, split into two columns. */
    static double nameSize(String name, int pillarHeight) {
        double space = pillarHeight - 40;
        double oneColumn = space / (name.length() * 0.62);
        if (oneColumn >= LEGIBLE) {
            return round(Math.min(17, oneColumn));
        }
        double two = space / (Math.ceil(name.length() / 2.0) * 0.62);
        return round(Math.max(9, Math.min(LEGIBLE, two)));
    }

    private static double round(double size) {
        return Math.round(size * 10) / 10.0;
    }

    /** The findings in words: only the factors that actually weighed. */
    private static List<String> reasons(Findings findings, int companions, String hour, String life, Texts t) {
        List<String> reasons = new ArrayList<>();
        if (findings.commitSize() >= 0.5 && companions > 0) {
            reasons.add(t.companions(companions));
        }
        if (findings.hourOfDeath() > 0) {
            reasons.add(t.hour(hour, findings.hourOfDeath() >= 1));
        }
        if (findings.suspiciousMessage() > 0) {
            reasons.add(t.suspiciousWords());
        }
        if (findings.noTests() > 0) {
            reasons.add(t.noTests());
        }
        if (findings.shortLife() >= 0.5) {
            reasons.add(t.shortLife(life));
        }
        return reasons;
    }

    private static String lesson(Lesson l, Texts t) {
        return t.severity(l.severity()) + " · " + l.title() + (l.line() > 0 ? " (" + t.line(l.line()) + ")" : "");
    }

    private static String firstLine(String message) {
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
