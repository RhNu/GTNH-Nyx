package rhynia.nyx.common.mte.prod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MaterialBalanceTest {
    @Test
    fun `mixed ingot and nugget preserves nugget remainder`() {
        val ingot = 3_628_800L
        val nugget = 403_200L

        assertEquals(MaterialBalancePlan(1, nugget), planMaterialBalance(0, ingot + nugget, ingot))
    }

    @Test
    fun `dense plate to double plate preserves one ingot`() {
        val ingot = 3_628_800L
        val doublePlate = ingot * 2
        val densePlate = ingot * 9

        assertEquals(MaterialBalancePlan(4, ingot), planMaterialBalance(0, densePlate, doublePlate))
    }

    @Test
    fun `stored remainder can emit after target change`() {
        val ingot = 3_628_800L
        val nugget = 403_200L

        assertEquals(MaterialBalancePlan(9, 0), planMaterialBalance(ingot, 0, nugget))
    }

    @Test
    fun `intermediate sum may exceed long without losing exactness`() {
        assertEquals(
            MaterialBalancePlan(1, Long.MAX_VALUE - 2),
            planMaterialBalance(Long.MAX_VALUE - 1, Long.MAX_VALUE - 1, Long.MAX_VALUE),
        )
    }

    @Test
    fun `invalid amounts are rejected`() {
        assertNull(planMaterialBalance(-1, 0, 1))
        assertNull(planMaterialBalance(0, -1, 1))
        assertNull(planMaterialBalance(0, 0, 0))
    }
}
