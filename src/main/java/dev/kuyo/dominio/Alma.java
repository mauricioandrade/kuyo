package dev.kuyo.dominio;

import java.util.List;

/** A autópsia é nula quando o Shishō não examinou o corpo; vazia quando examinou e não achou nada. */
public record Alma(
        Falecido falecido,
        CausaMortis causa,
        Laudo laudo,
        String epitafio,
        List<Licao> autopsia) {

    public Alma(Falecido falecido, CausaMortis causa, Laudo laudo, String epitafio) {
        this(falecido, causa, laudo, epitafio, null);
    }

    public double inquietacao() {
        return laudo.inquietacao();
    }

    public Alma comEpitafio(String epitafio) {
        return new Alma(falecido, causa, laudo, epitafio, autopsia);
    }

    public Alma comAutopsia(List<Licao> autopsia) {
        return new Alma(falecido, causa, laudo, epitafio, autopsia);
    }
}
