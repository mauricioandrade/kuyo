package dev.kuyo.domain;

import java.util.List;

/** The autopsy is null when the Shishō never examined the body; empty when it did and found nothing. */
public record Soul(
        Deceased deceased,
        CauseOfDeath cause,
        Findings findings,
        String epitaph,
        List<Lesson> autopsy) {

    public Soul(Deceased deceased, CauseOfDeath cause, Findings findings, String epitaph) {
        this(deceased, cause, findings, epitaph, null);
    }

    public double restlessness() {
        return findings.restlessness();
    }

    public Soul withEpitaph(String epitaph) {
        return new Soul(deceased, cause, findings, epitaph, autopsy);
    }

    public Soul withAutopsy(List<Lesson> autopsy) {
        return new Soul(deceased, cause, findings, epitaph, autopsy);
    }
}
