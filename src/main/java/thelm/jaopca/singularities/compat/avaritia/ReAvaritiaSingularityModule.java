package thelm.jaopca.singularities.compat.avaritia;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia.core.singularity.SingularityReloadListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thelm.jaopca.api.JAOPCAApi;
import thelm.jaopca.api.config.IDynamicSpecConfig;
import thelm.jaopca.api.helpers.IMiscHelper;
import thelm.jaopca.api.materials.IMaterial;
import thelm.jaopca.api.materials.MaterialType;
import thelm.jaopca.api.modules.IModule;
import thelm.jaopca.api.modules.IModuleData;
import thelm.jaopca.api.modules.JAOPCAModule;
import thelm.jaopca.api.resources.IInMemoryResourcePack;
import thelm.jaopca.singularities.ReJAOPCASingularities;
import thelm.jaopca.singularities.compat.avaritia.recipes.SingularityCompressorRecipeSerializer;
import thelm.jaopca.singularities.util.ConfigLocalization;
import thelm.jaopca.utils.ApiImpl;
import thelm.jaopca.utils.MiscHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@JAOPCAModule(modDependencies = "avaritia")
public class ReAvaritiaSingularityModule implements IModule {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Set<String> ALWAYS_SKIPPED = Set.of(
            "aluminum",
            "amethyst",
            "amethyst_shard",
            "blue_ice",
            "bronze",
            "coal",
            "copper",
            "diamond",
            "electrum",
            "emerald",
            "glowstone",
            "gold",
            "invar",
            "iron",
            "lapis",
            "lapis_lazuli",
            "lead",
            "netherite",
            "netherite_scrap",
            "neutron",
            "neutronium",
            "nickel",
            "obsidian",
            "osmium",
            "platinum",
            "quartz",
            "redstone",
            "refined_obsidian",
            "silver",
            "steel",
            "tin",
            "uranium",
            "packed_ice",
            "ice");

    private Map<IMaterial, IDynamicSpecConfig> configs = Map.of();
    private Set<String> extraBlacklist = new TreeSet<>();

    private IDynamicSpecConfig moduleConfig;
    private int defaultCount = 1000;
    private int defaultTimeCost = -1;

    @Override
    public String getName() {
        return "avaritia_singularity";
    }

    @Override
    public Set<MaterialType> getMaterialTypes() {
        return EnumSet.allOf(MaterialType.class);
    }

    @Override
    public Set<String> getDefaultMaterialBlacklist() {
        return ALWAYS_SKIPPED;
    }

    @Override
    public void defineModuleConfig(IModuleData moduleData, IDynamicSpecConfig config) {
        moduleConfig = config;
        readModuleConfig(config);
    }

    private void readModuleConfig(IDynamicSpecConfig config) {
        defaultCount = config.getDefinedInt("general.count", 1000, 1, Integer.MAX_VALUE,
                ConfigLocalization.get(ConfigLocalization.COUNT));
        defaultTimeCost = config.getDefinedInt("general.timeCost", -1, -1, Integer.MAX_VALUE,
                ConfigLocalization.get(ConfigLocalization.TIME_COST));
        extraBlacklist = new TreeSet<>(config.getDefinedStringList("general.singularityBlacklist", new ArrayList<>(),
                ConfigLocalization.get(ConfigLocalization.BLACKLIST)));
    }

    @Override
    public void onMaterialConfigAvailable(IModuleData moduleData, Map<IMaterial, IDynamicSpecConfig> configs) {
        this.configs = configs;
    }

    @Override
    public void onCommonSetup(IModuleData moduleData, FMLCommonSetupEvent event) {
        if (moduleConfig != null) {
            readModuleConfig(moduleConfig);
        }
        JAOPCAApi api = ApiImpl.INSTANCE;
        IMiscHelper miscHelper = MiscHelper.INSTANCE;
        Set<Identifier> itemTags = api.getItemTags();
        int registered = 0;
        for (IMaterial material : moduleData.getMaterials()) {
            if (isSkipped(material)) {
                continue;
            }
            Identifier materialTag = miscHelper.getTagLocation(material.getType().getFormName(), material.getName());
            if (!itemTags.contains(materialTag)) {
                LOGGER.debug("Skipping singularity for material {} as tag {} does not exist", material.getName(), materialTag);
                continue;
            }
            try {
                IDynamicSpecConfig config = configs.get(material);
                if (config == null) {
                    LOGGER.warn("No config available for material {}, skipping singularity", material.getName());
                    continue;
                }
                int count = config.getDefinedInt("avaritia_singularity.count", defaultCount, 1, Integer.MAX_VALUE,
                        ConfigLocalization.get(ConfigLocalization.MATERIAL_COUNT));
                int timeCost = config.getDefinedInt("avaritia_singularity.timeCost", defaultTimeCost, -1, Integer.MAX_VALUE,
                        ConfigLocalization.get(ConfigLocalization.MATERIAL_TIME_COST));
                Identifier singularityId = ReJAOPCASingularities.identifier(material.getName());
                Singularity singularity = createSingularity(singularityId, materialTag, count, timeCost, material, config);
                SingularityReloadListener.INSTANCE.registerSingularity(singularity);
                api.registerRecipe(
                        ReJAOPCASingularities.identifier(material.getName() + "_singularity"),
                        new SingularityCompressorRecipeSerializer(singularityId, materialTag, singularity));
                ++registered;
            } catch (IllegalArgumentException | IllegalStateException e) {
                LOGGER.error("Unable to register singularity for material {}", material.getName(), e);
            }
        }
        LOGGER.info("Registered {} Avaritia singularities for JAOPCA materials", registered);
    }

