package rhynia.nyx.common.mte.proxy

enum class ProxyStrategy {
    DEFAULT,
    DENY,
    TREE,
    ALGAE,
    MASS_FABRICATOR,
    ROCK_BREAKER,
}

/**
 * Pure routing policy audited against GT5-Unofficial f8453bce (GTNH 2.9.0-RC1).
 *
 * Names are exact Java class names and RecipeMapBuilder registration names. Keeping
 * this catalogue independent of GT classes avoids initializing optional machines
 * just to classify a controller. Callers supply the complete superclass hierarchy.
 */
object ProxyPolicy {
    val deniedMachineTypeNames: Set<String> =
        setOf(
            "kubatech.tileentity.gregtech.multiblock.MTEHighTempGasCooledReactor",
            "tectech.thing.metaTileEntity.multi.MTEEyeOfHarmony",
            "gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTESolarTower",
            "gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleManager",
            "gregtech.common.tileentities.machines.multi.beamcrafting.MTELargeHadronCollider",
            // These bases cover all four materials in both current and legacy families.
            "gregtech.common.tileentities.machines.multi.MTELargeBoilerBase",
            "gregtech.common.tileentities.machines.multi.MTELargeBoiler",
            "gregtech.common.tileentities.machines.multi.MTEDecayWarehouse",
            "tectech.thing.metaTileEntity.multi.MTEQuantumComputer",
            "bartworks.common.tileentities.tiered.MTERadioHatch",
            "tectech.thing.metaTileEntity.multi.godforge.MTEForgeOfGods",
            "gregtech.common.tileentities.machines.basic.MTEScanner",
            "bartworks.common.tileentities.debug.MTECreativeScanner",
            "tectech.thing.metaTileEntity.multi.MTEResearchStation",
            "gregtech.common.tileentities.machines.multi.MTEResearchCompleter",
            "gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleResearch",
        )

    val mockMachineTypeNames: Map<ProxyStrategy, Set<String>> =
        mapOf(
            ProxyStrategy.TREE to
                setOf(
                    "gregtech.common.tileentities.machines.multi.MTETreeFarm",
                    "gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTETreeFarmLegacy",
                ),
            ProxyStrategy.ALGAE to
                setOf(
                    "gregtech.common.tileentities.machines.multi.MTEAlgaePond",
                    "gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.algae.MTEAlgaePondBaseLegacy",
                ),
            ProxyStrategy.MASS_FABRICATOR to
                setOf("gregtech.common.tileentities.machines.basic.MTEMassfabricator"),
            ProxyStrategy.ROCK_BREAKER to
                setOf("gregtech.common.tileentities.machines.basic.MTERockBreaker"),
        )

    /**
     * Maps excluded from the ordinary path, including aliases exposed by other
     * controllers. Mock strategies supply their own maps instead of using these
     * original display templates. Real maps sharing a controller remain usable.
     */
    val deniedRecipeMapNames: Set<String> =
        setOf(
            // gregtech.api.recipe.RecipeMaps
            "gt.recipe.fakeAssemblylineProcess",
            "gt.recipe.fakespaceprojects",
            "gt.recipe.large_hadron_collider",
            "gt.recipe.largeboilerfakefuels",
            "gt.recipe.isotope-decay",
            "gt.recipe.quantumcomputer",
            "gt.recipe.scanner",
            "gt.recipe.solartower",
            "gt.recipe.foundry_modules",
            "gt.recipe.treefarm",
            "gt.recipe.algae_pond",
            "gt.recipe.massfab",
            "gt.recipe.rockbreaker",
            // tectech.recipe.TecTechRecipeMaps
            "gt.recipe.eyeofharmony",
            "gt.recipe.researchStation",
            "gt.recipe.upgrade_costs",
            // kubatech.loaders.HTGRLoader / bartworks.API.recipe.BartWorksRecipeMaps
            "kubatech.htgrrecipes",
            "bw.recipe.radhatch",
            // gtnhintergalactic.recipe.IGRecipeMaps: research is explicitly unsupported.
            "gt.recipe.spaceResearch",
        )

    /**
     * Selects from a concrete machine's class name and all superclass names.
     * Denial takes precedence over mock strategies, independently of input order;
     * unknown machines retain their normal backend-supported recipe maps.
     */
    fun select(typeNames: Sequence<String>): ProxyStrategy {
        val hierarchy = typeNames.toSet()
        if (hierarchy.any { it in deniedMachineTypeNames }) return ProxyStrategy.DENY
        return mockMachineTypeNames.entries
            .firstOrNull { (_, names) -> hierarchy.any { it in names } }
            ?.key ?: ProxyStrategy.DEFAULT
    }

    fun select(typeNames: Iterable<String>): ProxyStrategy = select(typeNames.asSequence())

    fun isRecipeMapDenied(unlocalizedName: String): Boolean = unlocalizedName in deniedRecipeMapNames
}
