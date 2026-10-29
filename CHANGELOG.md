# Changelog

## 0.1.0 (2026-10-28)

First release: the framework and its vertical slice (specification milestones M0 and M1). New project; everything
below was created for this version.

### Performances

- **Sigil Inscription** at the Ars Nouveau Scribe's Table: glyph crafting waits for a 3-6 stroke quill inscription
  before the table drops the glyph. XP, ingredients and the crafting window stay Ars's own.
- **Scroll Forging** at the Iron's Spells 'n Spellbooks Scroll Forge: taking a scroll begins a focus-ink-seal
  forging; finishing it performs the player's own take once. Works with Iron's 3.15.0 and 3.16.3.
- **Glyph Embossing** at the Ars 'n Spells Glyph Press: a die-pressure-seal embossing inside the Press's own processing
  window; the Press completes its job only when both its time and the embossing are done.
- Isolated practice of every game with a demonstration (`/immersivespellcraft practice`).

### Framework

- Adapter contract (inspect, begin, validate, commit, interrupt) with immediate-commit and native-job capabilities;
  one owner per operation, recorded at startup.
- Server sessions: station locks (one operator per station), a scoped one-use commit permit, interruption on stop,
  idle, expiry, closed menu, disconnect, death, attack, dimension change, distance, station change, reload and config
  change; nothing is ever lost.
- Deterministic shared simulation (align, hold-in-band, timed trigger, inked strokes) judged on the server from bounded,
  rate-limited, timestamp-checked inputs; workmanship score `Q = round(100 x weighted mean)` with Crude/Standard/Fine/
  Masterwork grades shown in the result panel only.
- Menu click guard covering pickup, shift-click, swaps, drops and direct packets; container packets locked during a
  performance; client-side click interception.
- Versioned, validated datapack operation profiles with selector precedence and safe reload (invalid files reported,
  previous valid versions kept).
- Server config (per world, synced) and client config; accessibility: keyboard-only, toggle holds, timing assist with a
  server cap, untimed mode, reduced motion, high contrast, colorblind-safe colors, captions, scalable panel.
- Commands: `integrations`, `coverage` (and `export`), `inspect`, `sessions`, `cancel`, `release`, `practice`.
- Runtime version checks for each integration; compat mixins applied only for supported versions; the mod loads with
  no integration installed.
- Runtime coverage manifest exporter and the full A01-A24 / I01-I16 / N01-N14 operation catalog.

### Verification and documentation

- 23 JUnit test cases over the simulation, 21 GameTests (core, profiles, coverage, and 17 station scenarios for native parity,
  interruption, abuse and automation) run across seven runtime profiles; a client smoke agent.
- `docs/architecture.md`, `docs/configuration.md`, `docs/compatibility.md` (dependency lock and tested matrix),
  `docs/coverage.md` and `docs/coverage.json` (generated from the running game), README, mod logo.
