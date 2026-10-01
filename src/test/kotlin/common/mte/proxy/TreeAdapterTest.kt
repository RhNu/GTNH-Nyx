package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TreeAdapterTest {
    @Test
    fun `tree yield applies each source factor once`() {
        assertEquals(200, treeOutputAmount(2, 5, 5, 4))
        assertEquals(5, treeOutputAmount(1, 5, 1, 1))
    }

    @Test
    fun `invalid and overflowing tree yields fail closed`() {
        assertNull(treeOutputAmount(1, 5, 1, -1))
        assertNull(treeOutputAmount(0, 5, 1, 1))
        assertNull(treeOutputAmount(Int.MAX_VALUE, 2, 2, 2))
        assertEquals(Int.MAX_VALUE, treeOutputAmount(Int.MAX_VALUE, 1, 1, 1))
    }
}
