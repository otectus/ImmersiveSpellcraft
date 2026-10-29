package com.otectus.immersivespellcraft.test;

import com.otectus.immersivespellcraft.network.Packets;
import com.otectus.immersivespellcraft.session.SessionManager;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.atomic.AtomicInteger;

/** Iron's-referencing helpers for the Scroll Forge tests; called only behind the Iron's gate. */
final class IronsFixtures {
    static final ResourceLocation FIREBOLT = new ResourceLocation("irons_spellbooks", "firebolt");
    private static final AtomicInteger IDS = new AtomicInteger(700);

    static final class Forge {
        final ScrollForgeTile tile;
        final ScrollForgeMenu menu;
        final ServerPlayer player;
        TestSupport.Driver driver;

        Forge(ScrollForgeTile tile, ScrollForgeMenu menu, ServerPlayer player) {
            this.tile = tile; this.menu = menu; this.player = player;
        }
    }

    /** A focus of the spell's own school, read from Iron's school registry rather than assumed. */
    static Item focus() {
        var tag = SpellRegistry.getSpell(FIREBOLT).getSchoolType().getFocus();
        return ForgeRegistries.ITEMS.tags().getTag(tag).stream().findFirst().orElseThrow();
    }

    /** A forge at {@code pos} holding {@code n} of each input, opened by {@code player} with Firebolt selected. */
    static Forge forge(GameTestHelper h, BlockPos pos, ServerPlayer player, int n) {
        h.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE);
        h.setBlock(pos, BlockRegistry.SCROLL_FORGE_BLOCK.get());
        ScrollForgeTile tile = (ScrollForgeTile) h.getLevel().getBlockEntity(h.absolutePos(pos));
        tile.getItemHandler().setStackInSlot(0, new ItemStack(ItemRegistry.INK_COMMON.get(), n));
        tile.getItemHandler().setStackInSlot(1, new ItemStack(Items.PAPER, n));
        tile.getItemHandler().setStackInSlot(2, new ItemStack(focus(), n));
        ScrollForgeMenu menu = (ScrollForgeMenu) tile.createMenu(IDS.incrementAndGet(), player.getInventory(), player);
        player.containerMenu = menu;
        menu.setRecipeSpell(SpellRegistry.getSpell(FIREBOLT));
        var spell = SpellRegistry.getSpell(FIREBOLT);
        TestSupport.check(menu.getResultSlot().hasItem(), "the forge previews a Firebolt scroll (spell " + spell.getSpellId()
                + ", craftable " + spell.allowCrafting() + ", school " + spell.getSchoolType().getId() + ", focus "
                + ForgeRegistries.ITEMS.getKey(focus()) + " schools " + SchoolRegistry.getSchoolsFromFocus(new ItemStack(focus())).size()
                + ", slots " + tile.getItemHandler().getStackInSlot(0) + "/" + tile.getItemHandler().getStackInSlot(1) + "/"
                + tile.getItemHandler().getStackInSlot(2) + ", menu slot " + menu.getInkSlot().getItem() + ")");
        return new Forge(tile, menu, player);
    }

    static int result(Forge f) {
        return f.menu.getResultSlot().index;
    }

    static int count(Forge f, int slot) {
        return f.tile.getItemHandler().getStackInSlot(slot).getCount();
    }

    static ItemStack preview(Forge f) {
        return f.menu.getResultSlot().getItem().copy();
    }

    static void begin(Forge f, ClickType type) {
        SessionManager.beginFromMenu(f.player, new Packets.Begin(f.menu.containerId, f.menu.getStateId(), result(f), 0, type.ordinal()));
    }

    static boolean isFirebolt(ItemStack scroll) {
        return scroll.is(ItemRegistry.SCROLL.get()) && ISpellContainer.isSpellContainer(scroll)
                && ISpellContainer.get(scroll).getSpellAtIndex(0).getSpell() == SpellRegistry.getSpell(FIREBOLT);
    }

    static int scrollsIn(ServerPlayer player) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(ItemRegistry.SCROLL.get())) n += s.getCount();
        return n;
    }

    private IronsFixtures() {}
}
