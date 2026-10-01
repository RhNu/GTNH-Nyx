package rhynia.nyx.common.mte.prod

/** Result of converting two non-negative material balances into one target denomination. */
internal data class MaterialBalancePlan(
    val outputCount: Long,
    val remainder: Long,
)

/**
 * Adds [storedAmount] and [incomingAmount] without overflowing their intermediate sum, then divides
 * the exact result by [targetAmount]. Returns null only for invalid input or an output-count overflow.
 */
internal fun planMaterialBalance(
    storedAmount: Long,
    incomingAmount: Long,
    targetAmount: Long,
): MaterialBalancePlan? {
    if (storedAmount < 0 || incomingAmount < 0 || targetAmount <= 0) return null

    val storedQuotient = storedAmount / targetAmount
    val incomingQuotient = incomingAmount / targetAmount
    val storedRemainder = storedAmount % targetAmount
    val incomingRemainder = incomingAmount % targetAmount
    val complement = targetAmount - incomingRemainder
    val carry = if (storedRemainder >= complement) 1L else 0L
    val remainder =
        if (carry > 0) {
            storedRemainder - complement
        } else {
            storedRemainder + incomingRemainder
        }

    val outputCount =
        try {
            Math.addExact(Math.addExact(storedQuotient, incomingQuotient), carry)
        } catch (_: ArithmeticException) {
            return null
        }

    return MaterialBalancePlan(outputCount, remainder)
}
