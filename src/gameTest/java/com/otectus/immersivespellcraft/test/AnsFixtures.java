package com.otectus.immersivespellcraft.test;

import com.otectus.arsnspells.block.GlyphPressBlockEntity;
import com.otectus.arsnspells.menu.GlyphPressMenu;
import com.otectus.arsnspells.registry.ModBlocksRegistry;
import com.otectus.arsnspells.registry.ModItemsRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Ars 'n Spells and Iron's-referencing helpers for the Glyph Press tests; called only behind both gates. */
final class AnsFixtures {
    static final BlockPos PRESS = new BlockPos(2, 2, 2);
    private static final AtomicInteger IDS = new AtomicInteger(800);

    static final class Press {
        final GameTestHelper h;
        final GlyphPressBlockEntity press;
        long startedAt = -1, finishedAt = -1;
        int sourceBefore, gemsBefore, platesBefore;
        TestSupport.Driver driver;

        Press(GameTestHelper h, GlyphPressBlockEntity press) { this.h = h; this.press = press; }
    }

    /** A loaded press: one {@code spell} scroll at {@code level}, two plates, {@code gems} Source gems and 4,000 Source. */
    static Press press(GameTestHelper h, String spell, int level, int gems) {
        h.setBlock(PRESS.below(), net.minecraft.world.level.block.Blocks.STONE);
        h.setBlock(PRESS, ModBlocksRegistry.GLYPH_PRESS.get());
        GlyphPressBlockEntity press = (GlyphPressBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(PRESS));
        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.set(scroll, ISpellContainer.createScrollContainer(SpellRegistry.getSpell(new ResourceLocation(spell)), level, scroll));
        press.getItems().setStackInSlot(GlyphPressBlockEntity.SLOT_SCROLL, scroll);
        press.getItems().setStackInSlot(GlyphPressBlockEntity.SLOT_PLATE, new ItemStack(ModItemsRegistry.blankGlyphPlate().get(), 2));
        press.getItems().setStackInSlot(GlyphPressBlockEntity.SLOT_CATALYST,
                new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ars_nouveau", "source_gem")), gems));
        press.addSource(4000);
        Press p = new Press(h, press);
        p.sourceBefore = press.getSource();
        p.gemsBefore = gems;
        p.platesBefore = 2;
        return p;
    }

    static GlyphPressMenu open(Press p, ServerPlayer player) {
        GlyphPressMenu menu = new GlyphPressMenu(IDS.incrementAndGet(), player.getInventory(), p.press, UUID.randomUUID());
        player.containerMenu = menu;
        return menu;
    }

    static String start(Press p, ServerPlayer player) {
        String result = p.press.start(player);
        p.startedAt = p.h.getLevel().getGameTime();
        return result;
    }

    static boolean working(Press p) { return p.press.working(); }
    static int progress(Press p) { return p.press.progress(); }
    static int duration(Press p) { return p.press.duration(); }
    static int source(Press p) { return p.press.getSource(); }
    static int count(Press p, int slot) { return p.press.getItems().getStackInSlot(slot).getCount(); }
    static ItemStack output(Press p) { return p.press.getItems().getStackInSlot(GlyphPressBlockEntity.SLOT_OUTPUT); }
    static int slotScroll() { return GlyphPressBlockEntity.SLOT_SCROLL; }
    static int slotPlate() { return GlyphPressBlockEntity.SLOT_PLATE; }
    static int slotCatalyst() { return GlyphPressBlockEntity.SLOT_CATALYST; }
    static void cancel(Press p) { p.press.cancel("player"); }
    static UUID job(Press p) { return p.press.jobId(); }

    static boolean gated(GlyphPressBlockEntity press) {
        return press.getPersistentData().contains("immersive_spellcraft");
    }

    static boolean gated(Press p) {
        return gated(p.press);
    }

    /** A save and load of the press into a fresh block entity keeps the gate under the same job. */
    static boolean gateSurvivesReload(Press p) {
        CompoundTag saved = p.press.saveWithFullMetadata();
        GlyphPressBlockEntity copy = new GlyphPressBlockEntity(p.press.getBlockPos(), p.press.getBlockState());
        copy.load(saved);
        return gated(copy) && p.press.jobId().equals(copy.jobId())
                && copy.getPersistentData().getCompound("immersive_spellcraft").getUUID("job").equals(p.press.jobId());
    }

    private AnsFixtures() {}
}
