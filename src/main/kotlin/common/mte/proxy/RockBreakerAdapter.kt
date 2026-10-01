package rhynia.nyx.common.mte.proxy

import gregtech.api.enums.GTValues
import gregtech.api.enums.ItemList
import gregtech.api.enums.TierEU
import gregtech.api.util.GTRecipe
import gregtech.api.util.GTUtility
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import rhynia.nyx.api.util.localize

internal data class RockBreakerSelection<T>(val circuit: Int, val template: T)

/** Every supplied circuit has its own source bucket; only a missing bucket may use the default. */
internal fun <T> selectRockBreakerCandidates(
    groups: Map<Int, List<T>>,
    suppliedCircuits: Iterable<Int>,
): List<RockBreakerSelection<T>> =
    suppliedCircuits.distinct().ifEmpty { listOf(-1) }.flatMap { circuit ->
        (rockBreakerCandidates(groups, circuit) ?: emptyList()).map { RockBreakerSelection(circuit, it) }
    }

/** Resource catalysts lose positions, but the selected source-derived mode must retain them. */
internal fun rockBreakerEnvironmentId(
    top: String?,
    bottom: String?,
    sides: Iterable<String>,
    anywhere: Iterable<String>,
): String =
    "top=${top ?: "-"};bottom=${bottom ?: "-"};" +
        "sides=${sides.distinct().sorted().joinToString(",")};" +
        "anywhere=${anywhere.distinct().sorted().joinToString(",")}"

/** One catalyst represents one environmental resource, even when several source positions need it. */
internal fun <B : Any, R : Any> rockBreakerResources(
    top: B?,
    bottom: B?,
    sides: Iterable<B>,
    anywhere: Iterable<B>,
    resourceOf: (B) -> R?,
): List<R>? {
    val resources = LinkedHashSet<R>()
    for (block in listOfNotNull(top, bottom) + sides + anywhere) {
        resources += resourceOf(block) ?: return null
    }
    return resources.toList()
}

/**
 * Environmental catalysts accept ordinary item NBT (for example renamed buckets). Copy that exact supplied
 * NBT into the executable recipe while preserving the source input's stricter item/meta/NBT comparison.
 */
internal fun <T : Any> buildRockBreakerInputs(
    catalysts: Iterable<T>,
    sourceInput: T?,
    consumesInput: Boolean,
    supplied: Iterable<T>,
    count: (T) -> Int,
    matchesCatalyst: (T, T) -> Boolean,
    matchesSource: (T, T) -> Boolean,
    copyWithCount: (T, Int) -> T,
): List<T>? {
    val available = supplied.filter { count(it) > 0 }
    val inputs = ArrayList<T>()
    for (requested in catalysts) {
        val provided = available.firstOrNull { matchesCatalyst(it, requested) } ?: return null
        inputs += copyWithCount(provided, 0)
    }
    if (sourceInput != null) {
        if (available.none { matchesSource(it, sourceInput) }) return null
        inputs += copyWithCount(sourceInput, if (consumesInput) 1 else 0)
    }
    return inputs
}

internal data class RockBreakerItem<T>(val value: T, val count: Int)

internal data class RockBreakerInputPlan(val parallel: Double, val deductions: List<Int>)

/**
 * The source has at most one charged item. Zero-count requirements are presence checks, independent of
 * parallel count, and their witness items remain available even if the source consumes that same item type.
 */
