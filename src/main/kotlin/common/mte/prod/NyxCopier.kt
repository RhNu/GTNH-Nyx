package rhynia.nyx.common.mte.prod

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil
import com.gtnewhorizons.modularui.api.math.Alignment
import com.gtnewhorizons.modularui.api.math.Color
import com.gtnewhorizons.modularui.api.screen.ModularWindow
import com.gtnewhorizons.modularui.api.screen.UIBuildContext
import com.gtnewhorizons.modularui.common.widget.CycleButtonWidget
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn
import com.gtnewhorizons.modularui.common.widget.SlotWidget
import com.gtnewhorizons.modularui.common.widget.TextWidget
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget
import gregtech.api.GregTechAPI
import gregtech.api.enums.HatchElement.OutputBus
import gregtech.api.enums.HatchElement.OutputHatch
import gregtech.api.enums.Textures.BlockIcons.OVERLAY_DTPF_OFF
import gregtech.api.enums.Textures.BlockIcons.OVERLAY_DTPF_ON
import gregtech.api.gui.modularui.GTUITextures
import gregtech.api.interfaces.IHatchElement
import gregtech.api.interfaces.IIconContainer
import gregtech.api.interfaces.metatileentity.IMetaTileEntity
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.logic.ProcessingLogic
import gregtech.api.metatileentity.BaseTileEntity
import gregtech.api.recipe.check.CheckRecipeResult
import gregtech.api.recipe.check.CheckRecipeResultRegistry
import gregtech.api.util.GTUtility
import gregtech.api.util.MultiblockTooltipBuilder
import gregtech.common.misc.WirelessNetworkManager.strongCheckOrAddUser
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.StatCollector
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.FluidStack
import rhynia.nyx.api.enums.CommonString
import rhynia.nyx.common.NyxItemList
import rhynia.nyx.common.material.NyxMaterials
import rhynia.nyx.common.mte.base.NyxMTECubeBase
import rhynia.nyx.config.ConfigMachine
import java.util.UUID

class NyxCopier : NyxMTECubeBase<NyxCopier> {
    constructor(
        aId: Int,
        aName: String,
    ) : super(aId, aName)

    constructor(aName: String) : super(aName)

    override fun newMetaEntity(aTileEntity: IGregTechTileEntity?): IMetaTileEntity = NyxCopier(mName)

    private var pItemMode = true
    private var pRunning = false
        set(value) {
            field = value
            if (value) {
                mMaxProgresstime = ConfigMachine.MTE_COPIER_TICK
                mEfficiency = 10000
                mEfficiencyIncrease = 10000
            } else {
                mMaxProgresstime = 0
                mEfficiency = 0
                mEfficiencyIncrease = 0
                mOutputItems = null
                mOutputFluids = null
                pDisplayName = "×"
            }
        }

    private var pDisplayName: String = "×"
    private val pCopyItemBase: ItemStack by lazy { NyxItemList.TestItem01.get(1) }
    private val pCopyFluidBase: FluidStack by lazy { NyxMaterials.Null.getFluid() }
    private var pAmount: Long = 0
        set(value) {
            field =
                if (value > 0) {
                    value
                } else {
                    0
                }
        }

    private lateinit var pUUID: UUID

    override fun onScrewdriverRightClick(
        side: ForgeDirection?,
        aPlayer: EntityPlayer?,
        aX: Float,
        aY: Float,
        aZ: Float,
        aTool: ItemStack?,
    ) {
        if (!baseMTE.isServerSide) return
        pItemMode = !pItemMode
        GTUtility.sendChatToPlayer(aPlayer, "COPIER Mode: ${if (pItemMode) "Item" else "Fluid"}")
    }

    override fun onFirstTick(aBaseMetaTileEntity: IGregTechTileEntity?) {
        super.onFirstTick(aBaseMetaTileEntity)
        pUUID = aBaseMetaTileEntity?.ownerUuid!!
        strongCheckOrAddUser(pUUID)
    }

    override fun createProcessingLogic(): ProcessingLogic? = null

    override fun checkProcessing(): CheckRecipeResult {
        pRunning = false
        val stackToCopy = controllerSlot ?: return CheckRecipeResultRegistry.NO_RECIPE
        if (pItemMode) {
            if (stackToCopy.isItemEqual(pCopyItemBase)) return CheckRecipeResultRegistry.NO_RECIPE
            if (!outputItem(stackToCopy, pAmount)) return CheckRecipeResultRegistry.NO_RECIPE
            // Resolving the display name is not cheap, so only do it on the running path.
            pDisplayName = stackToCopy.displayName
        } else {
            val fluidToCopy = GTUtility.convertCellToFluid(stackToCopy) ?: return CheckRecipeResultRegistry.NO_RECIPE
            if (fluidToCopy.isFluidEqual(pCopyFluidBase)) return CheckRecipeResultRegistry.NO_RECIPE
            if (!outputFluid(fluidToCopy, pAmount)) return CheckRecipeResultRegistry.NO_RECIPE
            pDisplayName = fluidToCopy.localizedName
        }
        pRunning = true
        return CheckRecipeResultRegistry.SUCCESSFUL
    }

    override fun supportsVoidProtection(): Boolean = true

    override fun supportsInputSeparation(): Boolean = false

    override fun supportsBatchMode(): Boolean = false

    override fun supportsSingleRecipeLocking(): Boolean = false

    override val sCasingBlock: Pair<Block, Int>
        get() = GregTechAPI.sBlockCasings2 to 0

