package rhynia.nyx.common

import gregtech.api.enums.GTValues
import gregtech.api.interfaces.IItemContainer
import gregtech.api.util.GTOreDictUnificator
import gregtech.api.util.GTUtility
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import rhynia.nyx.ModLogger
import rhynia.nyx.api.util.copyAmount
import rhynia.nyx.api.util.debugItem
import codechicken.nei.api.API as CodeChickenAPI

interface ItemList {
    fun set(itemStack: ItemStack): Any

    fun get(
        aAmount: Long,
        vararg aReplacements: Any?,
    ): ItemStack

    fun get(
        aAmount: Int,
        vararg aReplacements: Any?,
    ): ItemStack = get(aAmount.toLong(), *aReplacements)
}

@Suppress("DEPRECATION")
enum class NyxItemList(
    private var mHasNotBeenSet: Boolean = true,
    private var mDeprecated: Boolean = false,
    private var mWarned: Boolean = false,
) : IItemContainer,
    ItemList {
    // Items
    ItemUltimate,

    // Meta Items 1
    TestItem01,

    // Meta Items 2
    Calibration,

    // Blocks
    TestMetaBlock01,

    // Machines
    ControllerCopier,
    ControllerProxy,
    ControllerConverter,

    // Single Block Machines
    MachineInjector,
    ;

    private lateinit var mStack: ItemStack

    @Suppress("UNUSED")
    constructor(deprecated: Boolean) : this(mDeprecated = deprecated)

    override fun set(item: Item): IItemContainer =
        apply {
            mHasNotBeenSet = false
            mStack = GTUtility.copyAmount(1, ItemStack(item, 1, 0))
        }

    override fun set(itemStack: ItemStack): IItemContainer =
        apply {
            mHasNotBeenSet = false
            mStack = GTUtility.copyAmount(1, itemStack)
        }

    override fun hidden(): IItemContainer =
        apply {
            CodeChickenAPI.hideItem(get(1))
        }

    override fun getItem(): Item {
        safetyCheck()
        if (GTUtility.isStackInvalid(mStack)) throw NullPointerException("The ItemStack for $this is invalid!")
        return mStack.item
    }

    override fun getBlock(): Block {
        safetyCheck()
        return GTUtility.getBlockFromItem(getItem())
    }

    override fun hasBeenSet(): Boolean = !mHasNotBeenSet

    override fun isStackEqual(aStack: Any?): Boolean = isStackEqual(aStack, aWildcard = false, aIgnoreNBT = false)

    override fun isStackEqual(
        aStack: Any?,
        aWildcard: Boolean,
        aIgnoreNBT: Boolean,
    ): Boolean {
        if (mDeprecated && !mWarned) {
            Exception("$this is now deprecated").let {
                ModLogger.error("Accessing deprecated entry $this", it)
            }
            mWarned = true
        }
        val asStack = aStack as? ItemStack ?: return false
        if (GTUtility.isStackInvalid(asStack)) return false
        return GTUtility.areUnificationsEqual(
            aStack,
            if (aWildcard) getWildcard(1) else get(1),
            aIgnoreNBT,
        )
    }

    override fun get(
        aAmount: Long,
        vararg aReplacements: Any?,
    ): ItemStack {
        safetyCheck()
        if (GTUtility.isStackInvalid(mStack)) {
            ModLogger.error("The ItemStack for $this is invalid!", NullPointerException())
            debugItem("The ItemStack for $this is invalid!").copyAmount(aAmount.toInt())
        }
        return mStack.copyAmount(aAmount.toInt())
    }

    override fun getWildcard(
        aAmount: Long,
        vararg aReplacements: Any?,
    ): ItemStack {
        safetyCheck()
        return if (GTUtility.isStackInvalid(mStack)) {
            GTUtility.copyAmount(aAmount, *aReplacements)
        } else {
            GTUtility.copyAmountAndMetaData(
                aAmount.toInt(),
                GTValues.W.toInt(),
                GTOreDictUnificator.get(mStack),
            )
        }
    }

    override fun getUndamaged(
        aAmount: Long,
        vararg aReplacements: Any?,
    ): ItemStack {
        safetyCheck()
        if (GTUtility.isStackInvalid(mStack)) {
            return GTUtility.copyAmount(aAmount, *aReplacements)
        }
        return GTUtility.copyAmountAndMetaData(
            aAmount,
            0,
            GTOreDictUnificator.get(mStack),
        )
    }

    override fun getAlmostBroken(
        aAmount: Long,
        vararg aReplacements: Any?,
    ): ItemStack {
        safetyCheck()
        if (GTUtility.isStackInvalid(mStack)) {
            return GTUtility.copyAmount(aAmount, *aReplacements)
        }
        return GTUtility.copyAmountAndMetaData(
            aAmount.toInt(),
            mStack.maxDamage - 1,
            GTOreDictUnificator.get(mStack),
        )
    }

    override fun getWithDamage(
        aAmount: Long,
        aMetaValue: Long,
        vararg aReplacements: Any?,
    ): ItemStack {
        safetyCheck()
        if (GTUtility.isStackInvalid(mStack)) {
            return GTUtility.copyAmount(aAmount, *aReplacements)
        }
        return GTUtility.copyAmountAndMetaData(
            aAmount,
            aMetaValue,
            GTOreDictUnificator.get(mStack),
        )
    }

    override fun getWithName(
        aAmount: Long,
        aDisplayName: String?,
        vararg aReplacements: Any?,
    ): ItemStack = this.get(aAmount, *aReplacements)

    override fun getWithCharge(
        aAmount: Long,
        aEnergy: Int,
        vararg aReplacements: Any?,
    ): ItemStack = this.get(aAmount, *aReplacements)

    override fun registerOre(vararg aOreNames: Any?): IItemContainer = this

    override fun registerWildcardAsOre(vararg aOreNames: Any?): IItemContainer = this

    private fun safetyCheck() {
        if (mHasNotBeenSet) {
            throw IllegalAccessError("The enum entry '$name' has not been set to an Item at this time!")
        }
        if (mDeprecated && !mWarned) {
            Exception("$this is now deprecated").let {
                ModLogger.error("Accessing deprecated entry $this", it)
            }
            mWarned = true
        }
    }
}
