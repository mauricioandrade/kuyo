package dev.kuyo.cemiterio;

import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.CausaMortis;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.dominio.Laudo;
import dev.kuyo.dominio.Licao;
import io.quarkus.qute.TemplateData;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** O que o template precisa saber de cada alma, já formatado. */
@TemplateData
public record Tumulo(
        String id,
        String caminho,
        String nome,
        String causa,
        String causaRotulo,
        String causaKanji,
        String inquietacao,
        int chamas,
        boolean violenta,
        boolean esquecida,
        boolean prematura,
        String nascimento,
        String morte,
        String vida,
        String horaDaMorte,
        String ultimasPalavras,
        String hash,
        String autores,
        String epitafio,
        List<String> motivos,
        boolean autopsiado,
        List<String> licoes,
        int alturaPilar,
        double tamanhoNome,
        boolean duasColunas) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int PILAR_MIN = 96;
    private static final int PILAR_MAX = 188;

    static Tumulo de(Alma alma, int indice, Textos t) {
        Falecido f = alma.falecido();
        var morte = f.commitDaMorte();
        String caminho = f.caminho();
        String nome = caminho.substring(caminho.lastIndexOf('/') + 1);
        Duration vida = Duration.between(f.nascimento(), f.morte());
        String vidaTexto = t.vida(vida);
        String hora = HORA.format(morte.quando());
        int altura = alturaPilar(vida);
        double inquietacao = alma.inquietacao();
        ZoneId zona = morte.quando().getZone();

        return new Tumulo(
                "t" + indice,
                caminho,
                nome,
                alma.causa().name().toLowerCase(Locale.ROOT),
                t.causa(alma.causa()),
                kanji(alma.causa()),
                String.format(Locale.ROOT, "%.2f", inquietacao),
                inquietacao < 0.3 ? 1 : inquietacao < 0.6 ? 2 : 3,
                alma.causa() == CausaMortis.VIOLENTA,
                alma.causa() == CausaMortis.ESQUECIDA,
                alma.causa() == CausaMortis.PREMATURA,
                t.data(f.nascimento().atZone(zona).toLocalDate()),
                t.data(morte.quando().toLocalDate()),
                vidaTexto,
                hora,
                primeiraLinha(morte.mensagem()),
                morte.hash(),
                String.join(", ", f.autores()),
                alma.epitafio(),
                motivos(alma.laudo(), morte.arquivosAlterados() - 1, hora, vidaTexto, t),
                alma.autopsia() != null,
                alma.autopsia() == null ? List.of() : alma.autopsia().stream().map(l -> licao(l, t)).toList(),
                altura,
                tamanhoNome(nome, altura),
                tamanhoNome(nome, altura) < LEGIVEL);
    }

    /** 横死 morte violenta · 夭折 morte prematura · 無縁 sem ninguém que cuide do túmulo · 天寿 a vida que coube. */
    private static String kanji(CausaMortis causa) {
        return switch (causa) {
            case VIOLENTA -> "横死";
            case PREMATURA -> "夭折";
            case ESQUECIDA -> "無縁";
            case NATURAL -> "天寿";
        };
    }

    /** O pilar cresce com o log da vida: de minutos a um ano cabem entre 96 e 188 pixels. */
    static int alturaPilar(Duration vida) {
        double horas = Math.max(0, vida.toMinutes() / 60.0);
        double fracao = Math.min(1, Math.log1p(horas) / Math.log1p(365 * 24));
        return (int) Math.round(PILAR_MIN + fracao * (PILAR_MAX - PILAR_MIN));
    }

    /** Abaixo disso o entalhe fica ilegível: o nome passa a ocupar duas colunas do pilar. */
    static final double LEGIVEL = 11;

    /** O nome é entalhado na vertical; nomes longos encolhem e, se preciso, dividem-se em duas colunas. */
    static double tamanhoNome(String nome, int alturaPilar) {
        double espaco = alturaPilar - 40;
        double umaColuna = espaco / (nome.length() * 0.62);
        if (umaColuna >= LEGIVEL) {
            return arredondar(Math.min(17, umaColuna));
        }
        double duas = espaco / (Math.ceil(nome.length() / 2.0) * 0.62);
        return arredondar(Math.max(9, Math.min(LEGIVEL, duas)));
    }

    private static double arredondar(double tamanho) {
        return Math.round(tamanho * 10) / 10.0;
    }

    /** O laudo em palavras: só os fatores que de fato pesaram. */
    private static List<String> motivos(Laudo laudo, int companheiros, String hora, String vida, Textos t) {
        List<String> motivos = new ArrayList<>();
        if (laudo.tamanhoDoCommit() >= 0.5 && companheiros > 0) {
            motivos.add(t.companheiros(companheiros));
        }
        if (laudo.horaDaMorte() > 0) {
            motivos.add(t.hora(hora, laudo.horaDaMorte() >= 1));
        }
        if (laudo.mensagemSuspeita() > 0) {
            motivos.add(t.palavrasSuspeitas());
        }
        if (laudo.semTestes() > 0) {
            motivos.add(t.semTestes());
        }
        if (laudo.vidaCurta() >= 0.5) {
            motivos.add(t.vidaCurta(vida));
        }
        return motivos;
    }

    private static String licao(Licao l, Textos t) {
        return t.gravidade(l.gravidade()) + " · " + l.titulo() + (l.linha() > 0 ? " (" + t.linha(l.linha()) + ")" : "");
    }

    private static String primeiraLinha(String mensagem) {
        int quebra = mensagem.indexOf('\n');
        return quebra < 0 ? mensagem : mensagem.substring(0, quebra);
    }
}
