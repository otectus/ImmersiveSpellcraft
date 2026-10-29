package com.otectus.immersivespellcraft.data;

import java.util.List;

/**
 * Every operation group the specification requires (A01-A24 Ars Nouveau, I01-I16 Iron's, N01-N14 Ars 'n Spells),
 * with this release's honest status. {@code unsupported} is a release gap, never a synonym for preserved: it means the
 * native behavior is untouched but the required treatment is not built yet, and names the milestone that builds it.
 */
public final class CoverageCatalog {
    public enum Status { PERFORMED, ATTUNED, NATIVE_PRESERVED, UNAVAILABLE_IN_THIS_VERSION, UNSUPPORTED }

    public record Row(String id, String integration, String system, String target, Status status, String adapter,
                      String profile, String entry, String routes, String milestone, String notes) {}

    private static Row gap(String id, String integration, String system, String target, String milestone, String notes) {
        return new Row(id, integration, system, target, Status.UNSUPPORTED, "", "", "", "", milestone, notes);
    }

    public static final List<Row> ROWS = List.of(
            new Row("A01", "ars_nouveau", "Scribe's Table glyph crafting (ars_nouveau:scribes_table, recipe type ars_nouveau:glyph)",
                    "perform: Sigil Inscription", Status.PERFORMED, "immersive_spellcraft:ars_scribe",
                    "immersive_spellcraft:ars_glyph_craft",
                    "PacketSetScribeRecipe -> ScribesTile.setRecipe (native XP charge, refund of a replaced job) -> ScribesTile.tick "
                            + "(ground items within 2 blocks, nearby inventories when auto-take is on) -> 120-tick crafting window -> "
                            + "glyph ItemEntity. Gate: ScribesTile.tick HEAD holds the window's last tick until the inscription is done.",
                    "manual: a player selects the glyph with a spell book; ingredients may be delivered by hand, by dropping them or "
                            + "from nearby inventories (all native). No automated start exists.",
                    "M1", "Glyph creation only. Learning the glyph (using the glyph item) stays native. XP is charged once, natively, "
                            + "at selection; the performance never charges or refunds XP."),
            gap("A02", "ars_nouveau", "Scribe's Table inscription (every IScribeable)", "perform: Sigil Inscription (copy/seal)", "M2",
                    "Native onScribe untouched; per-implementation audit of IScribeable pending."),
            gap("A03", "ars_nouveau", "Imbuement Chamber", "perform: Source Tuning (attended job)", "M2", "Native imbuement untouched."),
            gap("A04", "ars_nouveau", "Enchanting Apparatus, Arcane Core, Arcane Pedestals", "perform: Constellation Assembly", "M2",
                    "Native attemptCraft untouched."),
            gap("A05", "ars_nouveau", "Apparatus special recipes (enchantment, reactive, spell_write, armor_upgrade)",
                    "perform: Binding / Sigil Inscription / Thread Weaving", "M2", "Native untouched."),
            gap("A06", "ars_nouveau", "Alteration Table", "perform: Thread Weaving / Unpicking", "M2", "Native untouched."),
            gap("A07", "ars_nouveau", "Ritual Brazier and Brazier Relay", "perform: Ritual Conducting (player activation)", "M3",
                    "Native rituals untouched."),
            gap("A08", "ars_nouveau", "Potion Melder", "perform: Confluence Mixing (attended merge / first setup)", "M4", "Native untouched."),
            gap("A09", "ars_nouveau", "Potion Jar and Potion Diffuser", "attune: Flow Attunement; live feedback for transfers", "M4",
                    "Native untouched."),
            gap("A10", "ars_nouveau", "Wixie Cauldron", "attune: Recipe Rehearsal when teaching", "M4", "Native untouched."),
            gap("A11", "ars_nouveau", "Spell turrets, runes, prisms", "attune: Trajectory Attunement", "M4", "Native untouched."),
            gap("A12", "ars_nouveau", "Source Jars (incl. creative)", "live feedback: flow and state; optional link attunement", "M4",
                    "Native untouched."),
            gap("A13", "ars_nouveau", "Five Sourcelinks", "attune: Source Tuning demonstration at commissioning", "M4", "Native untouched."),
            gap("A14", "ars_nouveau", "Source relay family", "attune: Link Attunement", "M4", "Native untouched."),
            gap("A15", "ars_nouveau", "Item Detector, Spell Sensor, Redstone Relay", "attune: Signal Alignment", "M4", "Native untouched."),
            gap("A16", "ars_nouveau", "Storage Lectern, Bookwyrm, Repository, Archwood Chest", "attune: Link Attunement; browsing preserved",
                    "M4", "Native untouched."),
            gap("A17", "ars_nouveau", "Magical helpers and homes", "attune: Bond/Task Attunement at assignment", "M4", "Native untouched."),
            gap("A18", "ars_nouveau", "Scryer's Oculus / Crystal", "attune: Lens Alignment on a new link", "M4", "Native untouched."),
            gap("A19", "ars_nouveau", "Warp and portal infrastructure", "attune: Gate Alignment", "M4", "Native untouched."),
            gap("A20", "ars_nouveau", "Familiars and native Binding ritual", "attune: Bond Attunement", "M3", "Native untouched."),
            gap("A21", "ars_nouveau", "Spellbook authoring and glyph knowledge", "preserve + practice inscription", "M4",
                    "Native editor untouched; practice not built for this surface."),
            gap("A22", "ars_nouveau", "Magic equipment and ordinary crafting routes", "via real station adapters and operation profiles",
                    "M2", "Native recipes untouched."),
            new Row("A23", "ars_nouveau", "Spell effects and in-world transformations", "preserve: native live execution",
                    Status.NATIVE_PRESERVED, "", "", "Native spell resolution (every registered glyph).",
                    "every cast route, including automation", "M1",
                    "Justified: no performance may interrupt a projectile, block break, crop or automated cast. Optional "
                            + "preparation cues are not built."),
            gap("A24", "ars_nouveau", "Remaining functional, environmental and world content",
                    "preserve (building, ecology, loot, world generation); short attunement for configurable devices", "M4",
                    "Native untouched; per-registry-entry triage pending."),

            new Row("I01", "irons_spellbooks", "Scroll Forge (irons_spellbooks:scroll_forge)", "perform: Scroll Forging",
                    Status.PERFORMED, "immersive_spellcraft:irons_scroll_forge", "immersive_spellcraft:irons_scroll_forge",
                    "ScrollForgeSelectSpellPacket -> ScrollForgeMenu.setRecipeSpell -> setupResultSlot (preview scroll in tile slot 3) "
                            + "-> result Slot.onTake consumes 1 ink, 1 paper and 1 focus. Gate: AbstractContainerMenu.clicked on the "
                            + "result slot; the performance replays the player's own click once under the commit permit.",
                    "manual only: pickup, shift-click (native repeat until inputs or space run out), number-key/offhand swap, drop",
                    "M1", "Identical menu bytecode in 3.15.0 and 3.16.3. The focus is consumed, as natively. Hopper access to the "
                            + "forge's item handler is native and unguarded; a change to any slot releases the session."),
            gap("I02", "irons_spellbooks", "Inscription Table", "perform: Binding / Unbinding", "M2", "Native untouched."),
            gap("I03", "irons_spellbooks", "Arcane Anvil: scroll level upgrade (ink)", "perform: Arcane Annealing", "M2", "Native untouched."),
            gap("I04", "irons_spellbooks", "Arcane Anvil: unique-item spell improvement", "perform: Seal Reinforcement", "M2", "Native untouched."),
            gap("I05", "irons_spellbooks", "Arcane Anvil: weapon/item imbuement", "perform: Channel Etching", "M2", "Native untouched."),
            gap("I06", "irons_spellbooks", "Arcane Anvil: upgrade orbs", "perform: Orb Seating", "M2", "Native untouched."),
            gap("I07", "irons_spellbooks", "Arcane Anvil: Shriving Stone", "perform: Unbinding", "M2", "Native untouched."),
            gap("I08", "irons_spellbooks", "Arcane Anvil: spell-slot upgrades", "perform: Page Stitching", "M2", "Native untouched."),
            gap("I09", "irons_spellbooks", "Arcane Anvil: Affinity Ring attunement", "perform: Resonant Attunement", "M2", "Native untouched."),
            gap("I10", "irons_spellbooks", "Alchemist Cauldron brewing", "perform: Confluence Mixing", "M2", "Native untouched."),
            gap("I11", "irons_spellbooks", "Alchemist Cauldron scroll recycling", "perform: Essence Separation (native RNG once)", "M2",
                    "Native untouched."),
            gap("I12", "irons_spellbooks", "Alchemist Cauldron transfer and blood", "live feedback; routine transfer native", "M2",
                    "Native untouched."),
            gap("I13", "irons_spellbooks", "Remaining registered functional blocks", "Gate/Seal Attunement for genuine setup; classify the rest",
                    "M4", "Native untouched; audit pending."),
            gap("I14", "irons_spellbooks", "Magic gear and normal recipes", "native recipes plus explicit finishing profiles", "M2",
                    "Native untouched."),
            gap("I15", "irons_spellbooks", "Casting and spell management", "preserve; optional nonblocking cues and a safe practice mode",
                    "M4", "Casting is untouched; the casting cues and casting practice are not built."),
            gap("I16", "irons_spellbooks", "Progression and world gameplay", "preserve native gates; journal feedback", "M5",
                    "Native untouched; journal/tutorial feedback not built."),

            new Row("N01", "ars_n_spells", "Glyph Press (ars_n_spells:glyph_press)", "perform: Glyph Embossing", Status.PERFORMED,
                    "immersive_spellcraft:ans_glyph_press", "immersive_spellcraft:ans_glyph_press",
                    "GlyphPressActionPacket -> GlyphPressRequestHandler.execute -> GlyphPressBlockEntity.start(player) (job id, "
                            + "Source reservation, input locks) -> tickJob (native duration) -> complete(). Gate: complete() HEAD holds a "
                            + "gated job until its starter finished the embossing; the native duration still runs in full.",
                    "manual start only (the Press refuses automation starts natively); hoppers stage inputs and take the glyph",
                    "M1", "No second cost, XP, glyph variant or random failure. The performance runs inside the Press's own window."),
            new Row("N02", "ars_n_spells", "Glyph Press restrictions and lifecycle", "perform (same adapter as N01)", Status.PERFORMED,
                    "immersive_spellcraft:ans_glyph_press", "immersive_spellcraft:ans_glyph_press",
                    "GlyphPressPlanner refusals are shown by the Press screen before any job (and so before any game); locks, hopper "
                            + "faces, cancel, save/load (gate saved in the press's ForgeData) and reload (mapping digest) stay native.",
                    "manual start only", "M1", "A cancelled native job never finishes: the gate only ever delays completion."),
            gap("N03", "ars_n_spells", "Spell Loom export", "perform: Spell Interlacing", "M3", "Native untouched."),
            gap("N04", "ars_n_spells", "Spell Loom conversion", "perform after the native destructive confirmation", "M3", "Native untouched."),
            gap("N05", "ars_n_spells", "Spell Loom school and cosmetics", "School Attunement (2-3 s); cosmetics instant", "M3", "Native untouched."),
            gap("N06", "ars_n_spells", "ANS carrier at Iron's Inscription Table", "perform: ANS-aware Page Binding", "M3", "Native untouched."),
            gap("N07", "ars_n_spells", "Spellbook Binding ritual (ars_n_spells:spellbook_binding)", "Ritual Conducting + Page Binding", "M3",
                    "Native untouched."),
            gap("N08", "ars_n_spells", "Spell Transcription ritual", "perform: Seal Transfer", "M3", "Native untouched."),
            gap("N09", "ars_n_spells", "Spell Uninscription ritual", "perform: Unweaving", "M3", "Native untouched."),
            gap("N10", "ars_n_spells", "Mana Infusion ritual", "perform: Pulse Conducting", "M3", "Native untouched."),
            gap("N11", "ars_n_spells", "Mana Well ritual", "attune: Well Attunement once", "M3", "Native untouched."),
            gap("N12", "ars_n_spells", "Imported Iron's glyphs", "registry-driven presentation and practice; casting native", "M3",
                    "Casting untouched; presentation/practice not built."),
            gap("N13", "ars_n_spells", "Mana, scaling and progression bridge", "read-only feedback from the bridge", "M4",
                    "Untouched; no feedback built."),
            gap("N14", "ars_n_spells", "Other integrations and routes", "preserve and test", "M3",
                    "Untouched by 0.1.0; the route-by-route verification is pending."));

    public static long count(Status status) {
        return ROWS.stream().filter(r -> r.status() == status).count();
    }

    private CoverageCatalog() {}
}
