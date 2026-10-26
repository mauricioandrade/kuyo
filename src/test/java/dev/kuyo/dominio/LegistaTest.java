package dev.kuyo.dominio;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegistaTest {

    private static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);
    private final Legista legista = new Legista();

    @Test
    void morteTranquilaENatural() {
        Alma alma = legista.examinar(falecido("A.java", Duration.ofDays(200), Duration.ofDays(10),
                morte("refactor: troca por KataGuide", 14, 2, 30, true)));

        assertEquals(CausaMortis.NATURAL, alma.causa());
        assertEquals(0.30 * Math.log1p(30) / Math.log1p(2000), alma.inquietacao(), 1e-9);
    }

    @Test
    void menosDeUmDiaEPrematura() {
        Alma alma = legista.examinar(falecido("A.java", Duration.ofHours(5), Duration.ZERO,
                morte("feat: troca componente", 14, 2, 10, true)));

        assertEquals(CausaMortis.PREMATURA, alma.causa());
    }

    @Test
    void violentaVenceAPrematura() {
        Alma alma = legista.examinar(falecido("A.java", Duration.ofHours(2), Duration.ZERO,
                morte("wip", 3, 60, 5_000, false)));

        assertEquals(CausaMortis.VIOLENTA, alma.causa());
        assertEquals(0.80 + 0.20 * (1 - Math.log1p(2) / Math.log1p(720)), alma.inquietacao(), 1e-9);
    }

    @Test
    void valaComumDeMadrugadaSemTestesEViolenta() {
        Alma alma = legista.examinar(falecido("A.java", Duration.ofDays(400), Duration.ofDays(1),
                morte("cleanup", 2, 50, 0, false)));

        assertEquals(CausaMortis.VIOLENTA, alma.causa());
        assertEquals(0.30 + 0.15 + 0.20 + 0.15, alma.inquietacao(), 1e-9);
    }

    @Test
    void maisDeUmAnoParadoEEsquecido() {
        Alma alma = legista.examinar(falecido("A.java", Duration.ofDays(900), Duration.ofDays(500),
                morte("chore: tira modulo antigo", 15, 1, 10, true)));

        assertEquals(CausaMortis.ESQUECIDA, alma.causa());
    }

    @Test
    void gitkeepCumpriuAMissao() {
        Alma alma = legista.examinar(falecido("src/domain/.gitkeep", Duration.ofMinutes(10), Duration.ZERO,
                morte("wip", 3, 60, 5_000, false)));

        assertEquals(CausaMortis.NATURAL, alma.causa());
        assertEquals(0.0, alma.inquietacao());
    }

    @Test
    void horaDaMorteUsaOFusoDoAutor() {
        // 3h em Brasília são 6h em UTC: continua sendo madrugada para quem apagou
        CommitDaMorte morte = morte("feat: x", 3, 1, 1, true);
        assertEquals(1.0, legista.laudo(falecido("A.java", Duration.ofDays(100), Duration.ZERO, morte)).horaDaMorte());
    }

    @Test
    void vidaCurtaCaiEmEscalaLogaritmica() {
        CommitDaMorte morte = morte("feat: x", 14, 1, 1, true);
        assertEquals(0.0, vidaCurta(Duration.ofDays(30), morte), 1e-9);
        assertEquals(0.0, vidaCurta(Duration.ofDays(400), morte), 1e-9);
        assertEquals(1 - Math.log1p(24) / Math.log1p(720), vidaCurta(Duration.ofDays(1), morte), 1e-9);
    }

    @Test
    void semTestesSoPesaParaCodigo() {
        CommitDaMorte morte = morte("feat: x", 14, 1, 1, false);
        assertEquals(1.0, legista.laudo(falecido("A.java", Duration.ofDays(100), Duration.ZERO, morte)).semTestes());
        assertEquals(0.0, legista.laudo(falecido("PLANO.md", Duration.ofDays(100), Duration.ZERO, morte)).semTestes());
    }

    @Test
    void mensagemVaziaOuEmPortuguesESuspeita() {
        assertEquals(1.0, mensagem(""));
        assertEquals(1.0, mensagem("Remoção de telas antigas"));
        assertEquals(1.0, mensagem("chore: apagar specs"));
        assertEquals(0.0, mensagem("refactor: removable flag"));
    }

    private double vidaCurta(Duration vida, CommitDaMorte morte) {
        return legista.laudo(falecido("A.java", vida, Duration.ZERO, morte)).vidaCurta();
    }

    private double mensagem(String mensagem) {
        return legista.laudo(falecido("A.java", Duration.ofDays(100), Duration.ZERO, morte(mensagem, 14, 1, 1, true)))
                .mensagemSuspeita();
    }

    private static CommitDaMorte morte(String mensagem, int hora, int arquivos, int linhas, boolean testes) {
        ZonedDateTime quando = ZonedDateTime.of(2026, 10, 1, hora, 0, 0, 0, BRASILIA);
        return new CommitDaMorte("abc12345", mensagem, "Ana", quando, arquivos, linhas, testes);
    }

    private static Falecido falecido(String caminho, Duration vida, Duration paradoAntesDeMorrer,
                                     CommitDaMorte morte) {
        Instant fim = morte.quando().toInstant();
        return new Falecido(caminho, fim.minus(vida), fim, fim.minus(paradoAntesDeMorrer), Set.of("Ana"),
                3, 10, false, morte, "class A {}");
    }
}
