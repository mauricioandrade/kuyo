package dev.kuyo.cemiterio;

import dev.kuyo.dominio.CausaMortis;
import dev.kuyo.epitafio.Lingua;
import io.quarkus.qute.TemplateData;

import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Os textos da página na língua escolhida com --lang. */
@TemplateData
public record Textos(
        String lang,
        String almas,
        String convite,
        String conviteDetalhe,
        String viveu,
        String morreuAs,
        String ultimasPalavras,
        String autores,
        String inquietacao,
        String porQue,
        String semEpitafio,
        String rodape,
        String fechar,
        String rotuloWashi,
        String autopsia,
        String semLicoes,
        String vazio,
        String vazioDetalhe,
        String som) {

    private static final Textos EN = new Textos("en",
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

    private static final Textos PT = new Textos("pt-BR",
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

    public static Textos de(Lingua lingua) {
        return lingua == Lingua.pt ? PT : EN;
    }

    private Locale locale() {
        return Locale.forLanguageTag(lang);
    }

    String causa(CausaMortis causa) {
        boolean pt = lang.startsWith("pt");
        return switch (causa) {
            case VIOLENTA -> pt ? "Violenta" : "Violent";
            case PREMATURA -> pt ? "Prematura" : "Premature";
            case ESQUECIDA -> pt ? "Esquecida" : "Forgotten";
            case NATURAL -> pt ? "Natural" : "Natural";
        };
    }

    String vida(Duration vida) {
        boolean pt = lang.startsWith("pt");
        if (vida.toDays() >= 1) {
            long d = vida.toDays();
            return d + (pt ? (d == 1 ? " dia" : " dias") : (d == 1 ? " day" : " days"));
        }
        if (vida.toHours() >= 1) {
            long h = vida.toHours();
            return h + (pt ? (h == 1 ? " hora" : " horas") : (h == 1 ? " hour" : " hours"));
        }
        long m = Math.max(1, vida.toMinutes());
        return m + (pt ? (m == 1 ? " minuto" : " minutos") : (m == 1 ? " minute" : " minutes"));
    }

    String mes(YearMonth mes) {
        String nome = mes.getMonth().getDisplayName(TextStyle.FULL, locale());
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " " + mes.getYear();
    }

    String data(java.time.LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern(lang.startsWith("pt") ? "d MMM yyyy" : "MMM d, yyyy", locale()));
    }

    String companheiros(int n) {
        boolean pt = lang.startsWith("pt");
        return pt ? "enterrado com outros " + n + " arquivos" : "buried with " + n + " others";
    }

    String hora(String hora, boolean madrugada) {
        boolean pt = lang.startsWith("pt");
        if (madrugada) {
            return pt ? "morreu de madrugada, às " + hora : "died in the small hours, at " + hora;
        }
        return pt ? "morreu tarde da noite, às " + hora : "died late at night, at " + hora;
    }

    String vala(int almas, String ultimasPalavras) {
        String palavras = ultimasPalavras.length() > 72 ? ultimasPalavras.substring(0, 71) + "…" : ultimasPalavras;
        return lang.startsWith("pt")
                ? almas + " almas caíram juntas com «" + palavras + "»"
                : almas + " souls fell together to “" + palavras + "”";
    }

    String palavrasSuspeitas() {
        return lang.startsWith("pt") ? "as últimas palavras confessam" : "its last words confess it";
    }

    String semTestes() {
        return lang.startsWith("pt") ? "nenhum teste testemunhou" : "no test witnessed it";
    }

    String vidaCurta(String vida) {
        return lang.startsWith("pt") ? "viveu só " + vida : "lived only " + vida;
    }

    /** As gravidades do Shishō, que ele escreve sempre em português. */
    String gravidade(String gravidade) {
        boolean pt = lang.startsWith("pt");
        return switch (gravidade) {
            case "GRAVE" -> pt ? "grave" : "severe";
            case "MEDIA" -> pt ? "média" : "medium";
            case "LEVE" -> pt ? "leve" : "minor";
            default -> pt ? "sugestão" : "suggestion";
        };
    }

    String linha(int linha) {
        return (lang.startsWith("pt") ? "linha " : "line ") + linha;
    }
}
