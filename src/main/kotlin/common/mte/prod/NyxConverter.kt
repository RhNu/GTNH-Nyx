package rhynia.nyx.common.mte.prod

import com.gtnewhorizons.modularui.api.math.Alignment
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn
import com.gtnewhorizons.modularui.common.widget.SlotWidget
import com.gtnewhorizons.modularui.common.widget.TextWidget
import gregtech.api.GregTechAPI
import gregtech.api.enums.HatchElement.InputBus
import gregtech.api.enums.HatchElement.OutputBus
import gregtech.api.enums.OrePrefixes
import gregtech.api.enums.Textures.BlockIcons.OVERLAY_DTPF_OFF
import gregtech.api.enums.Textures.BlockIcons.OVERLAY_DTPF_ON
import gregtech.api.interfaces.IHatchElement
import gregtech.api.interfaces.IIconContainer
import gregtech.api.interfaces.metatileentity.IMetaTileEntity
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.logic.ProcessingLogic
import gregtech.api.recipe.check.CheckRecipeResult
import gregtech.api.recipe.check.CheckRecipeResultRegistry
import gregtech.api.util.MultiblockTooltipBuilder
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.EnumChatFormatting
import rhynia.nyx.api.enums.CommonString
import rhynia.nyx.api.item.MetaItemToken
import rhynia.nyx.api.item.asToken
import rhynia.nyx.api.util.getItemOrNull
import rhynia.nyx.api.util.localize
import rhynia.nyx.api.util.setItem
import rhynia.nyx.common.mte.base.NyxMTECubeBase
import rhynia.nyx.init.MaterialMapper

class NyxConverter : NyxMTECubeBase<NyxConverter> {
    constructor(
        aId: Int,
        aName: String,
    ) : super(aId, aName)

    constructor(aName: String) : super(aName)

    override fun newMetaEntity(aTileEntity: IGregTechTileEntity?): IMetaTileEntity = NyxConverter(mName)

    private var pControllerToken: MetaItemToken = MetaItemToken.EMPTY
    private var pOrePrefix: OrePrefixes? = null
    private val pMaterialCredits = Object2LongOpenHashMap<String>()

    private class MaterialBatch(
        val material: MaterialMapper.MaterialData,
    ) {
        val inputStacks = mutableListOf<ItemStack>()
        var incomingAmount = 0L
        var overflowed = false
    }

    private data class PlannedBatch(
        val batch: MaterialBatch,
        val remainder: Long,
    )

    override fun createProcessingLogic(): ProcessingLogic? = null

    override fun checkProcessing(): CheckRecipeResult {
        val controllerStack = controllerSlot ?: return pStop(true)
        val currentToken = controllerStack.asToken()
        if (currentToken.isEmpty()) return pStop(true)

        if (isRecipeLockingEnabled) {
            if (pControllerToken.isEmpty() || pOrePrefix == null) {
                return pStop(true)
            }
            val currentPrefix = MaterialMapper.lookupOrePrefix(controllerStack) ?: return pStop(true)
            if (currentToken != pControllerToken || currentPrefix !== pOrePrefix) {
                return pStop(true)
            }
        } else {
            pOrePrefix = MaterialMapper.lookupOrePrefix(controllerStack) ?: return pStop(true)
            pControllerToken = currentToken
        }

        val targetPrefix = pOrePrefix!!
        val targetMaterialAmount = targetPrefix.materialAmount
        if (targetMaterialAmount <= 0) return pStop(true)

        val inputItem = storedInputs.also { it.remove(controllerStack) }
        val batches =
            Object2ObjectOpenHashMap<MaterialMapper.MaterialData, MaterialBatch>(
                inputItem.size + pMaterialCredits.size,
            )

        // One pass over all input slots: lookup once, then aggregate exact integer material amounts.
        for (stack in inputItem) {
            if (stack.stackSize <= 0) continue
            val lookup = MaterialMapper.lookup(stack) ?: continue
            if (!lookup.material.hasOrePrefix(targetPrefix)) continue

            val batch =
                batches[lookup.material]
                    ?: MaterialBatch(lookup.material).also { batches[lookup.material] = it }
            batch.inputStacks.add(stack)
            if (batch.overflowed) continue

            try {
                val stackAmount = Math.multiplyExact(lookup.materialAmount, stack.stackSize.toLong())
                batch.incomingAmount = Math.addExact(batch.incomingAmount, stackAmount)
            } catch (_: ArithmeticException) {
                // Do not consume an unrepresentable batch; unsafe oversized stacks must not wrap.
                batch.overflowed = true
            }
        }

        // A target-prefix change can make an existing remainder large enough to emit by itself.
        val creditIterator = pMaterialCredits.object2LongEntrySet().fastIterator()
        while (creditIterator.hasNext()) {
            val entry = creditIterator.next()
            if (entry.longValue <= 0) continue
            val material = MaterialMapper.lookupMaterial(entry.key) ?: continue
            if (!material.hasOrePrefix(targetPrefix)) continue
            if (!batches.containsKey(material)) {
                batches[material] = MaterialBatch(material)
            }
        }

        if (batches.isEmpty()) return pStop()

        val outputs = mutableListOf<ItemStack>()
        val plans = mutableListOf<PlannedBatch>()

        for (batch in batches.values) {
            if (batch.overflowed) continue

            val materialPlan =
                planMaterialBalance(
                    pMaterialCredits.getLong(batch.material.key),
                    batch.incomingAmount,
                    targetMaterialAmount,
                ) ?: continue
            if (materialPlan.outputCount <= 0) continue

            // Build every output before touching inputs so failures remain atomic.
            var remaining = materialPlan.outputCount
            val outputStart = outputs.size
            while (remaining > 0) {
                val currentAmount = minOf(remaining, Int.MAX_VALUE.toLong()).toInt()
                val output = batch.material.getByOrePrefixUnsafe(targetPrefix, currentAmount)
                if (output == null) {
                    while (outputs.size > outputStart) outputs.removeAt(outputs.lastIndex)
                    break
                }
                outputs.add(output)
                remaining -= currentAmount
            }
            if (remaining > 0) continue

            plans.add(PlannedBatch(batch, materialPlan.remainder))
        }

        if (plans.isEmpty()) return pStop()

        val outputArray = outputs.toTypedArray()
        if (!canOutputAll(outputArray)) {
            return pStop(result = CheckRecipeResultRegistry.ITEM_OUTPUT_FULL)
        }

        // Commit the already-validated plan. Whole stacks are consumed and exact leftovers become
        // persistent material credit, so no indivisible-item rounding or material loss is possible.
        for ((batch, remainder) in plans) {
            batch.inputStacks.forEach { it.stackSize = 0 }
            if (remainder > 0) {
                pMaterialCredits.put(batch.material.key, remainder)
            } else {
                pMaterialCredits.removeLong(batch.material.key)
            }
        }

        mOutputItems = outputArray
        updateSlots()
        return pStart(plans.size * 20)
    }

