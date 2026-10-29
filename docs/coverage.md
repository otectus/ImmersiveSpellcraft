# Immersive Spellcraft coverage manifest

Generated from the running game by `immersive_spellcraft 0.1.0` at 2026-10-29T00:45:33.148868822Z, run profile `ars-irons316-ans`. Machine-readable source: [`coverage.json`](coverage.json). Regenerate with `gradlew runGameTestServer -Pwith=all -PcoverageExport=$PWD/docs/coverage.json` and `python3 tools/coverage_report.py`.

## Summary

| Status | Operation groups |
| --- | --- |
| `performed` | 4 |
| `attuned` | 0 |
| `native_preserved` | 1 |
| `unavailable_in_this_version` | 0 |
| `unsupported` | 49 |

`unsupported` is a release gap, not a synonym for preserved: the station's native behavior is untouched, but its required treatment is not built yet; the milestone column names when it is due. Registry entries are listed below but not yet triaged one by one (that is the M4 gate).

## Loaded mods

| Mod | Loaded | Version | File | SHA-256 |
| --- | --- | --- | --- | --- |
| `minecraft` | yes | 1.20.1 | forge-1.20.1-47.4.23_mapped_official_1.20.1.jar | `78b463b52bc398e9ff0d91fe23ffa4dd8350ffb66d0aca6b2b71b24e53bfaaba` |
| `forge` | yes | 47.4.23 |  |  |
| `immersive_spellcraft` | yes | 0.1.0 | main |  |
| `ars_nouveau` | yes | 4.12.7 | ars-nouveau-401955-6688854_mapped_official_1.20.1.jar | `40744bab8f9e0d65d0ce8a009c77d8f024ed7c4010431e7aaafb155287b79f40` |
| `irons_spellbooks` | yes | 1.20.1-3.16.3 | irons-spells-n-spellbooks-855414-8680180_mapped_official_1.20.1.jar | `356b569627630b5f11c76888505718f76b1609e6daef8d7f2e54774ad71703b9` |
| `ars_n_spells` | yes | 3.4.0 | ars_n_spells-3.4.0_mapped_official_1.20.1.jar | `cc1c6a88142dc8af3660267aff494d1e80d60e14bf6b90c97d209c0154be0ba0` |
| `geckolib` | yes | 4.8.3 | geckolib-forge-1.20.1-4.8.3_mapped_official_1.20.1.jar | `6e7a3f5e6fe920fcb89b14c3d5d2f2cf20cbc18d96fc3d7b569a25d9365c2530` |
| `curios` | yes | 5.14.1+1.20.1 | curios-forge-5.14.1+1.20.1_mapped_official_1.20.1.jar | `d5060c7ec9325c92f7118982e5a134e25334fbbbca4453ce45c4d29ee98de3ec` |
| `playeranimator` | yes | 1.0.2-rc1+1.20 | player-animation-lib-forge-1.0.2-rc1+1.20_mapped_official_1.20.1.jar | `75ad3ea0b79adb455e52e0cc932fbb0f48b85c1efea42c9a87f3feb7e8fab5b7` |
| `irons_lib` | yes | 1.20.1-2.1.0 | irons-lib-1.20.1-2.1.0_mapped_official_1.20.1.jar | `3427f1a2d5261dd078303b0fa1faf323fe8f66e78733f1f878789d4780ee578a` |

Hashes are of the files the run loaded. In a development run the target mods are ForgeGradle's deobfuscated copies; the release artifacts' hashes are pinned in [compatibility.md](compatibility.md).

## Integrations and owners

| Integration | Version | Supported range | Active | Detail |
| --- | --- | --- | --- | --- |
| `irons_spellbooks` | 1.20.1-3.16.3 | [1.20.1-3.15.0, 1.20.1-3.17) (tested: 3.15.0, 3.16.3) | yes | Scroll Forge: Scroll Forging |
| `ars_n_spells` | 3.4.0 | [3.4.0, 3.5) | yes | Glyph Press: Glyph Embossing |
| `ars_nouveau` | 4.12.7 | [4.12.7, 4.13) | yes | Scribe's Table: Sigil Inscription |

