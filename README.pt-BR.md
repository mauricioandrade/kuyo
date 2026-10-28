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

<p align="center">
  <img src="docs/demo.gif" alt="Caminhando pelo cemitério: ao se aproximar de um túmulo violento, um yūrei surge enquanto o epitáfio é escrito na faixa de washi" width="100%">
</p>

<p align="center"><a href="https://mauricioandrade.github.io/kuyo/cemetery.html"><b>Caminhe pelo cemitério →</b></a></p>

> O cemitério do demo vem do **stocklentes-hub, um repositório privado meu**: 137 arquivos apagados em cinco meses. O código continua privado; aqui aparecem só nomes de arquivo, datas, mensagens de commit e os epitáfios que o Gemma escreveu para eles. É esse o ponto do Kuyō: o código nunca saiu da minha máquina para gerar esta página.

## 🪦 Comandos

| Comando | O que faz | Estado |
|---|---|---|
| `kuyo scan <repo>` | Lista os mortos e a causa mortis, sem LLM | ✅ funcionando |
| `kuyo bury <repo> [-o saida]` | Escreve os epitáfios com o Gemma e gera o cemitério | ✅ funcionando |
| `kuyo seance <repo> <arquivo>` | Conversa com um arquivo morto | ✅ funcionando |
| `kuyo haunt` | Possui o seu shell: arquivos apagados sussurram de volta | ✅ funcionando |

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
| `--order restlessness` | ordena pela inquietação em vez da ordem cronológica |
| `--findings` | mostra cada fator por trás da inquietação |

```
✝ src/lib/rate-limit.ts  [VIOLENT · 0.66]
    2026-06-05 02:14 → 2026-06-12 01:55 -03:00  (6 days, 1 commits, 16 lines)
    authors: mauricioandrade
    killed in 135a99b3 by mauricioandrade: "fix: limite de tamanho nos inputs de palpites e awards; remove rate-limit em memoria"
    commit: 3 files, -18 lines, without tests
    findings: size 0.39 · hour 1.00 · message 1.00 · no tests 1.00 · short life 0.22
```

Apagado à 1h55 da manhã, num commit cuja mensagem diz `remove`, sem testes. Uma morte violenta.

### 🕯️ Sepultando

```bash
java -jar target/quarkus-app/quarkus-run.jar bury caminho/do/repo -o cemetery.html --lang pt
```

O Gemma escreve um epitáfio por alma, cerca de um segundo cada na minha máquina, e o Kuyō gera um único `cemetery.html` autocontido: uma caminhada noturna por um cemitério japonês, um terraço por mês (ou por dia, quando um mês tem mortes demais). A altura do pilar é o tempo de vida do arquivo, as chamas são a inquietação, túmulos violentos ficam tortos e erguem um yūrei, os esquecidos criam musgo. Aproxime-se de um túmulo para ler o epitáfio.

<p align="center"><img src="docs/gifs/scared.gif" alt="Uma garota de anime gritando de pavor" width="320"></p>

> Aqui jaz src/lib/rate-limit.ts, que serviu por seis dias guardando os limites. Seu fim veio com a resignação: "Remove src/lib/rate-limit.ts, que nao tinha mais nenhum uso — o Map em memoria nao funciona em serverless".

| Opção | Efeito |
|---|---|
| `--lang en\|pt` | língua dos epitáfios (padrão `en`) |
| `--no-epitaphs` | gera o cemitério sem chamar o modelo |
| `--autopsy` | pede ao Shishō que examine cada arquivo Java morto (veja abaixo) |

Os epitáfios ficam em cache num `kuyo.json` ao lado da saída, pela chave commit da morte, língua e modelo, então rodar de novo é instantâneo. O modelo é configurado em tempo de execução, sem recompilar:

| Variável de ambiente | Padrão |
|---|---|
| `KUYO_MODEL` | `gemma4:e4b` |
| `KUYO_OLLAMA_URL` | `http://localhost:11434` |
| `KUYO_SHISHO` | `shisho` (um comando no `PATH` ou o caminho de um `shisho.jar`) |

### 師 Autópsia

