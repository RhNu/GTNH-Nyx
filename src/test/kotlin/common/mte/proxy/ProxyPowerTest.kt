package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyPowerTest {
    @Test
    fun `power budgets saturate without wrapping negative`() {
        assertEquals(128, proxyPower(32, 4))
        assertEquals(Long.MAX_VALUE, proxyPower(Long.MAX_VALUE, 2))
        assertEquals(0, proxyPower(-1, 1))
    }

    @Test
    fun `long cost bounds parallel exactly above the int range`() {
        assertEquals(2, proxyPowerParallel(10_000_000_000, 4_000_000_000, 64))
        assertEquals(0, proxyPowerParallel(3_999_999_999, 4_000_000_000, 64))
        assertEquals(1, proxyPowerParallel(Long.MAX_VALUE, 4_000_000_000, 1))
        assertEquals(0, proxyPowerParallel(100, 0, 64))
    }
}