| Operation | Adapter | Capability | Game |
| --- | --- | --- | --- |
| `immersive_spellcraft:ars/glyph_craft` | `immersive_spellcraft:ars_scribe` v1 | existing_native_job | `immersive_spellcraft:sigil_inscription` |
| `immersive_spellcraft:irons/scroll_forge` | `immersive_spellcraft:irons_scroll_forge` v1 | immediate_commit | `immersive_spellcraft:scroll_forging` |
| `immersive_spellcraft:ans/glyph_press` | `immersive_spellcraft:ans_glyph_press` v1 | existing_native_job | `immersive_spellcraft:glyph_embossing` |

Compat mixins applied: `GlyphPressBlockEntityMixin` yes, `ScribesTileMixin` yes.

## Operations

| ID | System | Target treatment | Status | Adapter / profile | Milestone | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| A01 | Scribe's Table glyph crafting (ars_nouveau:scribes_table, recipe type ars_nouveau:glyph) | perform: Sigil Inscription | `performed` | `immersive_spellcraft:ars_scribe` (active)<br>`immersive_spellcraft:ars_glyph_craft` | M1 | Glyph creation only. Learning the glyph (using the glyph item) stays native. XP is charged once, natively, at selection; the performance never charges or refunds XP. |
| A02 | Scribe's Table inscription (every IScribeable) | perform: Sigil Inscription (copy/seal) | `unsupported` |  | M2 | Native onScribe untouched; per-implementation audit of IScribeable pending. |
| A03 | Imbuement Chamber | perform: Source Tuning (attended job) | `unsupported` |  | M2 | Native imbuement untouched. |
| A04 | Enchanting Apparatus, Arcane Core, Arcane Pedestals | perform: Constellation Assembly | `unsupported` |  | M2 | Native attemptCraft untouched. |
| A05 | Apparatus special recipes (enchantment, reactive, spell_write, armor_upgrade) | perform: Binding / Sigil Inscription / Thread Weaving | `unsupported` |  | M2 | Native untouched. |
| A06 | Alteration Table | perform: Thread Weaving / Unpicking | `unsupported` |  | M2 | Native untouched. |
| A07 | Ritual Brazier and Brazier Relay | perform: Ritual Conducting (player activation) | `unsupported` |  | M3 | Native rituals untouched. |
| A08 | Potion Melder | perform: Confluence Mixing (attended merge / first setup) | `unsupported` |  | M4 | Native untouched. |
| A09 | Potion Jar and Potion Diffuser | attune: Flow Attunement; live feedback for transfers | `unsupported` |  | M4 | Native untouched. |
| A10 | Wixie Cauldron | attune: Recipe Rehearsal when teaching | `unsupported` |  | M4 | Native untouched. |
| A11 | Spell turrets, runes, prisms | attune: Trajectory Attunement | `unsupported` |  | M4 | Native untouched. |
| A12 | Source Jars (incl. creative) | live feedback: flow and state; optional link attunement | `unsupported` |  | M4 | Native untouched. |
| A13 | Five Sourcelinks | attune: Source Tuning demonstration at commissioning | `unsupported` |  | M4 | Native untouched. |
| A14 | Source relay family | attune: Link Attunement | `unsupported` |  | M4 | Native untouched. |
| A15 | Item Detector, Spell Sensor, Redstone Relay | attune: Signal Alignment | `unsupported` |  | M4 | Native untouched. |
| A16 | Storage Lectern, Bookwyrm, Repository, Archwood Chest | attune: Link Attunement; browsing preserved | `unsupported` |  | M4 | Native untouched. |
| A17 | Magical helpers and homes | attune: Bond/Task Attunement at assignment | `unsupported` |  | M4 | Native untouched. |
| A18 | Scryer's Oculus / Crystal | attune: Lens Alignment on a new link | `unsupported` |  | M4 | Native untouched. |
| A19 | Warp and portal infrastructure | attune: Gate Alignment | `unsupported` |  | M4 | Native untouched. |
| A20 | Familiars and native Binding ritual | attune: Bond Attunement | `unsupported` |  | M3 | Native untouched. |
| A21 | Spellbook authoring and glyph knowledge | preserve + practice inscription | `unsupported` |  | M4 | Native editor untouched; practice not built for this surface. |
| A22 | Magic equipment and ordinary crafting routes | via real station adapters and operation profiles | `unsupported` |  | M2 | Native recipes untouched. |
| A23 | Spell effects and in-world transformations | preserve: native live execution | `native_preserved` |  | M1 | Justified: no performance may interrupt a projectile, block break, crop or automated cast. Optional preparation cues are not built. |
| A24 | Remaining functional, environmental and world content | preserve (building, ecology, loot, world generation); short attunement for configurable devices | `unsupported` |  | M4 | Native untouched; per-registry-entry triage pending. |
| I01 | Scroll Forge (irons_spellbooks:scroll_forge) | perform: Scroll Forging | `performed` | `immersive_spellcraft:irons_scroll_forge` (active)<br>`immersive_spellcraft:irons_scroll_forge` | M1 | Identical menu bytecode in 3.15.0 and 3.16.3. The focus is consumed, as natively. Hopper access to the forge's item handler is native and unguarded; a change to any slot releases the session. |
| I02 | Inscription Table | perform: Binding / Unbinding | `unsupported` |  | M2 | Native untouched. |
| I03 | Arcane Anvil: scroll level upgrade (ink) | perform: Arcane Annealing | `unsupported` |  | M2 | Native untouched. |
| I04 | Arcane Anvil: unique-item spell improvement | perform: Seal Reinforcement | `unsupported` |  | M2 | Native untouched. |
| I05 | Arcane Anvil: weapon/item imbuement | perform: Channel Etching | `unsupported` |  | M2 | Native untouched. |
| I06 | Arcane Anvil: upgrade orbs | perform: Orb Seating | `unsupported` |  | M2 | Native untouched. |
| I07 | Arcane Anvil: Shriving Stone | perform: Unbinding | `unsupported` |  | M2 | Native untouched. |
| I08 | Arcane Anvil: spell-slot upgrades | perform: Page Stitching | `unsupported` |  | M2 | Native untouched. |
| I09 | Arcane Anvil: Affinity Ring attunement | perform: Resonant Attunement | `unsupported` |  | M2 | Native untouched. |
| I10 | Alchemist Cauldron brewing | perform: Confluence Mixing | `unsupported` |  | M2 | Native untouched. |
| I11 | Alchemist Cauldron scroll recycling | perform: Essence Separation (native RNG once) | `unsupported` |  | M2 | Native untouched. |
| I12 | Alchemist Cauldron transfer and blood | live feedback; routine transfer native | `unsupported` |  | M2 | Native untouched. |
| I13 | Remaining registered functional blocks | Gate/Seal Attunement for genuine setup; classify the rest | `unsupported` |  | M4 | Native untouched; audit pending. |
| I14 | Magic gear and normal recipes | native recipes plus explicit finishing profiles | `unsupported` |  | M2 | Native untouched. |
| I15 | Casting and spell management | preserve; optional nonblocking cues and a safe practice mode | `unsupported` |  | M4 | Casting is untouched; the casting cues and casting practice are not built. |
| I16 | Progression and world gameplay | preserve native gates; journal feedback | `unsupported` |  | M5 | Native untouched; journal/tutorial feedback not built. |
| N01 | Glyph Press (ars_n_spells:glyph_press) | perform: Glyph Embossing | `performed` | `immersive_spellcraft:ans_glyph_press` (active)<br>`immersive_spellcraft:ans_glyph_press` | M1 | No second cost, XP, glyph variant or random failure. The performance runs inside the Press's own window. |
| N02 | Glyph Press restrictions and lifecycle | perform (same adapter as N01) | `performed` | `immersive_spellcraft:ans_glyph_press` (active)<br>`immersive_spellcraft:ans_glyph_press` | M1 | A cancelled native job never finishes: the gate only ever delays completion. |
| N03 | Spell Loom export | perform: Spell Interlacing | `unsupported` |  | M3 | Native untouched. |
| N04 | Spell Loom conversion | perform after the native destructive confirmation | `unsupported` |  | M3 | Native untouched. |
| N05 | Spell Loom school and cosmetics | School Attunement (2-3 s); cosmetics instant | `unsupported` |  | M3 | Native untouched. |
| N06 | ANS carrier at Iron's Inscription Table | perform: ANS-aware Page Binding | `unsupported` |  | M3 | Native untouched. |
| N07 | Spellbook Binding ritual (ars_n_spells:spellbook_binding) | Ritual Conducting + Page Binding | `unsupported` |  | M3 | Native untouched. |
| N08 | Spell Transcription ritual | perform: Seal Transfer | `unsupported` |  | M3 | Native untouched. |
| N09 | Spell Uninscription ritual | perform: Unweaving | `unsupported` |  | M3 | Native untouched. |
| N10 | Mana Infusion ritual | perform: Pulse Conducting | `unsupported` |  | M3 | Native untouched. |
| N11 | Mana Well ritual | attune: Well Attunement once | `unsupported` |  | M3 | Native untouched. |
| N12 | Imported Iron's glyphs | registry-driven presentation and practice; casting native | `unsupported` |  | M3 | Casting untouched; presentation/practice not built. |
| N13 | Mana, scaling and progression bridge | read-only feedback from the bridge | `unsupported` |  | M4 | Untouched; no feedback built. |
| N14 | Other integrations and routes | preserve and test | `unsupported` |  | M3 | Untouched by 0.1.0; the route-by-route verification is pending. |

