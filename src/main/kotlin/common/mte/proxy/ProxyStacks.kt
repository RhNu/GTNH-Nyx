package rhynia.nyx.common.mte.proxy

import gregtech.api.util.GTRecipe
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/** Immutable-by-ownership key: an input's mutable NBT must never become a map key by reference. */
internal data class ProxyStackKey(
    val item: Item,
    val meta: Int,
    val count: Int,
    private val tag: NBTTagCompound?,
) {
    companion object {
        fun of(stack: ItemStack, includeCount: Boolean = true): ProxyStackKey =
            ProxyStackKey(
                stack.item,
                stack.itemDamage,
                if (includeCount) stack.stackSize else 1,
                stack.tagCompound?.copy() as? NBTTagCompound,
            )
    }
}

/** GTRecipe.copy deep-copies stacks but shares chance arrays; adapter results own both. */
internal fun GTRecipe.proxyCopy(): GTRecipe = copy().also {
    it.mInputChances = mInputChances?.clone()
    it.mOutputChances = mOutputChances?.clone()
    it.mFluidInputChances = mFluidInputChances?.clone()
    it.mFluidOutputChances = mFluidOutputChances?.clone()
    it.mCanBeBuffered = false
}
