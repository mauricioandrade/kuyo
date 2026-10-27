---
name: Kuyō
description: A night walk through a Japanese cemetery where every deleted file has its own grave.
colors:
  noite: "#0d1130"
  noite-alta: "#070920"
  horizonte: "#1d2452"
  chao: "#1a1f45"
  chao-borda: "#3b4170"
  granito: "#565b74"
  granito-luz: "#7a809b"
  granito-sombra: "#343850"
  ouro: "#eadcb0"
  madeira: "#c8b48a"
  madeira-sombra: "#9c8862"
  musgo: "#5d7a45"
  washi: "#f1e9d8"
  washi-fundo: "#e7dcc4"
  sumi: "#231d19"
  sumi-suave: "#5a4f45"
  shu: "#c8402b"
  lanterna: "#ff9a48"
  chama-calma: "#a8dcff"
  chama-agitada: "#80aaff"
  chama-violenta: "#b98dff"
  texto: "#ece7f7"
  texto-suave: "#b2aed0"
typography:
  display:
    fontFamily: "Shippori Mincho B1, Hiragino Mincho ProN, Yu Mincho, serif"
    fontSize: "clamp(2rem, 5vw, 3.1rem)"
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: "-0.01em"
  kanji:
    fontFamily: "Shippori Mincho B1, serif"
    fontSize: "clamp(2.6rem, 7vw, 4.2rem)"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "0.08em"
  terrace:
    fontFamily: "Shippori Mincho B1, serif"
    fontSize: "clamp(1.15rem, 2.6vw, 1.45rem)"
    fontWeight: 700
    lineHeight: 1.3
  epitaph:
    fontFamily: "Shippori Mincho B1, serif"
    fontSize: "clamp(1.05rem, 2.2vw, 1.2rem)"
    fontWeight: 400
    lineHeight: 1.65
  body:
    fontFamily: "Shippori Mincho B1, serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.6
  carving:
    fontFamily: "Shippori Mincho B1, serif"
    fontSize: "8.5px to 17px, fitted to the pillar"
    fontWeight: 700
    lineHeight: 1
rounded:
  stone: "0"
  paper: "4px"
  board: "6px"
spacing:
  row-gap: "30px"
  grave-gap: "26px"
  terrace-gap: "46px"
  gutter: "16px"
components:
  seal:
    backgroundColor: "{colors.shu}"
    textColor: "{colors.washi}"
    rounded: "{rounded.paper}"
    size: "30px"
  washi-strip:
    backgroundColor: "{colors.washi}"
    textColor: "{colors.sumi}"
    rounded: "{rounded.paper}"
    padding: "18px 24px 20px"
  kanban:
    backgroundColor: "{colors.madeira}"
    textColor: "{colors.sumi}"
    rounded: "{rounded.board}"
  grave-pillar:
    backgroundColor: "{colors.granito}"
    textColor: "{colors.ouro}"
    rounded: "{rounded.stone}"
    width: "46px"
---

# Design System: Kuyō

## Overview

The cemetery is a Japanese *hakaba* at night: family graves (a granite *sao-ishi* pillar on two stacked bases) with wooden *sotoba* slats behind them, an incense holder and flower vases in front, stone *tōrō* lanterns at the head of each terrace, vermilion *chōchin* hanging over a wooden signboard. Each deleted file is one grave. It is a memorial, not a Halloween page: no pumpkins, bats, crosses or cartoon ghosts.

Data drives the scene. Facts appear as material properties, not decoration:

- pillar height is the log of the file's lifespan;
- the number and color of the *hitodama* flames is the restlessness band;
- a tilted pillar and a yūrei mark a violent death;
- moss and cold incense mark a forgotten grave (*muen*, no one left to tend it).

## Colors

Full palette in named roles. The ground is aizome indigo, never black.

### Primary
- **Shu vermilion** (`shu`): seals, lantern paper, the typing caret, the "why it does not rest" line.

### Secondary
- **Lantern amber** (`lanterna`): real light sources only (chōchin, tōrō firebox, incense embers) and the focus ring.
- **Hitodama blues and violet** (`chama-*`): the only cold light. Calm flames are pale blue, agitated ones blue, violent ones violet.

### Neutral
- Night (`noite`, `noite-alta`, `horizonte`), granite (`granito*`), ground (`chao*`), washi paper (`washi*`) with sumi ink (`sumi*`), and wood (`madeira*`).

### Named Rules
- **Light has a source.** Glow is allowed only on things that emit light in the scene: moon, lanterns, embers, spirit flames. Nothing else glows.
- **Gold leaf carving.** Names on pillars are filled with `ouro`, as Japanese grave inscriptions are painted, so they stay legible on granite.

## Typography

One family: Shippori Mincho B1 (SIL OFL), embedded as base64 subsets (Latin, Latin Extended, and only the kanji used: 供養 横死 夭折 無縁 天寿, and 師 for the Shishō’s autopsy). Every kanji face is declared once per weight, because Chrome groups faces by weight before it combines `unicode-range`.

