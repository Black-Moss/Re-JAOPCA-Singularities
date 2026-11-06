package com.blackmoss.rejaopcasingularities.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Stream;

public class NbtIngredient extends Ingredient {
    private final ItemStack stack;

    public NbtIngredient(ItemStack stack) {
        super(Stream.empty());
        this.stack = stack;
    }

    @Override
    public ItemStack @NotNull [] getItems() {
        return new ItemStack[]{stack};
    }

    @Override
    public boolean test(ItemStack candidate) {
        return !Objects.requireNonNull(candidate).isEmpty() && ItemStack.isSameItemSameTags(candidate, stack);
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public @NotNull IntList getStackingIds() {
        return new IntArrayList(new int[]{StackedContents.getStackingIndex(stack)});
    }
}
