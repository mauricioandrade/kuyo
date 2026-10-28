package dev.kuyo.cemetery;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraveTest {

    @Test
    void pillarGrowsWithTheLogOfLife() {
        assertEquals(96, Grave.pillarHeight(Duration.ZERO));
        assertEquals(188, Grave.pillarHeight(Duration.ofDays(365)));
        assertEquals(188, Grave.pillarHeight(Duration.ofDays(4000)));
        assertTrue(Grave.pillarHeight(Duration.ofDays(1)) < Grave.pillarHeight(Duration.ofDays(30)));
    }

    @Test
    void shortNameFitsOneColumnAndLongNameSplitsIntoTwo() {
        assertEquals(17, Grave.nameSize("App.css", 140));
        double longName = Grave.nameSize("ArmacaoTratamentosStepPlaceholder.tsx", 110);
        assertTrue(longName >= 9 && longName <= Grave.LEGIBLE, "size " + longName);
    }
}