    override val sCasingHatch: Array<IHatchElement<in NyxCopier>>
        get() = arrayOf(OutputBus, OutputHatch)

    override val sControllerIcon: Pair<IIconContainer, IIconContainer>
        get() = OVERLAY_DTPF_OFF to OVERLAY_DTPF_OFF

    override val sControllerIconActive: Pair<IIconContainer, IIconContainer>
        get() = OVERLAY_DTPF_ON to OVERLAY_DTPF_ON

    override fun createTooltip(): MultiblockTooltipBuilder =
        MultiblockTooltipBuilder()
            .addMachineTypeLocalized()
            .addInfoListLocalized(2)
            .addChangeModeByScrewdriver()
            .beginStructureCube()
            .addInputBus()
            .addOutputBus()
            .toolTipFinisher(CommonString.NyxMagical)

    override fun addCustomUIWidgets(
        builder: ModularWindow.Builder,
        buildContext: UIBuildContext?,
    ) {
        builder.apply {
            widget(
                CycleButtonWidget()
                    .setToggle({ pItemMode }, { pItemMode = it })
                    .setTextureGetter {
                        if (it == 1) {
                            GTUITextures.OVERLAY_BUTTON_AUTOOUTPUT_ITEM
                        } else {
                            GTUITextures.OVERLAY_BUTTON_AUTOOUTPUT_FLUID
                        }
                    }.setPlayClickSound(true)
                    .setUpdateTooltipEveryTick(true)
                    .setBackground(GTUITextures.BUTTON_STANDARD)
                    .setPos(80, 91)
                    .setSize(16, 16)
                    .dynamicTooltip {
                        listOf(StatCollector.translateToLocal("nyx.machine.copier.gui.t.${if (pItemMode) 0 else 1}"))
                    }.setTooltipShowUpDelay(BaseTileEntity.TOOLTIP_DELAY),
            )
            widget(
                TextFieldWidget()
                    .setGetterLong { pAmount }
                    .setSetterLong { pAmount = it }
                    .setNumbersLong { it.takeIf { it >= 0 } ?: 0L }
                    .setTextColor(Color.WHITE.normal)
                    .setTextAlignment(Alignment.CenterLeft)
                    .addTooltip(StatCollector.translateToLocal("nyx.machine.copier.gui.t.2"))
                    .setBackground(GTUITextures.BACKGROUND_TEXT_FIELD)
                    .setPos(98, 91)
                    .setSize(70, 16),
            )
        }
    }

    override fun drawTexts(
        screenElements: DynamicPositionedColumn,
        inventorySlot: SlotWidget,
    ) {
        super.drawTexts(screenElements, inventorySlot)
        screenElements
            .widget(
                TextWidget
                    .dynamicString {
                        StatCollector.translateToLocalFormatted("nyx.machine.copier.waila.0", pDisplayName)
                    }.setSynced(true)
                    .setTextAlignment(Alignment.CenterLeft)
                    .setEnabled { baseMTE.isActive },
            ).widget(
                TextWidget
                    .dynamicString {
                        StatCollector.translateToLocalFormatted(
                            "nyx.machine.copier.waila.1",
                            NumberFormatUtil.formatNumber(pAmount),
                        )
                    }.setSynced(true)
                    .setTextAlignment(Alignment.CenterLeft)
                    .setEnabled { baseMTE.isActive },
            ).widget(
                TextWidget
                    .dynamicString {
                        if (pItemMode) {
                            StatCollector.translateToLocal("nyx.machine.copier.waila.2.item")
                        } else {
                            StatCollector.translateToLocal("nyx.machine.copier.waila.2.fluid")
                        }
                    }.setSynced(true)
                    .setTextAlignment(Alignment.CenterLeft)
                    .setEnabled { baseMTE.isActive },
            )
    }

    override fun getWailaBody(
        itemStack: ItemStack?,
        currentTip: MutableList<String>,
        accessor: IWailaDataAccessor,
        config: IWailaConfigHandler?,
    ) {
        super.getWailaBody(itemStack, currentTip, accessor, config)
        val displayName = accessor.nbtData.getString("pDisplayName")
        if (displayName.isEmpty()) return
        currentTip.add(StatCollector.translateToLocalFormatted("nyx.machine.copier.waila.0", displayName))
        currentTip.add(
            StatCollector.translateToLocalFormatted(
                "nyx.machine.copier.waila.1",
                accessor.nbtData.getString("pAmount"),
            ),
        )
        val mode = accessor.nbtData.getInteger("pItemMode").takeIf { it != 0 } ?: return
        if (mode == 1) {
            currentTip.add(StatCollector.translateToLocal("nyx.machine.copier.waila.2.item"))
        } else {
            currentTip.add(StatCollector.translateToLocal("nyx.machine.copier.waila.2.fluid"))
        }
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
        if (baseMetaTileEntity?.isActive == true) {
            tag.setString("pDisplayName", pDisplayName)
            tag.setString("pAmount", NumberFormatUtil.formatNumber(pAmount))
            tag.setInteger("pItemMode", if (pItemMode) 1 else -1)
        }
    }

    override fun saveNBTData(aNBT: NBTTagCompound) {
        super.saveNBTData(aNBT)
        aNBT.setBoolean("pItemMode", pItemMode)
        aNBT.setLong("pAmount", pAmount)
    }

    override fun loadNBTData(aNBT: NBTTagCompound) {
        super.loadNBTData(aNBT)
        pItemMode = aNBT.getBoolean("pItemMode")
        pAmount = aNBT.getLong("pAmount")
    }
}