    @Override
    public void onCreateResourcePack(IModuleData moduleData, IInMemoryResourcePack resourcePack) {
        JsonObject english = new JsonObject();
        for (IMaterial material : moduleData.getMaterials()) {
            english.addProperty(getTranslationKey(material), splitAndCapitalize(material.getName()));
        }
        resourcePack.putJson(PackType.CLIENT_RESOURCES, ReJAOPCASingularities.identifier("lang/en_us.json"), english);

        Path langDir = FMLPaths.CONFIGDIR.get().resolve("jaopca").resolve("lang");
        try (DirectoryStream<Path> files = Files.newDirectoryStream(langDir, "*.json")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String language = fileName.substring(0, fileName.length() - ".json".length());
                if (language.equals("en_us")) {
                    continue;
                }
                try {
                    JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                    if (!parsed.isJsonObject()) {
                        continue;
                    }
                    JsonObject localization = parsed.getAsJsonObject();
                    JsonObject lang = new JsonObject();
                    for (IMaterial material : moduleData.getMaterials()) {
                        JsonElement name = localization.get("jaopca.material." + material.getName());
                        if (name != null && name.isJsonPrimitive() && !name.getAsString().isEmpty()) {
                            lang.addProperty(getTranslationKey(material), name.getAsString());
                        }
                    }
                    if (!lang.isEmpty()) {
                        resourcePack.putJson(PackType.CLIENT_RESOURCES, ReJAOPCASingularities.identifier("lang/" + language + ".json"), lang);
                    }
                } catch (IOException | JsonParseException | UnsupportedOperationException e) {
                    LOGGER.warn("Unable to read JAOPCA localization file {}", file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Unable to read JAOPCA localization directory {}", langDir);
        }
    }

    private boolean isSkipped(IMaterial material) {
        if (ALWAYS_SKIPPED.contains(material.getName()) || extraBlacklist.contains(material.getName())) {
            return true;
        }
        for (String alternativeName : material.getAlternativeNames()) {
            if (ALWAYS_SKIPPED.contains(alternativeName) || extraBlacklist.contains(alternativeName)) {
                return true;
            }
        }
        return false;
    }

    private static Singularity createSingularity(
            Identifier singularityId,
            Identifier ingredientTag,
            int count,
            int timeCost,
            IMaterial material,
            IDynamicSpecConfig config) {
        Singularity singularity = new Singularity(singularityId);
        singularity.setDisplayName(getTranslationKey(material));
        int[] colors = getColors(material, config);
        singularity.setColors(colors[0], colors[1]);
        singularity.setIngredient(Ingredient.of(new LazyTagHolderSet(TagKey.create(Registries.ITEM, ingredientTag))));
        singularity.setCount(count);
        if (timeCost > 0) {
            singularity.setTimeCost(timeCost);
        }
        return singularity;
    }

    private static int[] getColors(IMaterial material, IDynamicSpecConfig config) {
        OptionalInt configuredColor = config.getOptionalInt("general.color");
        int color = configuredColor.isPresent()
                ? configuredColor.getAsInt() & 0xFFFFFF
                : fingerprintColor(material.getName());
        return new int[]{color, ARGB.scaleRGB(color, 0.45F) & 0xFFFFFF};
    }

    private static int fingerprintColor(String materialName) {
        float hue = Math.floorMod(materialName.hashCode(), 360) / 360F;
        return Mth.hsvToRgb(hue, 0.45F, 0.9F);
    }

    private static String getTranslationKey(IMaterial material) {
        return "singularity." + ReJAOPCASingularities.MODID + "." + material.getName();
    }

    private static String splitAndCapitalize(String underscore) {
        return Arrays.stream(underscore.split("_")).
                filter(s -> !s.isEmpty()).
                map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1)).
                collect(Collectors.joining(" "));
    }
}
