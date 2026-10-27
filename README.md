<p align="center">
  <img src="docs/banner.svg" alt="Kuyō 供養: a funeral for the code that's gone" width="100%">
</p>

<p align="center">
  <b>English</b> · <a href="README.pt-BR.md">Português</a>
</p>

<p align="center">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-e76f00?style=flat-square&logo=openjdk&logoColor=white">
  <img alt="Quarkus" src="https://img.shields.io/badge/Quarkus-3.40-4695eb?style=flat-square&logo=quarkus&logoColor=white">
  <img alt="Gemma on Ollama" src="https://img.shields.io/badge/Gemma-on%20Ollama-6e56cf?style=flat-square&logo=ollama&logoColor=white">
  <img alt="Runs offline" src="https://img.shields.io/badge/runs-offline-2e8b57?style=flat-square">
  <img alt="MIT license" src="https://img.shields.io/badge/license-MIT-555?style=flat-square">
  <img alt="Hacktoberfest 2026" src="https://img.shields.io/badge/Hacktoberfest-2026-ff7a00?style=flat-square">
</p>

---

In Japan, *kuyō* (供養) is a memorial service for objects that served well: needles, dolls, brushes, even eyeglasses. They are thanked and laid to rest instead of just thrown away.

**Kuyō does the same for source code.** It reads a repository's git history, finds every file that was deleted, and holds a funeral for each one: how it lived, how it died, and how restless its soul is. A local open-weight model writes the epitaphs, and the result is a cemetery in a single HTML page, where restless souls haunt their graves.

> 🔒 **Your code never leaves your machine.** Everything runs offline, with a model you control.

Built for the DEV Hacktoberfest 2026 challenge, week 4: *Ghost in the Machine*.

## 🪦 Commands

| Command | What it does | State |
|---|---|---|
| `kuyo scan <repo>` | Lists the dead and their cause of death, no LLM | ✅ working |
| `kuyo bury <repo> [-o out]` | Writes epitaphs with Gemma and renders the cemetery | ✅ working |
| `kuyo seance <repo> <file>` | Talk to a dead file | 💭 if time allows |

## 🚀 Getting started

**Requirements**

- ☕ Java 25 (the repo pins it with [mise](https://mise.jdx.dev): `mise install`)
- 🦙 For `bury`: [Ollama](https://ollama.com) with a Gemma model, e.g. `ollama pull gemma4:e4b`

**Run**

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar scan path/to/repo
```

| Option | Effect |
|---|---|
| `--ordem inquietacao` | sort by restlessness instead of chronologically |
| `--laudo` | print every factor behind the restlessness score |

```
✝ src/lib/rate-limit.ts  [VIOLENTA · 0.66]
    2026-06-05 02:14 → 2026-06-12 01:55 -03:00  (6 dias, 1 commits, 16 linhas)
    autores: mauricioandrade
    morto em 135a99b3 por mauricioandrade: "fix: limite de tamanho nos inputs de palpites e awards; remove rate-limit em memoria"
    commit: 3 arquivos, -18 linhas, sem testes
    laudo: tamanho 0.39 · hora 1.00 · mensagem 1.00 · sem testes 1.00 · vida curta 0.22
```

Deleted at 1:55 a.m., in a commit whose message says `remove`, with no tests. A violent death.

The CLI output is in Portuguese for now.

### 🕯️ Burying

```bash
java -jar target/quarkus-app/quarkus-run.jar bury path/to/repo -o cemiterio.html
```

Gemma writes one epitaph per soul, about a second each on a laptop GPU, and Kuyō renders a single self-contained `cemiterio.html`: a night walk through a Japanese cemetery, one terrace per month (or per day, when a month holds too many deaths). Pillar height is the file's lifespan, the flames are its restlessness, violent graves lean and raise a yūrei, forgotten ones grow moss. Approach a grave to read its epitaph.

> Here rests SpikeResource.java, a brief experiment that lasted but nine hours. It met its end with the decree, "chore(web): remove phase 2 spike endpoint." May its ephemeral existence find quiet in the void.

| Option | Effect |
|---|---|
| `--lang en\|pt` | language of the epitaphs (default `en`) |
| `--sem-epitafios` | render the cemetery without calling the model |

Epitaphs are cached in a `kuyo.json` next to the output, keyed by death commit, language and model, so reruns are instant. The model is configured at runtime, no rebuild needed:

| Environment variable | Default |
|---|---|
| `KUYO_MODELO` | `gemma4:e4b` |
| `KUYO_OLLAMA_URL` | `http://localhost:11434` |

## 🔍 How a death is investigated

### ⛏️ Exhumation

Kuyō reads history with [JGit](https://www.eclipse.org/jgit/), in pure Java, without calling the `git` binary. It walks the first-parent history from the oldest commit to the newest:

- **Renames and moves are not deaths.** The file's life continues under its new path, using the same 50% similarity threshold as `git` itself.
- **Resurrections count as new lives.** A file deleted and later recreated at the same path dies twice.
- **A merge is one event.** The same deletion is never counted twice, and files that died on branches never merged are left alone.
- **OS junk gets no funeral** (`*:Zone.Identifier`, `.DS_Store`, `Thumbs.db`).

### 👻 Restlessness

Restlessness is a score from 0 to 1, computed deterministically in Java. The model only ever receives the result, never decides it: the facts are Java's, the words are Gemma's.

| Factor | Weight | Rule |
|---|---|---|
| 📦 Size of the death commit | 0.30 | log scale, maxes out at 50 files or 2,000 lines removed |
| 🌙 Hour of death | 0.15 | midnight to 5 a.m. in the author's own timezone; 10 p.m. to midnight counts half |
| 💬 Commit message | 0.20 | empty, or contains `remove`, `delete`, `wip`, `hotfix`, `cleanup`, `oops` (and Portuguese equivalents) |
| 🧪 No tests | 0.15 | source file deleted in a commit that touched no tests |
| ⏳ Short life | 0.20 | log scale in hours: one hour ≈ 0.9, one day ≈ 0.5, zero at 30 days |

The weights were calibrated against 175 real deaths across nine of my own repositories. The first draft used linear scales, which saturated almost immediately: in practice files die young and in mass graves.

### ⚰️ Cause of death

Checked in this order:

1. ⚔️ **VIOLENTA** (violent): restlessness ≥ 0.6
2. 🥀 **PREMATURA** (premature): lived less than a day
3. 🕸️ **ESQUECIDA** (forgotten): untouched for more than a year before deletion
4. 🕯️ **NATURAL**: everything else

`.gitkeep` files always die of natural causes. They exist to be replaced by real code, so their mission was accomplished.

## 🌱 Why local and open

Proprietary code can't be sent to a closed API. Kuyō runs entirely on your machine, costs nothing per run, works offline, and the model is just a configuration value you can swap.

## 🧰 Stack

Java 25 · Quarkus (picocli) · JGit · LangChain4j + Ollama · Gemma · Qute

Every visual, including the banner above, is drawn by hand in SVG and CSS. No downloaded images.

The cemetery uses [Shippori Mincho B1](https://github.com/fontdasu/ShipporiMincho) by the Shippori Mincho Project Authors, under the [SIL Open Font License](src/main/resources/fonts/OFL.txt), embedded as small subsets so the page stays a single offline file.

## 📜 License

[MIT](LICENSE)
