package rhynia.nyx.common.mte.base

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil
import gregtech.api.enums.Textures
import gregtech.api.interfaces.ITexture
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.MetaTileEntity
import gregtech.api.metatileentity.implementations.MTEHatchInput
import gregtech.api.objects.XSTR
import gregtech.api.render.TextureFactory
import gregtech.api.util.GTUtility
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidStack
import rhynia.nyx.api.enums.CommonString.AddByNyx

@Suppress("UNUSED", "SpellCheckingInspection")
abstract class NyxHatchFluidGenerator : MTEHatchInput {
    constructor(
        aID: Int,
        aName: String,
        aNameRegional: String,
        aTier: Int,
    ) : super(aID, aName, aNameRegional, aTier)

    constructor(
        aName: String,
        aTier: Int,
        aDescription: Array<String>,
        aTextures: Array<Array<Array<ITexture>>>?,
    ) : super(aName, aTier, aDescription, aTextures)

    abstract override fun newMetaEntity(aTileEntity: IGregTechTileEntity?): MetaTileEntity?

    var mProgresstime: Int = 0
    var mMaxProgresstime: Int = 0

    companion object {
        protected val floatGen: XSTR = XSTR()
    }

    abstract val customTooltip: Array<String>

    abstract val fluidToGenerate: Fluid

    override fun getDescription(): Array<String> {
        val capacityString = "容量: ${NumberFormatUtil.formatNumber(capacity.toLong())}L"
        val hatchTierString = "仓室等级: ${GTUtility.getColoredTierNameFromTier(mTier)}"
        return buildList {
            addAll(mDescriptionArray)
            add(capacityString)
            add(hatchTierString)
            addAll(customTooltip)
            add(AddByNyx)
        }.filterNotNull().toTypedArray()
    }

    override fun getTexturesActive(aBaseTexture: ITexture?): Array<ITexture?> =
        arrayOf(aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_FUSION1))

    override fun getTexturesInactive(aBaseTexture: ITexture?): Array<ITexture?> =
        arrayOf(aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_FUSION1))

    override fun isFacingValid(facing: ForgeDirection?): Boolean = true

    override fun isAccessAllowed(aPlayer: EntityPlayer?): Boolean = true

    override fun isValidSlot(aIndex: Int): Boolean = false

    override fun allowPullStack(
        aBaseMetaTileEntity: IGregTechTileEntity?,
        aIndex: Int,
        side: ForgeDirection?,
        aStack: ItemStack?,
    ): Boolean = false

    override fun allowPutStack(
        aBaseMetaTileEntity: IGregTechTileEntity?,
        aIndex: Int,
        side: ForgeDirection?,
        aStack: ItemStack?,
    ): Boolean = false

    override fun onPostTick(
        aBaseMetaTileEntity: IGregTechTileEntity,
        aTick: Long,
    ) {
        super.onPostTick(aBaseMetaTileEntity, aTick)
        if (!aBaseMetaTileEntity.isAllowedToWork) {
            aBaseMetaTileEntity.isActive = false
            mProgresstime = 0
            mMaxProgresstime = 0
        } else {
            aBaseMetaTileEntity.isActive = true
            mMaxProgresstime = 100
            if (++mProgresstime >= mMaxProgresstime) {
                if (this.canTankBeFilled()) {
                    addFluidToHatch(aTick)
                }
                mProgresstime = 0
            }
        }
    }

    override fun getProgresstime(): Int = mProgresstime

    override fun maxProgresstime(): Int = mMaxProgresstime

    override fun increaseProgress(aProgress: Int): Int {
        mProgresstime += aProgress
        return mMaxProgresstime - mProgresstime
    }

    override fun getCapacity(): Int = 2_000_000_000

    override fun canTankBeEmptied(): Boolean = true

    override fun canTankBeFilled(): Boolean = true

    override fun doesEmptyContainers(): Boolean = false

    override fun doesFillContainers(): Boolean = true

    fun addFluidToHatch(aTick: Long): Boolean = this.fill(FluidStack(fluidToGenerate, getCapacity()), true) > 0

    override fun fill(
        aFluid: FluidStack?,
        doFill: Boolean,
    ): Int {
        if (baseMetaTileEntity == null ||
            aFluid == null ||
            aFluid.getFluid().id <= 0 ||
            aFluid.amount <= 0 ||
            aFluid.getFluid() != fluidToGenerate ||
            !canTankBeFilled()
        ) {
            return 0
        }

        if (fillableStack == null || fillableStack.getFluid().id <= 0) {
            if (aFluid.amount <= getCapacity()) {
                if (doFill) {
                    setFillableStack(aFluid.copy())
                    baseMetaTileEntity!!.markDirty()
                }
                return aFluid.amount
            }
            if (doFill) {
                setFillableStack(aFluid.copy())
                fillableStack.amount = getCapacity()
                baseMetaTileEntity!!.markDirty()
            }
            return getCapacity()
        }

        if (!fillableStack.isFluidEqual(aFluid)) return 0

        val space = getCapacity() - fillableStack.amount
        if (aFluid.amount <= space) {
            if (doFill) {
                fillableStack.amount += aFluid.amount
                baseMetaTileEntity!!.markDirty()
            }
            return aFluid.amount
        }
        if (doFill) fillableStack.amount = getCapacity()
        return space
    }

    override fun canFill(
        aSide: ForgeDirection?,
        aFluid: Fluid?,
    ): Boolean = false

    override fun fill(
        arg0: ForgeDirection?,
        arg1: FluidStack?,
        arg2: Boolean,
    ): Int = 0

    override fun saveNBTData(aNBT: NBTTagCompound) {
        aNBT.setInteger("mProgresstime", mProgresstime)
        aNBT.setInteger("mMaxProgresstime", mMaxProgresstime)
        super.saveNBTData(aNBT)
    }

    override fun loadNBTData(aNBT: NBTTagCompound) {
        mProgresstime = aNBT.getInteger("mProgresstime")
        mMaxProgresstime = aNBT.getInteger("mMaxProgresstime")
        super.loadNBTData(aNBT)
    }
}
