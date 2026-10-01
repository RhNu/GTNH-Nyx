package rhynia.nyx.common.mte.base

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable
import com.gtnewhorizon.structurelib.structure.IStructureDefinition
import com.gtnewhorizons.modularui.api.screen.ModularWindow
import com.gtnewhorizons.modularui.api.screen.UIBuildContext
import com.gtnewhorizons.modularui.api.widget.Widget
import gregtech.api.enums.Textures
import gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE
import gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE
import gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW
import gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_GLOW
import gregtech.api.interfaces.IHatchElement
import gregtech.api.interfaces.IIconContainer
import gregtech.api.interfaces.ITexture
import gregtech.api.interfaces.metatileentity.IMetaTileEntity
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.logic.ProcessingLogic
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase
import gregtech.api.metatileentity.implementations.MTEHatch
import gregtech.api.metatileentity.implementations.MTEHatchDynamo
import gregtech.api.render.TextureFactory
import gregtech.api.util.GTUtility
import gregtech.api.util.GTUtility.filterValidMTEs
import gregtech.api.util.IGTHatchAdder
import gregtech.api.util.MultiblockTooltipBuilder
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumChatFormatting.AQUA
import net.minecraft.util.EnumChatFormatting.GOLD
import net.minecraft.util.StatCollector
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidStack
import org.jetbrains.annotations.ApiStatus.OverrideOnly
import rhynia.nyx.api.enums.CommonString
import rhynia.nyx.api.interfaces.mte.ProcessInfo
import rhynia.nyx.api.process.NyxAutoProcessingLogic
import rhynia.nyx.api.util.idEqual
import rhynia.nyx.api.util.localize
import rhynia.nyx.api.util.size
import rhynia.nyx.common.mte.hatch.MTEHatchAddition
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoMulti

