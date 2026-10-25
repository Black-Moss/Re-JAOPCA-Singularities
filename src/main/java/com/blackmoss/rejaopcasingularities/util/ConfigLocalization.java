package com.blackmoss.rejaopcasingularities.util;

import com.blackmoss.rejaopcasingularities.ReJAOPCASingularities;
import net.minecraft.locale.Language;

import java.util.Map;

public final class ConfigLocalization {
    public static final String COUNT = key("count");
    public static final String TIME_COST = key("time_cost");
    public static final String BLACKLIST = key("singularity_blacklist");
    public static final String MATERIAL_COUNT = key("material_count");
    public static final String MATERIAL_TIME_COST = key("material_time_cost");

    private static final Map<String, String> FALLBACK = Map.of(
            COUNT, "The default amount of items required to create one singularity.",
            TIME_COST, "The default time required to create one singularity, in ticks. -1 uses Avaritia's value.",
            BLACKLIST, "Additional materials that should not get a singularity. Materials that already have an Avaritia singularity and the always excluded materials are skipped.",
            MATERIAL_COUNT, "The amount of items required to create this material's singularity.",
            MATERIAL_TIME_COST, "The time required to create this material's singularity, in ticks. -1 uses the default value.");

    private ConfigLocalization() {
    }

    private static String key(String name) {
        return ReJAOPCASingularities.MODID + ".configuration." + name;
    }

    public static String get(String key) {
        String localized = Language.getInstance().getOrDefault(key);
        if (!localized.isEmpty() && !localized.equals(key)) {
            return localized;
        }
        return FALLBACK.getOrDefault(key, key);
    }
}