Com `--autopsy`, cada arquivo Java morto vai para a mesa do [Shishō](https://github.com/mauricioandrade/shisho), o meu revisor de código Java. O Kuyō grava o último conteúdo do arquivo numa pasta temporária, ao lado do `pom.xml` ou `build.gradle` com que ele viveu, roda `shisho analyze` e entalha no túmulo só as lições encontradas no próprio morto:

> 師 **Autópsia do Shishō:** média · No centralized error handling (linha 21)

Quando o Shishō não acha nada, o washi diz: *morreu limpo*. Como a inquietação, a autópsia é determinística. Nenhum modelo participa.

### 🕯️ Séance

```bash
java -jar target/quarkus-app/quarkus-run.jar seance caminho/do/repo SpikeResource.java --lang pt
```

O fantasma do arquivo responde em primeira pessoa. Ele lembra do último conteúdo e dos fatos da vida e da morte, e de mais nada:

```
> Você tinha medo de ser apagado?
SpikeResource.java: Medo é um conceito muito... tangível, não é? Eu existi por apenas
nove horas, e o fim chegou rápido. Meu destino foi selado pelo commit `ff97d994`,
um ato de remoção bem direto.
```

<p align="center"><img src="docs/gifs/that-was-scary.gif" alt="That was scary!" width="245"></p>

Aceita o caminho completo ou só o nome do arquivo. `-q "pergunta"` pergunta sem abrir a conversa interativa, e `/bye` deixa o espírito descansar.

### 霊 Assombração

```bash
java -jar target/quarkus-app/quarkus-run.jar haunt --lang pt >> ~/.bashrc   # ou ~/.zshrc
```

Isso possui o seu shell. A partir daí, quando você apaga um arquivo rastreado, ele sussurra de volta um segundo depois, em primeira pessoa, com uma fala que o Gemma escreve a partir dos fatos da vida dele:

```
$ rm src/EmailService.java
$
  霊 Eu sou EmailService.java. Vivi 27 dias, em 2 commits, e ainda estou aqui, no HEAD. Por enquanto.
     ainda dá tempo: git restore -SW -- ':/src/EmailService.java'
```

Antes do commit, o arquivo é examinado como se morresse agora, com o seu comando como últimas palavras, então ele já sabe do que vai morrer. Depois do `git commit`, as almas que aquele commit levou falam uma vez, já mortas. Outras formas de perder trabalho ganham uma frase fixa: `git reset --hard`, `git clean -f`, `git push --force`, `git branch -D`, `git stash drop` e `git checkout -- .`. Todo o resto recebe silêncio.

O hook são poucas linhas de shell que você pode ler no seu arquivo rc. Ele só chama o Kuyō para comandos que podem matar alguma coisa, e roda em segundo plano para o prompt voltar na hora. `--no-monk` usa frases fixas em vez do modelo e `NO_COLOR` é respeitado.

### 🤖 GitHub Action

O Kuyō também é uma GitHub Action. Este workflow enterra o seu repositório a cada push na `main` e publica o cemitério no GitHub Pages (em *Settings → Pages → Source*, escolha *GitHub Actions*):

```yaml
name: Cemetery
on:
  push:
    branches: [main]
permissions:
  contents: read
  pages: write
  id-token: write
jobs:
  bury:
    runs-on: ubuntu-latest
    environment:
      name: github-pages
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: mauricioandrade/kuyo@main
        with:
          output: _site/index.html
      - uses: actions/upload-pages-artifact@v3
        with:
          path: _site
      - uses: actions/deploy-pages@v4
```

| Entrada | Padrão | Efeito |
|---|---|---|
| `path` | `.` | repositório a enterrar |
| `output` | `cemetery.html` | onde gravar o cemitério |
| `lang` | `en` | `en` ou `pt` |
| `model` | `gemma4:e4b` | modelo do Ollama para os epitáfios |
| `epitaphs` | `true` | `false` dispensa o modelo, e o Ollama nem é instalado |
| `autopsy` | `false` | `true` baixa o [Shishō](https://github.com/mauricioandrade/shisho) e examina cada arquivo Java morto |

Quando há alguém para enterrar, a action instala o Ollama no runner e baixa o modelo, então o código continua sem sair da infraestrutura que você escolheu. Os epitáfios ficam em cache entre execuções, e só as mortes novas chamam o modelo. O modelo roda na CPU do runner, então o primeiro enterro de um histórico grande demora.

O Kuyō se enterra assim: [o cemitério dele mesmo](https://mauricioandrade.github.io/kuyo/self/) é refeito a cada push. Ninguém morreu aqui ainda.

## 🔍 Como uma morte é investigada

### ⛏️ Exumação

O Kuyō lê o histórico com o [JGit](https://www.eclipse.org/jgit/), em Java puro, sem chamar o binário `git`. Ele percorre o histórico do primeiro pai, do commit mais antigo ao mais novo:

- **Renomear ou mover não é morrer.** A vida do arquivo continua no caminho novo, com o mesmo limiar de 50% de semelhança que o próprio `git` usa.
- **Ressurreição é uma vida nova.** Um arquivo apagado e recriado depois no mesmo caminho morre duas vezes.
- **Um merge é um evento só.** A mesma deleção nunca é contada duas vezes, e arquivos que morreram em branches nunca mergeadas ficam em paz.
- **Lixo do sistema operacional não ganha funeral** (`*:Zone.Identifier`, `.DS_Store`, `Thumbs.db`).

<p align="center"><img src="docs/gifs/are-you.gif" alt="Are you... (morto mesmo?)" width="320"></p>

### 👻 Inquietação

A inquietação é uma nota de 0 a 1, calculada de forma determinística em Java. O modelo só recebe o resultado, nunca decide: os fatos são do Java, as palavras são do Gemma.

| Fator | Peso | Regra |
|---|---|---|
| 📦 Tamanho do commit da morte | 0,30 | escala logarítmica, chega ao máximo com 50 arquivos ou 2.000 linhas removidas |
| 🌙 Hora da morte | 0,15 | da meia-noite às 5h no fuso de quem apagou; das 22h à meia-noite conta metade |
| 💬 Mensagem do commit | 0,20 | vazia, ou contém `remove`, `delete`, `wip`, `hotfix`, `cleanup`, `oops`, `apagar`, `excluir`… |
| 🧪 Sem testes | 0,15 | arquivo de código apagado num commit que não mexeu em nenhum teste |
| ⏳ Vida curta | 0,20 | escala logarítmica em horas: uma hora ≈ 0,9, um dia ≈ 0,5, zera aos 30 dias |

Os pesos foram calibrados com 175 mortes reais em nove repositórios meus. O primeiro rascunho usava escalas lineares, que saturavam quase na hora: na prática, arquivos morrem jovens e em valas comuns.

<p align="center"><img src="docs/gifs/pikachu.gif" alt="Pikachu, nada bem" width="300"></p>

### ⚰️ Causa mortis

Verificada nesta ordem:

1. ⚔️ **VIOLENT** (violenta): inquietação ≥ 0,6
2. 🥀 **PREMATURE** (prematura): viveu menos de um dia
3. 🕸️ **FORGOTTEN** (esquecida): passou mais de um ano sem ser tocada antes de ser apagada
4. 🕯️ **NATURAL**: todo o resto

<p align="center"><img src="docs/gifs/triggered.gif" alt="TRIGGERED" width="240"></p>

Um `.gitkeep` sempre morre de causas naturais. Ele existe para ser substituído por código de verdade, então cumpriu a missão.

## 🌱 Por que local e aberto

Código proprietário não pode ser enviado para uma API fechada. O Kuyō roda inteiro na sua máquina, não custa nada por execução, funciona offline, e o modelo é só uma configuração que você troca.

## 🧰 Stack

Java 25 · Quarkus (picocli) · JGit · LangChain4j + Ollama · Gemma · Qute

O cemitério e o banner são desenhados à mão em SVG e CSS, sem nenhuma imagem baixada. Os GIFs de reação deste README não são meus.

O cemitério usa a [Shippori Mincho B1](https://github.com/fontdasu/ShipporiMincho), dos Shippori Mincho Project Authors, sob a [SIL Open Font License](src/main/resources/fonts/OFL.txt), embutida em recortes pequenos para a página continuar um arquivo único e offline.

## 📜 Licença

[MIT](LICENSE)
