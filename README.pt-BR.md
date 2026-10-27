<p align="center">
  <img src="docs/banner.svg" alt="Kuyō 供養: um funeral para o código que se foi" width="100%">
</p>

<p align="center">
  <a href="README.md">English</a> · <b>Português</b>
</p>

<p align="center">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-e76f00?style=flat-square&logo=openjdk&logoColor=white">
  <img alt="Quarkus" src="https://img.shields.io/badge/Quarkus-3.40-4695eb?style=flat-square&logo=quarkus&logoColor=white">
  <img alt="Gemma no Ollama" src="https://img.shields.io/badge/Gemma-no%20Ollama-6e56cf?style=flat-square&logo=ollama&logoColor=white">
  <img alt="Roda offline" src="https://img.shields.io/badge/roda-offline-2e8b57?style=flat-square">
  <img alt="Licença MIT" src="https://img.shields.io/badge/licen%C3%A7a-MIT-555?style=flat-square">
  <img alt="Hacktoberfest 2026" src="https://img.shields.io/badge/Hacktoberfest-2026-ff7a00?style=flat-square">
</p>

---

No Japão, *kuyō* (供養) é a cerimônia de memorial para objetos que serviram bem: agulhas, bonecas, pincéis, até óculos. Em vez de simplesmente ir para o lixo, eles recebem agradecimento e descanso.

**O Kuyō faz isso com código.** Ele lê o histórico git de um repositório, encontra cada arquivo que foi deletado e faz um funeral para cada um: como viveu, como morreu e quão inquieta ficou a alma. Um modelo open-weight local escreve os epitáfios, e o resultado é um cemitério numa única página HTML, onde as almas inquietas assombram os próprios túmulos.

> 🔒 **Seu código nunca sai da sua máquina.** Tudo roda offline, com um modelo que você controla.

Feito para o desafio DEV Hacktoberfest 2026, semana 4: *Ghost in the Machine*.

## 🪦 Comandos

| Comando | O que faz | Estado |
|---|---|---|
| `kuyo scan <repo>` | Lista os mortos e a causa mortis, sem LLM | ✅ funcionando |
| `kuyo bury <repo> [-o saida]` | Escreve os epitáfios com o Gemma e gera o cemitério | 🚧 em andamento |
| `kuyo seance <repo> <arquivo>` | Conversa com um arquivo morto | 💭 se der tempo |

## 🚀 Começando

**Requisitos**

- ☕ Java 25 (o repo fixa a versão com [mise](https://mise.jdx.dev): `mise install`)
- 🦙 Para o `bury`: [Ollama](https://ollama.com) com um modelo Gemma, por exemplo `ollama pull gemma4:e4b`

**Rodando**

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar scan caminho/do/repo
```

| Opção | Efeito |
|---|---|
| `--ordem inquietacao` | ordena pela inquietação em vez da ordem cronológica |
| `--laudo` | mostra cada fator por trás da inquietação |

```
✝ src/lib/rate-limit.ts  [VIOLENTA · 0.66]
    2026-06-05 02:14 → 2026-06-12 01:55 -03:00  (6 dias, 1 commits, 16 linhas)
    autores: mauricioandrade
    morto em 135a99b3 por mauricioandrade: "fix: limite de tamanho nos inputs de palpites e awards; remove rate-limit em memoria"
    commit: 3 arquivos, -18 linhas, sem testes
    laudo: tamanho 0.39 · hora 1.00 · mensagem 1.00 · sem testes 1.00 · vida curta 0.22
```

Apagado à 1h55 da manhã, num commit cuja mensagem diz `remove`, sem testes. Uma morte violenta.

## 🔍 Como uma morte é investigada

### ⛏️ Exumação

O Kuyō lê o histórico com o [JGit](https://www.eclipse.org/jgit/), em Java puro, sem chamar o binário `git`. Ele percorre o histórico do primeiro pai, do commit mais antigo ao mais novo:

- **Renomear ou mover não é morrer.** A vida do arquivo continua no caminho novo, com o mesmo limiar de 50% de semelhança que o próprio `git` usa.
- **Ressurreição é uma vida nova.** Um arquivo apagado e recriado depois no mesmo caminho morre duas vezes.
- **Um merge é um evento só.** A mesma deleção nunca é contada duas vezes, e arquivos que morreram em branches nunca mergeadas ficam em paz.
- **Lixo do sistema operacional não ganha funeral** (`*:Zone.Identifier`, `.DS_Store`, `Thumbs.db`).

### 👻 Inquietação

A inquietação é uma nota de 0 a 1, calculada de forma determinística em Java. O modelo só recebe o resultado, nunca decide.

| Fator | Peso | Regra |
|---|---|---|
| 📦 Tamanho do commit da morte | 0,30 | escala logarítmica, chega ao máximo com 50 arquivos ou 2.000 linhas removidas |
| 🌙 Hora da morte | 0,15 | da meia-noite às 5h no fuso de quem apagou; das 22h à meia-noite conta metade |
| 💬 Mensagem do commit | 0,20 | vazia, ou contém `remove`, `delete`, `wip`, `hotfix`, `cleanup`, `oops`, `apagar`, `excluir`… |
| 🧪 Sem testes | 0,15 | arquivo de código apagado num commit que não mexeu em nenhum teste |
| ⏳ Vida curta | 0,20 | escala logarítmica em horas: uma hora ≈ 0,9, um dia ≈ 0,5, zera aos 30 dias |

Os pesos foram calibrados com 175 mortes reais em nove repositórios meus. O primeiro rascunho usava escalas lineares, que saturavam quase na hora: na prática, arquivos morrem jovens e em valas comuns.

### ⚰️ Causa mortis

Verificada nesta ordem:

1. ⚔️ **VIOLENTA**: inquietação ≥ 0,6
2. 🥀 **PREMATURA**: viveu menos de um dia
3. 🕸️ **ESQUECIDA**: passou mais de um ano sem ser tocada antes de ser apagada
4. 🕯️ **NATURAL**: todo o resto

Um `.gitkeep` sempre morre de causas naturais. Ele existe para ser substituído por código de verdade, então cumpriu a missão.

## 🌱 Por que local e aberto

Código proprietário não pode ser enviado para uma API fechada. O Kuyō roda inteiro na sua máquina, não custa nada por execução, funciona offline, e o modelo é só uma configuração que você troca.

## 🧰 Stack

Java 25 · Quarkus (picocli) · JGit · LangChain4j + Ollama · Gemma · Qute

Todo o visual, incluindo o banner acima, é desenhado à mão em SVG e CSS. Nenhuma imagem baixada.

## 📜 Licença

[MIT](LICENSE)
