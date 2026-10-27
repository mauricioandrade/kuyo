package dev.kuyo.cemiterio;

import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.dominio.Laudo;
import io.quarkus.qute.TemplateData;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** O que o template precisa saber de cada alma, já formatado. */
@TemplateData
public record Tumulo(
        String caminho,
        String nome,
        String causa,
        double inquietacao,
        int chamas,
        String nascimento,
        String morte,
        String horaDaMorte,
        String ultimasPalavras,
        String hash,
        String autores,
        String epitafio,
        Laudo laudo) {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public static Tumulo de(Alma alma) {
        Falecido f = alma.falecido();
        var morte = f.commitDaMorte();
        String caminho = f.caminho();
        return new Tumulo(
                caminho,
                caminho.substring(caminho.lastIndexOf('/') + 1),
                alma.causa().name().toLowerCase(Locale.ROOT),
                alma.inquietacao(),
                alma.inquietacao() < 0.3 ? 1 : alma.inquietacao() < 0.6 ? 2 : 3,
                DATA.format(f.nascimento()),
                DATA.format(f.morte()),
                HORA.format(morte.quando()),
                primeiraLinha(morte.mensagem()),
                morte.hash(),
                String.join(", ", f.autores()),
                alma.epitafio(),
                alma.laudo());
    }

    public String inquietacaoFormatada() {
        return String.format(Locale.ROOT, "%.2f", inquietacao);
    }

    private static String primeiraLinha(String mensagem) {
        int quebra = mensagem.indexOf('\n');
        return quebra < 0 ? mensagem : mensagem.substring(0, quebra);
    }
}
