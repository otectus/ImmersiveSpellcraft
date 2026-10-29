# Compatibility: pinned artifacts and the tested matrix (0.1.0)

Everything below was run on 2026-10-28 on Linux with OpenJDK 17.0.20.1, Gradle 8.8 and ForgeGradle 6.0.54. It
records what was verified and how; "tested" in this project never means "it compiled".

## Toolchain

| Component | Version | Source |
| --- | --- | --- |
| Minecraft | 1.20.1 | |
| Forge | 47.4.23 | `net.minecraftforge:forge:1.20.1-47.4.23`; server installer SHA-1 `ed31ce02ac69176f34353235cb2508d5a0f1e088` (matches Forge's published checksum) |
| Mappings | official 1.20.1 | |
| Mixin | 0.8.5 (annotation processor), MixinGradle 0.7.38 | |
| Java | 17 | |

## Integration artifacts

All integrations are `compileOnly`; no dependency jar is bundled. Version pins live only in `gradle.properties`.

| Mod | Version | Coordinates | Release jar SHA-256 |
| --- | --- | --- | --- |
| Ars Nouveau | 4.12.7 | `curse.maven:ars-nouveau-401955:6688854` | `1f1debc282a0c379c1141f2840ea294eede6f6b544c589663f40bbe17b59a1af` |
| Iron's Spells 'n Spellbooks | 1.20.1-3.16.3 | `curse.maven:irons-spells-n-spellbooks-855414:8680180` | `54b5aaa52887c38f570fbd820288171234d8541c17a5e953f9271189fdd480fb` |
| Iron's Spells 'n Spellbooks | 1.20.1-3.15.0 | `curse.maven:irons-spells-n-spellbooks-855414:7402504` | `92c046383b4960c655f840d8846732a481edcf7c5ed89028b3d7b2cc2910b224` |
| Ars 'n Spells | 3.4.0 | `libs/ars_n_spells-3.4.0.jar` (slim; compile and dev runtime) | `f49689612247a33d702a92b30783feffe5f6640d9025fdb2f8f8bfe287ec404e` |
| Ars 'n Spells | 3.4.0 | `libs/ars_n_spells-3.4.0-release.jar` (release jar with MixinExtras jar-in-jar; production check) | `0fd42beca4d8613d02e54179669e8452a8df37f9cace6bbb8ed1bf4429b85a6b` |

Both Ars 'n Spells jars were built on 2026-10-21 from `otectus/ars-n-spells` at commit
`0da9de067fac60db06f18ba407b266261c65db47` (the inspected 3.4.0 release commit), copied from that repository's
`build/libs/`. `libs/` is git-ignored.

Runtime-only libraries of the integration profiles (never compiled against):

| Library | Version | SHA-256 |
| --- | --- | --- |
| GeckoLib | `software.bernie.geckolib:geckolib-forge-1.20.1:4.8.3` | `8fada49c19464069316b94203cffeeafc06e0af3d572443e7020c62cd0f20300` |
| Curios | `top.theillusivec4.curios:curios-forge:5.14.1+1.20.1` | `6d77ae8ad532fdf303390f404b0081b3b4ac4f61e7f1f4b4d4a9077e132dae4f` |
| playerAnimator | `dev.kosmx.player-anim:player-animation-lib-forge:1.0.2-rc1+1.20` | `4a624602ba90d305a4cdcfbde07c893466ddbc0a1210b559bf607bf715626cb0` |
| irons_lib (Iron's 3.16.x only) | `maven.modrinth:irons-lib:1.20.1-2.1.0` | `db4c7cc853c0ffb1d36a0f19af22c224a8608b2a55c747adc70fb44455baf097` |
| MixinExtras (dev runs with Ars 'n Spells) | `io.github.llamalad7:mixinextras-forge:0.5.4` | `7922899a121a27f63a69a9ffe57470d8719cc52d239dfa9408e15d32a7b4c264` |

GeckoLib 4.8.3 satisfies Ars Nouveau (>= 4.2.1) and both Iron's versions (>= 4.8 and >= 4.8.2); Curios 5.14.1 is the
floor of Iron's 3.16.3.

## Supported ranges (runtime-checked)

| Mod | Supported | Outside the range |
| --- | --- | --- |
| Ars Nouveau | [4.12.7, 4.13) | Dormant: the Scribe's Table stays native; the compat mixin is not applied. |
| Iron's Spells 'n Spellbooks | [1.20.1-3.15.0, 1.20.1-3.17) | Dormant: the Scroll Forge stays native. Only 3.15.0 and 3.16.3 were tested. |
| Ars 'n Spells | [3.4.0, 3.5), and a supported Ars Nouveau | Dormant: the Glyph Press stays native; the compat mixin is not applied. |

Iron's 1.20.1-3.15.0 and 1.20.1-3.16.3 were compared for every symbol the Scroll Forge adapter uses: the
`ScrollForgeMenu` bytecode is identical in both (decompiled and diffed), so one adapter serves both. The sources compile
against either (`-PironsVersion=315` switches compilation as well as runtime), which is how the 3.15.0 profile below
was built. The older `1.20.1-legacy` line (3.4.0.11) is not a target.

## Automated checks

| Check | Command | Result |
| --- | --- | --- |
| Static consistency (lang keys, models, version traps) | `python3 .mcmod-tools/check_mod.py .` | 0 errors, 0 warnings |
| Unit tests (simulation) | `.mcmod-tools/gradlew-quiet.sh . test --offline` | PASS, 23 test cases |
| Production build | `.mcmod-tools/gradlew-quiet.sh . build --offline` | PASS; `build/libs/immersive-spellcraft-0.1.0.jar` (397,589 bytes, SHA-256 `3417a22b0bf76b23ca55a00a4bd1f2f355c92df600160aec26bc7da1c73d0788`), reobfuscated, 164 classes, refmap included, no test code |

### GameTest runtime matrix

Each profile is a separate GameTest server with its own world (`run-gametest/<profile>`). Every profile runs all 21
tests; `profileMatchesTheRequestedMods` fails a profile whose requested mods (and the exact Iron's version) did not load
or whose integration did not activate, and the station scenarios run for every integration present (they self-skip
only when their integration is absent).

| Profile | Gradle arguments | Loaded | Result |
| --- | --- | --- | --- |
| Core only | (none) | no target mods | 21/21 passed |
| Ars Nouveau only | `-Pwith=ars` | Ars 4.12.7 | 21/21 passed (6 Scribe scenarios ran) |
| Iron's only | `-Pwith=irons` | Iron's 3.16.3 | 21/21 passed (6 Scroll Forge scenarios ran) |
| Ars + Iron's, no ANS | `-Pwith=ars,irons` | Ars 4.12.7, Iron's 3.16.3 | 21/21 passed |
| Ars + ANS, no Iron's | `-Pwith=ans` | Ars 4.12.7, ANS 3.4.0 | 21/21 passed (Press scenarios self-skip: a Press needs Iron's scrolls) |
| All three, Iron's 3.16.3 | `-Pwith=all` | Ars 4.12.7, Iron's 3.16.3, ANS 3.4.0 | 21/21 passed (all 17 scenarios ran); runtime coverage manifest exported |
| All three, Iron's 3.15.0 | `-Pwith=all -PironsVersion=315` | Ars 4.12.7, Iron's 3.15.0, ANS 3.4.0 | 21/21 passed; compiled against 3.15.0 |

The 17 station scenarios assert:

- **Scribe's Table (A01).** XP is charged once, natively, at selection. The glyph is held past the native 120-tick
  window until the inscription, then dropped exactly once with no extra charge or refund. A recipe kept native by a
  profile override drops natively (the parity reference). Stopping keeps the job, with no drop or refund, and it can
  resume. Breaking the table mid-performance takes Ars's one refund path (each ingredient once, exactly the recipe's
  XP as orbs) and makes no glyph. A second player is refused at the same table. Forged, future-dated, wrong-nonce and
  impossible-stage inputs complete nothing and end the session.
- **Scroll Forge (I01).** Pickup, shift-click, number-key swap and drop of the result are all refused without a
  performance. The performed take yields a scroll identical (`ItemStack.matches`) to the native take made by automation
  on an identical forge, with identical consumption, the focus included. A performed shift-click forges as many scrolls
  as the native one. An input changed mid-session releases it with nothing consumed. A stale begin request changes
  nothing. A second viewer cannot touch the forge's slots.
- **Glyph Press (N01/N02).** A gated job is held past its full native duration, then completes once with exactly one
  scroll, one plate, the tier's gems and 500 Source. The glyph never appears before the native duration. The Press's
  own Cancel ends the session, returns the inputs and the reserved Source, and never finishes the job. Only the
  starter may emboss and can resume; the gate survives a save and load of the press under the same job. Automation
  starts are never gated.

### Production dedicated server

A real Forge 47.4.23 dedicated server (installed with the official installer) was started twice, the second time with
the final `immersive-spellcraft-0.1.0.jar` above, and the original release jars of Ars Nouveau 4.12.7, Iron's 1.20.1-3.16.3, irons_lib
2.1.0, GeckoLib 4.8.3, Curios 5.14.1, playerAnimator and the Ars 'n Spells 3.4.0 release jar, with no development
remapping. It reached "Done", and from its console:

- all three integrations reported active;
- both compat mixins reported applied (`ScribesTileMixin` against the SRG-named production class, `GlyphPressBlockEntityMixin`);
- `coverage` reported 54 groups (4 performed, 1 preserved, 49 not yet supported);
- `coverage export` wrote the manifest.

The log has no mixin, injection or refmap warning. The two Iron's loot-table parse errors in that log are Iron's own and
appear without Immersive Spellcraft too.

### Client

`runClient -PsmokeTest` (core profile) launched a development client. Under Hyprland it was floated and sized by its
PID. It played every game's practice demonstration on the title screen, in the default look and in high contrast,
saved screenshots of each mid-performance and at its result, wrote `smoketest-result.json`, and quit.

- **First round** (all four runs completed). The screenshots showed two problems: the vector shapes (strokes, quill,
  rings, arcs, discs) were culled because of their triangle winding, and the footer text overlapped the practice
  buttons. Both were fixed: culling is off in the vector batch, and the footer and practice note were re-laid out and
  shortened.
- **High contrast**. The buttons' white labels sat on light gray; the button background now follows the panel color.
- **Final run** on the final jar's sources (launches took 64-138 s). All four runs completed, with every shape visible
  and all text inside the panel:
  - Sigil Inscription: Masterwork 96, 5.4 s.
  - Scroll Forging: Fine 89, 5.0 s.
  - Glyph Embossing: Masterwork 93, 6.1 s.
  - Glyph Embossing in high contrast: Masterwork 91, 6.0 s.

  Grades vary slightly between runs because the demonstration's inputs are sampled on the client's frame clock.

One launch on this desktop sat behind Forge's loading screen for about ten minutes before reaching the title screen,
with nothing logged in that time (a thread dump showed the normal main loop idling). Later launches took 64 to
138 seconds. It looked like a display/compositor effect of the dev client, not mod code: nothing of Immersive
Spellcraft runs before the title screen except event registration.

## Not verified in 0.1.0

- A live multiplayer session with two real clients, and play under 100/250 ms latency, jitter, low FPS or reduced TPS.
  The server-side authority, rate and timestamp rules are covered by GameTests only.
- An end-to-end survival playthrough on a real client: obtaining materials, then a real Scribe's Table, Scroll Forge
  and Glyph Press in a world, played by hand. The performances were exercised by GameTests through the real server
  input path and by the practice screen on a client, not by a person at the stations.
- GUI scales other than the smoke client's, and keyboard-only rebinding, beyond what the panel code implements.
- Packs mixing these mods with the other Immersive family mods (no family mod owns any 0.1.0 operation).
