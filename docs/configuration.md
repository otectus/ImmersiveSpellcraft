# Configuration, profiles and commands (0.1.0)

## Server settings: `immersive_spellcraft-server.toml`

A Forge **SERVER** config. It is per world: `<world>/serverconfig/immersive_spellcraft-server.toml` (on a dedicated
server, `world/serverconfig/`), copied from `defaultconfigs/immersive_spellcraft-server.toml` when a world is created,
and synced to every client on join, so clients read the same switches. It is not a global config. Reloading it releases
running performances (nothing is lost) so none continues under old rules.

| Group | Key | Default | Range | Meaning |
| --- | --- | --- | --- | --- |
| general | `enabled` | true | | Master switch; false keeps every station native. |
| general | `creativeBypass` | false | | Creative players complete operations natively, without a performance. |
| integrations | `arsNouveau`, `ironsSpellbooks`, `arsNSpells` | true | | Per-integration switches. An integration also needs its mod in a supported version. |
| operations | `arsGlyphCrafting` | true | | Sigil Inscription at the Scribe's Table. |
| operations | `ironsScrollForge` | true | | Scroll Forging at the Scroll Forge. |
| operations | `ansGlyphPress` | true | | Glyph Embossing at the Glyph Press. |
| operations | `excludedRecipes` | [] | recipe ids | Recipes that keep native behavior. |
| operations | `excludedOutputs` | [] | item ids or `#tags` | Outputs that keep native behavior (matched against the operation's output). |
| difficulty | `durationMultiplier` | 1.0 | 0.5-2.0 | Scales every profile's target and maximum duration. |
| difficulty | `windowMultiplier` | 1.0 | 0.5-2.0 | Scales timing windows, bands and alignment tolerances (higher is easier). |
| difficulty | `maxActions` | 6 | 1-8 | Caps the meaningful actions (strokes) any profile may ask for. |
| accessibility | `maxClientTimingAssist` | 1.5 | 1.0-2.0 | The largest window widening a player may request. 1.0 gives everyone identical windows. |
| accessibility | `allowUntimedMode` | true | | Lets players choose untimed mode (markers wait on their marks; nothing expires). Same native result. |
| multiplayer | `reach` | 6.0 | 2-16 | Distance a player may be from a block station while performing. Menu stations use the station's own reach. |
| multiplayer | `maxConcurrentSessions` | 64 | 1-1024 | Performances running at once on the server. |
| multiplayer | `maxInputEventsPerSecond` | 120 | 20-1000 | Input rate above which a session counts a violation; more than three end it. |
| multiplayer | `idleTimeoutSeconds` | 30 | 5-600 | A session with no input for this long is released. |
| multiplayer | `startCooldownMillis` | 250 | 0-5000 | Minimum time between two start requests from one player. |
| debugging | `debugLogging` | false | | Logs adapter choice, profile resolution and every session transition at INFO; never dumps inventories. |

Turning an operation off (or excluding its recipe) while a native job is already waiting for a performance lets that
job finish natively. There are no outcome settings: the result is always the native one, mistakes never cost anything,
and no mechanical bonus exists. There are no automation settings in 0.1.0: automation acting through fake players is never
gated, so machine takes from the Scroll Forge and machine-started Press jobs behave exactly as natively; the Scribe's
Table has no automated way to choose a glyph.

## Client settings: `config/immersive_spellcraft-client.toml`

| Group | Key | Default | Meaning |
| --- | --- | --- | --- |
| controls | `holdMode` | HOLD | `TOGGLE`: press once to start holding, again to release. No hold is ever required. |
| controls | `pointerAlignment` | true | The mouse position may steer alignments (the keys always work). |
| accessibility | `timingAssist` | 1.0 | Requested window widening, 1.0-2.0; the server caps it. |
| accessibility | `untimedMode` | false | Requests untimed performances (if the server allows them). |
| accessibility | `reducedMotion` | false | Steady highlights instead of the gentle pulse on the active target. Moving markers and drifting bands are the game itself and still move; untimed mode makes markers wait. |
| accessibility | `highContrast` | false | Near-black panel, white marks, solid bands. |
| accessibility | `colorblindSafe` | true | Okabe-Ito colors for good/warning cues. Cues always also differ by shape and caption. |
| accessibility | `uiScale` | 1.0 | Panel scale, 0.75-1.5 (also fits itself to small windows). |
| accessibility | `captions` | true | A caption for every cue. |
| accessibility | `soundVolume` | 1.0 | Cue volume. |
| display | `backgroundDim` | 0.45 | Dimming behind the panel; the station stays visible. |
| display | `showQuality` | true | Show the numeric workmanship beside the grade. |
| display | `resultSeconds` | 4 | How long the result panel stays. |

Keys (Options > Controls > Immersive Spellcraft): primary action (Space; the left mouse button always works too), turn
left and right (arrow keys; A and D also work). Escape stops a performance without losing anything.

## Operation profiles

Files: `data/<namespace>/immersive_spellcraft/operation_profiles/<name>.json`, loaded from datapacks and validated on
every reload. A profile controls the performance only. Recipe economics (ingredients, XP, Source, catalysts, Press
terms) always come from the native recipe or the native mod's own data and can never be set here.

```json
{
  "schema_version": 1,
  "priority": 0,
  "adapter": "immersive_spellcraft:ars_scribe",
  "operation": "immersive_spellcraft:ars/glyph_craft",
  "selector": { "blocks": ["ars_nouveau:scribes_table"], "recipe_types": ["ars_nouveau:glyph"] },
  "treatment": "perform",
  "game": {
    "type": "immersive_spellcraft:sigil_inscription",
    "min_actions": 3, "max_actions": 6,
    "target_duration_seconds": 8, "max_duration_seconds": 20,
    "timing_assist_allowed": true
  },
  "outcome": { "policy": "native_exact", "persistent_grade": false },
  "presentation": { "theme": "immersive_spellcraft:ars_source", "instruction_key": "immersive_spellcraft.game.sigil_inscription.instruction" }
}
```

| Field | Rules |
| --- | --- |
| `schema_version` | Required, must be 1. |
| `adapter` / `operation` | Required. Known pairs: `immersive_spellcraft:ars_scribe` / `immersive_spellcraft:ars/glyph_craft`, `immersive_spellcraft:irons_scroll_forge` / `immersive_spellcraft:irons/scroll_forge`, `immersive_spellcraft:ans_glyph_press` / `immersive_spellcraft:ans/glyph_press`. A known adapter whose mod is absent is dormant, not an error. |
| `treatment` | `perform` or `preserve` (native behavior). `attune` is not available for these three operations. |
| `selector` | Optional. Dimensions `blocks`, `recipes`, `recipe_types`, `items` (ids or `#tags`, matched against every item the operation involves, output first) and `namespaces` (the recipe's, else the output's). Every given dimension must match (AND); any entry within one may match (OR). Ids of absent mods simply never match. |
| `game` | Required for `perform`. `type` is one of `immersive_spellcraft:sigil_inscription`, `immersive_spellcraft:scroll_forging`, `immersive_spellcraft:glyph_embossing`; actions 1-8 with min <= max; target 2-30 s; maximum target-60 s. The Glyph Press always extends the maximum to fit its native window. |
| `outcome` | `policy` must be `native_exact`; `persistent_grade` must be false. |
| `presentation` | Optional `theme` id and `instruction_key` (a translation key). |
| `priority` | Integer, default 0. |
| `comment` | Ignored; any other unknown field is an error. |

**Precedence** for an operation: exact recipe override, then exact item or block override, then tag or recipe-type
profile, then namespace fallback, then the adapter's built-in generic profile (identical to the shipped default). Ties go
to the higher `priority`, then to the lexically smaller profile id. `/immersivespellcraft inspect` shows which profile
won and why.

**Validation and reload.** A profile with any error is rejected whole and reported in the log with its file id and
every problem. If an earlier reload had a valid profile under the same id, that version stays in force. Each applied
snapshot gets a new generation and releases sessions planned under the previous one, so nothing is silently repriced
mid-performance. Example: the GameTest datapack keeps `ars_nouveau:glyph_break` native with an exact recipe override.

```json
{ "schema_version": 1, "adapter": "immersive_spellcraft:ars_scribe", "operation": "immersive_spellcraft:ars/glyph_craft",
  "selector": { "recipes": ["ars_nouveau:glyph_break"] }, "treatment": "preserve" }
```

## Commands

| Command | Permission | Does |
| --- | --- | --- |
| `/immersivespellcraft integrations` | all | Each target mod's version, supported range and status; active adapters and owners; which compat mixins applied. |
| `/immersivespellcraft coverage` | all | Coverage summary and the operations performed or preserved in this release. |
| `/immersivespellcraft coverage export` | 2 | Writes the runtime manifest to `<server dir>/immersive_spellcraft/coverage.json`. |
| `/immersivespellcraft inspect` | 2 | For the open menu or the block you look at: adapter, eligibility or refusal, the resolved profile and why, and whether a performance is required. Read-only. |
| `/immersivespellcraft sessions` | 2 | Active performances: player, operation, station, state, stage, time, inputs. |
| `/immersivespellcraft cancel [player]` | all (self), 2 (others) | Stops a performance. Nothing is lost. |
| `/immersivespellcraft release <pos>` | 2 | Operator recovery for a native job held waiting for a performance (Scribe's Table, Glyph Press): lets it finish natively, once. Logged. Never moves an item itself. |
| `/immersivespellcraft practice [game]` | all | Opens the isolated practice screen. Nothing is sent back, crafted or consumed. |
