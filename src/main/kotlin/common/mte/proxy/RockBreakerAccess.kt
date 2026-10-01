package rhynia.nyx.common.mte.proxy

import gregtech.common.tileentities.machines.basic.MTERockBreaker
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import rhynia.nyx.ModLogger
import java.lang.reflect.Field

internal data class RockBreakerTemplate(
    val top: Block?,
    val bottom: Block?,
    val sides: List<Block>,
    val anywhere: List<Block>,
    val circuit: Int,
    val duration: Int,
    val input: ItemStack?,
    val consumesInput: Boolean,
    val output: ItemStack,
)

/** A missing circuit group falls back; an existing but nonmatching group does not. */
internal fun <T> rockBreakerCandidates(groups: Map<Int, T>, circuit: Int): T? =
    groups[circuit] ?: groups[-1]

/**
 * RC1 exposes registration but not its registry/getters. Resolve those accessors once,
 * then read the live source each lookup so other mods' later registrations remain visible.
 */
internal class RockBreakerAccess {
    private class Fields {
        val registry = field(MTERockBreaker::class.java, "ROCK_BREAKER_RECIPES")
        private val recipeClass = MTERockBreaker.RockBreakerRecipe::class.java
        val top = field(recipeClass, "topBlock")
        val bottom = field(recipeClass, "bottomBlock")
        val sides = field(recipeClass, "sideBlocks")
        val anywhere = field(recipeClass, "anywhereBlocks")
        val circuit = field(recipeClass, "circuit")
        val duration = field(recipeClass, "duration")
        val input = field(recipeClass, "inputItem")
        val consumes = field(recipeClass, "inputConsumed")
        val output = field(recipeClass, "outputItem")
    }

    private val fields: Fields? by lazy {
        try {
            Fields()
        } catch (error: ReflectiveOperationException) {
            ModLogger.error("Cannot access the RC1 Rock Breaker registry; proxy adapter is unavailable", error)
            null
        }
    }

    /** Preserve empty buckets too: an existing empty group must never fall back to the default. */
    fun templatesByCircuit(): Map<Int, List<RockBreakerTemplate>> {
        val access = fields ?: return emptyMap()
        @Suppress("UNCHECKED_CAST")
        val registry = access.registry.get(null) as Map<Int, Set<MTERockBreaker.RockBreakerRecipe>>
        return registry.mapValues { (_, recipes) ->
            recipes.map { recipe ->
                @Suppress("UNCHECKED_CAST")
                fun blocks(field: Field): List<Block> = (field.get(recipe) as? Array<Block>)?.toList() ?: emptyList()
                RockBreakerTemplate(
                    access.top.get(recipe) as? Block,
                    access.bottom.get(recipe) as? Block,
                    blocks(access.sides),
                    blocks(access.anywhere),
                    access.circuit.getInt(recipe),
                    access.duration.getInt(recipe),
                    (access.input.get(recipe) as? ItemStack)?.copy(),
                    access.consumes.getBoolean(recipe),
                    (access.output.get(recipe) as ItemStack).copy(),
                )
            }
        }
    }

    fun allTemplates(): List<RockBreakerTemplate> = templatesByCircuit().values.flatten()

    fun templates(circuit: Int): List<RockBreakerTemplate> =
        rockBreakerCandidates(templatesByCircuit(), circuit) ?: emptyList()

    companion object {
        private fun field(owner: Class<*>, name: String): Field =
            owner.getDeclaredField(name).apply { isAccessible = true }
    }
}
