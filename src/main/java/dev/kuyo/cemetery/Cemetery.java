package dev.kuyo.cemetery;

import dev.kuyo.domain.CauseOfDeath;
import dev.kuyo.domain.Soul;
import io.quarkus.qute.TemplateData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Everything the template receives: the cemetery split into terraces, one per month of death. */
@TemplateData
public record Cemetery(
        String repo,
        int total,
        List<Census> census,
        List<Terrace> terraces,
        Texts texts,
        Map<String, String> fonts) {

    @TemplateData
    public record Census(String cause, String label, String kanji, int count) {
    }

    @TemplateData
    public record Terrace(String label, List<Grave> graves, String note) {

        Terrace(String label, List<Grave> graves, Texts texts) {
            this(label, graves, massGrave(graves, texts));
        }

        /** When a single commit took many of the terrace's souls, it deserves a line. */
        private static String massGrave(List<Grave> graves, Texts texts) {
            Map<String, List<Grave>> byCommit = new LinkedHashMap<>();
            graves.forEach(g -> byCommit.computeIfAbsent(g.hash(), h -> new ArrayList<>()).add(g));
            return byCommit.values().stream()
                    .filter(v -> v.size() >= MASS_GRAVE)
                    .max(Comparator.comparingInt(List::size))
                    .map(v -> texts.massGrave(v.size(), v.getFirst().lastWords()))
                    .orElse(null);
        }
    }

    /** From how many souls a single commit becomes a mass grave worth a note. */
    static final int MASS_GRAVE = 5;

    private static final String[] FONTS = {
            "shippori-400-latin", "shippori-400-latin-ext",
            "shippori-700-latin", "shippori-700-latin-ext", "shippori-700-kanji"};

    /** Above this a monthly terrace becomes a wall; the month is split by day of death. */
    static final int MAX_TERRACE = 24;

    public static Cemetery build(String repo, List<Soul> souls, Texts texts) {
        Map<YearMonth, Map<LocalDate, List<Grave>>> byMonth = new LinkedHashMap<>();
        Map<CauseOfDeath, Integer> count = new EnumMap<>(CauseOfDeath.class);
        int index = 0;
        for (Soul soul : souls) {
            LocalDate day = soul.deceased().deathCommit().when().toLocalDate();
            byMonth.computeIfAbsent(YearMonth.from(day), m -> new LinkedHashMap<>())
                    .computeIfAbsent(day, d -> new ArrayList<>())
                    .add(Grave.of(soul, index++, texts));
            count.merge(soul.cause(), 1, Integer::sum);
        }

        List<Terrace> terraces = new ArrayList<>();
        byMonth.forEach((month, byDay) -> {
            List<Grave> ofMonth = byDay.values().stream().flatMap(List::stream).toList();
            if (ofMonth.size() <= MAX_TERRACE) {
                terraces.add(new Terrace(texts.month(month), ofMonth, texts));
            } else {
                byDay.forEach((day, graves) -> terraces.add(new Terrace(texts.date(day), graves, texts)));
            }
        });
        List<Census> census = count.entrySet().stream()
                .map(e -> {
                    Grave example = terraces.stream().flatMap(t -> t.graves().stream())
                            .filter(g -> g.cause().equals(e.getKey().name().toLowerCase(Locale.ROOT)))
                            .findFirst().orElseThrow();
                    return new Census(example.cause(), example.causeLabel(), example.causeKanji(), e.getValue());
                })
                .toList();
        return new Cemetery(repo, souls.size(), census, terraces, texts, loadFonts());
    }

    /** The fonts are embedded in base64: the cemetery is a single file, with nothing coming from the network. */
    private static Map<String, String> loadFonts() {
        Map<String, String> fonts = new LinkedHashMap<>();
        for (String name : FONTS) {
            try (InputStream in = Cemetery.class.getResourceAsStream("/fonts/" + name + ".woff2")) {
                fonts.put(name.replace("shippori-", "f").replace("-", ""),
                        Base64.getEncoder().encodeToString(in.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return fonts;
    }
}
