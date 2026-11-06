package com.blackmoss.rejaopcasingularities.compat.avaritia.recipes;

import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia.init.registry.ModRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompoundCompressionRecipe extends ShapelessTableCraftingRecipe {
    private final List<ItemStack> expected;

    public CompoundCompressionRecipe(
            ResourceLocation recipeId,
            NonNullList<Ingredient> inputs,
            ItemStack output,
            List<ItemStack> expected) {
        super(recipeId, inputs, output, 4);
        this.expected = expected;
    }

    @Override
    public boolean matches(@NotNull IItemHandler inventory) {
        if (inventory.getSlots() < this.expected.size()) {
            return false;
        }
        boolean[] found = new boolean[this.expected.size()];
        int validItems = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ++validItems;
            boolean matched = false;
            for (int j = 0; j < this.expected.size(); j++) {
                if (!found[j] && ItemStack.isSameItemSameTags(stack, this.expected.get(j))) {
                    found[j] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        if (validItems != this.expected.size()) {
            return false;
        }
        for (boolean value : found) {
            if (!value) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matches(@NotNull Container inventory, @NotNull Level level) {
        return this.matches(new InvWrapper(inventory));
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SHAPELESS_CRAFT_SERIALIZER.get();
    }
}
