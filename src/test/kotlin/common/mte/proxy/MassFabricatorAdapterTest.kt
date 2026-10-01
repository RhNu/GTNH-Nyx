package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MassFabricatorAdapterTest {
    private val config = MassFabricatorConfig(3200, 4, 4, false, 256)

    @Test
    fun `enough amplifier wins without a circuit`() {
        assertEquals(
            MassFabricatorPlan(MassFabricatorBranch.AMPLIFIED, 800, 4),
            planMassFabricator(config, listOf(4), false),
        )
    }

    @Test
    fun `enough amplifier still wins with an integrated circuit`() {
        assertEquals(
            MassFabricatorPlan(MassFabricatorBranch.AMPLIFIED, 800, 4),
            planMassFabricator(config, listOf(4), true),
        )
    }

    @Test
    fun `unamplified recipe can run with no inputs`() {
        assertEquals(
            MassFabricatorPlan(MassFabricatorBranch.UNAMPLIFIED, 3200, 0),
            planMassFabricator(config, emptyList(), false),
        )
    }

    @Test
    fun `insufficient amplifier falls back when allowed`() {
        assertEquals(
            MassFabricatorPlan(MassFabricatorBranch.UNAMPLIFIED, 3200, 0),
            planMassFabricator(config, listOf(3), false),
        )
    }

    @Test
    fun `circuit blocks unamplified branch`() {
        assertNull(planMassFabricator(config, emptyList(), true))
        assertNull(planMassFabricator(config, listOf(3), true))
    }

    @Test
    fun `required amplifier blocks fallback`() {
        val required = config.copy(requiresAmplifier = true)
        assertNull(planMassFabricator(required, emptyList(), false))
        assertNull(planMassFabricator(required, listOf(3), false))
        assertEquals(MassFabricatorBranch.AMPLIFIED, planMassFabricator(required, listOf(4), false)?.branch)
    }

    @Test
    fun `amplifier supply combines tanks and cannot overflow int`() {
        assertTrue(hasMassFabricatorAmplifier(4, listOf(2, 2)))
        assertTrue(hasMassFabricatorAmplifier(Int.MAX_VALUE, listOf(Int.MAX_VALUE - 1, 2)))
        assertFalse(hasMassFabricatorAmplifier(4, listOf(-4, 3)))
    }

    @Test
    fun `zero amplifier cost still requires matching fluid presence`() {
        assertFalse(hasMassFabricatorAmplifier(0, emptyList()))
        assertFalse(hasMassFabricatorAmplifier(0, listOf(-1)))
        assertTrue(hasMassFabricatorAmplifier(0, listOf(0)))
    }

    @Test
    fun `config changes immediately change decision and integer duration`() {
        assertEquals(
            MassFabricatorPlan(MassFabricatorBranch.AMPLIFIED, 333, 2),
            planMassFabricator(
                config.copy(durationMultiplier = 1000, amplifierPerMatter = 2, amplifierSpeedBonus = 3),
                listOf(2),
                false,
            ),
        )
        assertNull(planMassFabricator(config.copy(amplifierPerMatter = 5, requiresAmplifier = true), listOf(4), false))
        assertNull(planMassFabricator(config.copy(durationMultiplier = 1), listOf(4), false))
    }

    @Test
    fun `invalid config fails closed`() {
        val invalidConfigs =
            listOf(
                config.copy(durationMultiplier = 0),
                config.copy(durationMultiplier = -1),
                config.copy(amplifierPerMatter = -1),
                config.copy(amplifierSpeedBonus = 0),
                config.copy(amplifierSpeedBonus = -1),
                config.copy(baseEUt = 0),
            )
        for (invalid in invalidConfigs) {
            assertNull(planMassFabricator(invalid, listOf(4), false))
        }
    }

    @Test
    fun `controller tier limits overclocks and handles invalid low tiers`() {
        assertEquals(0, massFabricatorMaxOverclocks(Int.MIN_VALUE))
        assertEquals(0, massFabricatorMaxOverclocks(0))
        assertEquals(0, massFabricatorMaxOverclocks(1))
        assertEquals(2, massFabricatorMaxOverclocks(3))
        assertEquals(11, massFabricatorMaxOverclocks(12))
    }

    @Test
    fun `controller power includes source eight amp envelope`() {
        assertEquals(256L, massFabricatorOverclockBudget(32, 32, 8))
        assertEquals(1024L, massFabricatorOverclockBudget(128, 8192, 8))
        assertEquals(4096L, massFabricatorOverclockBudget(512, 8192, 8))
    }

    @Test
    fun `actual proxy power restricts controller envelope`() {
        assertEquals(128L, massFabricatorOverclockBudget(512, 32, 4))
        assertEquals(0L, massFabricatorOverclockBudget(512, -1, 4))
        assertEquals(0L, massFabricatorOverclockBudget(512, 32, 0))
        assertEquals(0L, massFabricatorOverclockBudget(-1, 32, 4))
    }

    @Test
    fun `power envelopes saturate without wrapping`() {
        assertEquals(Int.MAX_VALUE.toLong(), massFabricatorOverclockBudget(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(256L, massFabricatorOverclockBudget(32, Long.MAX_VALUE, 2))
    }

    @Test
    fun `parallel power scales source envelope but remains bounded by shared supply`() {
        assertEquals(8192L, massFabricatorOverclockBudget(512, 32768, 1, 2))
        assertEquals(16384L, massFabricatorOverclockBudget(512, 32768, 1, 4))
        assertEquals(8192L, massFabricatorOverclockBudget(512, 8192, 1, 4))
        assertEquals(0L, massFabricatorOverclockBudget(512, 8192, 1, 0))
        assertEquals(4294967294L, massFabricatorOverclockBudget(Long.MAX_VALUE, Long.MAX_VALUE, 2, 2))
    }

    @Test
    fun `each parallel preserves source overclocks with adequate shared power`() {
        for ((parallel, consumption) in listOf(1 to 1024L, 2 to 2048L, 4 to 4096L)) {
            val calculator = calculator(32768).setParallel(64).setCurrentParallel(parallel).calculate()
            assertEquals(2, calculator.performedOverclocks)
            assertEquals(consumption, calculator.consumption)
            assertEquals(800, calculator.duration)
        }
    }

    @Test
    fun `shared power reduces source overclock without exceeding supply`() {
        val two = calculator(2048).setParallel(64).setCurrentParallel(2).calculate()
        assertEquals(1, two.performedOverclocks)
        assertEquals(1024L, two.consumption)
        assertEquals(1600, two.duration)

        val four = calculator(2048).setParallel(64).setCurrentParallel(4).calculate()
        assertEquals(0, four.performedOverclocks)
        assertEquals(1024L, four.consumption)
        assertEquals(3200, four.duration)
    }

    @Test
    fun `parallel setter recalculates budget before final parallel is known`() {
        val calculator = calculator(32768).setParallel(4).calculate()
        assertEquals(2, calculator.performedOverclocks)
        assertEquals(4096L, calculator.consumption)
        assertEquals(800, calculator.duration)
    }

    @Test
    fun `modifiers apply once to source overclocked parallel results`() {
        val calculator =
            calculator(32768).setEUtDiscount(0.5).setDurationModifier(0.5)
                .setParallel(2).setCurrentParallel(2).calculate()
        assertEquals(2, calculator.performedOverclocks)
        assertEquals(1024L, calculator.consumption)
        assertEquals(400, calculator.duration)
    }

    @Test
    fun `parallel fluid filter excludes same-fluid incompatible tags`() {
        data class Fluid(val name: String, val tag: String?, val amount: Int)
        val exact = Fluid("uua", null, 2)
        val tagged = Fluid("uua", "unrelated", 100)
        val other = Fluid("water", null, 100)
        assertEquals(
            listOf(exact),
            selectMassFabricatorFluids(listOf(exact, tagged, other), listOf(Fluid("uua", null, 1))) { supplied, required ->
                supplied.name == required.name && supplied.tag == required.tag
            },
        )
        assertEquals(
            emptyList(),
            selectMassFabricatorFluids(listOf(exact, tagged, other), emptyList()) { _, _ -> true },
        )
    }

    private fun calculator(availablePower: Long) =
        MassFabricatorOverclockCalculator(512, availablePower, 1)
            .setRecipeEUt(256)
            .setDuration(3200)
            .setEUtIncreasePerOC(2.0)
            .setDurationDecreasePerOC(2.0)
            .setMaxOverclocks(2)
}
