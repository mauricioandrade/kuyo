package dev.kuyo.domain;

/**
 * Each restlessness factor, from 0 to 1, before the weights are applied.
 * Kept so the cemetery can explain why a soul does not rest.
 */
public record Findings(
        double commitSize,
        double hourOfDeath,
        double suspiciousMessage,
        double noTests,
        double shortLife) {

    public static final double WEIGHT_SIZE = 0.30;
    public static final double WEIGHT_HOUR = 0.15;
    public static final double WEIGHT_MESSAGE = 0.20;
    public static final double WEIGHT_NO_TESTS = 0.15;
    public static final double WEIGHT_SHORT_LIFE = 0.20;

    public double restlessness() {
        return WEIGHT_SIZE * commitSize
                + WEIGHT_HOUR * hourOfDeath
                + WEIGHT_MESSAGE * suspiciousMessage
                + WEIGHT_NO_TESTS * noTests
                + WEIGHT_SHORT_LIFE * shortLife;
    }
}
