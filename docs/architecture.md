# Immersive Spellcraft architecture (0.1.0)

This describes the framework and the three vertical-slice adapters as built in 0.1.0. The specification is
`Immersive_Magic_Coding_Agent_Guidance.md` (it calls the mod "Immersive Magic"); 0.1.0 delivers its milestones M0
(audit and pin) and M1 (framework and vertical slice). What is not built yet is listed, operation by operation, in
[coverage.md](coverage.md).

## Decisions

| # | Decision | Why |
| --- | --- | --- |
| D-01 | 0.1.0 = specification milestones M0 + M1. | The specification defines milestones, not version numbers; M1 is the first independently buildable, reviewable release. |
| D-02 | Mod name **Immersive Spellcraft**, mod id `immersive_spellcraft`, package `com.otectus.immersivespellcraft`. Every specification name of the form `immersive_magic` becomes `immersive_spellcraft` (data folder, config files, command root `/immersivespellcraft`). | The specification marks its ids as proposals; the project was named Immersive Spellcraft. |
| D-03 | Vertical slice: A01 Scribe's Table glyph crafting, I01 Scroll Forge, N01/N02 Glyph Press. | The M1 row of the specification names exactly these three. |
| D-04 | Every native operation is invoked by its own code exactly once; Immersive Spellcraft never builds a result. | Rules 1 and 2 ("native rules remain authoritative", "one action has one owner"). |
| D-05 | Successful performance yields the exact native result. The workmanship grade is shown in the result panel and never stored on the item. | §2.1 and its rule against tagging glyph knowledge and stackable intermediates. Profiles that ask for `persistent_grade: true` are rejected. |
| D-06 | Performance mistakes never cost anything: they lower the grade or need a local correction. Running out of time, stopping or being interrupted releases the session and loses nothing. | Rule 8; §6 "Success and correction". |
| D-07 | Automation keeps native behavior: fake players are never asked to perform. | Rule 9. A machine taking a forge result or starting a press job does exactly what it did before. |
| D-08 | The client sends only bounded control edges; the server runs the same deterministic simulation and decides completion. | §7.6. |
| D-09 | No shared Immersive library is required; family patterns (one-use permit, `clicked()` guard, SERVER+CLIENT config, reflective compat bootstrap, GameTest conventions) were re-implemented from the user's own repositories. | §1.1. |
| D-10 | Integration versions are checked at runtime, not in `mods.toml`. An unsupported version leaves that mod native and is reported. | §7.1 "Instantiate adapters only after their required mod/version checks"; avoids refusing to launch a pack. |
| D-11 | Mixins into mod classes use `remap = false`, naming vanilla overrides in both dev and production form (`saveAdditional`/`m_183515_`). | Removes any dependence on the refmap for mod targets; vanilla targets use the refmap as usual. |
| D-12 | Iron's is compiled against 3.16.3 by default; `-PironsVersion=315` compiles and runs against 3.15.0. | ForgeGradle cannot deobfuscate two versions of one artifact in one build. The Scroll Forge menu bytecode is identical in both. |

## Package map

```text
com.otectus.immersivespellcraft
  core/          adapter contract, PreparedOperation, Inspection/Admission/Validation/CommitResult, StationKey, AdapterRegistry, MenuGuard
  session/       SessionManager (admission, locks, permit, commit), PerformanceSession, InputValidator, SessionClock, MenuGuards, SessionEvents
  minigame/      pure and deterministic, no Minecraft imports: stages, Simulation, GameTypes, ScoreCard, Grade, Autoplay (unit-tested)
  network/       one version-locked channel, bounded codecs (SpecCodec), Packets, ClientTarget bridge
  data/          OperationProfile, ProfileParser (strict), ProfileManager (validated snapshot per reload), CoverageCatalog, CoverageExporter
  config/        ServerConfig (per world, synced), ClientConfig, OperationSwitches
  command/       /immersivespellcraft
  client/        panel, game renderers, overlay over station screens, block-station screen, practice screen, input model, captions
  compat/        CompatibilityBootstrap, IntegrationVersions, CompatMixinPlugin
  compat/ars/    Scribe's Table adapter, gate, event hook, coverage contributor
  compat/irons/  Scroll Forge adapter and menu guard, client intercept, coverage contributor
  compat/ans/    Glyph Press adapter, gate, coverage contributor
  compat/family/ the other Immersive mods and the operations they own
  mixin/core/    vanilla targets: AbstractContainerMenu.clicked guard, container packet lock; client click intercept, integrated-server pause
  mixin/compat/  ars/ScribesTileMixin, ans/GlyphPressBlockEntityMixin (applied only when the mod is present in a supported version)
```

