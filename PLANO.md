# Kuyō (供養) — um funeral para o código que se foi

> Hacktoberfest 2026 · DEV Challenge Semana 4 · tema **Ghost in the Machine**
> Janela: 26 a 31 de outubro de 2026 (entrega até 23h59 PDT do dia 31)

## Conceito

No Japão, *kuyō* é a cerimônia de memorial para objetos que serviram bem: agulhas, bonecas, pincéis. O Kuyō faz isso com código. Ele lê o histórico git de um repositório, encontra os arquivos que foram deletados e realiza um funeral para cada um: calcula como viveram e como morreram, pede a um modelo open-weight local que escreva o epitáfio e gera um cemitério em HTML onde a inquietação de cada alma vira assombração animada.

O código nunca sai da máquina. Esse é o argumento central do post: código proprietário não pode ser enviado a uma API fechada, e o Kuyō funciona offline, de graça, com um modelo que você controla.

## Stack

| Parte | Escolha |
|---|---|
| Linguagem | Java 25 |
| Framework | Quarkus (modo CLI com a extensão picocli) |
| Leitura do git | JGit (Java puro, sem chamar o binário `git`) |
| LLM | Gemma rodando no Ollama, via `quarkus-langchain4j-ollama` |
| Template | Qute |
| Saída | Um único `cemiterio.html` autocontido (CSS, SVG e JS inline) |

## Comandos

```
kuyo scan  <repo>              # lista os mortos e a causa mortis, sem LLM
kuyo bury  <repo> [-o saida]   # escreve epitáfios e gera o cemitério HTML
kuyo seance <repo> <arquivo>   # conversa com um arquivo morto (bônus)
```

## Modelo de domínio

```java
record Falecido(
    String caminho,
    Instant nascimento,        // primeiro commit que criou o arquivo
    Instant morte,             // commit que deletou
    Set<String> autores,
    int commitsEmVida,
    int linhasAoMorrer,
    CommitDaMorte commitDaMorte,
    String ultimoConteudo      // usado na séance
) {}

record CommitDaMorte(String hash, String mensagem, String autor,
                     Instant quando, int arquivosAlterados,
                     int linhasRemovidas, boolean mexeuEmTestes) {}

enum CausaMortis { NATURAL, VIOLENTA, PREMATURA, ESQUECIDA }

record Alma(Falecido falecido, CausaMortis causa,
            double inquietacao, String epitafio) {}
```

## Fórmula de inquietação (rascunho)

Valor entre 0 e 1, calculado em Java (determinístico). O modelo só recebe o resultado.

| Fator | Peso | Regra |
|---|---|---|
| Tamanho do commit da morte | 0,30 | mais de 20 arquivos ou 1.000 linhas removidas → máximo |
| Hora da morte | 0,15 | entre 0h e 5h → máximo |
| Mensagem do commit | 0,20 | contém `remove`, `delete`, `lixo`, `wip`, `hotfix`, `cleanup`, `oops` ou está vazia |
| Sem testes no commit | 0,15 | commit da morte não tocou em nenhum arquivo de teste |
| Vida curta | 0,20 | menos de 7 dias de vida → máximo |

Causa mortis derivada:

- **PREMATURA**: viveu menos de 7 dias
- **VIOLENTA**: inquietação ≥ 0,6
- **ESQUECIDA**: mais de 1 ano sem nenhum commit antes de ser deletado
- **NATURAL**: o resto

Calibrar os pesos rodando em repositórios reais (Clareza, Kokusen, WIRED).

## Assombrações (inquietação → visual)

| Inquietação | Hitodama | Movimento | Extras |
|---|---|---|---|
| 0,0 – 0,3 | 1 chama azul | flutua devagar | fumaça de incenso calma |
| 0,3 – 0,6 | 2 chamas | flutua mais rápido | névoa mais densa |
| 0,6 – 1,0 | 3 chamas violeta | tremor (`jitter`) | yūrei aparece no hover |

Efeitos planejados, todos CSS keyframes e SVG inline:

- [ ] Hitodama (人魂): chama em `border-radius` + `@keyframes float` e `jitter`
- [ ] Névoa: elipses translúcidas com `drift` lateral
- [ ] Incenso (*senkō*): `path` SVG com `stroke-dashoffset` animado subindo da lápide
- [ ] Yūrei: fantasma em SVG desenhado à mão, surge com fade nos túmulos violentos
- [ ] Lanternas de papel (*chōchin*) no fundo, piscando de leve
- [ ] Epitáfio aparece com efeito de escrita ao passar o mouse ou tocar
- [ ] `prefers-reduced-motion` desliga todas as animações
- [ ] Opcional: pixel art própria em sprite sheet, animada com `steps()`

Regra de ouro: nenhuma imagem baixada da internet. Tudo desenhado por nós, para o HTML seguir autocontido e sem risco de direitos autorais.

## Prompt do epitáfio (rascunho)

Temperatura baixa e saída curta. Epitáfios ficam em cache num `kuyo.json` para não regenerar a cada execução.

```
You are a Buddhist monk performing kuyō, a memorial for objects that served well.
Write an epitaph for a source file that was deleted. Max 3 sentences.
Tone: solemn with dry humor. Never invent facts beyond the data below.

File: {caminho}
Lived: {dias} days ({nascimento} – {morte})
Authors: {autores}
Commits in life: {commitsEmVida}
Cause of death: {causa}
Last words (commit message): "{mensagem}"
Died at: {hora}
```

Idioma do epitáfio configurável (`--lang en|pt`); padrão em inglês por causa da submissão.

## Cronograma

| Dia | Entrega |
|---|---|
| Seg 26 | Repo criado, projeto Quarkus, JGit listando arquivos deletados |
| Ter 27 | Domínio completo, fórmula de inquietação, comando `scan` |
| Qua 28 | Integração com Gemma via Ollama, cache de epitáfios |
| Qui 29 | Template Qute do cemitério e animações |
| Sex 30 | Polimento visual, rodar nos repos antigos, gravar demo |
| Sáb 31 | Post no DEV e submissão (séance só se sobrar tempo) |

Ajustar conforme a escala de turnos da semana.

## Post (inglês)

Seções do template oficial, com o gancho de cada uma:

1. **What I Built**: abrir com a tradição do kuyō e o primeiro epitáfio de verdade, de uma classe que eu mesmo apaguei
2. **Demo**: GIF ou vídeo curto do cemitério (gravado da própria tela) + link do HTML publicado
3. **Code**: embed do repo
4. **How I Built It**: JGit → fórmula de inquietação → Gemma no Ollama → Qute; por que o cálculo é Java e o texto é do modelo
5. **Why Does Open Innovation Matter?**: código proprietário não sai da máquina, roda offline, custo zero, modelo trocável
6. **The Spirit of It**: Finados no Brasil cai no mesmo 2 de novembro; honrar o código que construiu os projetos de hoje
7. **Prize Categories**: Best Use of Gemma (e GitHub Copilot se a Action de enterro automático sair)

## Bônus, se der tempo

- `kuyo seance`: chat com o arquivo morto usando o último conteúdo e os commits como contexto
- GitHub Action que roda o Kuyō a cada merge e publica o cemitério no GitHub Pages
- Shishō opinando sobre a causa da morte (overengineering, código morto)

## Lembretes das regras

- Repo e código precisam ser criados dentro da janela do desafio
- Commits depois do prazo precisam ser anotados no README
- Créditos de qualquer código de terceiros no post
- Post em inglês para concorrer a prêmios
