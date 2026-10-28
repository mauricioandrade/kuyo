package dev.kuyo.epitafio;

public enum Lingua {
    en("English"),
    pt("Brazilian Portuguese");

    public final String nomeParaOModelo;

    Lingua(String nomeParaOModelo) {
        this.nomeParaOModelo = nomeParaOModelo;
    }
}