No core class names an optional mod in a field, signature, annotation, serializer or static initializer. Compat
packages are reached by class name after `ModList` and version checks (`CompatibilityBootstrap`), and their client
parts through `DistExecutor`. Common code reaches the client only through `ModNetwork.client` (a `ClientTarget`
installed by client setup), so a dedicated server never loads a client class.

## Adapter contract

`core/MagicOperationAdapter` implements the specification's contract:

| Method | Responsibility |
| --- | --- |
| `inspect(context)` | Read-only: whether the adapter handles this block or menu, the native refusal if any, and the registry identities profile selection matches on. No XP drain, roll, mutation or cast. |
| `begin(context, inspection)` | Admission: snapshot and claim only what the operation owns; returns a `PreparedOperation` or a refusal. |
| `validate(operation, player)` | Recheck the station instance, native job or input identity, the player's menu and reach. Run every 4 ticks and before the commit. |
| `commit(operation, player, score)` | Invoke the native transition once, under the scoped permit. |
| `interrupt(operation, reason)` | Release claims or hand back control; never undoes anything native. |

Each adapter declares one capability, with its own interruption semantics:

| Capability | Adapter | Commit | Interruption |
| --- | --- | --- | --- |
| `IMMEDIATE_COMMIT` | Scroll Forge | The player's own result click, replayed once. | Nothing happened yet; release. |
| `EXISTING_NATIVE_JOB` | Scribe's Table, Glyph Press | Mark the native job performed; the station's own tick finishes it. | The job stays, with its native costs and native cancel path; the player can resume. |
| `NATIVE_ONGOING_EFFECT` | none in 0.1.0 (rituals, M3) | | |

`PreparedOperation` carries the operation id, adapter id and version, the selection identity (recipe id or
`spell@level`), the profile generation and config revision, the player, the station key (dimension and normalized
master position), the menu and the exact click where applicable, the native job id, copies of the inputs and a copy of
the output (a preview, never owned), the display title and native cost lines, the complexity and native processing
window, and the adapter's private snapshot (the exact block entity and menu instances). Sessions are never saved, so no
live player, menu or block entity is ever persisted.

## Lifecycle and commit boundary

`session/SessionState`: Reserved (count-in) → Performing → Ready (revalidate) → Committing (permit) → Completed;
Released from any earlier state; Held when a commit's outcome cannot be reconciled. Native-job adapters are never
Suspended inside Immersive Spellcraft: their station already saves the job, so an interrupted session is released and
a new one can begin against the same job.

At commitment, on the server thread, `SessionManager.commit` revalidates through the adapter, then opens the permit
(`committing` = this session, `permitUsed` = false), calls the adapter's `commit`, and clears the permit in `finally`.
The menu guard lets exactly one click through: the committing session's own player, menu, slot, button and click type,
once. The permit is never stored on a player and never outlives the call. A commit that throws is Held: the actual
inventory and station state are kept, the error is logged, and nothing is retried or reissued.

## Custody and early native consumption, per adapter

