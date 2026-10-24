package com.blackmoss.rejaopcasingularities;

import java.util.Arrays;
import java.util.Optional;

public enum CompoundSingularityType {
    NITRONIC("nitronic"),
    PSYCHOTIC("psychotic"),
    SPHAGHETTIC("sphaghettic"),
    PNEUMATIC("pneumatic"),
    CRYPTIC("cryptic"),
    HISTORIC("historic"),
    METEORIC("meteoric"),
    GASTRONOMIC("gastronomic"),
    CHROMATIC("chromatic"),
    PRISMATIC("prismatic"),
    ROBOTIC("robotic"),
    GALACTIC("galactic"),
    HYDROLIC("hydrolic"),
    GEOLOGIC("geologic"),
    ANGELIC("angelic"),
    CHRONIC("chronic"),
    CELESTIAL("celestial"),
    ARCHAIC("archaic"),
    BANANULAR("bananular"),
    STATIC("static"),
    AQUATIC("aquatic"),
    THERMIONIC("thermionic"),
    NECROTIC("necrotic"),
    RHUBARBIC("rhubarbic"),
    SONIC("sonic"),
    APOCALYPTIC("apocalyptic"),
    MACARONIC("macaronic"),
    ENIGMATIC("enigmatic"),
    ELECTRIC("electric"),
    BENZOIC("benzoic"),
    CYCLONIC("cyclonic"),
    FANTASTIC("fantastic"),
    MANIC("manic"),
    CUBIC("cubic"),
    CRYONIC("cryonic"),
    STRATEGIC("strategic"),
    IDIOTIC("idiotic"),
    VOLCANIC("volcanic"),
    IRONIC("ironic"),
    YTTRIC("yttric"),
    MATHEMATIC("mathematic"),
    VELCONIC("velconic"),
    UNIVERSAL("universal"),
    BIONIC("bionic"),
    MUSICAL("musical"),
    AMAZING("amazing"),
    TRANSCENDENTAL("transcendental"),
    QUANTUM("quantum"),
    RIDICULOUS("ridiculous"),
    COSMIC("cosmic"),
    METRIC("metric"),
    CACOPHONIC("cacophonic"),
    ASTRONOMIC("astronomic"),
    DELICIOUS("delicious"),
    THERMAL("thermal"),
    TARTARIC("tartaric"),
    MONOLITHIC("monolithic"),
    SCIENTIFIC("scientific"),
    MORPHIC("morphic"),
    PACIFIC("pacific"),
    HUMUNGOUS("humungous"),
    MAGMATIC("magmatic"),
    TRANSONIC("transonic"),
    EGREGIOUS("egregious"),
    SPECIFIC("specific"),
    AROMATIC("aromatic"),
    SATANIC("satanic"),
    QUADRATIC("quadratic"),
    PATHETIC("pathetic"),
    AUTOMATIC("automatic"),
    SCRUMPTIOUS("scrumptious"),
    SUPERCALIFRAGILISTIC("supercalifragilistic"),
    BARONIC("baronic"),
    DEXTRONIC("dextronic"),
    MAGNETIC("magnetic"),
    TERRIFIC("terrific"),
    SMELLY("smelly"),
    ACIDIC("acidic"),
    BASILIC("basilic"),
    EXQUISITE("exquisite"),
    OCTAGONAL("octagonal");

    private final String id;

    CompoundSingularityType(String id) {
        this.id = id;
    }

    public static Optional<CompoundSingularityType> byId(String id) {
        return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst();
    }

    public static CompoundSingularityType byIndex(int index) {
        return values()[index];
    }

    public static int count() {
        return values().length;
    }

    public String getId() {
        return id;
    }

    public int getIndex() {
        return ordinal();
    }

    public String getItemId() {
        return id + "_singularity";
    }

    public String getTranslationKey() {
        return "item." + ReJAOPCASingularities.MODID + "." + getItemId();
    }

    public String getTooltipKey() {
        return "tooltip." + ReJAOPCASingularities.MODID + "." + getItemId();
    }
}
