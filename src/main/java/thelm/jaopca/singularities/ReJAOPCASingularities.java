package thelm.jaopca.singularities;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ReJAOPCASingularities.MODID)
public class ReJAOPCASingularities {
    public static final String MODID = "rejaopcasingularities";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final List<String> COMPOUND_SINGULARITY_NAMES = List.of(
            "nitronic",
            "psychotic",
            "sphaghettic",
            "pneumatic",
            "cryptic",
            "historic",
            "meteoric",
            "gastronomic",
            "chromatic",
            "prismatic",
            "robotic",
            "galactic",
            "hydrolic",
            "geologic",
            "angelic",
            "chronic",
            "celestial",
            "archaic",
            "bananular",
            "static",
            "aquatic",
            "thermionic",
            "necrotic",
            "rhubarbic",
            "sonic",
            "apocalyptic",
            "macaronic",
            "enigmatic",
            "electric",
            "benzoic",
            "cyclonic",
            "fantastic",
            "manic",
            "cubic",
            "cryonic",
            "strategic",
            "idiotic",
            "volcanic",
            "ironic",
            "yttric",
            "mathematic",
            "velconic",
            "universal",
            "bionic",
            "musical",
            "amazing",
            "transcendental",
            "quantum",
            "ridiculous",
            "cosmic",
            "metric",
            "cacophonic",
            "astronomic",
            "delicious",
            "thermal",
            "tartaric",
            "monolithic",
            "scientific",
            "morphic",
            "pacific",
            "humungous",
            "magmatic",
            "transonic",
            "egregious",
            "specific",
            "aromatic",
            "satanic",
            "quadratic",
            "pathetic",
            "automatic",
            "scrumptious",
            "supercalifragilistic",
            "baronic",
            "dextronic",
            "magnetic",
            "terrific",
            "smelly",
            "acidic",
            "basilic",
            "exquisite",
            "octagonal");

    public static final List<DeferredItem<Item>> COMPOUND_SINGULARITIES = COMPOUND_SINGULARITY_NAMES.stream()
            .map(name -> ITEMS.register(name + "_singularity",
                    id -> new Item(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, id)))))
            .toList();

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static ItemStack compoundSingularity(int index) {
        return new ItemStack(COMPOUND_SINGULARITIES.get(index).get());
    }

    public ReJAOPCASingularities(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
    }
}
