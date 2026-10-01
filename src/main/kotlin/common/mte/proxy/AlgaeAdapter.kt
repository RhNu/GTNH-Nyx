package rhynia.nyx.common.mte.proxy

import gregtech.api.enums.GTValues
import gregtech.api.recipe.RecipeMaps
import gregtech.api.util.GTRecipe
import gregtech.api.util.GTUtility
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

/** Integer 90% power, without overflowing the intermediate multiplication. */
internal fun algaeEUt(voltage: Long): Long =
    if (voltage <= 0) 0 else voltage / 10 * 9 + voltage % 10 * 9 / 10

/** Appended compost consumes normally; the water requirement has a zero-count stack. */
internal fun algaeInputChances(
    original: IntArray?,
    originalInputCount: Int,
    addedInputCount: Int,
): IntArray? {
    if (original == null) return null
    return IntArray(originalInputCount + addedInputCount) { slot ->
        if (slot < originalInputCount) original.getOrElse(slot) { 10000 } else 10000
    }
}

/** Instantiate every live output template at the voltage tier, privately and only when needed. */
internal class AlgaeAdapter {
    private val tiers = ProxyTierIndex<GTRecipe>()

    fun actualEUt(tier: Int): Long = GTValues.V.getOrNull(tier)?.let(::algaeEUt) ?: 0

    fun findRecipes(
        tier: Int,
        items: Array<out ItemStack?>,
    ): List<GTRecipe> {
        val eut = actualEUt(tier)
        if (eut <= 0) return emptyList()

        // A bucket represents the pond's water environment. It is never emptied or consumed.
        val supplied = items.filterNotNull()
        val water = supplied.firstOrNull {
            it.stackSize > 0 && it.item === Items.water_bucket
        }?.copy()?.apply { stackSize = 0 } ?: return emptyList()

        val compost = GregtechItemList.Compost.get(1)
        val plan = planAlgae(tier, supplied.filter { GTUtility.areStacksEqual(compost, it) }.map { it.stackSize })
            ?: return emptyList()
        return tiers.recipes(RecipeMaps.algaePondRecipes.allRecipes, { it.mSpecialValue }, plan.outputTier)
            .filter { it.mEnabled && !it.mFakeRecipe }
            .map { template ->
                template.proxyCopy().also { recipe ->
                    val originalInputCount = recipe.mInputs.size
                    val inputs = recipe.mInputs.toMutableList()
                    if (plan.compostConsumed > 0) {
                        inputs.add(compost.copy().also { it.stackSize = plan.compostConsumed })
                    }
                    inputs.add(water.copy())
                    recipe.mInputs = inputs.toTypedArray()
                    recipe.mInputChances =
                        algaeInputChances(recipe.mInputChances, originalInputCount, inputs.size - originalInputCount)
                    // GTRecipe has an Int field. The caller MUST budget and charge actualEUt(tier)
                    // through its custom parallel limit and no-overclock calculator, including above Int.MAX_VALUE.
                    recipe.mEUt = eut.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                }
            }
    }
}