internal fun <T> planRockBreakerInputs(
    maxParallel: Int,
    required: List<RockBreakerItem<T>>,
    supplied: List<RockBreakerItem<T>>,
    matches: (T, T) -> Boolean,
): RockBreakerInputPlan {
    val deductions = IntArray(supplied.size)
    fun missing() = RockBreakerInputPlan(0.0, deductions.toList())
    if (maxParallel <= 0 || required.any { it.count < 0 }) return missing()
    val consumed = required.filter { it.count > 0 }
    if (consumed.size > 1) return missing()

    val witnesses = BooleanArray(supplied.size)
    for (catalyst in required.filter { it.count == 0 }) {
        if (supplied.indices.any { witnesses[it] && matches(supplied[it].value, catalyst.value) }) continue
        val witness = supplied.indices.firstOrNull {
            supplied[it].count > 0 && matches(supplied[it].value, catalyst.value)
        } ?: return missing()
        witnesses[witness] = true
    }
    val charged = consumed.singleOrNull()
        ?: return RockBreakerInputPlan(maxParallel.toDouble(), deductions.toList())
    val available = supplied.indices.map { index ->
        if (matches(supplied[index].value, charged.value)) {
            (supplied[index].count.toLong() - if (witnesses[index]) 1 else 0).coerceAtLeast(0)
        } else 0L
    }
    val amount = available.sum()
    val parallel = minOf(maxParallel.toDouble(), amount.toDouble() / charged.count)
    var remaining = minOf(maxParallel.toLong(), amount / charged.count) * charged.count
    for (index in supplied.indices) {
        val deduction = minOf(available[index], remaining).toInt()
        deductions[index] = deduction
        remaining -= deduction
    }
    return RockBreakerInputPlan(parallel, deductions.toList())
}

internal data class RockBreakerMode(val id: String, val name: String)

/** Private executable recipes from the live basic Rock Breaker registry; no in-world or fluid checks. */
internal class RockBreakerAdapter {
    private data class TemplateKey(
        val mode: String,
        val circuit: Int,
        val inputs: List<ProxyStackKey>,
        val output: ProxyStackKey,
        val duration: Int,
    )

    private val access = RockBreakerAccess()
    private val modes = ProxyModes<RockBreakerMode> { it.id }
    private var modesInitialized = false
    private val recipes = ProxyCache<TemplateKey, GTRecipe>(128) { it.proxyCopy() }

    val modeIndex: Int
        get() {
            ensureModes()
            return modes.index
        }
    val modeId: String?
        get() {
            ensureModes()
            return modes.current?.id
        }
    val modeName: String?
        get() {
            ensureModes()
            return modes.current?.name
        }
    val availableModes: List<RockBreakerMode>
        get() {
            refreshModes()
            return modes.values.toList()
        }

    fun nextMode() {
        refreshModes()
        modes.next()
    }

    fun restoreMode(savedIndex: Int, savedId: String?) {
        refreshModes()
        modes.restore(savedIndex, savedId)
    }

    fun invalidate() = recipes.clear()

    fun findRecipes(items: Array<ItemStack>): List<GTRecipe> {
        val groups = access.templatesByCircuit()
        refreshModes(groups.values.flatten())
        val mode = modes.current ?: return emptyList()
        val suppliedItems = items.filterNotNull()
        val circuits = suppliedItems.filter { it.stackSize > 0 && ItemList.Circuit_Integrated.isStackEqual(it, true, true) }
            .distinctBy { it.itemDamage }

        return selectRockBreakerCandidates(groups, circuits.map { it.itemDamage }).mapNotNull { selection ->
            val source = selection.template
            if (environmentMode(source)?.id != mode.id || source.duration <= 0 || source.output.stackSize <= 0) {
                return@mapNotNull null
            }
            val environmentalInputs = rockBreakerResources(source.top, source.bottom, source.sides, source.anywhere) {
                resourceStack(it)?.let { stack -> ProxyStackKey.of(stack) }
            }?.map { key -> ItemStack(key.item, 0, key.meta) } ?: return@mapNotNull null
            val circuit = circuits.firstOrNull { it.itemDamage == selection.circuit }
            val catalysts = environmentalInputs + listOfNotNull(circuit)
            val inputs = buildRockBreakerInputs(
                catalysts,
                source.input,
                source.consumesInput,
                suppliedItems,
                { it.stackSize },
                { supplied, requested -> GTUtility.areStacksEqual(supplied, requested, true) },
                { supplied, requested -> GTUtility.areStacksEqual(supplied, requested) },
                { stack, count -> stack.copy().also { it.stackSize = count } },
            ) ?: return@mapNotNull null
            val key = TemplateKey(
                mode.id,
                source.circuit,
                inputs.map { ProxyStackKey.of(it) },
                ProxyStackKey.of(source.output),
                source.duration,
            )
            recipes.getOrCreate(key) {
                GTValues.RA.stdBuilder()
                    .itemInputsUnified(*inputs.toTypedArray())
                    .itemOutputs(source.output.copy())
                    .duration(source.duration)
                    .eut(TierEU.RECIPE_LV)
                    .nbtSensitive()
                    .noBuffer()
                    .build().orElse(null)
            }
        }
    }

