package rhynia.nyx.common.mte.proxy

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ProxyStackKeyTest {
    @Test
    fun `NBT mutation changes a new key without corrupting a previous key`() {
        val stack = ItemStack(Item(), 1, 0)
        stack.tagCompound = NBTTagCompound().apply { setString("species", "oak") }
        val initial = ProxyStackKey.of(stack)
        val lookup = hashMapOf(initial to "oak product")
        stack.tagCompound.setString("species", "birch")
        assertNotEquals(initial, ProxyStackKey.of(stack))
        assertEquals("oak product", lookup[initial])
        stack.tagCompound.setString("species", "oak")
        assertEquals(initial, ProxyStackKey.of(stack))
    }

    @Test
    fun `counts matter only when requested and damage always matters`() {
        val stack = ItemStack(Item(), 1, 0)
        val catalyst = ProxyStackKey.of(stack, false)
        val amount = ProxyStackKey.of(stack)
        stack.stackSize = 4
        assertEquals(catalyst, ProxyStackKey.of(stack, false))
        assertNotEquals(amount, ProxyStackKey.of(stack))
        stack.itemDamage = 1
        assertNotEquals(catalyst, ProxyStackKey.of(stack, false))
    }
    @Test
    fun `cached stack copies isolate mutable genome NBT and counts`() {
        val genome = NBTTagCompound().apply { setString("primary", "oak") }
        val source = ItemStack(Item(), 3, 0).apply {
            tagCompound = NBTTagCompound().apply { setTag("genome", genome) }
        }
        val cache = ProxyCache<String, ItemStack>(2) { it.copy() }
        val first = cache.getOrCreate("sapling") { source }!!
        source.stackSize = 99
        genome.setString("primary", "birch")
        first.stackSize = 44
        first.tagCompound.getCompoundTag("genome").setString("primary", "pine")
        val second = cache.getOrCreate("sapling") { error("cache missed") }!!
        assertEquals(3, second.stackSize)
        assertEquals("oak", second.tagCompound.getCompoundTag("genome").getString("primary"))
    }

}
