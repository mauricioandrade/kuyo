package dev.kuyo.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public record Falecido(
        String caminho,
        Instant nascimento,
        Instant morte,
        Instant ultimoCommitEmVida,
        Set<String> autores,
        int commitsEmVida,
        int linhasAoMorrer,
        boolean binario,
        CommitDaMorte commitDaMorte,
        String ultimoConteudo) {

    public long diasDeVida() {
        return Duration.between(nascimento, morte).toDays();
    }
}
