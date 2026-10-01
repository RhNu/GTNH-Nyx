package rhynia.nyx.common.mte.proxy

import gregtech.api.enums.GTValues
import gregtech.api.enums.ItemList
import gregtech.api.enums.Materials
import gregtech.api.util.GTRecipe
import gregtech.api.util.OverclockCalculator
import gregtech.common.tileentities.machines.basic.MTEMassfabricator
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack

internal data class MassFabricatorConfig(
    val durationMultiplier: Int,
    val amplifierPerMatter: Int,
    val amplifierSpeedBonus: Int,
    val requiresAmplifier: Boolean,
    val baseEUt: Int,
)

internal enum class MassFabricatorBranch {
    UNAMPLIFIED,
    AMPLIFIED,
}

internal data class MassFabricatorPlan(
    val branch: MassFabricatorBranch,
    val duration: Int,
    val amplifierAmount: Int,
)

/** Multiple proxy tanks form one supply; a zero-cost amplifier still needs its fluid to be present. */
internal fun hasMassFabricatorAmplifier(
    required: Int,
    amounts: Iterable<Int>,
): Boolean {
    if (required < 0) return false
    var remaining = required.toLong()
    for (amount in amounts) {
        if (amount < 0) continue
        remaining -= amount.toLong()
        if (remaining <= 0) return true
    }
    return false
}

/** Integrated circuits suppress only the unamplified branch, regardless of their setting. */
internal fun planMassFabricator(
    config: MassFabricatorConfig,
    amplifierAmounts: Iterable<Int>,
    hasIntegratedCircuit: Boolean,
): MassFabricatorPlan? {
    if (config.durationMultiplier <= 0 || config.amplifierPerMatter < 0 ||
        config.amplifierSpeedBonus <= 0 || config.baseEUt <= 0
    ) return null

    if (hasMassFabricatorAmplifier(config.amplifierPerMatter, amplifierAmounts)) {
        val duration = config.durationMultiplier / config.amplifierSpeedBonus
        if (duration <= 0) return null
        return MassFabricatorPlan(
            MassFabricatorBranch.AMPLIFIED,
            duration,
            config.amplifierPerMatter,
        )
    }
    if (config.requiresAmplifier || hasIntegratedCircuit) return null
    return MassFabricatorPlan(MassFabricatorBranch.UNAMPLIFIED, config.durationMultiplier, 0)
}

internal fun massFabricatorMaxOverclocks(controllerTier: Int): Int =
    if (controllerTier <= 1) 0 else controllerTier - 1

/** Each parallel emulates one singleblock, sharing the actual proxy power supply. */
internal fun massFabricatorOverclockBudget(
    controllerVoltage: Long,
    availableVoltage: Long,
    availableAmperage: Long,
    parallel: Int = 1,
): Long {
    fun saturatedProduct(left: Long, right: Long, limit: Long): Long {
        if (left <= 0 || right <= 0) return 0
        return if (left > limit / right) limit else left * right
    }

    // The source saturates each singleblock's folded 8A envelope to Int before parallel scaling.
    val sourceBudget = saturatedProduct(controllerVoltage, 8, Int.MAX_VALUE.toLong())
    val parallelBudget = saturatedProduct(sourceBudget, parallel.toLong(), Long.MAX_VALUE)
    val availableBudget = saturatedProduct(availableVoltage, availableAmperage, Long.MAX_VALUE)
    return minOf(parallelBudget, availableBudget)
}

/** Keep the voltage budget consistent with both ParallelHelper's estimate and its final parallel. */
internal class MassFabricatorOverclockCalculator(
    private val controllerVoltage: Long,
    private val availableVoltage: Long,
    private val availableAmperage: Long,
) : OverclockCalculator() {
    init {
        updateBudget()
    }

    override fun setParallel(aParallel: Int): OverclockCalculator {
        super.setParallel(aParallel)
        updateBudget()
        return this
    }

    override fun setCurrentParallel(currentParallel: Int): OverclockCalculator {
        super.setCurrentParallel(currentParallel)
        updateBudget()
        return this
    }

    private fun updateBudget() {
        super.setEUt(massFabricatorOverclockBudget(controllerVoltage, availableVoltage, availableAmperage, parallel))
    }
}

