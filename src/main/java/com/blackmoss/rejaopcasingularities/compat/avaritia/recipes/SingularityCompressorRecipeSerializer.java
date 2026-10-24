package com.blackmoss.rejaopcasingularities.compat.avaritia.recipes;

import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import committee.nova.mods.avaritia.core.singularity.Singularity;
import net.minecraft.resources.Identifier;
import thelm.jaopca.api.recipes.IRecipeSerializer;

/**
 * 生成「材料物品 -> 奇点」的 Avaritia 中子压缩机配方 JSON。
 * <p>
 * 输入是材料对应的物品标签（{@code c:ingots/<材料>} 这类），和 Avaritia 自带的奇点配方保持一致。
 * Avaritia 会为运行时注册的奇点自动生成压缩机配方，但只处理「输入配料非空」的奇点，而配方加载阶段
 * 物品标签还没有绑定，所以自动生成的那条路走不通。JAOPCA 会在
 * {@code ModifyRecipeJsonsEvent} 里注入这个 JSON，该事件发生在配料被解析之前，标签引用由游戏自己解析。
 * <p>
 * 这里故意不加 {@code not tag_empty} 条件：条件在配方加载阶段按暂存标签内容判断，标签为空或加载失败时
 * 会把整条配方悄悄丢掉，表现为「奇点存在但没有任何配方」。配方始终写入，标签为空时游戏只会记录一条
 * 「配料不可放置」的警告，标签正常时则一切照常。
 */
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

		// 配料写成标签引用，真正解析时会被规范化为 avaritia:tag 自定义配料
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
