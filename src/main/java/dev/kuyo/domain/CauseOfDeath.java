package dev.kuyo.domain;

/** In order of precedence: a violent death is still violent if it was also brief. */
public enum CauseOfDeath {
    /** Died restless: a large commit, in the small hours, without tests, with a suspicious message. */
    VIOLENT,
    /** Lived less than a day. */
    PREMATURE,
    /** Went more than a year untouched before being deleted. */
    FORGOTTEN,
    NATURAL
}
