package com.blackmoss.rejaopcasingularities;

import java.util.Arrays;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.blackmoss.rejaopcasingularities.compat.avaritia.EternalSingularityCompressionHandler;

@Mod(ReJAOPCASingularities.MODID)
public class ReJAOPCASingularities {
    public static final String MODID = "rejaopcasingularities";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);

    public static final List<DeferredHolder<Item, Item>> COMPOUND_SINGULARITIES =
            Arrays.stream(CompoundSingularityType.values())
                    .map(type -> ITEMS.register(type.getItemId(),
                            () -> new Item(new Item.Properties()
                                    .stacksTo(64)
                                    .rarity(Rarity.EPIC))))
                    .toList();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static ItemStack compoundSingularity(CompoundSingularityType type) {
        return new ItemStack(COMPOUND_SINGULARITIES.get(type.getIndex()).get());
    }

    public static ItemStack compoundSingularity(int index) {
        return compoundSingularity(CompoundSingularityType.byIndex(index));
    }

    public static ItemStack compoundSingularity(String id) {
        return CompoundSingularityType.byId(id)
                .map(ReJAOPCASingularities::compoundSingularity)
                .orElse(ItemStack.EMPTY);
    }

    public ReJAOPCASingularities(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(EternalSingularityCompressionHandler::onBuildCreativeModeTabContents);
    }
}
