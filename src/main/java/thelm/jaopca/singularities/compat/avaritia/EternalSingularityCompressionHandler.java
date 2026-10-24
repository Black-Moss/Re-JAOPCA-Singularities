package thelm.jaopca.singularities.compat.avaritia;

import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import committee.nova.mods.avaritia.Const;
import committee.nova.mods.avaritia.api.init.event.RegisterRecipesEvent;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia.core.singularity.SingularityReloadListener;
import committee.nova.mods.avaritia.init.registry.ModItems;
import committee.nova.mods.avaritia.util.SingularityUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import thelm.jaopca.singularities.ReJAOPCASingularities;

public final class EternalSingularityCompressionHandler {
    public static final int MAX_SINGULARITIES = 81;
    public static final int SINGULARITIES_PER_COMPOUND = 9;

    public static final Identifier ETERNAL_SINGULARITY_RECIPE_ID = Identifier.parse("avaritia:eternal_singularity");
    public static final ResourceKey<Recipe<?>> ETERNAL_SINGULARITY_RECIPE_KEY =
            ResourceKey.create(Registries.RECIPE, ETERNAL_SINGULARITY_RECIPE_ID);

    private EternalSingularityCompressionHandler() {}

    public static void onRegisterRecipes(RegisterRecipesEvent event) {
        List<Singularity> singularities = SingularityReloadListener.INSTANCE.getAllSingularities().values().stream()
                .sorted(Comparator.comparing(singularity -> singularity.getRegistryName().toString()))
                .toList();
        int count = singularities.size();
        if (count <= MAX_SINGULARITIES) {
            return;
        }
        int compounds = (count + SINGULARITIES_PER_COMPOUND - 1) / SINGULARITIES_PER_COMPOUND;
        int nameCount = ReJAOPCASingularities.COMPOUND_SINGULARITY_NAMES.size();
        if (compounds > nameCount) {
            ReJAOPCASingularities.LOGGER.warn("{} singularities need {} compound singularities but only {} names are available, using {}",
                    count, compounds, nameCount, nameCount);
            compounds = nameCount;
        }
        ReJAOPCASingularities.LOGGER.info("Compressing {} singularities into {} compound singularities for the Eternal Singularity recipe",
                count, compounds);
        event.removeRecipe(ETERNAL_SINGULARITY_RECIPE_KEY);
        NonNullList<Ingredient> eternalInputs = NonNullList.create();
        for (int compound = 0; compound < compounds; compound++) {
            NonNullList<Ingredient> inputs = NonNullList.create();
            for (int i = 0; i < SINGULARITIES_PER_COMPOUND; i++) {
                int index = compound * SINGULARITIES_PER_COMPOUND + i;
                if (index >= count) {
                    break;
                }
                inputs.add(Const.getStackIngredient(SingularityUtils.getItemForSingularity(singularities.get(index))));
            }
            ItemStack compressed = ReJAOPCASingularities.compoundSingularity(compound);
            event.addRecipe(new RecipeHolder<>(
                    ResourceKey.create(Registries.RECIPE, ReJAOPCASingularities.identifier("compressed_singularity_" + compound)),
                    new ShapelessTableCraftingRecipe(inputs, compressed, 1)));
            eternalInputs.add(Const.getStackIngredient(compressed));
        }
        event.addRecipe(new RecipeHolder<>(ETERNAL_SINGULARITY_RECIPE_KEY,
                new ShapelessTableCraftingRecipe(eternalInputs, new ItemStack(ModItems.eternal_singularity.get()), 4)));
    }
}
