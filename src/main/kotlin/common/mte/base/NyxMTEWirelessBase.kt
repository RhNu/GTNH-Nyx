package rhynia.nyx.common.mte.base

/*
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.logic.ProcessingLogic
import gregtech.api.recipe.check.CheckRecipeResult
import gregtech.api.recipe.check.CheckRecipeResultRegistry
import gregtech.api.util.GTRecipe
import gregtech.api.util.OverclockCalculator
import gregtech.common.misc.WirelessNetworkManager.addEUToGlobalEnergyMap
import net.minecraft.nbt.NBTTagCompound
import org.jetbrains.annotations.ApiStatus
import java.util.UUID

/**
 * Base class for machines that can operate in wireless mode.
 *
 * Credit: Twist-Space-Technology-Mod
 */
abstract class NyxMTEWirelessBase<T : NyxMTEWirelessBase<T>> : NyxMTEBase<T> {
    protected constructor(
        aId: Int,
        aName: String,
    ) : super(aId, aName)

    protected constructor(aName: String) : super(aName)

    /**
     * The default wireless mode of the machine, if the machine supports wireless mode, this value
     * will be used as the initial value of the wireless mode when the machine is first placed
     * in the world.
     */
    open val pWirelessDefault: Boolean
        @ApiStatus.OverrideOnly
        get() = false

    /**
     * The processing time in ticks when the machine is in wireless mode.
     * Default is 5 seconds (5 * 20 ticks).
     */
    open val pWirelessProcessingTime: Int
        @ApiStatus.OverrideOnly
        get() = 5 * 20

    /**
     * Whether the machine is in wireless mode or not.
     */
    var pWireless: Boolean = pWirelessDefault
        protected set

    protected var pUUID: UUID? = null
    protected var pRecipeProcessing: Boolean = false

    override fun createProcessingLogic(): ProcessingLogic? =
        object : ProcessingLogic() {
            override fun process(): CheckRecipeResult {
                setEuModifier(rEuModifier)
                setSpeedBonus(rTimeModifier)
                setOverclock(rOverclockType.timeDec, rOverclockType.powerInc)
                return super.process()
            }

            override fun createOverclockCalculator(recipe: GTRecipe): OverclockCalculator =
                if (pWireless) OverclockCalculator.ofNoOverclock(recipe) else super.createOverclockCalculator(recipe)
        }.setMaxParallelSupplier(::rMaxParallel)

    override fun checkProcessing(): CheckRecipeResult {
        if (!pWireless) return super.checkProcessing()

        val result = wirelessProcess()
        updateSlots()

        if (!result.wasSuccessful()) return result

        mEfficiency = 10000
        mEfficiencyIncrease = 10000
        mMaxProgresstime = pWirelessProcessingTime

        return result
    }

    protected fun wirelessProcess(): CheckRecipeResult {
        if (!pRecipeProcessing) startRecipeProcessing()
        setupProcessingLogic(processingLogic)

        val result = doCheckRecipe().also { if (!it.wasSuccessful()) return it }
        val cost =
            processingLogic.calculatedEut.toBigInteger() *
                processingLogic.duration.toLong().toBigInteger()

        if (!addEUToGlobalEnergyMap(pUUID, -cost)) {
            return CheckRecipeResultRegistry.insufficientPower(cost.toLong())
        }

        mOutputItems += processingLogic.outputItems
        mOutputFluids += processingLogic.outputFluids

        endRecipeProcessing()
        return result
    }

    override fun onFirstTick(aBaseMetaTileEntity: IGregTechTileEntity?) {
        super.onFirstTick(aBaseMetaTileEntity)
        pUUID = baseMTE.ownerUuid
    }

    override fun startRecipeProcessing() {
        super.startRecipeProcessing()
        pRecipeProcessing = true
    }

    override fun endRecipeProcessing() {
        super.endRecipeProcessing()
        pRecipeProcessing = false
    }

    override fun saveNBTData(aNBT: NBTTagCompound) {
        super.saveNBTData(aNBT)
        aNBT.setBoolean("pWireless", pWireless)
    }

    override fun loadNBTData(aNBT: NBTTagCompound) {
        super.loadNBTData(aNBT)
        pWireless = aNBT.getBoolean("pWireless")
    }
}
*/