**Scribe's Table (A01).** Ars charges XP points in `ScribesTile.setRecipe` when the player chooses the glyph, refunds a
replaced job's items and XP itself (`refundConsumed`), gathers ingredients in its tick, and then runs a 120-tick
crafting window whose last tick drops the glyph. The gate (`compat/ars/ScribeGate`, called at the head of the master
tile's tick by `ScribesTileMixin`) touches none of that: when a job enters its crafting window it is marked
`AWAITING` (or `EXEMPT` if no performance applies), and while it awaits, the window is held on its last tick
(`craftingTicks` is kept at 2, so the native decrement leaves 1 and the drop branch never runs). Completing the
inscription marks the job `INSCRIBED`; the native code then drops the glyph exactly once. The gate is saved with the
tile under the key `immersive_spellcraft` (ScribesTile does not call `super.saveAdditional`, so Forge's persistent data
would be lost) and travels in the tile's update tag, so clients mirror the hold and show the tooltip. The station key is
always the master (head) half. A replaced or refunded job resets the gate; breaking the table takes Ars's own
refund path and the session is released because the block entity is gone.

**Scroll Forge (I01).** Iron's puts a preview scroll into the tile's slot 3 when a spell is selected; taking it runs the
result slot's `onTake`, which consumes one ink, one paper and one focus. Every click on the result slot that would take
it (pickup, shift-click, number-key and offhand swap, drop) is refused by the `clicked()` guard unless it is the
permitted commit; the client sends that same click as a begin request instead. During a session every click in the
performer's menu is refused and container packets are dropped after the thread check; a second viewer of the same forge
cannot touch its four slots. The forge's item handler is exposed to hoppers by Iron's without sides; Immersive
Spellcraft does not wrap it, so automation may still change a slot, and any change to the ink, paper, focus or preview
releases the session before anything moves. The commit replays the player's click; shift-click keeps its native meaning
(repeat while inputs and inventory space last), each unit paying its own inputs. The commit checks that the three
inputs dropped by the same count; a mismatch would be Held (never observed).

**Glyph Press (N01, N02).** The Press saves its own job with its Source reservation, input locks and progress. The
gate (`compat/ans/PressGate`, from `GlyphPressBlockEntityMixin`) observes the Press's own `start(player)`: if it
returned `started` and a performance applies to that player, the job id and starter are written to the press's Forge
persistent data (the press calls `super.saveAdditional`, so this saves and loads with it) and the embossing begins at
once. At the head of the Press's private `complete()`, a gated job that is not yet embossed returns early, so it waits
with its inputs locked and its Source reserved; once embossed, `complete()` runs unchanged: revalidation, the output-full
pause, and the single mutation that consumes the inputs and Source and inserts the glyph. The native duration always runs
in full; a cancelled job no longer exists, so the gate can never make it finish. Only the starter may emboss (their
knowledge admitted the job); the native Cancel stays available to anyone the Press allows. Turning the operation off
releases waiting jobs to finish natively.

## Interruption and persistence

| Situation | Behavior |
| --- | --- |
| Stop (button or Escape), idle (`idleTimeoutSeconds`), out of time | Released; nothing lost. Native-job adapters leave the job waiting with a note on how to resume. |
| Station menu closed, disconnect, death, attack by an entity, dimension change, out of reach | Released through the same path. |
| Station broken, exploded or replaced | The adapter's validation sees a different or removed block entity and releases; the station's own drop/refund path is the only one. |
| `/reload` | Every applied profile snapshot has a new generation; sessions planned under the old one are released. An invalid profile file is reported and the previous valid version of that id stays in force. |
| Server config reload | The config revision changes; running sessions are released. |
| Clean stop or chunk unload | Sessions are released; native jobs and their gates are saved by their stations. |
| Abrupt termination during a commit | Each commit is a single server-thread call: the Forge take, or setting one flag that the station saves. Crash consistency beyond that is the station's own (Minecraft has no cross-inventory transaction). |

## Server authority

A begin request names only a menu (id and revision) and the click, or comes from a server-side block interaction; the
server resolves the recipe, costs and result itself. Input batches carry the session id, a nonce, a sequence number, a
stage hint and at most 64 control edges with times in milliseconds. `InputValidator` ignores replayed sequence numbers
and rejects gapped sequences, out-of-order times, times more than 250 ms ahead of the server's own elapsed time, and more than
`maxInputEventsPerSecond`; an impossible stage hint counts as a violation, and more than three violations end the
session. Inputs that arrive late are applied at the simulation's current time, never earlier (bounded latency
compensation: the server advances its simulation to `elapsed - maxLag`, where `maxLag` is 300-1500 ms from the measured
ping). If any input was applied late the client is sent the applied list (at most every 400 ms) and rebuilds its prediction. There is no
completion, score or item message from the client at all.

Generated games are sent whole (`SpecCodec`, bounded) with the generator version; a client of a different version
cancels. One channel, version-locked: a client without the mod cannot join.

## Ownership and family coexistence

`AdapterRegistry` records one owner per operation at startup; a duplicate claim is logged with an actionable message
and resolved to the lexically smaller adapter id. Immersive family mods found at startup are listed
(`compat/family/FamilyIntegrations`); none of them owns glyph crafting, scroll forging or glyph pressing, so 0.1.0 needs
no runtime arbitration. JEI, Jade and The One Probe integrations are M4 work.

## Verification in 0.1.0

- `src/test`: 23 JUnit test cases (13 methods, five parameterized over the three games) over the simulation (determinism by exact replay, correctable mistakes, expiry, untimed
  mode, duration targets, rate-limited pointer, essential stages, human input rate).
- `src/gameTest`: 21 GameTests (core invariants, profile validation and precedence, the coverage manifest, and 17
  station scenarios: native parity, interruption, abuse and automation), run in every profile of the matrix in
  [compatibility.md](compatibility.md).
- `src/smokeTest`: a client agent that plays every game's practice demonstration on the title screen and saves
  screenshots.

See [compatibility.md](compatibility.md) for what ran, with which artifacts, and what has not been verified.
