package dev.kuyo.dominio;

/** Na ordem de precedência: uma morte violenta não deixa de ser violenta por ter sido breve. */
public enum CausaMortis {
    /** Morreu inquieta: commit grande, de madrugada, sem testes, mensagem suspeita. */
    VIOLENTA,
    /** Viveu menos de um dia. */
    PREMATURA,
    /** Passou mais de um ano sem ser tocada antes de ser apagada. */
    ESQUECIDA,
    NATURAL
}
