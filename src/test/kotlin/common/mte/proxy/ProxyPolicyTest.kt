package rhynia.nyx.common.mte.proxy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProxyPolicyTest {
    @Test
    fun `every catalogued denied machine rejects inherited aliases`() {
        for (typeName in ProxyPolicy.deniedMachineTypeNames) {
            assertEquals(
                ProxyStrategy.DENY,
                ProxyPolicy.select(sequenceOf("example.ControllerAlias", typeName, "java.lang.Object")),
                typeName,
            )
        }
    }

    @Test
    fun `every catalogued mock family covers its current and legacy variants`() {
        for ((strategy, typeNames) in ProxyPolicy.mockMachineTypeNames) {
            for (typeName in typeNames) {
                assertEquals(
                    strategy,
                    ProxyPolicy.select(listOf("example.ControllerAlias", typeName, "java.lang.Object")),
                    typeName,
                )
            }
        }
    }

    @Test
    fun `basic machines route to independent mocks`() {
        assertEquals(
            ProxyStrategy.MASS_FABRICATOR,
            ProxyPolicy.select(setOf("gregtech.common.tileentities.machines.basic.MTEMassfabricator")),
        )
        assertEquals(
            ProxyStrategy.ROCK_BREAKER,
            ProxyPolicy.select(setOf("gregtech.common.tileentities.machines.basic.MTERockBreaker")),
        )
    }

    @Test
    fun `tree and algae controllers select their own mock families`() {
        assertEquals(
            ProxyStrategy.TREE,
            ProxyPolicy.select(setOf("gregtech.common.tileentities.machines.multi.MTETreeFarm")),
        )
        assertEquals(
            ProxyStrategy.ALGAE,
            ProxyPolicy.select(
                setOf(
                    "gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.algae.MTEAlgaePondBaseLegacy",
                ),
            ),
        )
    }

    @Test
    fun `both boiler families and creative scanner are denied through ancestry`() {
        assertEquals(
            ProxyStrategy.DENY,
            ProxyPolicy.select(
                sequenceOf(
                    "gregtech.common.tileentities.machines.multi.MTELargeBoilerTitanium",
                    "gregtech.common.tileentities.machines.multi.MTELargeBoilerBase",
                ),
            ),
        )
        assertEquals(
            ProxyStrategy.DENY,
            ProxyPolicy.select(
                sequenceOf(
                    "gregtech.common.tileentities.machines.multi.MTELargeBoilerBronzeLegacy",
                    "gregtech.common.tileentities.machines.multi.MTELargeBoiler",
                ),
            ),
        )
        assertEquals(
            ProxyStrategy.DENY,
            ProxyPolicy.select(
                sequenceOf(
                    "bartworks.common.tileentities.debug.MTECreativeScanner",
                    "gregtech.common.tileentities.machines.basic.MTEScanner",
                ),
            ),
        )
    }

    @Test
    fun `research is denied even when its original map is real`() {
        assertEquals(
            ProxyStrategy.DENY,
            ProxyPolicy.select(setOf("gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleResearch")),
        )
        assertTrue(ProxyPolicy.isRecipeMapDenied("gt.recipe.spaceResearch"))
    }

    @Test
    fun `denial wins over a mock regardless of hierarchy order`() {
        val hierarchy =
            listOf(
                "gregtech.common.tileentities.machines.basic.MTERockBreaker",
                "gregtech.common.tileentities.machines.basic.MTEScanner",
            )
        assertEquals(ProxyStrategy.DENY, ProxyPolicy.select(hierarchy))
        assertEquals(ProxyStrategy.DENY, ProxyPolicy.select(hierarchy.reversed().asSequence()))
    }

    @Test
    fun `unknown machines and similarly named classes retain the ordinary strategy`() {
        assertEquals(ProxyStrategy.DEFAULT, ProxyPolicy.select(emptyList()))
        val ordinaryTypes =
            listOf(
                "example.MTEScanner",
                "gregtech.common.tileentities.machines.basic.MTEScannerHelper",
                "gregtech.common.tileentities.machines.multi.MTEIndustrialRockBreaker",
                "gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTEIndustrialRockBreakerLegacy",
                "gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTEMassFabricator",
                "bartworks.common.tileentities.tiered.MTEBioLab",
                "gregtech.common.tileentities.machines.multi.MTEIndustrialChisel",
                "gregtech.common.tileentities.machines.multi.foundry.MTEExoFoundry",
            )
        for (typeName in ordinaryTypes) {
            assertEquals(ProxyStrategy.DEFAULT, ProxyPolicy.select(setOf(typeName)), typeName)
        }
    }

    @Test
    fun `denied maps are rejected by exact name including map aliases`() {
        for (name in ProxyPolicy.deniedRecipeMapNames) {
            assertTrue(ProxyPolicy.isRecipeMapDenied(name), name)
            assertFalse(ProxyPolicy.isRecipeMapDenied("$name.extra"), name)
        }
        assertTrue(ProxyPolicy.isRecipeMapDenied("gt.recipe.fakeAssemblylineProcess"))
        assertFalse(ProxyPolicy.isRecipeMapDenied("example.fakeAssemblylineProcess"))
        assertFalse(ProxyPolicy.isRecipeMapDenied("gt.recipe.researchstation"))
        assertFalse(ProxyPolicy.isRecipeMapDenied(""))
    }

    @Test
    fun `Exo Foundry keeps solidification when its module cost map is filtered`() {
        val availableMaps = listOf("gt.recipe.foundry_modules", "gt.recipe.fluidsolidifier")

        assertEquals(
            listOf("gt.recipe.fluidsolidifier"),
            availableMaps.filterNot(ProxyPolicy::isRecipeMapDenied),
        )
    }

    @Test
    fun `mixed and dynamic backend maps remain available`() {
        val ordinaryMaps =
            listOf(
                "bw.recipe.biolab",
                "gt.recipe.industrialchisel",
                "gt.recipe.furnace",
                "gt.recipe.canner",
                "gt.recipe.press",
                "gt.recipe.assembler",
                "gt.recipe.unpackager",
                "gt.recipe.matterfab2",
                "gt.recipe.multiblockrockbreaker",
            )
        for (name in ordinaryMaps) {
            assertFalse(ProxyPolicy.isRecipeMapDenied(name), name)
        }
    }
}
