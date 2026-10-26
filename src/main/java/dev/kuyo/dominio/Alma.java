package dev.kuyo.dominio;

public record Alma(
        Falecido falecido,
        CausaMortis causa,
        Laudo laudo,
        String epitafio) {

    public double inquietacao() {
        return laudo.inquietacao();
    }

    public Alma comEpitafio(String epitafio) {
        return new Alma(falecido, causa, laudo, epitafio);
    }
}
