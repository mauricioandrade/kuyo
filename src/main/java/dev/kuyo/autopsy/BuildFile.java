package dev.kuyo.autopsy;

/** The pom.xml or build.gradle closest to the deceased, as it was when it died. */
public record BuildFile(String folder, String name, String contents) {
}
