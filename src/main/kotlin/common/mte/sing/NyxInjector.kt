@file:Suppress("DEPRECATION")

package rhynia.nyx.common.mte.sing

import com.gtnewhorizons.modularui.api.drawable.IDrawable
import com.gtnewhorizons.modularui.api.screen.ModularWindow
import com.gtnewhorizons.modularui.api.screen.UIBuildContext
import com.gtnewhorizons.modularui.common.widget.ButtonWidget
import com.gtnewhorizons.modularui.common.widget.DrawableWidget
import com.gtnewhorizons.modularui.common.widget.TextWidget
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import gregtech.api.enums.GTValues.VN
import gregtech.api.enums.Textures
import gregtech.api.gui.modularui.GTUITextures
import gregtech.api.interfaces.ITexture
import gregtech.api.interfaces.modularui.IAddGregtechLogo
import gregtech.api.interfaces.modularui.IAddUIWidgets
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.MetaTileEntity
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock
import gregtech.api.render.TextureFactory
import gregtech.api.util.GTUtility
import gregtech.common.misc.WirelessNetworkManager.addEUToGlobalEnergyMap
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.StatCollector
import net.minecraftforge.common.util.ForgeDirection
import rhynia.nyx.api.enums.CommonString
import rhynia.nyx.api.enums.Texture.Logo32
import tectech.thing.metaTileEntity.Textures.MACHINE_CASINGS_TT
import tectech.thing.metaTileEntity.Textures.OVERLAYS_ENERGY_IN_LASER_TT
import tectech.thing.metaTileEntity.Textures.OVERLAYS_ENERGY_IN_POWER_TT
import tectech.thing.metaTileEntity.Textures.OVERLAYS_ENERGY_OUT_LASER_TT
import tectech.thing.metaTileEntity.Textures.OVERLAYS_ENERGY_OUT_POWER_TT
import tectech.util.CommonValues.TRANSFER_AT
import tectech.util.TTUtility
import java.util.UUID
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.math.abs

