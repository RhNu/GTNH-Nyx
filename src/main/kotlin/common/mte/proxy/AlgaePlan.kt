package rhynia.nyx.common.mte.proxy

internal data class AlgaePlan(val outputTier: Int, val compostConsumed: Int)

/** Compost upgrades only the selected output tier, not the power tier. */
internal fun planAlgae(tier: Int, compostAmounts: Iterable<Int>): AlgaePlan? {
    if (tier < 0) return null
    val required = if (tier <= 1) 1 else if (tier >= 7) 64 else 1 shl (tier - 1)
    var remaining = required.toLong()
    for (amount in compostAmounts) {
        remaining -= amount.coerceAtLeast(0).toLong()
        if (remaining <= 0) {
            if (tier == Int.MAX_VALUE) return null
            return AlgaePlan(tier + 1, required)
        }
    }
    return AlgaePlan(tier, 0)
}

/** Live mutable template lists can gain, lose, or retier entries without freezing a partial first load. */
internal class ProxyTierIndex<T : Any> {
    private var snapshot: List<Pair<T, Int>> = emptyList()
    private var byTier: Map<Int, List<T>> = emptyMap()

    fun recipes(entries: Iterable<T>, tierOf: (T) -> Int, tier: Int): List<T> {
        val current = entries.map { it to tierOf(it) }
        if (current != snapshot) {
            snapshot = current
            byTier = current.groupBy({ it.second }, { it.first })
        }
        return byTier[tier]?.toList() ?: emptyList()
    }
}
