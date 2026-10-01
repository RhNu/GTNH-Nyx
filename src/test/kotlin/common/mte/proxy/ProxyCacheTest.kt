package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProxyCacheTest {
    @Test
    fun `repeated keys build once and copies cannot poison cache`() {
        val cache = ProxyCache<String, MutableList<Int>>(2) { it.toMutableList() }
        var builds = 0
        fun get() = cache.getOrCreate("oak") { builds++; mutableListOf(3) }!!
        get().add(99)
        assertEquals(listOf(3), get())
        assertEquals(1, builds)
    }

    @Test
    fun `changed dependencies build fresh results and clear invalidates`() {
        data class Key(val gene: String, val tier: Int, val revision: Int)
        val cache = ProxyCache<Key, String>(3) { it }
        var builds = 0
        fun get(key: Key) = cache.getOrCreate(key) { (++builds).toString() }
        assertEquals("1", get(Key("a", 1, 0)))
        assertEquals("2", get(Key("b", 1, 0)))
        assertEquals("3", get(Key("b", 2, 0)))
        assertEquals("4", get(Key("b", 2, 1)))
        cache.clear()
        assertEquals("5", get(Key("b", 2, 1)))
    }

    @Test
    fun `late registration after a miss is visible`() {
        val cache = ProxyCache<String, String>(2) { it }
        assertNull(cache.getOrCreate("new species") { null })
        assertEquals("registered", cache.getOrCreate("new species") { "registered" })
    }

    @Test
    fun `least recently used values are evicted at the bound`() {
        val cache = ProxyCache<String, String>(2) { it }
        cache.getOrCreate("a") { "first" }
        cache.getOrCreate("b") { "second" }
        cache.getOrCreate("a") { "unexpected" }
        cache.getOrCreate("c") { "third" }
        assertEquals("recreated", cache.getOrCreate("b") { "recreated" })
        assertEquals(2, cache.size)
    }

    @Test
    fun `selected modes are instance owned and restored indices are clamped`() {
        val first = ProxyModes<String> { it }
        val second = ProxyModes<String> { it }
        first.replace(listOf("one", "two"))
        second.replace(first.values)
        first.next()
        assertEquals("two", first.current)
        assertEquals("one", second.current)
        first.restore(Int.MAX_VALUE)
        assertEquals("one", first.current)
        first.restore(-1, "two")
        assertEquals("two", first.current)
        first.replace(listOf("two", "three"))
        assertEquals("two", first.current)
        first.replace(emptyList())
        first.next()
        first.restore(0, "two")
        assertNull(first.current)
    }
}
