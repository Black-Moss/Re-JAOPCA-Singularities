package thelm.jaopca.singularities;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ReJAOPCASingularities.MODID)
public class ReJAOPCASingularities {
	public static final String MODID = "rejaopcasingularities";

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MODID, path);
	}


	public ReJAOPCASingularities(IEventBus modEventBus, ModContainer modContainer) {}
}