class NyxInjector :
    MTETieredMachineBlock,
    IAddUIWidgets,
    IAddGregtechLogo {
    private var wireless = false
    private var ownerUUID: UUID? = null
    var eUT: Int = 0
    var aMP: Int = 0
    var producing: Boolean = true

    constructor(
        aID: Int,
        aName: String,
        aTier: Int,
    ) : super(
        aID,
        aName,
        StatCollector.translateToLocal("$aName.name"),
        aTier,
        0,
        arrayOf(
            CommonString.ChangeModeByScrewdriver,
            CommonString.AddByNyx,
        ),
    ) {
        TTUtility.setTier(aTier, this)
    }

    constructor(
        aName: String,
        aTier: Int,
        aDescription: Array<String?>?,
        aTextures: Array<Array<Array<ITexture?>?>?>?,
    ) : super(aName, aTier, 0, aDescription, aTextures) {
        TTUtility.setTier(aTier, this)
    }

    override fun newMetaEntity(aTileEntity: IGregTechTileEntity?): MetaTileEntity =
        NyxInjector(mName, mTier.toInt(), mDescriptionArray, mTextures)

    override fun onScrewdriverRightClick(
        side: ForgeDirection?,
        aPlayer: EntityPlayer?,
        aX: Float,
        aY: Float,
        aZ: Float,
        aTool: ItemStack?,
    ) {
        wireless = !wireless
        if (wireless) {
            GTUtility.sendChatToPlayer(aPlayer, "WIRELESS: ON")
        } else {
            GTUtility.sendChatToPlayer(aPlayer, "WIRELESS: OFF")
        }
    }

    @SideOnly(Side.CLIENT)
    override fun registerIcons(aBlockIconRegister: IIconRegister?) {
        super.registerIcons(aBlockIconRegister)
        GENNY
    }

    override fun getTexture(
        aBaseMetaTileEntity: IGregTechTileEntity?,
        side: ForgeDirection?,
        facing: ForgeDirection?,
        colorIndex: Int,
        aActive: Boolean,
        aRedstone: Boolean,
    ): Array<ITexture> {
        val tier = mTier.toInt()
        val overlay =
            if (side != facing) {
                when {
                    wireless && aActive -> OVERLAYS_ENERGY_OUT_LASER_TT[tier]
                    wireless -> OVERLAYS_ENERGY_IN_LASER_TT[tier]
                    aActive -> OVERLAYS_ENERGY_OUT_POWER_TT[tier]
                    else -> OVERLAYS_ENERGY_IN_POWER_TT[tier]
                }
            } else {
                GENNY
            }
        return arrayOf(MACHINE_CASINGS_TT[tier][colorIndex + 1], overlay)
    }

    override fun getTextureSet(aTextures: Array<ITexture?>?): Array<Array<Array<ITexture?>?>?>? = null

    override fun allowPutStack(
        iGregTechTileEntity: IGregTechTileEntity?,
        i: Int,
        side: ForgeDirection?,
        itemStack: ItemStack?,
    ): Boolean = false

    override fun allowPullStack(
        iGregTechTileEntity: IGregTechTileEntity?,
        i: Int,
        side: ForgeDirection?,
        itemStack: ItemStack?,
    ): Boolean = false

    override fun saveNBTData(aNBT: NBTTagCompound) {
        aNBT.setInteger("eEUT", this.eUT)
        aNBT.setInteger("eAMP", this.aMP)
        aNBT.setBoolean("eWireless", wireless)
    }

    override fun loadNBTData(aNBT: NBTTagCompound) {
        this.eUT = aNBT.getInteger("eEUT")
        this.aMP = aNBT.getInteger("eAMP")
        wireless = aNBT.getBoolean("eWireless")
        producing = aMP.toLong() * this.eUT >= 0

        baseMetaTileEntity?.isActive = producing
    }

    override fun onPostTick(
        aBaseMetaTileEntity: IGregTechTileEntity,
        aTick: Long,
    ) {
        if (!aBaseMetaTileEntity.isServerSide) return

        aBaseMetaTileEntity.isActive = producing
        if (!wireless) {
            euVar = if (aBaseMetaTileEntity.isActive) maxEUStore() else 0
            return
        }

        if (TRANSFER_AT != (aTick % 20).toByte()) return
        if (aBaseMetaTileEntity.isActive) {
            // Resolved once instead of on every tick; the owner cannot change in place.
            if (ownerUUID == null) ownerUUID = aBaseMetaTileEntity.ownerUuid
            euVar = maxEUStore()
            addEUToGlobalEnergyMap(ownerUUID, abs(euVar))
        } else {
            euVar = 0
        }
    }

    override fun onRightclick(
        aBaseMetaTileEntity: IGregTechTileEntity?,
        aPlayer: EntityPlayer?,
    ): Boolean {
        openGui(aPlayer)
        return true
    }

    override fun isFacingValid(facing: ForgeDirection?): Boolean = true

    override fun isAccessAllowed(aPlayer: EntityPlayer?): Boolean = true

    override fun isElectric(): Boolean = true

    override fun isEnetOutput(): Boolean = !wireless

    override fun isEnetInput(): Boolean = !wireless

    override fun isInputFacing(side: ForgeDirection?): Boolean = !producing && side != baseMetaTileEntity!!.frontFacing

    override fun isOutputFacing(side: ForgeDirection?): Boolean = producing && side != baseMetaTileEntity!!.frontFacing

    // These are polled by the energy net every tick, so keep them on integer math.
    override fun maxAmperesIn(): Long = if (producing) 0 else abs(aMP.toLong())

    override fun maxAmperesOut(): Long = if (producing) abs(aMP.toLong()) else 0

    override fun maxEUInput(): Long = if (producing) 0 else Int.MAX_VALUE.toLong()

    override fun maxEUOutput(): Long = if (producing) abs(eUT.toLong()) else 0

    override fun maxEUStore(): Long {
        val base = abs(eUT.toLong() * aMP)
        return if (wireless) {
            if (base > Long.MAX_VALUE / 24) Long.MAX_VALUE else base * 24
        } else {
            (base.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() shl 2).toLong()
        }
    }

    override fun maxProgresstime(): Int = baseMetaTileEntity!!.universalEnergyCapacity.toInt()

    override fun addGregTechLogo(builder: ModularWindow.Builder) {
        builder.widget(DrawableWidget().setDrawable(Logo32).setSize(17, 17).setPos(113, 56))
    }

    override fun addUIWidgets(
        builder: ModularWindow.Builder,
        buildContext: UIBuildContext?,
    ) {
        builder
            .widget(
                DrawableWidget()
                    .setDrawable(GTUITextures.PICTURE_SCREEN_BLACK)
                    .setSize(90, 72)
                    .setPos(43, 4),
            ).widget(
                TextWidget
                    .dynamicString {
                        "TIER: " + VN[GTUtility.getTier(abs(eUT.toLong())).toInt()]
                    }.setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setPos(46, 22),
            ).widget(
                TextWidget
                    .dynamicString { "SUM: " + aMP.toLong() * this.eUT }
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setPos(46, 46),
            )

        addLabelledIntegerTextField(builder, "EUT: ", { this.eUT }, { this.eUT = it!! }, 8)
        addLabelledIntegerTextField(builder, "AMP: ", { this.aMP }, { this.aMP = it!! }, 34)

        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_MINUS_LARGE,
            { eUT -= it!! },
            512,
            64,
            7,
            4,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_MINUS_LARGE,
            { eUT /= it!! },
            512,
            64,
            7,
            22,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_MINUS_LARGE,
            { aMP -= it!! },
            512,
            64,
            7,
            40,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_MINUS_LARGE,
            { aMP /= it!! },
            512,
            64,
            7,
            58,
        )

        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_MINUS_SMALL,
            { eUT -= it!! },
            16,
            1,
            25,
            4,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_MINUS_SMALL,
            { eUT /= it!! },
            16,
            2,
            25,
            22,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_MINUS_SMALL,
            { aMP -= it!! },
            16,
            1,
            25,
            40,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_MINUS_SMALL,
            { aMP /= it!! },
            16,
            2,
            25,
            58,
        )

        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_PLUS_SMALL,
            { eUT += it!! },
            16,
            1,
            133,
            4,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_PLUS_SMALL,
            { eUT *= it!! },
            16,
            2,
            133,
            22,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_PLUS_SMALL,
            { aMP += it!! },
            16,
            1,
            133,
            40,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_PLUS_SMALL,
            { aMP *= it!! },
            16,
            2,
            133,
            58,
        )

        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_PLUS_LARGE,
            { eUT += it!! },
            512,
            64,
            151,
            4,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_PLUS_LARGE,
            { eUT *= it!! },
            512,
            64,
            151,
            22,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_PLUS_LARGE,
            { aMP += it!! },
            512,
            64,
            151,
            40,
        )
        addChangeNumberButton(
            builder,
            GTUITextures.OVERLAY_BUTTON_PLUS_LARGE,
            { aMP *= it!! },
            512,
            64,
            151,
            58,
        )
    }

    private fun addLabelledIntegerTextField(
        builder: ModularWindow.Builder,
        label: String,
        getter: Supplier<Int?>?,
        setter: Consumer<Int?>?,
        yPos: Int,
    ) {
        val t = TextFieldWidget()
        val ltw = TextWidget(label)
        val xPos = 46
        builder
            .widget(ltw.setDefaultColor(COLOR_TEXT_WHITE.get()).setPos(xPos, yPos))
            .widget(
                t
                    .setSetterInt(setter)
                    .setGetterInt(getter)
                    .setTextColor(COLOR_TEXT_WHITE.get())
                    .setBackground(
                        GTUITextures.BACKGROUND_TEXT_FIELD.withOffset(
                            (-1).toFloat(),
                            (-1).toFloat(),
                            (2).toFloat(),
                            (2).toFloat(),
                        ),
                    ).setPos(xPos + 24, yPos - 1)
                    .setSize(56, 10),
            )
    }

    private fun addChangeNumberButton(
        builder: ModularWindow.Builder,
        overlay: IDrawable?,
        setter: Consumer<Int?>,
        changeNumberShift: Int,
        changeNumber: Int,
        xPos: Int,
        yPos: Int,
    ) {
        builder.widget(
            ButtonWidget()
                .setOnClick { clickData, _ ->
                    setter.accept(if (clickData!!.shift) changeNumberShift else changeNumber)
                    producing = aMP.toLong() * this.eUT >= 0
                }.setBackground(GTUITextures.BUTTON_STANDARD, overlay)
                .setSize(18, 18)
                .setPos(xPos, yPos),
        )
    }

    companion object {
        @Suppress("SpellCheckingInspection")
        val GENNY: ITexture by lazy {
            TextureFactory.of(Textures.BlockIcons.custom("iconsets/GENNY"))
        }
    }
}
