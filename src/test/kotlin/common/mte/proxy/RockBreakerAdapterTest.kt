package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class RockBreakerAdapterTest {
    private data class Stack(val item: String, val count: Int = 1, val tag: String? = null)

    private fun exact(left: Stack, right: Stack) = left.item == right.item && left.tag == right.tag

    @Test
    fun `positional requirements remain distinct after catalyst resources collapse`() {
        val above = rockBreakerEnvironmentId("hot", null, listOf("wet"), emptyList())
        val beside = rockBreakerEnvironmentId(null, null, listOf("wet", "hot"), emptyList())
        assertNotEquals(above, beside)
        assertNotEquals(
            rockBreakerEnvironmentId(null, "base", listOf("hot"), emptyList()),
            rockBreakerEnvironmentId("base", null, listOf("hot"), emptyList()),
        )
        assertNotEquals(
            rockBreakerEnvironmentId(null, null, listOf("hot"), emptyList()),
            rockBreakerEnvironmentId(null, null, emptyList(), listOf("hot")),
        )
    }

    @Test
    fun `environment IDs ignore source set order and repeated identical demands`() {
        assertEquals(
            rockBreakerEnvironmentId("top", "base", listOf("a", "b"), listOf("c", "d")),
            rockBreakerEnvironmentId("top", "base", listOf("b", "a", "b"), listOf("d", "c", "d")),
        )
    }

    @Test
    fun `one reusable resource satisfies repeated positional environmental demands`() {
        val result = rockBreakerResources("wet-flow", "base", listOf("wet", "hot"), listOf("hot", "base")) {
            when (it) {
                "wet", "wet-flow" -> "wet-container"
                "hot" -> "hot-container"
                "base" -> "base-block"
                else -> null
            }
        }
        assertEquals(listOf("wet-container", "base-block", "hot-container"), result)
    }

    @Test
    fun `unrepresentable environmental requirement fails closed`() {
        assertNull(rockBreakerResources(null, null, listOf("known", "unknown"), emptyList()) {
            it.takeIf { value -> value == "known" }
        })
        assertEquals(emptyList(), rockBreakerResources<String, String>(null, null, emptyList(), emptyList()) { it })
    }

    @Test
    fun `every requested circuit retains all its own candidates`() {
        val groups = mapOf(-1 to listOf("default"), 1 to listOf("one-a", "one-b"), 6 to listOf("six"))
        assertEquals(
            listOf(
                RockBreakerSelection(1, "one-a"),
                RockBreakerSelection(1, "one-b"),
                RockBreakerSelection(6, "six"),
            ),
            selectRockBreakerCandidates(groups, listOf(1, 6, 1)),
        )
        assertEquals(listOf(RockBreakerSelection(-1, "default")), selectRockBreakerCandidates(groups, emptyList()))
    }

    @Test
    fun `only absent circuit groups fall back without suppressing another requested group`() {
        val groups = mapOf(-1 to listOf("default"), 1 to listOf("one"), 6 to emptyList())
        assertEquals(
            listOf(RockBreakerSelection(24, "default"), RockBreakerSelection(1, "one")),
            selectRockBreakerCandidates(groups, listOf(6, 24, 1)),
        )
        assertEquals(emptyList(), selectRockBreakerCandidates(emptyMap<Int, List<String>>(), listOf(1)))
    }

    @Test
    fun `source additions replacements and removals are visible each selection`() {
        val groups = mutableMapOf(-1 to listOf("default"), 1 to listOf("first"))
        assertEquals(listOf(RockBreakerSelection(1, "first")), selectRockBreakerCandidates(groups, listOf(1)))
        groups[1] = listOf("first", "later")
        assertEquals(
            listOf(RockBreakerSelection(1, "first"), RockBreakerSelection(1, "later")),
            selectRockBreakerCandidates(groups, listOf(1)),
        )
        groups[1] = emptyList()
        assertEquals(emptyList(), selectRockBreakerCandidates(groups, listOf(1)))
        groups.remove(1)
        assertEquals(listOf(RockBreakerSelection(1, "default")), selectRockBreakerCandidates(groups, listOf(1)))
    }

    @Test
    fun `live environment mode refresh preserves selection across registration changes`() {
        val modes = ProxyModes<RockBreakerMode> { it.id }
        val first = RockBreakerMode("a", "First")
        val second = RockBreakerMode("b", "Second")
        modes.replace(listOf(first, second))
        modes.next()
        modes.replace(listOf(first, RockBreakerMode("aa", "Added"), second.copy(name = "Changed label")))
        assertEquals("b", modes.current?.id)
        assertEquals("Changed label", modes.current?.name)
        modes.restore(0, "b")
        assertEquals("b", modes.current?.id)
        modes.replace(listOf(first))
        assertEquals("a", modes.current?.id)
        modes.replace(emptyList())
        assertNull(modes.current)
    }

    @Test
    fun `renamed environmental catalysts are retained but never charged`() {
        val source = Stack("dust", 37)
        val supplied = listOf(Stack("wet-container", 1, "renamed"), Stack("circuit", 1, "configuration"), Stack("dust", 4))
        assertEquals(
            listOf(Stack("wet-container", 0, "renamed"), Stack("circuit", 0, "configuration"), Stack("dust", 1)),
            ingredients(listOf(Stack("wet-container"), Stack("circuit")), source, true, supplied),
        )
        assertEquals(1, supplied.first().count)
        assertEquals(4, supplied.last().count)
    }

    @Test
    fun `source reusable input preserves exact NBT and requires actual positive presence`() {
        val source = Stack("reusable", 0, "required")
        assertEquals(listOf(source), ingredients(emptyList(), source, false, listOf(source.copy(count = 1))))
        assertNull(ingredients(emptyList(), source, false, listOf(source.copy(count = 1, tag = "different"))))
        assertNull(ingredients(emptyList(), source, false, listOf(source)))
        assertNull(ingredients(listOf(Stack("wet-container")), source, false, listOf(Stack("wet-container"))))
    }

    @Test
    fun `missing or empty catalyst never grants an environmental requirement`() {
        assertNull(ingredients(listOf(Stack("wet-container")), null, false, emptyList()))
        assertNull(ingredients(listOf(Stack("wet-container")), null, false, listOf(Stack("wet-container", 0))))
        assertNull(ingredients(listOf(Stack("wet-container")), null, false, listOf(Stack("empty-container"))))
    }

    @Test
    fun `catalyst-only recipes keep full requested parallel without consumption`() {
        val required = listOf(Stack("wet-container", 0), Stack("hot-container", 0), Stack("reusable", 0))
        val supplied = listOf(Stack("wet-container"), Stack("hot-container"), Stack("reusable"))
        assertEquals(RockBreakerInputPlan(Int.MAX_VALUE.toDouble(), listOf(0, 0, 0)), plan(Int.MAX_VALUE, required, supplied))
        assertEquals(RockBreakerInputPlan(0.0, listOf(0, 0)), plan(9, required, supplied.dropLast(1)))
    }

    @Test
    fun `charged source input limits parallel while every environmental item survives`() {
        val required = listOf(Stack("wet-container", 0), Stack("hot-container", 0), Stack("circuit", 0), Stack("dust"))
        val supplied = listOf(Stack("wet-container"), Stack("hot-container"), Stack("circuit"), Stack("dust", 3), Stack("dust", 4))
        assertEquals(RockBreakerInputPlan(7.0, listOf(0, 0, 0, 3, 4)), plan(64, required, supplied))
        assertEquals(RockBreakerInputPlan(5.0, listOf(0, 0, 0, 3, 2)), plan(5, required, supplied))
    }

    @Test
    fun `parallel and consumption share strict source identity and NBT matching`() {
        val required = listOf(Stack("input", 1, "exact"))
        val supplied = listOf(Stack("ore-alias", 100, "exact"), Stack("input", 100, "wrong"), Stack("input", 2, "exact"))
        assertEquals(RockBreakerInputPlan(2.0, listOf(0, 0, 2)), plan(64, required, supplied))
    }

    @Test
    fun `overlapping catalyst witnesses are retained and identical demands share one witness`() {
        val required = listOf(Stack("shared", 0), Stack("shared", 0), Stack("shared"))
        assertEquals(RockBreakerInputPlan(2.0, listOf(2)), plan(64, required, listOf(Stack("shared", 3))))
        assertEquals(RockBreakerInputPlan(0.0, listOf(0)), plan(64, required, listOf(Stack("shared"))))
    }

    @Test
    fun `large split supplies cannot overflow and shortages do not overconsume`() {
        assertEquals(
            RockBreakerInputPlan(Int.MAX_VALUE.toDouble(), listOf(Int.MAX_VALUE, 0)),
            plan(Int.MAX_VALUE, listOf(Stack("input")), listOf(Stack("input", Int.MAX_VALUE), Stack("input", Int.MAX_VALUE))),
        )
        assertEquals(RockBreakerInputPlan(0.5, listOf(0)), plan(3, listOf(Stack("input", 2)), listOf(Stack("input"))))
        assertEquals(RockBreakerInputPlan(0.0, listOf(0)), plan(3, listOf(Stack("input")), listOf(Stack("input", -1))))
    }

    @Test
    fun `invalid requests never consume anything`() {
        assertEquals(RockBreakerInputPlan(0.0, listOf(0)), plan(0, listOf(Stack("input")), listOf(Stack("input", 10))))
        assertEquals(RockBreakerInputPlan(0.0, listOf(0)), plan(5, listOf(Stack("input", -1)), listOf(Stack("input", 10))))
    }

    private fun ingredients(catalysts: List<Stack>, source: Stack?, consumed: Boolean, supplied: List<Stack>) =
        buildRockBreakerInputs(
            catalysts,
            source,
            consumed,
            supplied,
            { it.count },
            { provided, requested -> provided.item == requested.item },
            ::exact,
            { stack, count -> stack.copy(count = count) },
        )

    private fun plan(maximum: Int, required: List<Stack>, supplied: List<Stack>) =
        planRockBreakerInputs(
            maximum,
            required.map { RockBreakerItem(it, it.count) },
            supplied.map { RockBreakerItem(it, it.count) },
            ::exact,
        )
}
