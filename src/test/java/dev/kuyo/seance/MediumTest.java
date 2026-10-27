package dev.kuyo.seance;

import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.CausaMortis;
import dev.kuyo.dominio.CommitDaMorte;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.dominio.Laudo;
import dev.kuyo.epitafio.Lingua;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediumTest {

    @Test
    void ofantasmaLembraDoConteudoEDasUltimasPalavras() {
        String invocacao = Medium.invocacao(alma("class EmailService { void enviar() {} }", false), Lingua.pt);

        assertTrue(invocacao.contains("class EmailService { void enviar() {} }"));
        assertTrue(invocacao.contains("\"chore: remover EmailService\""));
        assertTrue(invocacao.contains("Brazilian Portuguese"));
        assertTrue(invocacao.contains("you lived 11 days"));
    }

    @Test
    void arquivoBinarioNaoLembraDePalavras() {
        String invocacao = Medium.invocacao(alma(null, true), Lingua.en);

        assertTrue(invocacao.contains("You were a binary file"));
    }

    @Test
    void arquivoEnormeERecortado() {
        String enorme = "x".repeat(Medium.LEMBRANCA_MAXIMA + 500);
        String recorte = Medium.recortar(enorme);

        assertTrue(recorte.endsWith("(the rest has faded)"));
        assertFalse(recorte.length() > Medium.LEMBRANCA_MAXIMA + 40);
    }

    private static Alma alma(String conteudo, boolean binario) {
        ZonedDateTime quando = ZonedDateTime.of(2026, 6, 1, 17, 1, 0, 0, ZoneOffset.ofHours(-3));
        CommitDaMorte morte = new CommitDaMorte("acb09f24", "chore: remover EmailService", "Ana", quando, 2, 40, false);
        Falecido f = new Falecido("src/EmailService.java", quando.toInstant().minus(Duration.ofDays(11)),
                quando.toInstant(), quando.toInstant().minus(Duration.ofDays(3)), Set.of("Ana"), 4, 40, binario,
                morte, conteudo);
        return new Alma(f, CausaMortis.NATURAL, new Laudo(0, 0, 1, 1, 0), null);
    }
}
