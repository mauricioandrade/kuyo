package dev.kuyo.cemiterio;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TumuloTest {

    @Test
    void pilarCresceComOLogDaVida() {
        assertEquals(96, Tumulo.alturaPilar(Duration.ZERO));
        assertEquals(188, Tumulo.alturaPilar(Duration.ofDays(365)));
        assertEquals(188, Tumulo.alturaPilar(Duration.ofDays(4000)));
        assertTrue(Tumulo.alturaPilar(Duration.ofDays(1)) < Tumulo.alturaPilar(Duration.ofDays(30)));
    }

    @Test
    void nomeCurtoCabeNumaColunaENomeLongoDivideEmDuas() {
        assertEquals(17, Tumulo.tamanhoNome("App.css", 140));
        double longo = Tumulo.tamanhoNome("ArmacaoTratamentosStepPlaceholder.tsx", 110);
        assertTrue(longo >= 9 && longo <= Tumulo.LEGIVEL, "tamanho " + longo);
    }
}