### Native entry and commit points of the implemented operations

- **A01**: PacketSetScribeRecipe -> ScribesTile.setRecipe (native XP charge, refund of a replaced job) -> ScribesTile.tick (ground items within 2 blocks, nearby inventories when auto-take is on) -> 120-tick crafting window -> glyph ItemEntity. Gate: ScribesTile.tick HEAD holds the window's last tick until the inscription is done. Entry routes: manual: a player selects the glyph with a spell book; ingredients may be delivered by hand, by dropping them or from nearby inventories (all native). No automated start exists.
- **A23**: Native spell resolution (every registered glyph). Entry routes: every cast route, including automation
- **I01**: ScrollForgeSelectSpellPacket -> ScrollForgeMenu.setRecipeSpell -> setupResultSlot (preview scroll in tile slot 3) -> result Slot.onTake consumes 1 ink, 1 paper and 1 focus. Gate: AbstractContainerMenu.clicked on the result slot; the performance replays the player's own click once under the commit permit. Entry routes: manual only: pickup, shift-click (native repeat until inputs or space run out), number-key/offhand swap, drop
- **N01**: GlyphPressActionPacket -> GlyphPressRequestHandler.execute -> GlyphPressBlockEntity.start(player) (job id, Source reservation, input locks) -> tickJob (native duration) -> complete(). Gate: complete() HEAD holds a gated job until its starter finished the embossing; the native duration still runs in full. Entry routes: manual start only (the Press refuses automation starts natively); hoppers stage inputs and take the glyph
- **N02**: GlyphPressPlanner refusals are shown by the Press screen before any job (and so before any game); locks, hopper faces, cancel, save/load (gate saved in the press's ForgeData) and reload (mapping digest) stay native. Entry routes: manual start only

## Registered surfaces of the target mods (untriaged inventory)

### `ars_nouveau`

- 56 blocks with a block entity, 122 without; 402 items; 48 entity types.
- Block entity types (52): `ars_nouveau:agronomic_sourcelink`, `ars_nouveau:alchemical_sourcelink`, `ars_nouveau:alteration_table`, `ars_nouveau:arcane_core`, `ars_nouveau:arcane_pedestal`, `ars_nouveau:archwood_chest`, `ars_nouveau:basic_spell_turret`, `ars_nouveau:brazier_relay`, `ars_nouveau:creative_source_jar`, `ars_nouveau:drygmy_stone`, `ars_nouveau:enchanting_apparatus`, `ars_nouveau:falseweave`, `ars_nouveau:ghostweave`, `ars_nouveau:imbuement_chamber`, `ars_nouveau:intangible_air`, `ars_nouveau:item_detector`, `ars_nouveau:light_block`, `ars_nouveau:mage_block`, `ars_nouveau:magelight_torch`, `ars_nouveau:mirrorweave`, `ars_nouveau:mob_jar`, `ars_nouveau:mycelial_sourcelink`, `ars_nouveau:portal`, `ars_nouveau:potion_diffuser`, `ars_nouveau:potion_jar`, `ars_nouveau:potion_melder`, `ars_nouveau:redstone_relay`, `ars_nouveau:relay`, `ars_nouveau:relay_collector`, `ars_nouveau:relay_deposit`, `ars_nouveau:relay_splitter`, `ars_nouveau:relay_warp`, `ars_nouveau:repository`, `ars_nouveau:ritual_brazier`, `ars_nouveau:rotating_spell_turret`, `ars_nouveau:rune`, `ars_nouveau:sconce`, `ars_nouveau:scribes_table`, `ars_nouveau:scryers_crystal`, `ars_nouveau:scryers_oculus`, `ars_nouveau:sky_block`, `ars_nouveau:source_jar`, `ars_nouveau:spell_sensor`, `ars_nouveau:spell_turret`, `ars_nouveau:storage_lectern`, `ars_nouveau:temporary_block`, `ars_nouveau:temporary_light_block`, `ars_nouveau:timer_spell_turret`, `ars_nouveau:vitalic_sourcelink`, `ars_nouveau:volcanic_sourcelink`, `ars_nouveau:whirlisprig_flower`, `ars_nouveau:wixie_cauldron`
- Menu types (1): `ars_nouveau:storage_lectern`
- Recipe types (16): `ars_nouveau:armor_upgrade`, `ars_nouveau:book_upgrade`, `ars_nouveau:budding_conversion`, `ars_nouveau:caster_tome`, `ars_nouveau:crush`, `ars_nouveau:dispel_entity`, `ars_nouveau:dye`, `ars_nouveau:enchanting_apparatus`, `ars_nouveau:enchantment`, `ars_nouveau:glyph`, `ars_nouveau:imbuement`, `ars_nouveau:potion_flask`, `ars_nouveau:reactive_enchantment`, `ars_nouveau:scry_ritual`, `ars_nouveau:spell_write`, `ars_nouveau:summon_ritual`
- Recipe serializers (16): `ars_nouveau:armor_upgrade`, `ars_nouveau:book_upgrade`, `ars_nouveau:budding_conversion`, `ars_nouveau:caster_tome`, `ars_nouveau:crush`, `ars_nouveau:dispel_entity`, `ars_nouveau:dye`, `ars_nouveau:enchanting_apparatus`, `ars_nouveau:enchantment`, `ars_nouveau:glyph`, `ars_nouveau:imbuement`, `ars_nouveau:potion_flask`, `ars_nouveau:reactive_enchantment`, `ars_nouveau:scry_ritual`, `ars_nouveau:spell_write`, `ars_nouveau:summon_ritual`
- Functional blocks: `ars_nouveau:agronomic_sourcelink`, `ars_nouveau:alchemical_sourcelink`, `ars_nouveau:alteration_table`, `ars_nouveau:arcane_core`, `ars_nouveau:arcane_pedestal`, `ars_nouveau:arcane_platform`, `ars_nouveau:archwood_chest`, `ars_nouveau:archwood_sconce`, `ars_nouveau:basic_spell_turret`, `ars_nouveau:brazier_relay`, `ars_nouveau:creative_source_jar`, `ars_nouveau:drygmy_stone`, `ars_nouveau:enchanting_apparatus`, `ars_nouveau:falseweave`, `ars_nouveau:ghostweave`, `ars_nouveau:imbuement_chamber`, `ars_nouveau:intangible_air`, `ars_nouveau:item_detector`, `ars_nouveau:light_block`, `ars_nouveau:mage_block`, `ars_nouveau:magelight_torch`, `ars_nouveau:mirrorweave`, `ars_nouveau:mob_jar`, `ars_nouveau:mycelial_sourcelink`, `ars_nouveau:polished_sconce`, `ars_nouveau:portal`, `ars_nouveau:potion_diffuser`, `ars_nouveau:potion_jar`, `ars_nouveau:potion_melder`, `ars_nouveau:redstone_relay`, `ars_nouveau:relay`, `ars_nouveau:relay_collector`, `ars_nouveau:relay_deposit`, `ars_nouveau:relay_splitter`, `ars_nouveau:relay_warp`, `ars_nouveau:repository`, `ars_nouveau:ritual_brazier`, `ars_nouveau:rotating_spell_turret`, `ars_nouveau:rune`, `ars_nouveau:sconce`, `ars_nouveau:scribes_table`, `ars_nouveau:scryers_crystal`, `ars_nouveau:scryers_oculus`, `ars_nouveau:sky_block`, `ars_nouveau:source_jar`, `ars_nouveau:sourcestone_sconce`, `ars_nouveau:spell_sensor`, `ars_nouveau:spell_turret`, `ars_nouveau:storage_lectern`, `ars_nouveau:temporary_block`, `ars_nouveau:temporary_light_block`, `ars_nouveau:timer_spell_turret`, `ars_nouveau:vitalic_sourcelink`, `ars_nouveau:volcanic_sourcelink`, `ars_nouveau:whirlisprig_flower`, `ars_nouveau:wixie_cauldron`

### `irons_spellbooks`

- 10 blocks with a block entity, 19 without; 234 items; 96 entity types.
- Block entity types (7): `irons_spellbooks:alchemist_cauldron`, `irons_spellbooks:pedestal`, `irons_spellbooks:portal_frame`, `irons_spellbooks:scroll_forge`, `irons_spellbooks:trial_spawner`, `irons_spellbooks:tyros_statue`, `irons_spellbooks:vault`
- Menu types (3): `irons_spellbooks:arcane_anvil_menu`, `irons_spellbooks:inscription_table_menu`, `irons_spellbooks:scroll_forge_menu`
- Recipe types (3): `irons_spellbooks:alchemist_cauldron_brew`, `irons_spellbooks:alchemist_cauldron_empty`, `irons_spellbooks:alchemist_cauldron_fill`
- Recipe serializers (3): `irons_spellbooks:alchemist_cauldron_brew`, `irons_spellbooks:alchemist_cauldron_empty`, `irons_spellbooks:alchemist_cauldron_fill`
- Functional blocks: `irons_spellbooks:alchemist_cauldron`, `irons_spellbooks:bone_vault`, `irons_spellbooks:cinderous_vault`, `irons_spellbooks:pedestal`, `irons_spellbooks:pocket_dimension_portal_frame`, `irons_spellbooks:portal_frame`, `irons_spellbooks:scroll_forge`, `irons_spellbooks:trial_spawner`, `irons_spellbooks:tyros_statue`, `irons_spellbooks:vault`

### `ars_n_spells`

- 2 blocks with a block entity, 0 without; 786 items; 0 entity types.
- Block entity types (2): `ars_n_spells:glyph_press`, `ars_n_spells:spell_loom`
- Menu types (2): `ars_n_spells:glyph_press`, `ars_n_spells:spell_loom`
- Recipe types (0): 
- Recipe serializers (0): 
- Functional blocks: `ars_n_spells:glyph_press`, `ars_n_spells:spell_loom`

### Loaded recipes by type

| Recipe type | Count |
| --- | --- |
| `ars_nouveau:armor_upgrade` | 2 |
| `ars_nouveau:budding_conversion` | 1 |
| `ars_nouveau:caster_tome` | 27 |
| `ars_nouveau:crush` | 26 |
| `ars_nouveau:dispel_entity` | 1 |
| `ars_nouveau:enchanting_apparatus` | 80 |
| `ars_nouveau:enchantment` | 96 |
| `ars_nouveau:glyph` | 81 |
| `ars_nouveau:imbuement` | 13 |
| `ars_nouveau:reactive_enchantment` | 1 |
| `ars_nouveau:scry_ritual` | 12 |
| `ars_nouveau:spell_write` | 1 |
| `ars_nouveau:summon_ritual` | 1 |
| `irons_spellbooks:alchemist_cauldron_brew` | 15 |
| `irons_spellbooks:alchemist_cauldron_empty` | 16 |
| `irons_spellbooks:alchemist_cauldron_fill` | 16 |
| `minecraft:blasting` | 5 |
| `minecraft:campfire_cooking` | 1 |
| `minecraft:crafting` | 375 |
| `minecraft:smelting` | 1 |
| `minecraft:smithing` | 38 |
| `minecraft:smoking` | 1 |
| `minecraft:stonecutting` | 65 |

### Integration inventories

- Ars Nouveau glyphs by namespace: `ars_n_spells` 777, `ars_nouveau` 81; rituals (29): `ars_n_spells:mana_infusion`, `ars_n_spells:mana_well`, `ars_nouveau:ritual_animal_summon`, `ars_nouveau:ritual_awakening`, `ars_nouveau:ritual_binding`, `ars_nouveau:ritual_burrowing`, `ars_nouveau:ritual_challenge`, `ars_nouveau:ritual_cloudshaping`, `ars_nouveau:ritual_conjure_island_desert`, `ars_nouveau:ritual_conjure_island_plains`, `ars_nouveau:ritual_containment`, `ars_nouveau:ritual_disintegration`, `ars_nouveau:ritual_fertility`, `ars_nouveau:ritual_flight`, `ars_nouveau:ritual_flowering`, `ars_nouveau:ritual_forestation`, `ars_nouveau:ritual_gravity`, `ars_nouveau:ritual_harvest`, `ars_nouveau:ritual_moonfall`, `ars_nouveau:ritual_overgrowth`, `ars_nouveau:ritual_restoration`, `ars_nouveau:ritual_sanctuary`, `ars_nouveau:ritual_scrying`, `ars_nouveau:ritual_sunrise`, `ars_nouveau:ritual_warping`, `ars_nouveau:ritual_wilden_summon`, `ars_n_spells:spell_transcription`, `ars_n_spells:spell_uninscription`, `ars_n_spells:spellbook_binding`
- Iron's spells: 119 registered, 119 enabled; schools: `irons_spellbooks:fire`, `irons_spellbooks:ice`, `irons_spellbooks:lightning`, `irons_spellbooks:holy`, `irons_spellbooks:ender`, `irons_spellbooks:blood`, `irons_spellbooks:evocation`, `irons_spellbooks:nature`, `irons_spellbooks:eldritch`
- Ars 'n Spells rituals: `ars_n_spells:mana_infusion`, `ars_n_spells:mana_well`, `ars_n_spells:spell_transcription`, `ars_n_spells:spell_uninscription`, `ars_n_spells:spellbook_binding`; imported Iron's glyphs: 777

The full glyph, spell and recipe-id lists are in `coverage.json`.
