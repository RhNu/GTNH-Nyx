package rhynia.nyx.common.mte.proxy

import gregtech.api.enums.GTValues
import gregtech.api.util.GTRecipe
import net.minecraft.item.ItemStack

/** Source yield factors are multiplied without silently overflowing a recipe stack. */
internal fun treeOutputAmount(base: Int, tier: Int, mode: Int, tool: Int): Int? {
    if (base <= 0 || tier <= 0 || mode <= 0 || tool <= 0) return null
    var amount = base.toLong()
    for (factor in listOf(tier, mode, tool)) {
        if (amount > Int.MAX_VALUE.toLong() / factor) return null
        amount *= factor
    }
    return amount.toInt()
}

/** Both generations retain source species/genetics and tool capabilities, with intentional free-use tools. */
internal class TreeAdapter(sourceClass: Class<*>) {
    private val source = TreeRegistryAccess(sourceClass)

    fun findRecipes(tier: Int, items: Array<out ItemStack?>): List<GTRecipe> {
        if (tier !in 1..GTValues.VP.lastIndex) return emptyList()
        val inputs = items.filterNotNull().filter { it.stackSize > 0 }
        val multiplier = source.tierMultiplier(tier)
        if (multiplier <= 0) return emptyList()
        return inputs.mapNotNull saplingLoop@ { sapling ->
            val products = source.outputs(sapling) ?: return@saplingLoop null
            val catalysts = mutableListOf(sapling.copy().apply { stackSize = 0 })
            val outputs = products.mapNotNull productLoop@ { product ->
                // Source chooses the last eligible tool per mode. Charge/durability are deliberately waived.
                val tool = inputs.asReversed().firstOrNull { source.toolMultiplier(it, product.mode) > 0 }
                    ?: return@productLoop null
                val amount = treeOutputAmount(
                    product.stack.stackSize,
                    multiplier,
                    source.modeMultiplier(product.mode),
                    source.toolMultiplier(tool, product.mode),
                ) ?: return@productLoop null
                catalysts += tool.copy().apply { stackSize = 0 }
                product.stack.copy().apply { stackSize = amount }
            }
            if (outputs.isEmpty()) return@saplingLoop null
            val uniqueCatalysts = catalysts.distinctBy { ProxyStackKey.of(it) }
            GTValues.RA.stdBuilder()
                .itemInputs(*uniqueCatalysts.toTypedArray())
                .itemOutputs(*outputs.toTypedArray())
                .duration(100)
                // Processing logic supplies the exact long EU/t and parallel bound at high tiers.
                .eut(GTValues.VP[tier].coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                .nbtSensitive()
                .noBuffer()
                .build().orElse(null)
        }
    }
}
