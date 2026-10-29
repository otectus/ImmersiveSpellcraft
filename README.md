# Immersive Spellcraft

Magic is performed at the workstations you already use. Immersive Spellcraft adds a short, tactile performance to
the moment a magical workstation changes state, while the original mod still decides what is made and what it costs:
your performance only decides how cleanly the physical work is done. It is the magic counterpart of the Immersive
family (Smithing, Tanning, Enchanting, Jewelry, Apotheosis, Brewing).

**Version 0.1.0** is the framework and its first vertical slice: one station from each supported magic mod.

| Station | Mod | Performance | What you do |
| --- | --- | --- | --- |
| Scribe's Table, glyph crafting | Ars Nouveau | **Sigil Inscription** | Guide the quill along 3-6 strokes of the glyph: hold to ink, lift at each glowing junction. |
| Scroll Forge | Iron's Spells 'n Spellbooks | **Scroll Forging** | Turn the school focus into alignment, feed ink inside the flow band, press the seal on its mark. |
| Glyph Press | Ars 'n Spells | **Glyph Embossing** | Slide the die over the glyph plate, hold the pressure band while it engraves, release on the seal mark. |

Every other station of the three mods keeps its exact native behavior in this release; [docs/coverage.md](docs/coverage.md)
lists all 54 required operation groups and which milestone builds each.

## How it plays

- **Scribe's Table.** Choose a glyph with your spell book and give the table its ingredients, as always. When the
  table starts its work, the glyph waits for its sigil: right-click the table with an empty hand (take anything off it
  first) and inscribe it. The table then finishes the glyph and drops it. XP is paid once, by Ars, when you choose the
  glyph. Learning the glyph is unchanged: use the glyph item.
- **Scroll Forge.** Load ink, paper and a school focus and pick a spell, as always. Clicking the result (or
  shift-clicking, or using a number key) begins the forging over the forge's own screen; finishing it takes the scroll
  exactly as that click would have. Shift-click still forges repeatedly while inputs and space last.
- **Glyph Press.** Press **Inscribe Glyph**, as always. The embossing begins at once and runs inside the Press's own
  processing window (5/7/10 s by default). The Press still needs its full time and its Source; when both the time and
  the embossing are done, it seals the glyph. Close the screen and the job simply waits for you; reopen the Press and
  choose **Begin embossing**, or cancel the job in the Press as usual.

Each game shows the item being made, one short instruction for the current action, captions for every cue, and the
native costs. Mistakes never cost anything: a missed mark is retried, overshot ink re-inks only its segment, too much
pressure just needs easing off. Stopping, closing, walking away, being attacked or running out of time loses nothing.
The result panel shows a workmanship grade (Crude, Standard, Fine, Masterwork) for the work; it is never stored on the
item and never changes the item.

`/immersivespellcraft practice` opens an isolated practice of any game, with a **Watch** demonstration. Nothing is sent
to the server, crafted, consumed or recorded.

## What never changes

- The native recipe, ingredients, XP, Source, catalysts, spell, level, school, rarity, slot limits, knowledge checks,
  recipe conditions and server disables. A perfect performance makes exactly the native item; there are no bonuses.
- One owner per action: the native station performs it, once. There is no second craft, cost or refund.
- Automation (machines acting through fake players) keeps its native behavior and is never asked to perform.
- Native refusals (a blank scroll, an unlearned spell, a full output) are shown by the station before any game.

## Accessibility

Keyboard-only play (rebindable keys, Options > Controls > Immersive Spellcraft); toggle instead of hold; timing
assistance up to the server's cap; untimed mode (markers wait on their marks, nothing expires) when the server allows it;
reduced motion; high contrast; colorblind-safe colors, with every cue also shown by shape and caption; scalable panel;
cue volume. None of these changes the result. Screens never pause multiplayer and the world stays visible.

## Requirements

- Minecraft 1.20.1, Forge 47.4.x (developed on 47.4.23), Java 17.
- Any of the following, all optional; with none installed the mod loads and every station is native:

| Mod | Supported | Tested |
| --- | --- | --- |
| Ars Nouveau | 4.12.x (>= 4.12.7) | 4.12.7 |
| Iron's Spells 'n Spellbooks | 1.20.1-3.15.x and 3.16.x | 1.20.1-3.15.0, 1.20.1-3.16.3 |
| Ars 'n Spells (needs Ars Nouveau; its Glyph Press needs Iron's) | 3.4.x | 3.4.0 |

A mod outside its supported range stays native and is reported by `/immersivespellcraft integrations`. The mod must be
installed on both the server and every client. Exact artifacts, hashes and the tested matrix are in
[docs/compatibility.md](docs/compatibility.md).

## Configuration, profiles and commands

Server settings are per world (`<world>/serverconfig/immersive_spellcraft-server.toml`, synced to clients); visual and
accessibility preferences are in `config/immersive_spellcraft-client.toml`. Pack authors can change or disable a
performance per recipe, item, block or namespace with datapack profiles in
`data/<namespace>/immersive_spellcraft/operation_profiles/`, which are validated on every reload. Commands:
`integrations`, `coverage`, `inspect`, `sessions`, `cancel`, `release` and `practice` under `/immersivespellcraft`. See
[docs/configuration.md](docs/configuration.md).

## Multiplayer

The server judges every performance from bounded inputs with its own copy of the deterministic simulation; clients
never send a score, an item or a completion. One player performs at a station at a time, at either half of a two-part
table. Only the player who started a Glyph Press job may emboss it. Inputs are rate-limited and timestamp-bounded, and
late inputs are applied late, never earlier.

## Building

```sh
./gradlew build                        # reobfuscated jar in build/libs, runs the JUnit suite
./gradlew runGameTestServer            # GameTests, core profile (no target mods)
./gradlew runGameTestServer -Pwith=all # with Ars Nouveau, Iron's 3.16.3 and Ars 'n Spells
./gradlew runGameTestServer -Pwith=all -PironsVersion=315
./gradlew runClient -Pwith=all         # a dev client with everything
./gradlew runClient -PsmokeTest        # client smoke agent: plays every practice game, saves screenshots
```

`-Pwith=` takes any comma-separated combination of `ars`, `irons` and `ans` (`ans` brings Ars Nouveau). Each profile
runs in its own directory under `run/` or `run-gametest/`. The Ars 'n Spells jar is read from `libs/` (see
[docs/compatibility.md](docs/compatibility.md)); everything else resolves from Maven.

## Status and roadmap

0.1.0 delivers specification milestones M0 (audit, pinned versions, coverage manifest) and M1 (framework and vertical
slice). Next: M2 the remaining crafting stations (Scribe inscription, Imbuement, every Apparatus recipe family,
Alteration, all seven Arcane Anvil branches, Inscription Table, Alchemist Cauldron), M3 Ars 'n Spells workflows and all
rituals, M4 infrastructure, helpers and viewers (JEI, Jade), M5 release validation and the in-game guide. Architecture:
[docs/architecture.md](docs/architecture.md).

## License

GPL-3.0. Original code and art; no dependency jar or upstream art is bundled.
