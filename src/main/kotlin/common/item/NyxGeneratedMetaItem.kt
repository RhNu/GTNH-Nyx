package rhynia.nyx.common.item

import bartworks.client.textures.PrefixTextureLinker
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import gregtech.api.enums.Materials
import gregtech.api.enums.OrePrefixes
import gregtech.api.interfaces.IIconContainer
import gregtech.api.items.MetaGeneratedItem
import gregtech.api.util.GTOreDictUnificator
import net.minecraft.client.gui.GuiScreen.isShiftKeyDown
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.util.StatCollector
import rhynia.nyx.api.util.firstCharUpperCase
import rhynia.nyx.api.util.hasLocalization
import rhynia.nyx.api.util.localized
import rhynia.nyx.api.util.shortObjMapOf
import rhynia.nyx.client.NyxTab
import rhynia.nyx.common.material.NyxMaterials
import rhynia.nyx.common.material.generation.NyxMaterialLoader.MaterialMap
import rhynia.nyx.common.material.generation.NyxMaterialLoader.MaterialSet

class NyxGeneratedMetaItem(
    val orePrefix: OrePrefixes,
) : MetaGeneratedItem("GenItem${orePrefix.name.firstCharUpperCase()}", 32766, 0) {
    init {
        creativeTab = NyxTab.TabItem
        MaterialSet.forEach {
            if (!it.isTypeValid(orePrefix)) return@forEach
            GTOreDictUnificator.registerOre(
                orePrefix.name.firstCharUpperCase() + it.internalName.firstCharUpperCase(),
                ItemStack(this, 1, it.id.toInt()),
            )
        }
    }

    companion object {
        private fun getLocalizedTypeName(prefix: OrePrefixes): String =
            "nyx.ore.${prefix.name}".let {
                if (hasLocalization(it)) {
                    it.localized()
                } else {
                    "${prefix.materialPrefix}%material${prefix.materialPostfix}"
                }
            }

        private val tooltipCache = shortObjMapOf<List<String>>()

        private fun getLocalizedTooltips(id: Short): List<String> =
            tooltipCache.getOrPut(id) {
                val material = MaterialMap[id] ?: return emptyList()
                val baseKey = "${material.localizationKey}.extra"
                val tooltips = mutableListOf<String>()

                var index = 0
                while (true) {
                    val key = "$baseKey.$index"
                    if (hasLocalization(key)) {
                        tooltips.add(key.localized())
                        index++
                    } else {
                        break
                    }
                }

                if (tooltips.isNotEmpty()) {
                    tooltipCache[id] = tooltips
                    tooltips
                } else {
                    emptyList()
                }
            }
    }

    private val typedName: String
        get() = getLocalizedTypeName(orePrefix)

    override fun addAdditionalToolTips(
        aList: MutableList<String>,
        aStack: ItemStack?,
        aPlayer: EntityPlayer?,
    ) {
        val material = MaterialMap.get(aStack?.itemDamage?.toShort() ?: return) ?: return
        material.elementTooltip.forEach { aList.add(it) }
        aStack.let { stack ->
            val tooltips = getLocalizedTooltips(stack.itemDamage.toShort())
            if (tooltips.isNotEmpty()) {
                if (isShiftKeyDown()) {
                    tooltips.forEach { aList.add(it) }
                } else {
                    aList.add(StatCollector.translateToLocal("nyx.tooltip.press_more_info"))
                }
            }
        }
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val material = MaterialMap[stack.itemDamage.toShort()] ?: NyxMaterials.Null
        return typedName.replace("%material", material.displayName)
    }

    override fun getIconContainer(aMetaData: Int): IIconContainer? =
        if (MaterialMap[aMetaData.toShort()] == null) {
            null
        } else if (orePrefix.textureIndex == -1) {
            proxyIconContainerBartWorks(aMetaData)
        } else {
            MaterialMap[aMetaData.toShort()]
                ?.textureSet
                ?.mTextures
                ?.get(orePrefix.textureIndex)
        }

    @SideOnly(Side.CLIENT)
    private fun proxyIconContainerBartWorks(aMetaData: Int): IIconContainer? =
        PrefixTextureLinker.texMap[orePrefix]?.get(MaterialMap[aMetaData.toShort()]?.textureSet)

    override fun getSubItems(
        aItem: Item?,
        aCreativeTab: CreativeTabs?,
        aList: MutableList<ItemStack>,
    ) {
        MaterialSet.forEach { material ->
            if (!material.isTypeValid(orePrefix)) return@forEach
            aList.add(ItemStack(this, 1, material.id.toInt()))
        }
    }

    override fun getRGBa(aStack: ItemStack?): ShortArray =
        aStack?.itemDamage?.toShort()?.let { MaterialMap[it]?.color }
            ?: shortArrayOf(0, 0, 0, 255)

    override fun getIconFromDamage(aMetaData: Int): IIcon? =
        if (aMetaData < 0 || aMetaData >= MaterialSet.size || MaterialMap[aMetaData.toShort()] == null) {
            null
        } else {
            getIconContainer(aMetaData)?.icon
        }

    override fun getItemStackLimit(aStack: ItemStack?): Int = orePrefix.defaultStackSize

    override fun getCapacity(aStack: ItemStack?): Int =
        when (orePrefix) {
            OrePrefixes.cell,
            OrePrefixes.capsule,
            OrePrefixes.cellPlasma,
            -> 1000

            OrePrefixes.cellMolten,
            OrePrefixes.capsuleMolten,
            -> 144

            else -> 0
        }

    override fun getContainerItem(aStack: ItemStack?): ItemStack? =
        when (orePrefix) {
            OrePrefixes.cell,
            OrePrefixes.cellPlasma,
            OrePrefixes.cellMolten,
            -> Materials.Empty.getCells(1)

            else -> null
        }
}
