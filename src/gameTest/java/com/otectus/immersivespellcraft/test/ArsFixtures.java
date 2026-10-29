package com.otectus.immersivespellcraft.test;

import com.hollingsworth.arsnouveau.common.block.TableBlock;
import com.hollingsworth.arsnouveau.common.block.ThreePartBlock;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.otectus.immersivespellcraft.compat.ars.ScribeGate;
import com.otectus.immersivespellcraft.compat.ars.ScribeGateHolder;
import com.otectus.immersivespellcraft.core.AdapterRegistry;
import com.otectus.immersivespellcraft.core.MagicOperationAdapter;
import com.otectus.immersivespellcraft.core.OperationContext;
import com.otectus.immersivespellcraft.core.Operations;
import com.otectus.immersivespellcraft.session.SessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Ars-referencing helpers for the Scribe's Table tests; called only behind the Ars gate. */
final class ArsFixtures {
    static final BlockPos FOOT = new BlockPos(1, 2, 2), HEAD = FOOT.east();

    static final class Table {
        final GameTestHelper h;
        final ScribesTile tile;
        final GlyphRecipe recipe;
        final ServerPlayer player;
        int xpAfterCharge;
        TestSupport.Driver driver;

        Table(GameTestHelper h, ScribesTile tile, GlyphRecipe recipe, ServerPlayer player) {
            this.h = h; this.tile = tile; this.recipe = recipe; this.player = player;
        }
    }

    /** A table at FOOT/HEAD, a player with 1,000 XP points, and a started job with all its ingredients delivered. */
    static Table job(GameTestHelper h, String recipeId, String name) {
        TestSupport.floor(h);
        BlockState foot = BlockRegistry.SCRIBES_BLOCK.get().defaultBlockState().setValue(TableBlock.FACING, Direction.EAST)
                .setValue(TableBlock.PART, ThreePartBlock.FOOT);
        h.setBlock(FOOT, foot);
        h.setBlock(HEAD, foot.setValue(TableBlock.PART, ThreePartBlock.HEAD));
        ScribesTile tile = (ScribesTile) h.getLevel().getBlockEntity(h.absolutePos(HEAD));
        GlyphRecipe recipe = (GlyphRecipe) h.getLevel().getRecipeManager().byKey(new ResourceLocation(recipeId)).orElseThrow();
        ServerPlayer player = TestSupport.player(h, name, HEAD.south());
        player.giveExperiencePoints(1000);
        Table t = new Table(h, tile, recipe, player);
        int before = ScribesTile.getTotalPlayerExperience(player);
        tile.setRecipe(recipe, player);
        t.xpAfterCharge = ScribesTile.getTotalPlayerExperience(player);
        TestSupport.check(before - t.xpAfterCharge == recipe.exp, "Ars charges the recipe's " + recipe.exp + " XP points once at selection, "
                + "charged " + (before - t.xpAfterCharge));
        for (Ingredient ingredient : recipe.inputs) {
            ItemStack stack = ingredient.getItems()[0].copy();
            stack.setCount(1);
            TestSupport.check(tile.consumeStack(stack), "the table accepts " + stack);
        }
        return t;
    }

    static int gate(Table t) {
        return ((ScribeGateHolder) t.tile).immersivespellcraft$gateState();
    }

    static boolean awaiting(Table t) {
        return t.tile.crafting && gate(t) == ScribeGate.AWAITING;
    }

    static boolean crafting(Table t) {
        return t.tile.crafting && t.tile.recipe != null;
    }

    static int drops(Table t, Item item) {
        BlockPos at = t.h.absolutePos(HEAD);
        List<ItemEntity> items = t.h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4));
        return items.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    static int glyphs(Table t) {
        return drops(t, t.recipe.output.getItem());
    }

    static int orbXp(Table t) {
        BlockPos at = t.h.absolutePos(HEAD);
        return t.h.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(at).inflate(4)).stream().mapToInt(ExperienceOrb::getValue).sum();
    }

    static Item ingredient(Table t, int index) {
        return t.recipe.inputs.get(index).getItems()[0].getItem();
    }

    static int xp(Table t) {
        return ScribesTile.getTotalPlayerExperience(t.player);
    }

    static MagicOperationAdapter adapter() {
        return AdapterRegistry.byId(Operations.ARS_SCRIBE_ADAPTER).orElseThrow();
    }

    static SessionManager.BeginResult begin(Table t, ServerPlayer player) {
        return SessionManager.begin(player, OperationContext.block(player, t.h.absolutePos(FOOT)), adapter());
    }

    static void breakTable(Table t) {
        t.player.gameMode.destroyBlock(t.h.absolutePos(HEAD));
    }

    private ArsFixtures() {}
}