/** Use the same equality for parallel counting as GTRecipe.consumeInput uses for consumption. */
internal fun <T> selectMassFabricatorFluids(
    supplied: Iterable<T>,
    required: Iterable<T>,
    matches: (T, T) -> Boolean,
): List<T> = supplied.filter { provided -> required.any { requested -> matches(provided, requested) } }

/** Private executable templates derived from live basic Mass Fabricator configuration. */
internal class MassFabricatorAdapter {
    private data class TemplateKey(
        val config: MassFabricatorConfig,
        val branch: MassFabricatorBranch,
    )

    private val recipes = ProxyCache<TemplateKey, GTRecipe>(2) { it.proxyCopy() }

    fun invalidate() = recipes.clear()

    fun findRecipe(
        items: Array<out ItemStack?>,
        fluids: Array<out FluidStack?>,
    ): GTRecipe? {
        val config =
            MassFabricatorConfig(
                MTEMassfabricator.sDurationMultiplier,
                MTEMassfabricator.sUUAperUUM,
                MTEMassfabricator.sUUASpeedBonus,
                MTEMassfabricator.sRequiresUUA,
                MTEMassfabricator.BASE_EUT,
            )
        val amplifier = Materials.UUAmplifier.getFluid(1L)
        val amounts = fluids.filterNotNull().filter { it.isFluidEqual(amplifier) }.map { it.amount }
        val hasCircuit = items.filterNotNull().any { ItemList.Circuit_Integrated.isStackEqual(it, true, true) }
        val plan = planMassFabricator(config, amounts, hasCircuit) ?: return null
        return recipes.getOrCreate(TemplateKey(config, plan.branch)) {
            val output = Materials.UUMatter.getFluid(1L) ?: return@getOrCreate null
            val builder =
                GTValues.RA.stdBuilder()
                    .fluidOutputs(output)
                    .duration(plan.duration)
                    .eut(config.baseEUt)
                    .noBuffer()
            if (plan.branch == MassFabricatorBranch.AMPLIFIED) {
                val input = Materials.UUAmplifier.getFluid(plan.amplifierAmount.toLong())
                    ?: return@getOrCreate null
                builder.fluidInputs(input)
            }
            // The NEI circuit-1 ingredient is display-only. Never alter or register its fake recipe.
            builder.build().orElse(null)
        }
    }

    fun maxParallel(
        recipe: GTRecipe,
        maxParallel: Int,
        fluids: Array<out FluidStack?>,
        items: Array<out ItemStack?>,
    ): Double {
        val matchingFluids =
            selectMassFabricatorFluids(fluids.filterNotNull(), recipe.mFluidInputs.filterNotNull()) { supplied, required ->
                supplied.isFluidEqual(required)
            }.toTypedArray()
        return recipe.maxParallelCalculatedByInputs(maxParallel, matchingFluids, *items.filterNotNull().toTypedArray())
    }

    fun createOverclockCalculator(
        recipe: GTRecipe,
        controllerTier: Int,
        availableVoltage: Long,
        amperage: Long,
        euModifier: Double,
        speedBonus: Double,
    ): OverclockCalculator {
        val tier = controllerTier.coerceIn(0, GTValues.V.lastIndex)
        return MassFabricatorOverclockCalculator(GTValues.V[tier], availableVoltage, amperage)
            .setRecipeEUt(recipe.mEUt.toLong())
            .setDuration(recipe.mDuration)
            .setAmperage(1L)
            .setAmperageOC(false)
            .setEUtDiscount(euModifier)
            .setDurationModifier(speedBonus)
            .setEUtIncreasePerOC(2.0)
            .setDurationDecreasePerOC(2.0)
            .setMaxOverclocks(massFabricatorMaxOverclocks(tier))
    }
}
