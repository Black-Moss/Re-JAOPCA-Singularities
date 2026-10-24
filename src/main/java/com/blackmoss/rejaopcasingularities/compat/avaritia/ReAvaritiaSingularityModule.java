package com.blackmoss.rejaopcasingularities.compat.avaritia;

import com.blackmoss.rejaopcasingularities.ReJAOPCASingularities;
import com.blackmoss.rejaopcasingularities.compat.avaritia.recipes.SingularityCompressorRecipeSerializer;
import com.blackmoss.rejaopcasingularities.util.ConfigLocalization;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia.core.singularity.SingularityReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thelm.jaopca.api.config.IDynamicSpecConfig;
import thelm.jaopca.api.helpers.IMiscHelper;
import thelm.jaopca.api.materials.IMaterial;
import thelm.jaopca.api.materials.MaterialType;
import thelm.jaopca.api.modules.IModule;
import thelm.jaopca.api.modules.IModuleData;
import thelm.jaopca.api.modules.JAOPCAModule;
import thelm.jaopca.api.resources.IInMemoryResourcePack;
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
            "steel",
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
            "tin",
            "uranium",
            "packed_ice",
            "ice",
            "otherworld_wood",
            "grains_of_vibrancy",
            "grains_of_prescience",
            "grains_of_the_end",
            "grains_of_pizeallity");
    public static final Set<String> FORCED = Set.of(
            "resin"
    );
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, Identifier> INGREDIENTS = new TreeMap<>();
    private static boolean listenersRegistered;
    private final Set<IMaterial> singularityMaterials = new TreeSet<>();
    private Map<IMaterial, IDynamicSpecConfig> configs = Map.of();
    private Set<String> extraBlacklist = new TreeSet<>();
    private IDynamicSpecConfig moduleConfig;
    private int defaultCount = 1000;
    private int defaultTimeCost = -1;

    private static IMaterial findMaterial(String name) {
        for (IMaterial material : ApiImpl.INSTANCE.getMaterials()) {
            if (material.getName().equals(name) || material.getAlternativeNames().contains(name)) {
                return material;
            }
        }
        return null;
    }

    private static Identifier findIngredientTag(IMaterial material) {
        Set<Identifier> itemTags = ApiImpl.INSTANCE.getItemTags();
        IMiscHelper miscHelper = MiscHelper.INSTANCE;
        Identifier tag = miscHelper.getTagLocation(material.getType().getFormName(), material.getName());
        if (itemTags.contains(tag)) {
            return tag;
        }
        if (isForced(material)) {
            for (String form : List.of("ingots", "gems", "crystals", "dusts")) {
                Identifier alternative = miscHelper.getTagLocation(form, material.getName());
                if (itemTags.contains(alternative)) {
                    return alternative;
                }
            }
        }
        return null;
    }

    private static boolean isForced(IMaterial material) {
        return matches(material, FORCED);
    }

    private static boolean matches(IMaterial material, Set<String> names) {
        if (names.contains(material.getName())) {
            return true;
        }
        for (String alternativeName : material.getAlternativeNames()) {
            if (names.contains(alternativeName)) {
                return true;
            }
        }
        return false;
    }

    private static void pruneEmptyIngredientSingularities(ServerStartedEvent event) {
        SingularityReloadListener listener = SingularityReloadListener.INSTANCE;
        Map<Identifier, Singularity> runSingularities = listener.getRunSingularities();
        List<Identifier> removed = new ArrayList<>();
        for (Map.Entry<String, Identifier> entry : INGREDIENTS.entrySet()) {
            Identifier id = ReJAOPCASingularities.identifier(entry.getKey());
            if (!runSingularities.containsKey(id) || tagSize(entry.getValue()) > 0) {
                continue;
            }
            removed.add(id);
            Identifier legacyTag = Identifier.fromNamespaceAndPath("forge", entry.getValue().getPath());
            int legacySize = tagSize(legacyTag);
            if (legacySize > 0) {
                ReJAOPCASingularities.LOGGER.warn("Removing singularity {}: {} is empty but {} has {} items (old tag convention)",
                        entry.getKey(), entry.getValue(), legacyTag, legacySize);
            } else {
                ReJAOPCASingularities.LOGGER.warn("Removing singularity {} as its ingredient tag {} is empty", entry.getKey(), entry.getValue());
            }
        }
        if (removed.isEmpty()) {
            return;
        }
        removed.forEach(runSingularities::remove);
        listener.setRunSingularities(runSingularities);
        listener.finalizeScriptTransaction();
        ReJAOPCASingularities.LOGGER.info("Removed {} singularities with empty ingredient tags", removed.size());
        MinecraftServer server = event.getServer();
        server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds()));
    }

    private static int tagSize(Identifier tagLocation) {
        int size = 0;
        for (Holder<Item> ignored : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tagLocation))) {
            ++size;
        }
        return size;
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
        if (!listenersRegistered) {
            listenersRegistered = true;
            NeoForge.EVENT_BUS.addListener(ReAvaritiaSingularityModule::pruneEmptyIngredientSingularities);
            NeoForge.EVENT_BUS.addListener(EternalSingularityCompressionHandler::onRegisterRecipes);
            NeoForge.EVENT_BUS.addListener(EternalSingularityCompressionHandler::onItemToolTip);
        }
        Set<IMaterial> materials = new TreeSet<>(moduleData.getMaterials());
        for (String forcedName : FORCED) {
            IMaterial forced = findMaterial(forcedName);
            if (forced != null) {
                materials.add(forced);
            } else {
                ReJAOPCASingularities.LOGGER.warn("Forced singularity material {} was not found in JAOPCA materials", forcedName);
            }
        }
        int registered = 0;
        for (IMaterial material : materials) {
            if (registerSingularity(material)) {
                ++registered;
            }
        }
        ReJAOPCASingularities.LOGGER.info("Registered {} Avaritia singularities for JAOPCA materials", registered);
    }

    private boolean registerSingularity(IMaterial material) {
        if (isSkipped(material)) {
            return false;
        }
        Identifier materialTag = findIngredientTag(material);
        if (materialTag == null) {
            ReJAOPCASingularities.LOGGER.warn("Skipping singularity for material {} as no item tag was found", material.getName());
            return false;
        }
        try {
            IDynamicSpecConfig config = configs.get(material);
            if (config == null) {
                ReJAOPCASingularities.LOGGER.warn("No config available for material {}, skipping singularity", material.getName());
                return false;
            }
            int count = config.getDefinedInt("avaritia_singularity.count", defaultCount, 1, Integer.MAX_VALUE,
                    ConfigLocalization.get(ConfigLocalization.MATERIAL_COUNT));
            int timeCost = config.getDefinedInt("avaritia_singularity.timeCost", defaultTimeCost, -1, Integer.MAX_VALUE,
                    ConfigLocalization.get(ConfigLocalization.MATERIAL_TIME_COST));
            Identifier singularityId = ReJAOPCASingularities.identifier(material.getName());
            Singularity singularity = createSingularity(singularityId, materialTag, count, timeCost, material, config);
            SingularityReloadListener.INSTANCE.registerSingularity(singularity);
            Identifier recipeId = ReJAOPCASingularities.identifier(material.getName() + "_singularity");
            if (!ApiImpl.INSTANCE.registerRecipe(recipeId,
                    new SingularityCompressorRecipeSerializer(singularityId, materialTag, singularity))) {
                ReJAOPCASingularities.LOGGER.warn("Compressor recipe {} was not injected (duplicate id or recipe blacklist)", recipeId);
            }
            INGREDIENTS.put(material.getName(), materialTag);
            singularityMaterials.add(material);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            ReJAOPCASingularities.LOGGER.error("Unable to register singularity for material {}", material.getName(), e);
            return false;
        }
    }

    private boolean isSkipped(IMaterial material) {
        if (matches(material, extraBlacklist)) {
            return true;
        }
        return !isForced(material) && matches(material, ALWAYS_SKIPPED);
    }

    @Override
    public void onCreateResourcePack(IModuleData moduleData, IInMemoryResourcePack resourcePack) {
        Set<IMaterial> materials = singularityMaterials.isEmpty() ? moduleData.getMaterials() : singularityMaterials;
        JsonObject english = new JsonObject();
        for (IMaterial material : materials) {
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
                    for (IMaterial material : materials) {
                        JsonElement name = localization.get("jaopca.material." + material.getName());
                        if (name != null && name.isJsonPrimitive() && !name.getAsString().isEmpty()) {
                            lang.addProperty(getTranslationKey(material), name.getAsString());
                        }
                    }
                    if (!lang.isEmpty()) {
                        resourcePack.putJson(PackType.CLIENT_RESOURCES, ReJAOPCASingularities.identifier("lang/" + language + ".json"), lang);
                    }
                } catch (IOException | JsonParseException | UnsupportedOperationException e) {
                    ReJAOPCASingularities.LOGGER.warn("Unable to read JAOPCA localization file {}", file, e);
                }
            }
        } catch (IOException e) {
            ReJAOPCASingularities.LOGGER.debug("Unable to read JAOPCA localization directory {}", langDir);
        }
    }
}
