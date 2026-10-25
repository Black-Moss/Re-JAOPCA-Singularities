package com.blackmoss.rejaopcasingularities.compat.avaritia;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import committee.nova.mods.avaritia.Const;
import committee.nova.mods.avaritia.api.init.event.RegisterRecipesEvent;
import committee.nova.mods.avaritia.common.crafting.recipe.CompressorRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapelessTableCraftingRecipe;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia.core.singularity.SingularityReloadListener;
import committee.nova.mods.avaritia.init.registry.ModCreativeModeTabs;
import committee.nova.mods.avaritia.init.registry.ModItems;
import committee.nova.mods.avaritia.util.SingularityUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import com.blackmoss.rejaopcasingularities.CompoundSingularityType;
import com.blackmoss.rejaopcasingularities.ReJAOPCASingularities;

public final class EternalSingularityCompressionHandler {
    public static final int MAX_SINGULARITIES = 81;
    public static final int SINGULARITIES_PER_COMPOUND = 81;

    public static final ResourceLocation ETERNAL_SINGULARITY_RECIPE_ID =
            ResourceLocation.fromNamespaceAndPath("avaritia", "eternal_singularity");

    public static final Map<String, Candidate> CANDIDATES = new TreeMap<>();

    public static final TreeSet<String> USABLE = new TreeSet<>();

    public record Candidate(ResourceLocation tag, int count, int timeCost, int overlayColor, int underlayColor, String displayName) {
    }

    private static final Field RECIPES_FIELD;

    static {
        Field field = null;
        try {
            field = RegisterRecipesEvent.class.getDeclaredField("recipes");
            field.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            ReJAOPCASingularities.LOGGER.error("Unable to access RegisterRecipesEvent recipes", e);
        }
        RECIPES_FIELD = field;
    }

    private static volatile int compoundCount;

    private EternalSingularityCompressionHandler() {
    }

    public static void onRegisterRecipes(RegisterRecipesEvent event) {
        USABLE.clear();
        for (Map.Entry<String, Candidate> entry : CANDIDATES.entrySet()) {
            if (tagSize(entry.getValue().tag()) > 0) {
                USABLE.add(entry.getKey());
            } else {
                ReJAOPCASingularities.LOGGER.debug("Material {} has no items in tag {}, skipping singularity",
                        entry.getKey(), entry.getValue().tag());
            }
        }
        ReJAOPCASingularities.LOGGER.info("{} of {} JAOPCA materials have items in their tags",
                USABLE.size(), CANDIDATES.size());

        injectCompressorRecipes(event);

        List<Singularity> singularities = new java.util.ArrayList<>();
        for (String name : USABLE) {
            singularities.add(candidateSingularity(name, CANDIDATES.get(name)));
        }
        for (Singularity singularity : SingularityReloadListener.INSTANCE.getAllSingularities().values()) {
            Ingredient ingredient = singularity.getIngredient();
            if (ingredient != Ingredient.EMPTY && !ingredient.isEmpty()) {
                singularities.add(singularity);
            }
        }
        singularities.sort(Comparator.comparing(singularity -> singularity.getRegistryName().toString()));

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

        removeRecipes(event, holder -> holder.id().equals(ETERNAL_SINGULARITY_RECIPE_ID));
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
                    ReJAOPCASingularities.id("compressed_singularity_" + compound),
                    new ShapelessTableCraftingRecipe(inputs, compressed, 4)));
            eternalInputs.add(Const.getStackIngredient(compressed));
        }
        event.addRecipe(new RecipeHolder<>(ETERNAL_SINGULARITY_RECIPE_ID,
                new ShapelessTableCraftingRecipe(eternalInputs, new ItemStack(ModItems.eternal_singularity.get()), 4)));
    }

    private static void injectCompressorRecipes(RegisterRecipesEvent event) {
        removeRecipes(event, holder -> isOurCompressorRecipe(holder.id()));
        int added = 0;
        for (String name : USABLE) {
            Candidate candidate = CANDIDATES.get(name);
            Ingredient ingredient = Ingredient.of(TagKey.create(Registries.ITEM, candidate.tag()));
            Singularity singularity = candidateSingularity(name, candidate);
            event.addRecipe(new RecipeHolder<>(ReJAOPCASingularities.id(name + "_singularity"),
                    new CompressorRecipe(ingredient, SingularityUtils.getItemForSingularity(singularity),
                            candidate.count(), candidate.timeCost())));
            ++added;
        }
        ReJAOPCASingularities.LOGGER.info("Injected {} compressor recipes for JAOPCA singularities", added);
    }

    private static Singularity candidateSingularity(String name, Candidate candidate) {
        Singularity singularity = new Singularity(ReJAOPCASingularities.id(name));
        singularity.setCount(candidate.count());
        if (candidate.timeCost() > 0) {
            singularity.setTimeCost(candidate.timeCost());
        }
        return singularity;
    }

    private static boolean isOurCompressorRecipe(ResourceLocation id) {
        if (!id.getNamespace().equals(ReJAOPCASingularities.MODID)) {
            return false;
        }
        String path = id.getPath();
        return path.endsWith("_singularity") && CANDIDATES.containsKey(path.substring(0, path.length() - "_singularity".length()));
    }

    private static void removeRecipes(RegisterRecipesEvent event, java.util.function.Predicate<RecipeHolder<?>> predicate) {
        if (RECIPES_FIELD == null) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            List<RecipeHolder<?>> recipes = (List<RecipeHolder<?>>) RECIPES_FIELD.get(event);
            recipes.removeIf(predicate);
        } catch (IllegalAccessException e) {
            ReJAOPCASingularities.LOGGER.error("Unable to modify the recipe list", e);
        }
    }

    public static int tagSize(ResourceLocation tagLocation) {
        int size = 0;
        for (var ignored : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tagLocation))) {
            ++size;
        }
        return size;
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
            event.getToolTip().add(Component.translatable(CompoundSingularityType.EGREGIOUS.getTooltipKey())
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
