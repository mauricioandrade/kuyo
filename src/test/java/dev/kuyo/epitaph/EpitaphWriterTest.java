package dev.kuyo.epitaph;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpitaphWriterTest {

    @Test
    void shortLifeBecomesHoursOrMinutes() {
        assertEquals("1 minute", EpitaphWriter.life(Duration.ofSeconds(20)));
        assertEquals("45 minutes", EpitaphWriter.life(Duration.ofMinutes(45)));
        assertEquals("1 hour", EpitaphWriter.life(Duration.ofMinutes(90)));
        assertEquals("13 hours", EpitaphWriter.life(Duration.ofHours(13)));
        assertEquals("1 day", EpitaphWriter.life(Duration.ofHours(30)));
        assertEquals("412 days", EpitaphWriter.life(Duration.ofDays(412)));
    }

    @Test
    void stripsQuotesTheModelAddsOnItsOwn() {
        assertEquals("Here lies A.java.", EpitaphWriter.clean("  \"Here lies A.java.\"\n"));
        assertEquals("Here lies A.java.", EpitaphWriter.clean("“Here lies A.java.”"));
        assertEquals("It said \"cleanup\".", EpitaphWriter.clean("It said \"cleanup\"."));
    }
}
