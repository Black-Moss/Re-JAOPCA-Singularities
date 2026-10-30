package com.blackmoss.rejaopcasingularities.compat.avaritia;

import com.blackmoss.rejaopcasingularities.ReJAOPCASingularities;
import committee.nova.mods.avaritia.core.singularity.Singularity;
import committee.nova.mods.avaritia.core.singularity.SingularityEvent;
import committee.nova.mods.avaritia.core.singularity.SingularityReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import thelm.jaopca.api.materials.IMaterial;
import thelm.jaopca.api.materials.MaterialColorEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MaterialColorBridge {
    private static final Map<Identifier, Integer> COLORS = new ConcurrentHashMap<>();
    private static boolean applying;

    private MaterialColorBridge() {
    }

    public static void onMaterialColor(MaterialColorEvent event) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }
        IMaterial material = event.getMaterial();
        COLORS.put(ReJAOPCASingularities.identifier(material.getName()), event.getColor() & 0xFFFFFF);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(MaterialColorBridge::applyColors);
    }

    public static void onSingularitiesReloaded(SingularityEvent.Reload event) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(MaterialColorBridge::applyColors);
    }

    private static void applyColors() {
        if (applying || COLORS.isEmpty()) {
            return;
        }
        applying = true;
        try {
            SingularityReloadListener listener = SingularityReloadListener.INSTANCE;
            boolean changed = false;
            for (Map.Entry<Identifier, Integer> entry : COLORS.entrySet()) {
                Singularity singularity = listener.getSingularity(entry.getKey());
                int color = entry.getValue();
                if (singularity == null || (singularity.getOverlayColor() & 0xFFFFFF) == color) {
                    continue;
                }
                listener.registerSingularity(singularity.copy()
                        .setColors(color, ARGB.scaleRGB(color, 0.45F) & 0xFFFFFF));
                changed = true;
            }
            if (changed) {
                listener.finalizeScriptTransaction();
            }
        } finally {
            applying = false;
        }
    }
}
