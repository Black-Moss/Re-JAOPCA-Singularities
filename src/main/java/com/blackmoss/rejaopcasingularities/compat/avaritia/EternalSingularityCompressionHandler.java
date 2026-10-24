package com.blackmoss.rejaopcasingularities.compat.avaritia;

import com.blackmoss.rejaopcasingularities.CompoundSingularityType;
import com.blackmoss.rejaopcasingularities.ReJAOPCASingularities;
import committee.nova.mods.avaritia.Const;
import committee.nova.mods.avaritia.api.init.event.RegisterRecipesEvent;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia.core.singularity.SingularityReloadListener;
import committee.nova.mods.avaritia.init.registry.ModCreativeModeTabs;
import committee.nova.mods.avaritia.init.registry.ModItems;
import committee.nova.mods.avaritia.util.SingularityUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Comparator;
import java.util.List;

public final class EternalSingularityCompressionHandler {
    public static final int MAX_SINGULARITIES = 81;
    public static final int SINGULARITIES_PER_COMPOUND = 81;

    public static final Identifier ETERNAL_SINGULARITY_RECIPE_ID = Identifier.parse("avaritia:eternal_singularity");
    public static final ResourceKey<Recipe<?>> ETERNAL_SINGULARITY_RECIPE_KEY =
            ResourceKey.create(Registries.RECIPE, ETERNAL_SINGULARITY_RECIPE_ID);

    private static volatile int compoundCount;

    private EternalSingularityCompressionHandler() {
    }

    public static void onRegisterRecipes(RegisterRecipesEvent event) {
        List<Singularity> singularities = SingularityReloadListener.INSTANCE.getAllSingularities().values().stream()
                .sorted(Comparator.comparing(singularity -> singularity.getRegistryName().toString()))
                .toList();
        int count = singularities.size();
        if (count <= MAX_SINGULARITIES) {
            compoundCount = 0;
            return;
        }
        int nameCount = CompoundSingularityType.count();
        int wanted = (count + SINGULARITIES_PER_COMPOUND - 1) / SINGULARITIES_PER_COMPOUND;
        int compounds = Math.min(wanted, nameCount);
        if (wanted > nameCount) {
            ReJAOPCASingularities.LOGGER.warn("{} singularities need {} compound singularities but only {} names are available, using {}",
                    count, wanted, nameCount, nameCount);
        }
        ReJAOPCASingularities.LOGGER.info("Compressing {} singularities into {} compound singularities for the Eternal Singularity recipe",
                count, compounds);
        compoundCount = compounds;

        event.removeRecipe(ETERNAL_SINGULARITY_RECIPE_KEY);
        NonNullList<Ingredient> eternalInputs = NonNullList.create();
        for (int compound = 0; compound < compounds; compound++) {
            int from = compound * SINGULARITIES_PER_COMPOUND;
            int to = Math.min(from + SINGULARITIES_PER_COMPOUND, count);
            NonNullList<Ingredient> inputs = NonNullList.create();
            for (Singularity singularity : singularities.subList(from, to)) {
                inputs.add(Const.getStackIngredient(SingularityUtils.getItemForSingularity(singularity)));
            }
            ItemStack compressed = ReJAOPCASingularities.compoundSingularity(compound);
            event.addRecipe(new RecipeHolder<>(
                    ResourceKey.create(Registries.RECIPE, ReJAOPCASingularities.identifier("compressed_singularity_" + compound)),
                    new ShapelessTableCraftingRecipe(inputs, compressed, 4)));
            eternalInputs.add(Const.getStackIngredient(compressed));
        }
        event.addRecipe(new RecipeHolder<>(ETERNAL_SINGULARITY_RECIPE_KEY,
                new ShapelessTableCraftingRecipe(eternalInputs, new ItemStack(ModItems.eternal_singularity.get()), 4)));
    }

    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(ModCreativeModeTabs.SINGULARITY_CREATIVE_TAB.getKey())) {
            return;
        }
        for (int compound = 0; compound < compoundCount; compound++) {
            event.accept(ReJAOPCASingularities.compoundSingularity(compound));
        }
    }

    public static void onItemToolTip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ReJAOPCASingularities.compoundSingularity(CompoundSingularityType.EGREGIOUS).getItem())) {
            event.getToolTip().add(Component.translatable(CompoundSingularityType.EGREGIOUS.getTooltipKey()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
