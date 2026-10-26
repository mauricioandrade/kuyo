package dev.kuyo.dominio;

import java.time.ZonedDateTime;

public record CommitDaMorte(
        String hash,
        String mensagem,
        String autor,
        // no fuso de quem fez o commit: 3h da manhã é 3h da manhã para o autor, não para quem roda o kuyo
        ZonedDateTime quando,
        int arquivosAlterados,
        int linhasRemovidas,
        boolean mexeuEmTestes) {
}