### Hierarchy
- Kanji on the signboard: vertical, 700, up to 4.2rem.
- Display (`Kuyō`): 700, up to 3.1rem.
- Terrace dates: 700, up to 1.45rem.
- Epitaph: 400, up to 1.2rem, line-height 1.65, max 68ch.
- Body and meta: 1rem and 0.85 to 0.95rem, lining numerals.
- Carving: vertical-rl, 700, sized in Java so the whole file name fits the pillar.

## Layout

Header (signboard between two lanterns, the census by cause, the flame legend), then terraces in order of death, then a footer line. Terraces group by month; a month with more than 24 souls splits into one terrace per day of death. Graves sit in a wrapping flex row; each `li` draws its own ground strip, so wrapped rows stand on ground too. The washi strip is fixed at the bottom (max 960px, 48vh), and the body pads itself by the strip's measured height. At ≤ 640px graves scale to 0.84 and center.

## Elevation & Depth

Depth comes from night layers: sky gradient, moon, two hill silhouettes, terraces, then the mist above the graves and the washi strip on top. Shadows are neutral and offset downward (`0 18px 40px rgba(0,0,0,.55)` on the board, `0 18px 50px rgba(0,0,0,.6)` on the strip, `0 8px 14px rgba(0,0,0,.4)` under grave bases).

## Shapes

Stone is square-cut. Pillar tops are chamfered by a 6-point clip-path. Sotoba tops are pointed with two notches. Paper gets 4px corners, the signboard 6px. Flames are rotated teardrops with a fading tail.

## Components

### Grave (signature)
A `<button>` named "file name, cause", described by a hidden record. Layers: sotoba, pillar with gold carving, two bases, vases with shikimi, incense with two sticks, an SVG smoke path, flames. Approaching it (mouse hover, focus or tap) brightens the pillar, quickens the smoke and raises the yūrei on violent graves.

### Washi strip
The reading place. Shows the cause seal, name and path, lifespan and hour of death, last words with the commit hash, the epitaph brushed in character by character (instant under reduced motion; the full text goes to an `aria-live` region), and the reasons for restlessness in plain words. A close button and Escape return it to the invitation.

### Seal
A vermilion square with the cause in vertical kanji: 横死 violent, 夭折 premature, 無縁 forgotten, 天寿 natural.

### Lanterns
Chōchin: vermilion paper with rib lines (the ribs are the object's real structure), black caps, a slow sway and an irregular flicker. Tōrō: a stone lantern at each terrace with a lit firebox.

## Motion

Motion only runs on graves inside the viewport (IntersectionObserver adds `.vivo`), and every loop animates only `transform` and `opacity` on HTML-level layers so the compositor carries it: never SVG attributes such as `stroke-dashoffset`, never layout properties. Terraces off screen are skipped with `content-visibility: auto`.

- **Arrival:** the first time a terrace enters the viewport, its hitodama ignite one by one (opacity and scale, 70 ms apart, capped at 900 ms).
- **Approach (the focal moment):** the cause seal is stamped onto the washi like a hanko (scale and slight rotation settling in 420 ms, `cubic-bezier(.16, 1, .3, 1)`), the name fades up, the incense embers flare, the flames grow 15%, the smoke quickens and violent graves raise the yūrei (0.9 to 1.2 s exponential ease-out, then hovering).
- **Companion:** with a mouse, one hitodama follows the visitor's cursor, floating above and to the right of it so it never covers the pointer. It trails on a spring, its tail points back along the motion and stretches with speed, and over a grave it takes that grave's restlessness color. It hides over the washi and the sound button, its frame loop stops once it settles, and it is absent on touch and under reduced motion. It is a spirit flame, not a cartoon ghost.
- **Ambient:** flames float on a 5 s cycle (agitated 3.2 s, violent 2.6 s plus a tremor on their container); smoke is two strands that rise, widen and fade under a static mask; lanterns sway; mist drifts over 46 to 62 s; stars twinkle in three groups.

`prefers-reduced-motion` turns every CSS animation off; the seal still fades in, and the epitaph appears at once.

## Sound

Off by default, toggled by a small button in the top left (icon only on phones). Everything is synthesized with Web Audio, so the page stays one offline file:

- **Ambient:** wind (filtered noise breathing on slow LFOs) and suzumushi, the bell cricket of Japanese autumn nights, chirping from random places in the stereo field.
- **Approach:** an orin, the bowl bell struck at Buddhist services, with inharmonic partials and long tails. Its tone follows the cause: natural 587 Hz, premature higher (784 Hz), forgotten lower and softer (466 Hz), violent 415 Hz with a second bell detuned by 3 Hz so the two beat against each other.

Nothing plays in a hidden tab.

## Do's and Don'ts

### Do:
- Encode facts as properties of the grave: height, tilt, moss, flames.
- Keep every visual hand-made in CSS and SVG, and the page in one file.
- Use the kanji subset only for the words embedded; add glyphs to the font subset before using new ones.

### Don't:
- Don't use western tombstones, crosses, pumpkins, bats or blood.
- Don't make the yūrei cute: no faces with expressions, no cartoon outlines.
- Don't add glow to anything that is not a light source.
- Don't load fonts, scripts or images from the network.