@Suppress("UNUSED", "NOTHING_TO_INLINE")
abstract class NyxMTEBase<T : MTEExtendedPowerMultiBlockBase<T>> :
    MTEExtendedPowerMultiBlockBase<T>,
    ISurvivalConstructable,
    ProcessInfo {
    protected constructor(
        aID: Int,
        aName: String,
    ) : super(
        aID,
        aName,
        localize("$aName.name"),
    )

    protected constructor(aName: String) : super(aName)

    protected companion object {
        const val STRUCTURE_PIECE_MAIN = "Main" // Just a name

        /** Simulated Hatch Element for Exotic Energy Hatch which has multiple allowed ampere. */
        val ExoticDynamo =
            object : IHatchElement<NyxMTEBase<*>> {
                override fun mteClasses(): List<Class<out IMetaTileEntity>> = listOf(MTEHatchDynamoMulti::class.java)

                override fun adder(): IGTHatchAdder<in NyxMTEBase<*>> =
                    IGTHatchAdder<NyxMTEBase<*>> { c, t, i -> c.addDynamoToMachineList(t, i.toInt()) }

                override fun name(): String = "ExoticDynamo"

                override fun count(t: NyxMTEBase<*>?): Long {
                    return (t ?: return 0).mExoticDynamoHatches.size.toLong()
                }
            }

        val AdditionHatch =
            object : IHatchElement<NyxMTEBase<*>> {
                override fun mteClasses(): List<Class<out IMetaTileEntity>> = listOf(MTEHatchAddition::class.java)

                override fun adder(): IGTHatchAdder<in NyxMTEBase<*>> =
                    IGTHatchAdder<NyxMTEBase<*>> { c, t, i -> c.addAdditionHatchToMachineList(t, i.toInt()) }

                override fun name(): String = "AdditionHatch"

                override fun count(t: NyxMTEBase<*>?): Long {
                    return (t ?: return 0).mAdditionHatches.size.toLong()
                }
            }

        /**
         * Structure Definition for the machine, set when first time calling getStructureDefinition().
         *
         * Keyed by the java class to keep the lookup allocation free on the structure check path.
         */
        val cachedStructureDefs = mutableMapOf<Class<out NyxMTEBase<*>>, IStructureDefinition<*>>()

        val infoMaxParallel by lazy { localize("nyx.common.info.maxParallel") }
        val infoTimeModifier by lazy { localize("nyx.common.info.timeModifier") }
        val infoEuModifier by lazy { localize("nyx.common.info.euModifier") }
    }

    protected inline val baseMTE get() = baseMetaTileEntity!!

    /** Remove maintenance requirement. */
    protected fun removeMaintenance() {
        mHardHammer = true
        mSoftMallet = true
        mScrewdriver = true
        mCrowbar = true
        mSolderingTool = true
        mWrench = true
    }

    protected val mExoticDynamoHatches: MutableList<MTEHatchDynamoMulti> = mutableListOf()

    protected val mAdditionHatches: MutableList<MTEHatchAddition> = mutableListOf()

    /**
     * Universal Hatch Adder for both normal and exotic energy and dynamo hatches.
     */
    final override fun addToMachineList(
        aTileEntity: IGregTechTileEntity?,
        aBaseCasingIndex: Int,
    ): Boolean =
        super.addToMachineList(aTileEntity, aBaseCasingIndex) ||
            addExoticEnergyInputToMachineList(aTileEntity, aBaseCasingIndex) ||
            addExoticDynamoToMachineList(aTileEntity, aBaseCasingIndex) ||
            addAdditionHatchToMachineList(aTileEntity, aBaseCasingIndex)

    fun addToMachineListCompatible(
        aTileEntity: IGregTechTileEntity?,
        aBaseCasingIndex: Short,
    ): Boolean = addToMachineList(aTileEntity, aBaseCasingIndex.toInt())

    final override fun addEnergyInputToMachineList(
        aTileEntity: IGregTechTileEntity?,
        aBaseCasingIndex: Int,
    ): Boolean =
        super.addEnergyInputToMachineList(aTileEntity, aBaseCasingIndex) ||
            addExoticEnergyInputToMachineList(aTileEntity, aBaseCasingIndex)

    final override fun addDynamoToMachineList(
        aTileEntity: IGregTechTileEntity?,
        aBaseCasingIndex: Int,
    ): Boolean {
        val mte = aTileEntity?.metaTileEntity ?: return false
        when (mte) {
            is MTEHatchDynamo -> {
                mte.updateTexture(aBaseCasingIndex)
                mte.updateCraftingIcon(machineCraftingIcon)
                return mDynamoHatches.add(mte)
            }
            is MTEHatchDynamoMulti -> {
                mte.updateTexture(aBaseCasingIndex)
                mte.updateCraftingIcon(machineCraftingIcon)
                return mExoticDynamoHatches.add(mte)
            }
            else -> return false
        }
    }

    override fun addExoticDynamoToMachineList(
        aTileEntity: IGregTechTileEntity?,
        aBaseCasingIndex: Int,
    ): Boolean {
        val mte = aTileEntity?.metaTileEntity ?: return false
        when (mte) {
            is MTEHatchDynamoMulti -> {
                mte.updateTexture(aBaseCasingIndex)
                mte.updateCraftingIcon(machineCraftingIcon)
                return mExoticDynamoHatches.add(mte)
            }

            else -> return false
        }
    }

    fun addAdditionHatchToMachineList(
        aTileEntity: IGregTechTileEntity?,
        aBaseCasingIndex: Int,
    ): Boolean {
        val mte = aTileEntity?.metaTileEntity ?: return false
        when (mte) {
            is MTEHatchAddition -> {
                mte.updateTexture(aBaseCasingIndex)
                mte.updateCraftingIcon(machineCraftingIcon)
                return mAdditionHatches.add(mte)
            }

            else -> return false
        }
    }

    final override fun addEnergyOutputMultipleDynamos(
        aEU: Long,
        aAllowMixedVoltageDynamos: Boolean,
    ): Boolean {
        var tEU = aEU
        if (tEU < 0) tEU = -aEU

        fun processHatches(
            hatches: List<MTEHatch>,
            tEU: Long,
        ): Boolean {
            var remainingEU = tEU
            var freeCap = 0L

            filterValidMTEs(hatches).forEach {
                freeCap += it.maxEUStore() - it.baseMetaTileEntity!!.storedEU
                if (freeCap > 0) {
                    if (remainingEU >= freeCap) {
                        it.euVar = it.maxEUStore()
                        remainingEU -= freeCap
                    } else {
                        it.euVar = it.baseMetaTileEntity!!.storedEU + remainingEU
                        return true
                    }
                }
            }
            return false
        }

        if (processHatches(mDynamoHatches, tEU)) return true
        if (processHatches(mExoticDynamoHatches, tEU)) return true

        return false
    }

    final override fun clearHatches() {
        super.clearHatches()
        mExoticDynamoHatches.clear()
    }

    /** Set the structure definition for the machine. */
    protected abstract fun genStructureDefinition(): IStructureDefinition<T>

    @Suppress("UNCHECKED_CAST")
    final override fun getStructureDefinition(): IStructureDefinition<T> =
        cachedStructureDefs.getOrPut(javaClass) { genStructureDefinition() }
            as IStructureDefinition<T>

    /** Controller Block and Meta, used for calculating casing texture index. */
    protected abstract val sControllerBlock: Pair<Block, Int>

    protected open val sControllerCasingIndex: Int by lazy {
        GTUtility.getCasingTextureIndex(sControllerBlock.first, sControllerBlock.second)
    }

    /** Controller Icon for active state, left is normal, right is glow. */
    protected open val sControllerIconActive: Pair<IIconContainer, IIconContainer>
        get() = OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE to OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW

    /** Controller Icon for inactive state, left is normal, right is glow. */
    protected open val sControllerIcon: Pair<IIconContainer, IIconContainer>
        get() = OVERLAY_FRONT_ASSEMBLY_LINE to OVERLAY_FRONT_ASSEMBLY_LINE_GLOW

    // getTexture() is hit by the renderer for every visible side of every frame, so the three
    // possible results are built once instead of allocating a new array per call.
    private val texturesSide: Array<ITexture> by lazy {
        arrayOf(Textures.BlockIcons.getCasingTextureForId(sControllerCasingIndex))
    }

    private val texturesFrontActive: Array<ITexture> by lazy { buildFrontTextures(sControllerIconActive) }

    private val texturesFrontInactive: Array<ITexture> by lazy { buildFrontTextures(sControllerIcon) }

    private fun buildFrontTextures(icons: Pair<IIconContainer, IIconContainer>): Array<ITexture> =
        arrayOf(
            Textures.BlockIcons.getCasingTextureForId(sControllerCasingIndex),
            TextureFactory
                .builder()
                .addIcon(icons.first)
                .extFacing()
                .build(),
            TextureFactory
                .builder()
                .addIcon(icons.second)
                .extFacing()
                .glow()
                .build(),
        )

    override fun getTexture(
        baseMetaTileEntity: IGregTechTileEntity,
        side: ForgeDirection,
        facing: ForgeDirection,
        colorIndex: Int,
        active: Boolean,
        redstoneLevel: Boolean,
    ): Array<ITexture> =
        when {
            side != facing -> texturesSide
            active -> texturesFrontActive
            else -> texturesFrontInactive
        }

    override fun getMaxEfficiency(aStack: ItemStack?): Int = 100_00

    override fun getDamageToComponent(aStack: ItemStack?): Int = 0

    override fun supportsVoidProtection(): Boolean = true

    override fun supportsInputSeparation(): Boolean = true

    override fun supportsBatchMode(): Boolean = true

    override fun supportsSingleRecipeLocking(): Boolean = true

    override fun createProcessingLogic(): ProcessingLogic? = NyxAutoProcessingLogic(this)

    protected fun consumeFluid(
        fluid: Fluid,
        amount: Int,
    ): Boolean {
        // getStoredFluids() walks every input hatch and builds a fresh list, so snapshot it once.
        val stored = storedFluids ?: return false
        if (stored.isEmpty()) return false

        // Check if there is enough fluid stored.
        var available = 0L
        stored.forEach { if (it idEqual fluid) available += it.amount }
        if (available < amount) return false

        var remaining = amount
        stored.forEach {
            if (it idEqual fluid) {
                if (it.amount >= remaining) {
                    it.amount -= remaining
                    return true
                }
                remaining -= it.amount
                it.amount = 0
            }
        }

        return remaining <= 0
    }

    protected fun outputItem(
        item: ItemStack?,
        amount: Long,
    ): Boolean {
        if (item == null || amount <= 0) return false
        var amount = amount

        if (amount <= Int.MAX_VALUE) {
            addOutputAtomic(item.copy() size amount.toInt())
        } else {
            while (amount > Int.MAX_VALUE) {
                addOutputAtomic(item.copy() size Int.MAX_VALUE)
                amount -= Int.MAX_VALUE
            }
        }

        updateSlots()

        return true
    }

    protected fun outputFluid(
        fluid: FluidStack?,
        amount: Long,
    ): Boolean {
        if (fluid == null || amount <= 0) return false
        var amount = amount
        if (amount <= Int.MAX_VALUE) {
            addOutput(fluid.copy() size amount.toInt())
        } else {
            while (amount > Int.MAX_VALUE) {
                addOutput(fluid.copy() size Int.MAX_VALUE)
                amount -= Int.MAX_VALUE
            }
        }

        updateSlots()

        return true
    }

    protected fun outputItemToAENetwork(
        item: ItemStack?,
        amount: Long,
    ) {
        if ((item == null) || (amount <= 0)) return
        var amount = amount

        mOutputBusses
            .firstOrNull { it is MTEHatchOutputBusME }
            ?.let {
                it as MTEHatchOutputBusME
                if (amount < Int.MAX_VALUE) {
                    it.storePartial(item.copy() size amount, false)
                } else {
                    // For item stacks > Int max.
                    while (amount >= Int.MAX_VALUE) {
                        it.storePartial(item.copy() size Int.MAX_VALUE, false)
                        amount -= Int.MAX_VALUE.toLong()
                    }

                    if (amount > 0) {
                        it.storePartial(item.copy() size amount, false)
                    }
                }
            }
    }

    protected fun outputFluidToAENetwork(
        fluid: FluidStack?,
        amount: Long,
    ) {
        if ((fluid == null) || (amount <= 0)) return
        var amount = amount

        mOutputHatches
            .firstOrNull { it is MTEHatchOutputME }
            ?.let {
                it as MTEHatchOutputME
                if (amount < Int.MAX_VALUE) {
                    it.fill(fluid.copy().apply { this.amount = amount.toInt() }, true)
                } else {
                    // For fluidStacks > Int max.
                    while (amount >= Int.MAX_VALUE) {
                        it.fill(fluid.copy().apply { this.amount = Int.MAX_VALUE }, true)
                        amount -= Int.MAX_VALUE.toLong()
                    }

                    if (amount > 0) {
                        it.fill(fluid.copy().apply { this.amount = amount.toInt() }, true)
                    }
                }
            }
    }

    /** Format Double number as % */
    protected inline fun Double.formatPercent() = "%.3f%%".format(this * 100)

    final override fun getInfoData(): Array<String> =
        super.getInfoData() +
            arrayOf(
                "${AQUA}$infoMaxParallel: ${GOLD}${NumberFormatUtil.formatNumber(rMaxParallel.toLong())}",
                "${AQUA}$infoTimeModifier: ${GOLD}${rTimeModifier.formatPercent()}",
                "${AQUA}$infoEuModifier: ${GOLD}${rEuModifier.formatPercent()}",
            ) +
            getInfoDataExtra()

    /** Extra information added after [getInfoData] */
    @OverrideOnly
    protected open fun getInfoDataExtra(): Array<String> = arrayOf()

    protected fun MultiblockTooltipBuilder.addMachineTypeLocalized(): MultiblockTooltipBuilder =
        this.addMachineType(
            if (StatCollector.canTranslate("$mName.type")) {
                localize("$mName.type")
            } else {
                localize("$mName.name")
            },
        )

    protected fun MultiblockTooltipBuilder.addInfoLocalized(key: String): MultiblockTooltipBuilder = addInfo(localize(key))

    protected fun MultiblockTooltipBuilder.addInfoLocalized(index: Int): MultiblockTooltipBuilder = addInfo(localize("$mName.info.$index"))

    protected fun MultiblockTooltipBuilder.addInfoListLocalized(untilIndex: Int): MultiblockTooltipBuilder =
        apply {
            (0..untilIndex)
                .map { localize("$mName.info.$it") }
                .forEach { addInfo(it) }
        }

    protected fun MultiblockTooltipBuilder.addInfoLocalized(
        key: String,
        vararg args: Any,
    ): MultiblockTooltipBuilder = addInfo(localize(key, *args))

    protected fun MultiblockTooltipBuilder.addInfoLocalized(
        index: Int,
        vararg args: Any,
    ): MultiblockTooltipBuilder = addInfo(localize("$mName.info.$index", *args))

    protected fun MultiblockTooltipBuilder.addChangeModeByScrewdriver(): MultiblockTooltipBuilder =
        addInfo(CommonString.ChangeModeByScrewdriver)

    protected fun MultiblockTooltipBuilder.addInputBus(aDot: Int = 1): MultiblockTooltipBuilder =
        addInputBus(CommonString.BluePrintInfo, aDot)

    protected fun MultiblockTooltipBuilder.addInputHatch(aDot: Int = 1): MultiblockTooltipBuilder =
        addInputHatch(CommonString.BluePrintInfo, aDot)

    protected fun MultiblockTooltipBuilder.addOutputBus(aDot: Int = 1): MultiblockTooltipBuilder =
        addOutputBus(CommonString.BluePrintInfo, aDot)

    protected fun MultiblockTooltipBuilder.addOutputHatch(aDot: Int = 1): MultiblockTooltipBuilder =
        addOutputHatch(CommonString.BluePrintInfo, aDot)

    protected fun MultiblockTooltipBuilder.addEnergyHatch(aDot: Int = 1): MultiblockTooltipBuilder =
        addEnergyHatch(CommonString.BluePrintInfo, aDot)

    protected fun MultiblockTooltipBuilder.addDynamo(aDot: Int = 1): MultiblockTooltipBuilder =
        addDynamoHatch(CommonString.BluePrintInfo, aDot)

    override fun getWailaBody(
        itemStack: ItemStack?,
        currentTip: MutableList<String>,
        accessor: IWailaDataAccessor,
        config: IWailaConfigHandler?,
    ) {
        super.getWailaBody(itemStack, currentTip, accessor, config)
    }

    override fun getWailaNBTData(
        player: EntityPlayerMP?,
        tile: TileEntity?,
        tag: NBTTagCompound,
        world: World?,
        x: Int,
        y: Int,
        z: Int,
    ) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z)
    }

    override fun saveNBTData(aNBT: NBTTagCompound) {
        super.saveNBTData(aNBT)
    }

    override fun loadNBTData(aNBT: NBTTagCompound) {
        super.loadNBTData(aNBT)
    }

    final override fun addUIWidgets(
        builder: ModularWindow.Builder,
        buildContext: UIBuildContext?,
    ) {
        super.addUIWidgets(builder, buildContext)
        addRowUIWidgets()?.let {
            var startingX = 80
            it.forEach { w ->
                w.setPos(startingX, 91)
                builder.widget(w)
                startingX += 18
            }
        }
        addCustomUIWidgets(builder, buildContext)
    }

    /**
     * Add a row of custom UI widgets to the machine UI.
     * The widgets will be placed starting from (80, 91) with a gap of 18 pixels.
     *
     * Widgets added here don't need to set their position.
     *
     * Return null if no widgets are to be added.
     */
    open fun addRowUIWidgets(): List<Widget>? = null

    /**
     * Add custom UI widgets to the machine UI.
     */
    open fun addCustomUIWidgets(
        builder: ModularWindow.Builder,
        buildContext: UIBuildContext?,
    ) { }

    /**
     * Get a localization key prefixed with the machine name.
     */
    protected inline fun locPrefixed(key: String): String = "$mName.$key"
}
