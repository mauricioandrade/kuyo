package dev.kuyo.epitaph;

public enum Language {
    en("English"),
    pt("Brazilian Portuguese");

    public final String nameForTheModel;

    Language(String nameForTheModel) {
        this.nameForTheModel = nameForTheModel;
    }
}
