package com.blackmoss.rejaopcasingularities.data;

import com.blackmoss.rejaopcasingularities.CompoundSingularityType;
import com.blackmoss.rejaopcasingularities.ReJAOPCASingularities;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public final class ModModelProvider extends ItemModelProvider {
    public ModModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ReJAOPCASingularities.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (CompoundSingularityType type : CompoundSingularityType.values()) {
            String itemId = type.getItemId();
            getBuilder(ReJAOPCASingularities.identifier(itemId).toString())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", ReJAOPCASingularities.identifier("item/" + itemId))
                    .texture("layer1", ReJAOPCASingularities.identifier("item/" + itemId + "_overlay"));
        }
    }
}
