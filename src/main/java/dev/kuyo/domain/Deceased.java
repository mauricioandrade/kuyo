package dev.kuyo.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public record Deceased(
        String path,
        Instant born,
        Instant died,
        Instant lastTouched,
        Set<String> authors,
        int commitsInLife,
        int linesAtDeath,
        boolean binary,
        DeathCommit deathCommit,
        String lastContents) {

    public long daysLived() {
        return Duration.between(born, died).toDays();
    }
}
