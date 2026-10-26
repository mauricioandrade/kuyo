package dev.kuyo.dominio;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Examina o falecido e decide quão inquieta a alma ficou e do que ela morreu.
 * Tudo determinístico: o modelo só recebe o resultado.
 */
@ApplicationScoped
public class Legista {

    static final double LIMIAR_VIOLENTA = 0.6;

    private static final int ARQUIVOS_MAXIMO = 50;
    private static final int LINHAS_MAXIMO = 2_000;
    private static final Duration PREMATURA = Duration.ofHours(24);
    private static final Duration VIDA_CURTA_FIM = Duration.ofDays(30);
    private static final Duration ESQUECIMENTO = Duration.ofDays(365);

    private static final Pattern MENSAGEM_SUSPEITA = Pattern.compile(
            "\\b(remov(e[ds]?|ing|al|er|endo|i|id[oa]s?)|remo[çc](ão|ao|ões|oes)"
                    + "|delet(e[ds]?|ing|ion|ar|ad[oa]s?|ei|ando)"
                    + "|apag(a|ar|ad[oa]s?|uei|ando)"
                    + "|exclu(i|ir|íd[oa]s?|id[oa]s?|são|sao|indo)"
                    + "|lixo|wip|hotfix|clean\\s?up|oops)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    /** "Sem testes" só pesa para código: ninguém apaga um .md ou um .png junto com testes. */
    private static final Pattern CODIGO = Pattern.compile(
            "\\.(java|kt|kts|scala|groovy|ts|tsx|js|jsx|mjs|cjs|vue|svelte|py|go|rs|rb|php|cs|fs"
                    + "|c|cc|cpp|h|hpp|swift|m|dart|ex|exs|clj|sql|sh)$",
            Pattern.CASE_INSENSITIVE);

    /** Marcadores de pasta vazia: morrem quando o primeiro arquivo de verdade chega. Missão cumprida. */
    private static final Pattern MARCADOR = Pattern.compile("(^|/)\\.(gitkeep|keep)$");

    public Alma examinar(Falecido f) {
        if (MARCADOR.matcher(f.caminho()).find()) {
            return new Alma(f, CausaMortis.NATURAL, new Laudo(0, 0, 0, 0, 0), null);
        }
        Laudo laudo = laudo(f);
        return new Alma(f, causa(f, laudo), laudo, null);
    }

    Laudo laudo(Falecido f) {
        CommitDaMorte morte = f.commitDaMorte();
        return new Laudo(
                tamanho(morte),
                hora(morte),
                mensagem(morte.mensagem()),
                semTestes(f),
                vidaCurta(vida(f)));
    }

    CausaMortis causa(Falecido f, Laudo laudo) {
        if (laudo.inquietacao() >= LIMIAR_VIOLENTA) {
            return CausaMortis.VIOLENTA;
        }
        if (vida(f).compareTo(PREMATURA) < 0) {
            return CausaMortis.PREMATURA;
        }
        if (Duration.between(f.ultimoCommitEmVida(), f.morte()).compareTo(ESQUECIMENTO) > 0) {
            return CausaMortis.ESQUECIDA;
        }
        return CausaMortis.NATURAL;
    }

    private static Duration vida(Falecido f) {
        return Duration.between(f.nascimento(), f.morte());
    }

    /**
     * Logarítmico, chega a 1 com 50 arquivos ou 2.000 linhas removidas. Linear saturava cedo demais:
     * limpezas de 25 arquivos são comuns e não deviam pesar igual a uma vala de 200.
     */
    private static double tamanho(CommitDaMorte morte) {
        double porArquivos = log(morte.arquivosAlterados(), ARQUIVOS_MAXIMO);
        double porLinhas = log(morte.linhasRemovidas(), LINHAS_MAXIMO);
        return Math.min(1, Math.max(porArquivos, porLinhas));
    }

    private static double log(int valor, int maximo) {
        return Math.log1p(valor) / Math.log1p(maximo);
    }

    /** Madrugada (0h–5h) pesa tudo; fim de noite (22h–0h) pesa metade. */
    private static double hora(CommitDaMorte morte) {
        int h = morte.quando().getHour();
        if (h < 5) {
            return 1;
        }
        return h >= 22 ? 0.5 : 0;
    }

    private static double semTestes(Falecido f) {
        boolean codigo = CODIGO.matcher(f.caminho()).find();
        return codigo && !f.commitDaMorte().mexeuEmTestes() ? 1 : 0;
    }

    private static double mensagem(String mensagem) {
        if (mensagem.isBlank()) {
            return 1;
        }
        return MENSAGEM_SUSPEITA.matcher(mensagem).find() ? 1 : 0;
    }

    /**
     * Logarítmico em horas: uma hora de vida pesa quase tudo, um dia pesa metade e zera aos 30 dias.
     * Um corte fixo de 7 dias marcava quase todo arquivo morto como vida curta.
     */
    private static double vidaCurta(Duration vida) {
        double horas = Math.max(0, vida.toMinutes() / 60.0);
        double fim = VIDA_CURTA_FIM.toHours();
        return Math.max(0, 1 - Math.log1p(horas) / Math.log1p(fim));
    }
}
