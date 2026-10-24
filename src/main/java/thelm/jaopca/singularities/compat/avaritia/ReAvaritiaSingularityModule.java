package thelm.jaopca.singularities.compat.avaritia;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import thelm.jaopca.api.materials.MaterialType;
import thelm.jaopca.api.modules.IModule;
import thelm.jaopca.api.modules.JAOPCAModule;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@JAOPCAModule(modDependencies = "avaritia")
public class ReAvaritiaSingularityModule implements IModule {
    private static final Set<String> BLACKLIST = new TreeSet<>(List.of(
            "copper", "gold", "iron"));
    @Override
    public String getName() {
        return "avaritia_singularity";
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public Multimap<Integer, String> getModuleDependencies() {
        ImmutableSetMultimap.Builder<Integer, String> builder = ImmutableSetMultimap.builder();
        builder.put(0, "block");
        return builder.build();
    }


    @Override
    public Set<String> getDefaultMaterialBlacklist() {
        return BLACKLIST;
    }

    @Override
    public Set<MaterialType> getMaterialTypes() {
        return EnumSet.of(MaterialType.INGOT, MaterialType.INGOT_LEGACY);
    }
}
