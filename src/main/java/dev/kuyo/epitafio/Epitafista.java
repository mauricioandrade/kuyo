package dev.kuyo.epitafio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.CausaMortis;
import dev.kuyo.dominio.CommitDaMorte;
import dev.kuyo.dominio.Falecido;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pede os epitáfios ao Monge e guarda cada um num kuyo.json, para não regenerar a cada execução.
 * A chave inclui o commit da morte, a língua e o modelo: trocar qualquer um gera um epitáfio novo.
 */
@ApplicationScoped
public class Epitafista {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Inject
    Monge monge;

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "kuyo.modelo")
    String modelo;

    private Path arquivoCache;
    private Map<String, String> cache = new TreeMap<>();

    public void abrirCache(Path arquivo) throws IOException {
        arquivoCache = arquivo;
        cache = Files.exists(arquivo)
                ? json.readValue(arquivo.toFile(), new TypeReference<TreeMap<String, String>>() {})
                : new TreeMap<>();
    }

    public boolean emCache(Alma alma, Lingua lingua) {
        return cache.containsKey(chave(alma, lingua));
    }

    /** Usa o cache quando pode; grava a cada epitáfio novo para não perder nada se o processo cair. */
    public String epitafio(Alma alma, Lingua lingua) throws IOException {
        String chave = chave(alma, lingua);
        String guardado = cache.get(chave);
        if (guardado != null) {
            return guardado;
        }
        String novo = limpar(escrever(alma, lingua));
        cache.put(chave, novo);
        if (arquivoCache != null) {
            json.writerWithDefaultPrettyPrinter().writeValue(arquivoCache.toFile(), cache);
        }
        return novo;
    }

    private String chave(Alma alma, Lingua lingua) {
        Falecido f = alma.falecido();
        return f.caminho() + "@" + f.commitDaMorte().hash() + "#" + lingua + "#" + modelo;
    }

    private String escrever(Alma alma, Lingua lingua) {
        Falecido f = alma.falecido();
        CommitDaMorte morte = f.commitDaMorte();
        return monge.epitafio(
                lingua.nomeParaOModelo,
                f.caminho(),
                vida(Duration.between(f.nascimento(), f.morte())),
                DATA.format(f.nascimento()),
                DATA.format(f.morte()),
                String.join(", ", f.autores()),
                f.commitsEmVida(),
                causa(alma.causa()),
                morte.mensagem().isBlank() ? "(none)" : morte.mensagem(),
                HORA.format(morte.quando()),
                morte.arquivosAlterados() - 1);
    }

    /** Em horas quando não chegou a um dia: "3 hours" rende mais que "0 days". */
    static String vida(Duration vida) {
        if (vida.toDays() >= 1) {
            return vida.toDays() + (vida.toDays() == 1 ? " day" : " days");
        }
        if (vida.toHours() >= 1) {
            return vida.toHours() + (vida.toHours() == 1 ? " hour" : " hours");
        }
        long minutos = Math.max(1, vida.toMinutes());
        return minutos + (minutos == 1 ? " minute" : " minutes");
    }

    private static String causa(CausaMortis causa) {
        return switch (causa) {
            case VIOLENTA -> "violent";
            case PREMATURA -> "premature";
            case ESQUECIDA -> "forgotten";
            case NATURAL -> "natural causes";
        };
    }

    /** Modelos às vezes embrulham a resposta em aspas mesmo quando pedimos que não. */
    static String limpar(String texto) {
        String t = texto.strip();
        if (t.length() > 1 && (t.startsWith("\"") && t.endsWith("\"") || t.startsWith("“") && t.endsWith("”"))) {
            t = t.substring(1, t.length() - 1).strip();
        }
        return t;
    }
}
