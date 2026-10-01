package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlgaeAdapterTest {
    @Test
    fun `base tier voltage power rounds down at ninety percent`() {
        assertEquals(7L, algaeEUt(8))
        assertEquals(28L, algaeEUt(32))
        assertEquals(115L, algaeEUt(128))
        assertEquals(460L, algaeEUt(512))
    }

    @Test
    fun `power above int remains exact and long multiplication cannot overflow`() {
        assertEquals(7_730_941_132L, algaeEUt(8_589_934_592L))
        assertEquals(8_301_034_833_169_298_226L, algaeEUt(Long.MAX_VALUE))
    }

    @Test
    fun `invalid voltage cannot create negative power`() {
        assertEquals(0L, algaeEUt(0))
        assertEquals(0L, algaeEUt(-1))
        assertEquals(0L, algaeEUt(Long.MIN_VALUE))
    }

    @Test
    fun `unconditional template inputs remain unconditional after augmentation`() {
        assertNull(algaeInputChances(null, 2, 2))
        assertContentEquals(intArrayOf(10000, 10000), algaeInputChances(intArrayOf(), 0, 2))
    }

    @Test
    fun `existing input chances are retained and extra inputs use default consumption`() {
        assertContentEquals(intArrayOf(2500, 0, 10000, 10000), algaeInputChances(intArrayOf(2500, 0), 2, 2))
        assertContentEquals(intArrayOf(2500, 10000, 10000), algaeInputChances(intArrayOf(2500), 2, 1))
    }

    @Test
    fun `recipe chance augmentation cannot mutate the source or another result`() {
        val source = intArrayOf(9000)
        val first = algaeInputChances(source, 1, 2)!!
        first[0] = 1
        assertContentEquals(intArrayOf(9000), source)
        assertContentEquals(intArrayOf(9000, 10000, 10000), algaeInputChances(source, 1, 2))
    }

    @Test
    fun `live tier index retains references to current template contents`() {
        class Template(var tier: Int, var duration: Int, var enabled: Boolean)
        val template = Template(2, 80, true)
        val entries = mutableListOf(template)
        val index = ProxyTierIndex<Template>()
        fun find() = index.recipes(entries, Template::tier, 2).filter { it.enabled }

        assertEquals(80, find().single().duration)
        template.duration = 60
        assertEquals(60, find().single().duration)
        template.enabled = false
        assertEquals(emptyList(), find())
        template.enabled = true
        assertEquals(60, find().single().duration)
    }
}
