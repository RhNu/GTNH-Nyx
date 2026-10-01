package rhynia.nyx.common.mte.proxy

import forestry.api.arboriculture.TreeManager
import gregtech.api.enums.Mods
import gregtech.api.items.MetaGeneratedTool
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import rhynia.nyx.ModLogger
import java.lang.reflect.Method

internal data class TreeProduct(val mode: String, val stack: ItemStack)

/** Lazy, NBT-aware access to both upstream tree registries and their Forestry genome calculation. */
internal class TreeRegistryAccess(private val sourceClass: Class<*>) {
    private data class ProductKey(val mode: String, val stack: ProxyStackKey)
    private data class Key(val sapling: ProxyStackKey, val registration: List<ProductKey>)
    private data class Access(
        val registry: Map<String, Map<*, ItemStack?>>,
        val output: Method,
        val tier: Method,
        val tool: Method,
        val modes: Map<String, Any>,
        val multipliers: Map<*, Int>,
    )

    private val access: Access? by lazy {
        try {
            @Suppress("UNCHECKED_CAST")
            val registry = sourceClass.getField("treeProductsMap").get(null) as Map<String, Map<*, ItemStack?>>
            val output = sourceClass.getDeclaredMethod("getOutputsForSapling", ItemStack::class.java)
                .apply { isAccessible = true }
            val tier = sourceClass.getDeclaredMethod("getTierMultiplier", Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
            val modeClass = sourceClass.declaredClasses.first { it.simpleName == "Mode" }
            val tool = sourceClass.getMethod("getToolMultiplier", ItemStack::class.java, modeClass)
            val modeField = sourceClass.getDeclaredField("modeMultiplier").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val multipliers = modeField.get(null) as Map<*, Int>
            val modes = modeClass.enumConstants.associateBy { (it as Enum<*>).name }
            Access(registry, output, tier, tool, modes, multipliers)
        } catch (error: ReflectiveOperationException) {
            ModLogger.error("Cannot access tree recipes for ${sourceClass.name}; proxy adapter is unavailable", error)
            null
        }
    }
    private val speciesKeys = ProxyCache<ProxyStackKey, String>(64) { it }
    private val products = ProxyCache<Key, List<TreeProduct>>(64) { list ->
        list.map { TreeProduct(it.mode, it.stack.copy()) }
    }

    fun tierMultiplier(tier: Int): Int = (access?.tier?.invoke(null, tier) as? Int) ?: 0

    fun modeMultiplier(mode: String): Int {
        val source = access ?: return 0
        return source.multipliers[source.modes[mode]] ?: 0
    }

    fun toolMultiplier(stack: ItemStack, mode: String): Int {
        val source = access ?: return -1
        val sourceMode = source.modes[mode] ?: return -1
        val selector = stack.copy()
        val item = selector.item
        // Empty electric tools keep their capability; only the copied selector is normalized.
        if (item is MetaGeneratedTool && item.getElectricStats(selector) != null) {
            selector.itemDamage = item.getChargedMetaData(selector).toInt()
        }
        return (source.tool.invoke(null, selector, sourceMode) as? Int) ?: -1
    }

    fun outputs(sapling: ItemStack): List<TreeProduct>? {
        val source = access ?: return null
        val registryName = Item.itemRegistry.getNameForObject(sapling.item)
        val registrationKey = if (Mods.Forestry.isModLoaded && registryName == "Forestry:sapling") {
            speciesKeys.getOrCreate(ProxyStackKey.of(sapling, false)) {
                val tree = TreeManager.treeRoot.getMember(sapling) ?: return@getOrCreate null
                "Forestry:sapling:${tree.ident}"
            } ?: return null
        } else {
            "$registryName:${sapling.itemDamage}"
        }
        // Only inspect this species, rather than hash the entire registry on every recipe check.
        val registration = source.registry[registrationKey] ?: return null
        val revision = registration.entries.mapNotNull { entry ->
            entry.value?.let { ProductKey((entry.key as Enum<*>).name, ProxyStackKey.of(it)) }
        }
        return products.getOrCreate(Key(ProxyStackKey.of(sapling, false), revision)) {
            @Suppress("UNCHECKED_CAST")
            val generated = source.output.invoke(null, sapling.copy()) as? Map<*, ItemStack?>
                ?: return@getOrCreate null
            generated.entries.mapNotNull { entry ->
                entry.value?.let { TreeProduct((entry.key as Enum<*>).name, it.copy()) }
            }
        }
    }
}
