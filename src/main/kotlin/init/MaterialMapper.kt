package rhynia.nyx.init

import bartworks.system.material.Werkstoff
import bartworks.system.material.WerkstoffLoader
import gregtech.api.enums.Materials
import gregtech.api.enums.OrePrefixes
import gregtech.api.util.GTOreDictUnificator
import gtPlusPlus.core.material.Material
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import rhynia.nyx.ModLogger
import rhynia.nyx.api.item.MetaItemToken
import rhynia.nyx.api.util.size
import kotlin.time.measureTime

/**
 * Maps materials from different mods (GT, BW, PP) into a unified structure for utility use.
 *
 * All provider lookups and one-item output stacks are cached at load-complete time. Runtime lookups
 * only walk an identity item map, an integer-meta map, and (only when needed) an NBT collision list.
 */
@Suppress("UNUSED")
object MaterialMapper {
    private val mapGT = Materials.getMaterialsMap()
    private val mapBW = Werkstoff.werkstoffNameHashMap
    private val mapPP = Material.mMaterialMap.associateBy { it.unlocalizedName }

    private val _data: MutableMap<String, MaterialData> = linkedMapOf()
    val data: Map<String, MaterialData> get() = _data

    private val reversedIndex =
        Reference2ObjectOpenHashMap<Item, Int2ObjectOpenHashMap<LookupBucket>>()
    private var reversedIndexSize = 0
    private var reversedIndexCollisions = 0

    fun init() {
        measureTime {
            _data.clear()
            reversedIndex.clear()
            reversedIndexSize = 0
            reversedIndexCollisions = 0

            // Preserve the old last-provider-wins intent deterministically: specialized provider
            // components take precedence over a GT unification result using the same item stack.
            mapPP.toSortedMap().forEach { (name, material) ->
                register(MaterialData.see("gtpp:$name", material))
            }
            mapBW.toSortedMap().forEach { (name, material) ->
                register(MaterialData.see("bw:$name", material))
            }
            mapGT.toSortedMap().forEach { (name, material) ->
                register(MaterialData.see("gt:$name", material))
            }

            buildReverseIndex()
        }.also {
            ModLogger.info(
                "Mapped ${data.size} materials with $reversedIndexSize reverse entries in ${it.inWholeMilliseconds} ms",
            )
            if (reversedIndexCollisions > 0) {
                ModLogger.warn(
                    "Ignored $reversedIndexCollisions ambiguous material mappings; see debug log for details",
                )
            }
            data.forEach { (key, value) ->
                ModLogger.debug(" - $key: ${value.name} (${value.validOrePrefixes.size} prefixes)")
            }
        }
    }

    private fun register(material: MaterialData) {
        _data[material.key] = material
    }

    /** Cached information needed by the converter for one source item. */
    data class LookupData(
        val material: MaterialData,
        val prefix: OrePrefixes,
        val materialAmount: Long,
        internal val prototype: ItemStack,
    ) {
        val materialKey: String get() = material.key
    }

    /** Looks up cached material, prefix, and unit amount for an input stack. */
    fun lookup(itemStack: ItemStack): LookupData? =
        reversedIndex[itemStack.item]
            ?.get(itemStack.itemDamage)
            ?.find(itemStack)

    /** Compatibility lookup for item/meta-only callers. */
    fun lookup(token: MetaItemToken): LookupData? = lookup(token.createStack())

    fun lookupMaterial(itemStack: ItemStack): MaterialData? = lookup(itemStack)?.material

    fun lookupMaterial(token: MetaItemToken): MaterialData? = lookup(token)?.material

    fun lookupMaterial(key: String): MaterialData? = data[key]

    fun lookupOrePrefix(itemStack: ItemStack): OrePrefixes? = lookup(itemStack)?.prefix

    fun lookupOrePrefix(token: MetaItemToken): OrePrefixes? = lookup(token)?.prefix

    private fun buildReverseIndex() {
        data.values.forEach { material ->
            material.forEachPrototype { prefix, prototype ->
                val amount = prefix.materialAmount
                if (amount <= 0) return@forEachPrototype

                val lookup = LookupData(material, prefix, amount, prototype)
                val byMeta = reversedIndex.computeIfAbsent(prototype.item) { Int2ObjectOpenHashMap() }
                val existing = byMeta[prototype.itemDamage]

                if (existing == null) {
                    byMeta[prototype.itemDamage] = LookupBucket(lookup)
                    reversedIndexSize++
                } else if (existing.addIfDistinct(lookup)) {
                    reversedIndexSize++
                }
            }
        }
    }

