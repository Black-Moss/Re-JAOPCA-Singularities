package com.blackmoss.rejaopcasingularities;

import com.blackmoss.rejaopcasingularities.compat.avaritia.EternalSingularityCompressionHandler;
import com.blackmoss.rejaopcasingularities.data.ModItemTagsProvider;
import com.blackmoss.rejaopcasingularities.data.ModModelProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mod(ReJAOPCASingularities.MODID)
public class ReJAOPCASingularities {
    public static final String MODID = "rejaopcasingularities";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);

    public static final List<RegistryObject<Item>> COMPOUND_SINGULARITIES =
            Arrays.stream(CompoundSingularityType.values())
                    .map(type -> ITEMS.register(type.getItemId(),
                            () -> new Item(new Item.Properties()
                                    .stacksTo(64)
                                    .rarity(Rarity.EPIC))))
                    .toList();

    public ReJAOPCASingularities() {
        //noinspection removal
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        modEventBus.addListener(EternalSingularityCompressionHandler::onBuildCreativeModeTabContents);
        modEventBus.addListener(ReJAOPCASingularities::onGatherData);
    }

    public static ResourceLocation identifier(String path) {
        return new ResourceLocation(MODID, path);
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

    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new ModModelProvider(output, helper));
        generator.addProvider(event.includeServer(), new ModItemTagsProvider(
                output,
                event.getLookupProvider(),
                CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                helper));
    }
}