    /** Avoid GT's oredict unification: source RockBreakerRecipe.testInputs uses exact item/meta/NBT. */
    fun maxParallel(
        recipe: GTRecipe,
        maxParallel: Int,
        @Suppress("UNUSED_PARAMETER") fluids: Array<FluidStack>,
        items: Array<ItemStack>,
    ): Double = inputPlan(recipe, maxParallel, items.filterNotNull()).parallel

    /** Only charged source input is decremented; buckets, block catalysts and circuits remain unchanged. */
    fun consumeInputs(
        recipe: GTRecipe,
        amount: Int,
        @Suppress("UNUSED_PARAMETER") fluids: Array<FluidStack>,
        items: Array<ItemStack>,
    ) {
        if (amount <= 0) return
        val supplied = items.filterNotNull()
        val plan = inputPlan(recipe, amount, supplied)
        if (plan.parallel < amount) return
        supplied.forEachIndexed { index, item -> item.stackSize -= plan.deductions[index] }
    }

    private fun inputPlan(recipe: GTRecipe, maximum: Int, items: List<ItemStack>): RockBreakerInputPlan =
        planRockBreakerInputs(
            maximum,
            recipe.mInputs.map { RockBreakerItem(it, it.stackSize) },
            items.map { RockBreakerItem(it, it.stackSize) },
        ) { supplied, required -> GTUtility.areStacksEqual(supplied, required) }

    private fun refreshModes(templates: List<RockBreakerTemplate> = access.allTemplates()) {
        modes.replace(templates.mapNotNull(::environmentMode).distinctBy { it.id }.sortedBy { it.id })
        modesInitialized = true
    }

    private fun ensureModes() {
        if (!modesInitialized) refreshModes()
    }

    private fun environmentMode(source: RockBreakerTemplate): RockBreakerMode? {
        fun registryName(block: Block): String? = Block.blockRegistry.getNameForObject(block)
        val top = source.top?.let { registryName(it) ?: return null }
        val bottom = source.bottom?.let { registryName(it) ?: return null }
        val sides = source.sides.map { registryName(it) ?: return null }
        val anywhere = source.anywhere.map { registryName(it) ?: return null }
        val name = buildList {
            source.top?.let { add(localize("nyx.machine.proxy.rock.top", it.localizedName)) }
            source.bottom?.let { add(localize("nyx.machine.proxy.rock.bottom", it.localizedName)) }
            if (source.sides.isNotEmpty()) add(localize("nyx.machine.proxy.rock.sides", source.sides.distinct().joinToString { it.localizedName }))
            if (source.anywhere.isNotEmpty()) add(localize("nyx.machine.proxy.rock.anywhere", source.anywhere.distinct().joinToString { it.localizedName }))
        }.joinToString("; ").ifEmpty { localize("nyx.machine.proxy.rock.none") }
        return RockBreakerMode(rockBreakerEnvironmentId(top, bottom, sides, anywhere), name)
    }

    private fun resourceStack(block: Block): ItemStack? = when (block) {
        Blocks.water, Blocks.flowing_water -> ItemStack(Items.water_bucket, 0)
        Blocks.lava, Blocks.flowing_lava -> ItemStack(Items.lava_bucket, 0)
        else -> Item.getItemFromBlock(block)?.let { ItemStack(it, 0, 0) }
    }
}
