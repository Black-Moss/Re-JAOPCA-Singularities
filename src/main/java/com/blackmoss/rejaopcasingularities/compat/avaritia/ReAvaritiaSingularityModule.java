package com.blackmoss.rejaopcasingularities.compat.avaritia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia.core.singularity.SingularityReloadListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
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
import com.blackmoss.rejaopcasingularities.ReJAOPCASingularities;
import com.blackmoss.rejaopcasingularities.util.ConfigLocalization;

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
            "tin",
            "uranium",
            "packed_ice",
            "ice",
            "otherworld_wood");

    private static boolean listenersRegistered;

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
        if (!listenersRegistered) {
            listenersRegistered = true;
            NeoForge.EVENT_BUS.addListener(ReAvaritiaSingularityModule::registerSingularities);
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EternalSingularityCompressionHandler::onRegisterRecipes);
            NeoForge.EVENT_BUS.addListener(EternalSingularityCompressionHandler::onItemToolTip);
        }
        Set<IMaterial> materials = new TreeSet<>(moduleData.getMaterials());
        for (IMaterial material : materials) {
            collectCandidate(material);
        }
        LOGGER.info("Collected {} candidate materials for Avaritia singularities",
                EternalSingularityCompressionHandler.CANDIDATES.size());
    }

    private void collectCandidate(IMaterial material) {
        if (isSkipped(material)) {
            return;
        }
        ResourceLocation materialTag = findIngredientTag(material);
        if (materialTag == null) {
            LOGGER.warn("Skipping singularity for material {} as no item tag was found", material.getName());
            return;
        }
        IDynamicSpecConfig config = configs.get(material);
        if (config == null) {
            LOGGER.warn("No config available for material {}, skipping singularity", material.getName());
            return;
        }
        int count = config.getDefinedInt("avaritia_singularity.count", defaultCount, 1, Integer.MAX_VALUE,
                ConfigLocalization.get(ConfigLocalization.MATERIAL_COUNT));
        int timeCost = config.getDefinedInt("avaritia_singularity.timeCost", defaultTimeCost, -1, Integer.MAX_VALUE,
                ConfigLocalization.get(ConfigLocalization.MATERIAL_TIME_COST));
        int[] colors = getColors(material, config);
        EternalSingularityCompressionHandler.CANDIDATES.put(material.getName(),
                new EternalSingularityCompressionHandler.Candidate(materialTag, count, timeCost, colors[0], colors[1],
                        getTranslationKey(material)));
    }

    private static ResourceLocation findIngredientTag(IMaterial material) {
        Set<ResourceLocation> itemTags = ApiImpl.INSTANCE.getItemTags();
        IMiscHelper miscHelper = MiscHelper.INSTANCE;
        ResourceLocation tag = miscHelper.getTagLocation(material.getType().getFormName(), material.getName());
        return itemTags.contains(tag) ? tag : null;
    }

    private boolean isSkipped(IMaterial material) {
        return matches(material, extraBlacklist) || matches(material, ALWAYS_SKIPPED);
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

    private static void registerSingularities(ServerStartedEvent event) {
        SingularityReloadListener listener = SingularityReloadListener.INSTANCE;
        int registered = 0;
        for (String name : EternalSingularityCompressionHandler.USABLE) {
            EternalSingularityCompressionHandler.Candidate candidate =
                    EternalSingularityCompressionHandler.CANDIDATES.get(name);
            if (candidate == null) {
                continue;
            }
            Ingredient ingredient = Ingredient.of(TagKey.create(Registries.ITEM, candidate.tag()));
            if (ingredient.isEmpty()) {
                continue;
            }
            Singularity singularity = new Singularity(ReJAOPCASingularities.id(name))
                    .setDisplayName(candidate.displayName())
                    .setColors(candidate.overlayColor(), candidate.underlayColor())
                    .setIngredient(ingredient)
                    .setCount(candidate.count());
            if (candidate.timeCost() > 0) {
                singularity.setTimeCost(candidate.timeCost());
            }
            listener.registerSingularity(singularity);
            ++registered;
        }
        if (registered == 0) {
            return;
        }
        listener.finalizeScriptTransaction();
        LOGGER.info("Registered {} Avaritia singularities from JAOPCA materials", registered);
    }

    @Override
    public void onCreateResourcePack(IModuleData moduleData, IInMemoryResourcePack resourcePack) {
        Set<IMaterial> materials = moduleData.getMaterials();
        JsonObject english = new JsonObject();
        for (IMaterial material : materials) {
            english.addProperty(getTranslationKey(material), splitAndCapitalize(material.getName()));
        }
        resourcePack.putJson(PackType.CLIENT_RESOURCES, ReJAOPCASingularities.id("lang/en_us.json"), english);

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
                        resourcePack.putJson(PackType.CLIENT_RESOURCES, ReJAOPCASingularities.id("lang/" + language + ".json"), lang);
                    }
                } catch (IOException | JsonParseException | UnsupportedOperationException e) {
                    LOGGER.warn("Unable to read JAOPCA localization file {}", file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Unable to read JAOPCA localization directory {}", langDir);
        }
    }

    private static int[] getColors(IMaterial material, IDynamicSpecConfig config) {
        OptionalInt configuredColor = config.getOptionalInt("general.color");
        int color = configuredColor.isPresent()
                ? configuredColor.getAsInt() & 0xFFFFFF
                : fingerprintColor(material.getName());
        return new int[]{color, scaleRgb(color)};
    }

    private static int scaleRgb(int color) {
        int red = Math.min(255, (int) ((color >> 16 & 0xFF) * (float) 0.45));
        int green = Math.min(255, (int) ((color >> 8 & 0xFF) * (float) 0.45));
        int blue = Math.min(255, (int) ((color & 0xFF) * (float) 0.45));
        return red << 16 | green << 8 | blue;
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
