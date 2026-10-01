package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlgaePlanTest {
    @Test
    fun `compost changes tier only once and is aggregated across inputs`() {
        assertEquals(AlgaePlan(3, 2), planAlgae(2, listOf(1, 1)))
        assertEquals(AlgaePlan(2, 0), planAlgae(2, listOf(1)))
        assertEquals(AlgaePlan(3, 2), planAlgae(2, listOf(64)))
    }

    @Test
    fun `compost requirement is bounded and arithmetic does not overflow`() {
        assertEquals(AlgaePlan(1, 1), planAlgae(0, listOf(1)))
        assertEquals(AlgaePlan(2, 1), planAlgae(1, listOf(1)))
        assertEquals(AlgaePlan(8, 64), planAlgae(7, listOf(64)))
        assertEquals(AlgaePlan(16, 64), planAlgae(15, listOf(Int.MAX_VALUE)))
        assertEquals(AlgaePlan(1, 0), planAlgae(1, listOf(-10)))
        assertNull(planAlgae(-1, listOf(1)))
        assertNull(planAlgae(Int.MAX_VALUE, listOf(64)))
    }

    @Test
    fun `runtime index sees all matching additions replacements removals and retiering`() {
        class Entry(var tier: Int)
        val index = ProxyTierIndex<Entry>()
        val entries = mutableListOf<Entry>()
        fun current() = index.recipes(entries, Entry::tier, 2)
        assertEquals(emptyList(), current())
        val a = Entry(2)
        val b = Entry(2)
        entries.addAll(listOf(a, b, Entry(1)))
        assertEquals(listOf(a, b), current())
        a.tier = 1
        assertEquals(listOf(b), current())
        entries.remove(b)
        assertEquals(emptyList(), current())
        val replacement = Entry(2)
        entries.add(replacement)
        assertEquals(listOf(replacement), current())
    }
}
