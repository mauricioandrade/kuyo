package dev.kuyo.epitafio;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpitafistaTest {

    @Test
    void vidaCurtaViraHorasOuMinutos() {
        assertEquals("1 minute", Epitafista.vida(Duration.ofSeconds(20)));
        assertEquals("45 minutes", Epitafista.vida(Duration.ofMinutes(45)));
        assertEquals("1 hour", Epitafista.vida(Duration.ofMinutes(90)));
        assertEquals("13 hours", Epitafista.vida(Duration.ofHours(13)));
        assertEquals("1 day", Epitafista.vida(Duration.ofHours(30)));
        assertEquals("412 days", Epitafista.vida(Duration.ofDays(412)));
    }

    @Test
    void tiraAspasQueOModeloColocaPorConta() {
        assertEquals("Here lies A.java.", Epitafista.limpar("  \"Here lies A.java.\"\n"));
        assertEquals("Here lies A.java.", Epitafista.limpar("“Here lies A.java.”"));
        assertEquals("It said \"cleanup\".", Epitafista.limpar("It said \"cleanup\"."));
    }
}
