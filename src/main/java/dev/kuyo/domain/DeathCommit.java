package dev.kuyo.domain;

import java.time.ZonedDateTime;

public record DeathCommit(
        String hash,
        String message,
        String author,
        // in the committer's time zone: 3 a.m. is 3 a.m. for the author, not for whoever runs kuyo
        ZonedDateTime when,
        int filesChanged,
        int linesRemoved,
        boolean touchedTests) {
}
