package rhynia.nyx.common.mte.proxy

internal fun proxyPower(voltage: Long, amperage: Long): Long =
    if (voltage <= 0 || amperage <= 0) 0
    else if (voltage > Long.MAX_VALUE / amperage) Long.MAX_VALUE
    else voltage * amperage

/** Bound by exact long power before any inputs are consumed; Int recipe EU is only a compatibility sentinel. */
internal fun proxyPowerParallel(availableEUt: Long, actualEUt: Long, maximum: Int): Int =
    if (actualEUt <= 0 || availableEUt < actualEUt || maximum <= 0) 0
    else minOf(maximum.toLong(), availableEUt / actualEUt).toInt()
