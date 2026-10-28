package dev.kuyo.cemetery;

import dev.kuyo.domain.CauseOfDeath;
import dev.kuyo.epitaph.Language;
import io.quarkus.qute.TemplateData;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** The page's texts in the language chosen with --lang. */
@TemplateData
public record Texts(
        String lang,
        String souls,
        String invitation,
        String invitationDetail,
        String lived,
        String diedAt,
        String lastWords,
        String authors,
        String restlessness,
        String whyRestless,
        String noEpitaph,
        String footer,
        String close,
        String washiLabel,
        String autopsy,
        String noLessons,
        String empty,
        String emptyDetail,
        String sound) {

    private static final Texts EN = new Texts("en",
            "souls laid to rest",
            "Approach a grave.",
            "Hover, focus or tap a grave to read how it lived and how it died.",
            "Lived", "Died at", "Last words", "Authors", "Restlessness", "Why it does not rest",
            "No epitaph was written. The monk was not called.",
            "Every file here served once. Kuyō, a memorial for objects that served well.",
            "Close", "Epitaph",
            "The Shishō's autopsy",
            "The Shishō examined the body and found no fault. It died clean.",
            "No file has died here yet.",
            "Every file in this repository is still alive. Come back after the next refactor.",
            "Sound");

    private static final Texts PT = new Texts("pt-BR",
            "almas sepultadas",
            "Aproxime-se de um túmulo.",
            "Passe o mouse, use o teclado ou toque num túmulo para ler como ele viveu e como morreu.",
            "Viveu", "Morreu às", "Últimas palavras", "Autores", "Inquietação", "Por que não descansa",
            "Nenhum epitáfio foi escrito. O monge não foi chamado.",
            "Todo arquivo aqui serviu um dia. Kuyō, o memorial para objetos que serviram bem.",
            "Fechar", "Epitáfio",
            "Autópsia do Shishō",
            "O Shishō examinou o corpo e não achou falha. Morreu limpo.",
            "Nenhum arquivo morreu aqui ainda.",
            "Todo arquivo deste repositório ainda está vivo. Volte depois da próxima refatoração.",
            "Som");

    public static Texts of(Language language) {
        return language == Language.pt ? PT : EN;
    }

    private Locale locale() {
        return Locale.forLanguageTag(lang);
    }

    private boolean pt() {
        return lang.startsWith("pt");
    }

    public String cause(CauseOfDeath cause) {
        boolean pt = pt();
        return switch (cause) {
            case VIOLENT -> pt ? "Violenta" : "Violent";
            case PREMATURE -> pt ? "Prematura" : "Premature";
            case FORGOTTEN -> pt ? "Esquecida" : "Forgotten";
            case NATURAL -> "Natural";
        };
    }

    public String life(Duration life) {
        boolean pt = pt();
        if (life.toDays() >= 1) {
            long d = life.toDays();
            return d + (pt ? (d == 1 ? " dia" : " dias") : (d == 1 ? " day" : " days"));
        }
        if (life.toHours() >= 1) {
            long h = life.toHours();
            return h + (pt ? (h == 1 ? " hora" : " horas") : (h == 1 ? " hour" : " hours"));
        }
        long m = Math.max(1, life.toMinutes());
        return m + (pt ? (m == 1 ? " minuto" : " minutos") : (m == 1 ? " minute" : " minutes"));
    }

    String month(YearMonth month) {
        String name = month.getMonth().getDisplayName(TextStyle.FULL, locale());
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + month.getYear();
    }

    String date(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(pt() ? "d MMM yyyy" : "MMM d, yyyy", locale()));
    }

    String companions(int n) {
        return pt() ? "enterrado com outros " + n + " arquivos" : "buried with " + n + " others";
    }

    String hour(String hour, boolean smallHours) {
        boolean pt = pt();
        if (smallHours) {
            return pt ? "morreu de madrugada, às " + hour : "died in the small hours, at " + hour;
        }
        return pt ? "morreu tarde da noite, às " + hour : "died late at night, at " + hour;
    }

    String massGrave(int souls, String lastWords) {
        String words = lastWords.length() > 72 ? lastWords.substring(0, 71) + "…" : lastWords;
        return pt()
                ? souls + " almas caíram juntas com «" + words + "»"
                : souls + " souls fell together to “" + words + "”";
    }

    String suspiciousWords() {
        return pt() ? "as últimas palavras confessam" : "its last words confess it";
    }

    String noTests() {
        return pt() ? "nenhum teste testemunhou" : "no test witnessed it";
    }

    String shortLife(String life) {
        return pt() ? "viveu só " + life : "lived only " + life;
    }

    /** The Shishō's severities, which it always writes in Portuguese. */
    String severity(String severity) {
        boolean pt = pt();
        return switch (severity) {
            case "GRAVE" -> pt ? "grave" : "severe";
            case "MEDIA" -> pt ? "média" : "medium";
            case "LEVE" -> pt ? "leve" : "minor";
            default -> pt ? "sugestão" : "suggestion";
        };
    }

    String line(int line) {
        return (pt() ? "linha " : "line ") + line;
    }
}
