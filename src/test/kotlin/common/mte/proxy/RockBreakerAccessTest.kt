package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RockBreakerAccessTest {
    @Test
    fun `only absent circuit groups fall back`() {
        val groups = mapOf(-1 to listOf("cobble"), 1 to emptyList())
        assertEquals(listOf("cobble"), rockBreakerCandidates(groups, 24))
        assertEquals(emptyList(), rockBreakerCandidates(groups, 1))
        assertNull(rockBreakerCandidates(emptyMap<Int, List<String>>(), 1))
    }

    @Test
    fun `circuit registrations added after an initial miss are visible`() {
        val groups = mutableMapOf(-1 to listOf("default"))
        assertEquals(listOf("default"), rockBreakerCandidates(groups, 6))
        groups[6] = listOf("netherrack", "custom")
        assertEquals(listOf("netherrack", "custom"), rockBreakerCandidates(groups, 6))
        groups.remove(6)
        assertEquals(listOf("default"), rockBreakerCandidates(groups, 6))
    }
}
