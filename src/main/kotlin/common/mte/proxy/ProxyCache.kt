package rhynia.nyx.common.mte.proxy

/** Small, access-ordered cache. Callers key every dependency and receive isolated values. */
internal class ProxyCache<K : Any, V : Any>(
    private val capacity: Int,
    private val copy: (V) -> V,
) {
    init {
        require(capacity > 0)
    }

    private val entries = LinkedHashMap<K, V>(capacity, 0.75f, true)

    @Synchronized
    fun getOrCreate(key: K, create: () -> V?): V? {
        entries[key]?.let { return copy(it) }
        // A missing runtime registration must not become a permanent negative cache entry.
        val value = create() ?: return null
        entries[key] = copy(value)
        if (entries.size > capacity) entries.remove(entries.keys.first())
        return copy(value)
    }

    @Synchronized
    fun clear() = entries.clear()

    @get:Synchronized
    val size: Int
        get() = entries.size
}

/** Instance-owned mode state: cached machine descriptors must never share a mutable cursor. */
internal class ProxyModes<T : Any>(
    private val name: (T) -> String,
) {
    var values: List<T> = emptyList()
        private set
    var index: Int = 0
        private set
    val current: T?
        get() = values.getOrNull(index)

    fun replace(next: List<T>) {
        val previous = current?.let(name)
        values = next.toList()
        index = values.indexOfFirst { name(it) == previous }.coerceAtLeast(0)
    }

    fun next() {
        if (values.isNotEmpty()) index = (index + 1) % values.size
    }

    fun restore(savedIndex: Int, savedName: String? = null) {
        val named = savedName?.let { n -> values.indexOfFirst { name(it) == n } } ?: -1
        index = if (named >= 0) named else savedIndex.takeIf { it in values.indices } ?: 0
    }
}
