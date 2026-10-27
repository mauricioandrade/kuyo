package dev.kuyo.autopsia;

/** O pom.xml ou build.gradle mais próximo do morto, como estava quando ele morreu. */
public record ArquivoDeBuild(String pasta, String nome, String conteudo) {
}