    private fun pStop(
        clearOrePrefix: Boolean = false,
        result: CheckRecipeResult = CheckRecipeResultRegistry.NO_RECIPE,
    ): CheckRecipeResult {
        if (clearOrePrefix) pOrePrefix = null
        mEfficiency = 0
        mEfficiencyIncrease = 0
        mMaxProgresstime = 0
        mProgresstime = 0

        return result
    }

    private fun pStart(processTime: Int): CheckRecipeResult {
        mEfficiency = 10000
        mEfficiencyIncrease = 10000
        mMaxProgresstime = processTime

        return CheckRecipeResultRegistry.SUCCESSFUL
    }

    override fun drawTexts(
        screenElements: DynamicPositionedColumn,
        inventorySlot: SlotWidget,
    ) {
        super.drawTexts(screenElements, inventorySlot)
        screenElements.apply {
            widget(
                TextWidget
                    .dynamicString {
                        "${localize(locPrefixed("gui.t.0"))}: ${EnumChatFormatting.AQUA}${
                            pOrePrefix?.defaultLocalName ?: "EMPTY"
                        }"
                    }.setSynced(true)
                    .setTextAlignment(Alignment.CenterLeft)
                    .setEnabled { baseMTE.isActive },
            )
        }
    }

    override fun supportsInputSeparation(): Boolean = false

    override fun supportsBatchMode(): Boolean = false

    override val sCasingBlock: Pair<Block, Int>
        get() = GregTechAPI.sBlockCasings2 to 0

    override val sCasingHatch: Array<IHatchElement<in NyxConverter>>
        get() = arrayOf(InputBus, OutputBus)

    override val sControllerIcon: Pair<IIconContainer, IIconContainer>
        get() = OVERLAY_DTPF_OFF to OVERLAY_DTPF_OFF

    override val sControllerIconActive: Pair<IIconContainer, IIconContainer>
        get() = OVERLAY_DTPF_ON to OVERLAY_DTPF_ON

    override fun createTooltip(): MultiblockTooltipBuilder? =
        MultiblockTooltipBuilder()
            .addMachineTypeLocalized()
            .addInfoListLocalized(2)
            .addChangeModeByScrewdriver()
            .beginStructureCube()
            .addInputBus()
            .addOutputBus()
            .toolTipFinisher(CommonString.NyxGigaFac)

    override fun loadNBTData(aNBT: NBTTagCompound) {
        super.loadNBTData(aNBT)
        pControllerToken = aNBT.getItemOrNull("pControllerToken")?.asToken() ?: MetaItemToken.EMPTY
        pOrePrefix =
            aNBT.getString("pOrePrefix").let {
                if (it.isEmpty()) null else OrePrefixes.getPrefix(it)
            }

        pMaterialCredits.clear()
        val credits = aNBT.getTagList(NBT_CREDITS, 10)
        for (i in 0 until credits.tagCount()) {
            val credit = credits.getCompoundTagAt(i)
            val key = credit.getString(NBT_CREDIT_KEY)
            val amount = credit.getLong(NBT_CREDIT_AMOUNT)
            if (key.isNotEmpty() && amount > 0) {
                pMaterialCredits.put(key, amount)
            }
        }
    }

    override fun saveNBTData(aNBT: NBTTagCompound) {
        super.saveNBTData(aNBT)
        aNBT.setItem("pControllerToken", pControllerToken.createStack())
        aNBT.setString("pOrePrefix", pOrePrefix?.name ?: "")

        val credits = NBTTagList()
        val iterator = pMaterialCredits.object2LongEntrySet().fastIterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.longValue <= 0) continue
            credits.appendTag(
                NBTTagCompound().apply {
                    setString(NBT_CREDIT_KEY, entry.key)
                    setLong(NBT_CREDIT_AMOUNT, entry.longValue)
                },
            )
        }
        aNBT.setTag(NBT_CREDITS, credits)
    }

    private companion object {
        const val NBT_CREDITS = "pMaterialCredits"
        const val NBT_CREDIT_KEY = "material"
        const val NBT_CREDIT_AMOUNT = "amount"
    }
}
