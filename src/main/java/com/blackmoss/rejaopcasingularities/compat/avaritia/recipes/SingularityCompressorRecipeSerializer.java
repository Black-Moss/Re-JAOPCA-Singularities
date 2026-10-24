package com.blackmoss.rejaopcasingularities.compat.avaritia.recipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import net.minecraft.resources.Identifier;
import thelm.jaopca.api.recipes.IRecipeSerializer;

import java.util.Objects;

public class SingularityCompressorRecipeSerializer implements IRecipeSerializer {
    private static final String SINGULARITY_ITEM = "avaritia:singularity";
    private static final String SINGULARITY_ID_COMPONENT = "avaritia:singularity_id";

    private final Identifier singularityId;
    private final Identifier ingredientTag;
    private final Singularity singularity;

    public SingularityCompressorRecipeSerializer(Identifier singularityId, Identifier ingredientTag, Singularity singularity) {
        this.singularityId = Objects.requireNonNull(singularityId);
        this.ingredientTag = Objects.requireNonNull(ingredientTag);
        this.singularity = Objects.requireNonNull(singularity);
    }

    @Override
    public JsonElement get() {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "avaritia:compressor");

        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("tag", ingredientTag.toString());
        recipe.add("ingredient", ingredient);

        JsonObject components = new JsonObject();
        components.addProperty(SINGULARITY_ID_COMPONENT, singularityId.toString());

        JsonObject result = new JsonObject();
        result.addProperty("id", SINGULARITY_ITEM);
        result.add("components", components);
        recipe.add("result", result);

        recipe.addProperty("inputCount", singularity.getCount());
        recipe.addProperty("timeCost", Math.max(1, singularity.getTimeCost()));
        return recipe;
    }
}