    /**
     * One item/meta may legitimately use NBT to distinguish variants. Keep the common no-collision
     * path as a single object and allocate a list only if such a variant is actually encountered.
     */
    private class LookupBucket(
        private val primary: LookupData,
    ) {
        private var alternatives: MutableList<LookupData>? = null

        fun find(stack: ItemStack): LookupData? {
            if (ItemStack.areItemStackTagsEqual(primary.prototype, stack)) return primary
            return alternatives?.firstOrNull { ItemStack.areItemStackTagsEqual(it.prototype, stack) }
        }

        fun addIfDistinct(candidate: LookupData): Boolean {
            val sameNbt =
                when {
                    ItemStack.areItemStackTagsEqual(primary.prototype, candidate.prototype) -> primary
                    else -> alternatives?.firstOrNull {
                        ItemStack.areItemStackTagsEqual(it.prototype, candidate.prototype)
                    }
                }

            if (sameNbt != null) {
                if (sameNbt.material !== candidate.material || sameNbt.prefix !== candidate.prefix) {
                    reversedIndexCollisions++
                    ModLogger.debug(
                        "Material lookup collision for ${candidate.prototype}: " +
                            "keeping ${sameNbt.material.key}/${sameNbt.prefix.name}, ignoring " +
                            "${candidate.material.key}/${candidate.prefix.name}",
                    )
                }
                return false
            }

            val entries = alternatives ?: mutableListOf<LookupData>().also { alternatives = it }
            entries.add(candidate)
            return true
        }
    }

    /** Represents material data from different provider mods. */
    interface MaterialData {
        /** Stable key used for persistent converter balances. */
        val key: String
        val name: String
        val validOrePrefixes: Set<OrePrefixes>

        val anyStack: ItemStack? get() =
            validOrePrefixes.firstOrNull()?.let { getByOrePrefix(it, 1) }

        fun hasOrePrefix(prefix: OrePrefixes): Boolean = prefix in validOrePrefixes

        /** Returns a fresh stack copied from the cached one-item prototype. */
        fun getByOrePrefix(
            prefix: OrePrefixes,
            count: Int,
        ): ItemStack?

        /** Like [gregtech.api.util.GTUtility.copyAmountUnsafe], including counts larger than 64. */
        fun getByOrePrefixUnsafe(
            prefix: OrePrefixes,
            count: Int,
        ): ItemStack? = getByOrePrefix(prefix, count)

        fun forEachPrototype(action: (OrePrefixes, ItemStack) -> Unit)

        companion object {
            fun see(
                key: String,
                obj: Any,
            ): MaterialData =
                when (obj) {
                    is Materials -> ImplGT(key, obj)
                    is Werkstoff -> ImplBW(key, obj)
                    is Material -> ImplPP(key, obj)
                    else -> throw IllegalArgumentException("Unsupported material type: ${obj::class.java}")
                }
        }

        private abstract class Cached(
            final override val key: String,
            final override val name: String,
        ) : MaterialData {
            private val prototypes: Reference2ObjectOpenHashMap<OrePrefixes, ItemStack> by lazy {
                Reference2ObjectOpenHashMap<OrePrefixes, ItemStack>().also { result ->
                    OrePrefixes.VALUES.forEach { prefix ->
                        resolve(prefix)?.copy()?.let { result[prefix] = it.size(1) }
                    }
                }
            }

            final override val validOrePrefixes: Set<OrePrefixes> get() = prototypes.keys

            protected abstract fun resolve(prefix: OrePrefixes): ItemStack?

            final override fun getByOrePrefix(
                prefix: OrePrefixes,
                count: Int,
            ): ItemStack? =
                if (count > 0) prototypes[prefix]?.copy()?.size(count) else null

            final override fun forEachPrototype(action: (OrePrefixes, ItemStack) -> Unit) {
                OrePrefixes.VALUES.forEach { prefix ->
                    prototypes[prefix]?.let { action(prefix, it) }
                }
            }
        }

        private class ImplGT(
            key: String,
            private val material: Materials,
        ) : Cached(key, material.mName) {
            override fun resolve(prefix: OrePrefixes): ItemStack? =
                GTOreDictUnificator.get(prefix, material, 1)
        }

        private class ImplBW(
            key: String,
            private val werkstoff: Werkstoff,
        ) : Cached(key, werkstoff.defaultName) {
            override fun resolve(prefix: OrePrefixes): ItemStack? =
                WerkstoffLoader.getCorrespondingItemStackUnsafe(prefix, werkstoff, 1)
        }

        private class ImplPP(
            key: String,
            private val material: Material,
        ) : Cached(key, material.unlocalizedName) {
            override fun resolve(prefix: OrePrefixes): ItemStack? =
                material.getComponentByPrefix(prefix, 1)
        }
    }
}
