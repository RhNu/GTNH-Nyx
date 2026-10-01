package rhynia.nyx.common.mte.proxy

import gregtech.api.GregTechAPI
import gregtech.api.interfaces.metatileentity.IMetaTileEntity
import gregtech.api.interfaces.tileentity.RecipeMapWorkable
import gregtech.api.recipe.RecipeMap
import gregtech.common.blocks.ItemMachines
import net.minecraft.item.ItemStack

internal data class ProxyMachine(
    val source: IMetaTileEntity,
    val strategy: ProxyStrategy,
    val maps: List<RecipeMap<*>>,
)

/** Resolve live registrations; only immutable class classification is memoized. */
internal object ProxyMachineRegistry {
    private val kinds = ProxyCache<Class<*>, ProxyStrategy>(128) { it }

    fun resolve(controller: ItemStack?): ProxyMachine? {
        if (controller == null || controller.stackSize <= 0 || controller.item !is ItemMachines) return null
        val id = controller.itemDamage
        if (id <= 0 || id >= GregTechAPI.METATILEENTITIES.size) return null
        // Check identity on every resolution, including late registrations and removed/replaced MTEs.
        val source = GregTechAPI.METATILEENTITIES[id] ?: return null
        val strategy = kinds.getOrCreate(source.javaClass) {
            ProxyPolicy.select(generateSequence<Class<*>>(source.javaClass) { it.superclass }.map { it.name })
        } ?: return null
        if (strategy == ProxyStrategy.DENY || source !is RecipeMapWorkable) return null
        // Keep the live backend and its runtime registrations. Never snapshot getAllRecipes().
        val maps = source.availableRecipeMaps.filterNotNull().filter {
            strategy != ProxyStrategy.DEFAULT || !ProxyPolicy.isRecipeMapDenied(it.unlocalizedName)
        }.distinct()
        return maps.takeIf { it.isNotEmpty() }?.let { ProxyMachine(source, strategy, it) }
    }
}
