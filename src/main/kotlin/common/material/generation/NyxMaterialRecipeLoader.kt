package rhynia.nyx.common.material.generation

import bartworks.client.textures.PrefixTextureLinker
import bartworks.util.BWColorUtil.getDyeFromColor
import cpw.mods.fml.common.FMLCommonHandler
import gregtech.api.enums.FluidState
import gregtech.api.enums.ItemList
import gregtech.api.enums.Materials
import gregtech.api.enums.Mods
import gregtech.api.enums.OrePrefixes
import gregtech.api.enums.OrePrefixes.block
import gregtech.api.enums.OrePrefixes.bolt
import gregtech.api.enums.OrePrefixes.dust
import gregtech.api.enums.OrePrefixes.dustSmall
import gregtech.api.enums.OrePrefixes.dustTiny
import gregtech.api.enums.OrePrefixes.foil
import gregtech.api.enums.OrePrefixes.gearGt
import gregtech.api.enums.OrePrefixes.gem
import gregtech.api.enums.OrePrefixes.gemChipped
import gregtech.api.enums.OrePrefixes.gemExquisite
import gregtech.api.enums.OrePrefixes.gemFlawed
import gregtech.api.enums.OrePrefixes.gemFlawless
import gregtech.api.enums.OrePrefixes.ingot
import gregtech.api.enums.OrePrefixes.lens
import gregtech.api.enums.OrePrefixes.plate
import gregtech.api.enums.OrePrefixes.plateDouble
import gregtech.api.enums.OrePrefixes.ring
import gregtech.api.enums.OrePrefixes.screw
import gregtech.api.enums.OrePrefixes.stick
import gregtech.api.enums.OrePrefixes.stickLong
import gregtech.api.enums.Textures
import gregtech.api.recipe.RecipeCategories
import gregtech.api.recipe.RecipeMaps.benderRecipes
import gregtech.api.recipe.RecipeMaps.cutterRecipes
import gregtech.api.recipe.RecipeMaps.extruderRecipes
import gregtech.api.recipe.RecipeMaps.fluidExtractionRecipes
import gregtech.api.recipe.RecipeMaps.fluidSolidifierRecipes
import gregtech.api.recipe.RecipeMaps.hammerRecipes
import gregtech.api.recipe.RecipeMaps.laserEngraverRecipes
import gregtech.api.recipe.RecipeMaps.latheRecipes
import gregtech.api.recipe.RecipeMaps.packagerRecipes
import gregtech.api.render.TextureFactory
import gregtech.api.util.GTModHandler
import gregtech.api.util.GTUtility
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.oredict.OreDictionary
import rhynia.nyx.api.enums.RecipeValues.RECIPE_EV
import rhynia.nyx.api.enums.RecipeValues.RECIPE_HV
import rhynia.nyx.api.enums.RecipeValues.RECIPE_LV
import rhynia.nyx.api.enums.RecipeValues.RECIPE_MV
import rhynia.nyx.api.enums.RecipeValues.SECOND
import rhynia.nyx.api.enums.RecipeValues.TICK
import rhynia.nyx.api.recipe.RecipePool
import rhynia.nyx.api.recipe.dsl.GRecipeBuilder
import rhynia.nyx.api.recipe.dsl.shapedRecipe
import rhynia.nyx.api.recipe.dsl.withRecipeMap
import rhynia.nyx.common.material.generation.NyxMaterial.Companion.shouldHasCell
import rhynia.nyx.common.material.generation.NyxMaterial.Companion.shouldHasForestryCell
import kotlin.math.max

/**
 * Automatically generates recipes for the given material.
 *
 * The recipes will be enabled by the flags in the material.
 */
class NyxMaterialRecipeLoader(
    private val mat: NyxMaterial,
) : RecipePool() {
    override fun loadRecipes() {
        loadDustRecipes()
        loadCellRecipes()
        loadGemRecipes()
        loadMetalRecipes()
        loadMiscRecipes()
    }

    private fun loadDustRecipes() {
        if (!mat.flagDust) return

        shapedRecipe(mat.get(dust)) {
            shape("TTT", "TTT", "TTT")
            def('T', mat.get(dustTiny))
        }

        shapedRecipe(mat.get(dust)) {
            shape("TT ", "TT ")
            def('T', mat.get(dustSmall))
        }

        shapedRecipe(mat.get(dustSmall, 4)) {
            shape(" T ")
            def('T', mat.get(dust))
        }

        shapedRecipe(mat.get(dustTiny, 9)) {
            shape("T  ")
            def('T', mat.get(dust))
        }

        withRecipeMap(packagerRecipes) {
            newRecipe {
                itemInputs(mat.get(dustTiny, 9), ItemList.Schematic_Dust.get(0))
                itemOutputs(mat.get(dust))
                durSec(5)
                eut(4)
            }
            newRecipe {
                itemInputs(mat.get(dustSmall, 4), ItemList.Schematic_Dust.get(0))
                itemOutputs(mat.get(dust))
                durSec(5)
                eut(4)
            }
            newRecipe {
                itemInputs(mat.get(dustTiny, 9), ItemList.Schematic_3by3.get(0))
                itemOutputs(mat.get(dust))
                durSec(5)
                eut(4)
            }
            newRecipe {
                itemInputs(mat.get(dustSmall, 4), ItemList.Schematic_2by2.get(0))
                itemOutputs(mat.get(dust))
                durSec(5)
                eut(4)
            }
        }
    }

    private fun loadCellRecipes() {
        if (!mat.flagFluid) return

        NyxMaterialLoader.FluidMap[mat.id]?.forEach { (state, fluid) ->
            if (state.shouldHasCell()) {
                FluidContainerRegistry
                    .FluidContainerData(
                        FluidStack(fluid, 1000),
                        mat.getCell(state),
                        Materials.Empty.getCells(1),
                    ).let {
                        GTUtility.insertInFluidToContainersMap(it)
                        FluidContainerRegistry.registerFluidContainer(it)
                    }
            }

            if (state.shouldHasForestryCell()) {
                FluidContainerRegistry
                    .FluidContainerData(
                        FluidStack(fluid, 1000),
                        GTModHandler.getModItem(Mods.Forestry.ID, "waxCapsule", 1),
                        Materials.Empty.getCells(1),
                    ).let {
                        GTUtility.insertInFluidToContainersMap(it)
                        FluidContainerRegistry.registerFluidContainer(it)
                    }
            }

            if (mat.flagDust && state in setOf(FluidState.LIQUID, FluidState.MOLTEN)) {
                val amount = if (state == FluidState.LIQUID) 1000 else 144
                val eu = if (mat.mass > 128) 64 else 30
                GRecipeBuilder
                    .itemInputs(mat.getDust())
                    .fluidOutputs(mat.getFluidStack(state, amount))
                    .duration(30)
                    .eut(mat.mass)
                    .recipeCategory(RecipeCategories.fluidExtractorRecycling)
                    .addTo(fluidExtractionRecipes)
                GRecipeBuilder
                    .itemInputs(GTUtility.getIntegratedCircuit(1))
                    .itemOutputs(mat.getDust())
                    .fluidInputs(mat.getFluidStack(state, amount))
                    .duration(mat.mass)
                    .eut(eu)
                    .addTo(fluidSolidifierRecipes)
            }
        }
    }

    private fun loadGemRecipes() {
        if (!mat.flagGem) return

        arrayOf(
            gemFlawless to gemExquisite,
            gem to gemFlawless,
            gemFlawed to gem,
            gemChipped to gemFlawed,
        ).forEach { (result, from) ->
            shapedRecipe(mat.get(result, 2)) {
                shape("h  ", "W  ")
                def('W', mat.get(from))
            }
            GRecipeBuilder
                .itemInputs(mat.get(from))
                .itemOutputs(mat.get(result, 2))
                .duration(3 * SECOND + 4 * TICK)
                .eut(16)
                .addTo(hammerRecipes)
        }

        if (mat.isTypeValid(dustTiny)) {
            GRecipeBuilder
                .itemInputs(mat.get(gemChipped))
                .itemOutputs(mat.get(dustTiny))
                .duration(3 * SECOND + 4 * TICK)
                .eut(16)
                .addTo(hammerRecipes)
        }

        if (mat.isTypeValid(lens)) {
            if (mat.isTypeValid(plate)) {
                GRecipeBuilder
                    .itemInputs(mat.get(plate))
                    .itemOutputs(mat.get(lens))
                    .durMin(1)
                    .eut(RECIPE_MV)
                    .addTo(cutterRecipes)
            }
            GRecipeBuilder
                .itemInputs(mat.get(gemExquisite))
                .itemOutputs(mat.get(lens), mat.get(dust, 2))
                .durMin(2)
                .eut(RECIPE_LV)
                .addTo(latheRecipes)

            val dye = getDyeFromColor(mat.color)
            TextureFactory
                .of(
                    Textures.BlockIcons.MACHINE_CASINGS[2][0],
                    TextureFactory.of(Textures.BlockIcons.OVERLAY_LENS, mat.color),
                ) // .let { GregTechAPI.registerCover(mat.get(lens), it, CoverLens(dye.mIndex, it)) }

            OreDictionary.getOres("craftingLens${dye.name.replace("\\s".toRegex(), "")}}").forEach {
                it.stackSize = 0

                GRecipeBuilder
                    .itemInputs(mat.get(gemChipped, 3), it)
                    .itemOutputs(mat.get(gemFlawed, 1))
                    .durSec(30)
                    .eut(RECIPE_LV)
                    .addTo(laserEngraverRecipes)

                GRecipeBuilder
                    .itemInputs(mat.get(gemFlawed, 3), it)
                    .itemOutputs(mat.get(gem, 1))
                    .durSec(30)
                    .eut(RECIPE_MV)
                    .addTo(laserEngraverRecipes)

                GRecipeBuilder
                    .itemInputs(mat.get(gem, 3), it)
                    .itemOutputs(mat.get(gemFlawless, 1))
                    .durMin(1)
                    .eut(RECIPE_HV)
                    .addTo(laserEngraverRecipes)

                GRecipeBuilder
                    .itemInputs(mat.get(gemFlawless, 3), it)
                    .itemOutputs(mat.get(gemExquisite, 1))
                    .durMin(2)
                    .eut(RECIPE_EV)
                    .addTo(laserEngraverRecipes)
            }
        }
    }

    private fun loadMetalRecipes() {
        if (!mat.flagPlate) return

        if (mat.flagGem && mat.flagMisc && mat.flagDust) {
            GRecipeBuilder
                .itemInputs(mat.getGem())
                .itemOutputs(mat.get(stick), mat.get(dustSmall, 2))
                .duration(max(1, mat.mass * 5))
                .eut(16)
                .addTo(latheRecipes)

            GRecipeBuilder
                .itemInputs(mat.get(stick, 2))
                .itemOutputs(mat.get(stickLong))
                .duration(max(1, mat.mass))
                .eut(16)
                .addTo(hammerRecipes)

            GTModHandler.addCraftingRecipe(
                mat.get(stick, 2),
                GTModHandler.RecipeBits.BITS_STD,
                arrayOf("s", "X", 'X', mat.get(stickLong)),
            )
            GTModHandler.addCraftingRecipe(
                mat.get(stick),
                GTModHandler.RecipeBits.BITS_STD,
                arrayOf("f ", " X", 'X', mat.get(gem)),
            )

            mat.textureSet.let { set ->
                (
                    if (FMLCommonHandler.instance().side.isClient) {
                        TextureFactory.of(
                            set.mTextures[
                                PrefixTextureLinker.blockTexMap
                                    .getOrDefault(set, block.textureIndex.toShort())
                                    .toInt(),
                            ],
                            mat.color,
                        )
                    } else {
                        TextureFactory.of(set.mTextures[block.textureIndex], mat.color)
                    }
                ) // .let { GregTechAPI.registerCover(mat.getPlate(), it, null) }
            }
        }

        if (mat.flagIngot) {
            if (mat.flagMisc) {
                GTModHandler.addCraftingRecipe(
                    mat.get(stick, 2),
                    GTModHandler.RecipeBits.BITS_STD,
                    arrayOf("s", "X", 'X', mat.get(stickLong)),
                )
                GTModHandler.addCraftingRecipe(
                    mat.get(stick),
                    GTModHandler.RecipeBits.BITS_STD,
                    arrayOf("f ", " X", 'X', mat.get(ingot)),
                )

                if (mat.flagDust) {
                    GRecipeBuilder
                        .itemInputs(mat.get(ingot))
                        .itemOutputs(mat.get(stick), mat.get(dustSmall, 2))
                        .duration(max(1, mat.mass * 5))
                        .eut(16)
                        .addTo(latheRecipes)
                }

                GRecipeBuilder
                    .itemInputs(mat.get(stick, 2))
                    .itemOutputs(mat.get(stickLong))
                    .duration(max(1, mat.mass))
                    .eut(16)
                    .addTo(hammerRecipes)
                GRecipeBuilder
                    .itemInputs(mat.get(ingot), ItemList.Shape_Extruder_Rod.get(0))
                    .itemOutputs(mat.get(stick, 2))
                    .duration(max(1, mat.mass * 2))
                    .eut(45)
                    .addTo(extruderRecipes)
            }
            GTModHandler.addCraftingRecipe(
                mat.get(plate),
                GTModHandler.RecipeBits.BITS_STD,
                arrayOf("h", "X", "X", 'X', mat.get(ingot)),
            )
            GTModHandler.addCraftingRecipe(
                mat.get(foil, 2),
                GTModHandler.RecipeBits.BITS_STD,
                arrayOf("hX", 'X', mat.get(plate)),
            )

            GRecipeBuilder
                .itemInputs(mat.get(ingot), GTUtility.getIntegratedCircuit(1))
                .itemOutputs(mat.get(plate))
                .duration(max(1, mat.mass))
                .eut(24)
                .addTo(benderRecipes)
            GRecipeBuilder
                .itemInputs(mat.get(ingot), GTUtility.getIntegratedCircuit(10))
                .itemOutputs(mat.get(foil, 4))
                .duration(max(1, mat.mass * 2))
                .eut(24)
                .addTo(benderRecipes)
            GRecipeBuilder
                .itemInputs(mat.get(ingot, 3))
                .itemOutputs(mat.get(plate, 2))
                .duration(max(1, mat.mass))
                .eut(16)
                .addTo(hammerRecipes)
            GRecipeBuilder
                .itemInputs(mat.get(ingot), ItemList.Shape_Extruder_Plate.get(0))
                .itemOutputs(mat.get(plate))
                .duration(max(1, mat.mass * 2))
                .eut(45)
                .addTo(extruderRecipes)
        }

        GRecipeBuilder
            .itemInputs(mat.get(plate), GTUtility.getIntegratedCircuit(1))
            .itemOutputs(mat.get(foil, 4))
            .duration(max(1, mat.mass))
            .eut(24)
            .addTo(benderRecipes)

        GRecipeBuilder
            .itemInputs(mat.get(ingot, 2), GTUtility.getIntegratedCircuit(2))
            .itemOutputs(mat.get(plateDouble))
            .duration(max(1, mat.mass * 2))
            .eut(60)
            .addTo(hammerRecipes)
//        GregTechAPI.registerCover(
//            mat.get(plateDouble),
//            TextureFactory.of(mat.textureSet.mTextures[72], mat.color, false),
//            null,
//        )
    }

    private fun loadMiscRecipes() {
        if (!mat.flagMisc) return
        val volMultiply = 30

        // Bolt
        GRecipeBuilder
            .itemInputs(
                if (mat.flagGem) mat.get(gem) else mat.get(ingot),
                ItemList.Shape_Extruder_Bolt.get(0L),
            ).itemOutputs(mat.get(bolt, 8))
            .duration(max(1, mat.mass * 2))
            .eut(8 * volMultiply)
            .addTo(extruderRecipes)
        GRecipeBuilder
            .itemInputs(mat.get(stick))
            .itemOutputs(mat.get(bolt, 4))
            .duration(max(1, mat.mass * 2))
            .eut(4)
            .addTo(cutterRecipes)

        // Screw
        GTModHandler.addCraftingRecipe(
            mat.get(screw),
            GTModHandler.RecipeBits.BITS_STD,
            arrayOf("fX", "X ", 'X', mat.get(bolt)),
        )
        GRecipeBuilder
            .itemInputs(mat.get(bolt))
            .itemOutputs(mat.get(screw))
            .duration(max(1, mat.mass / 8))
            .eut(4)
            .addTo(latheRecipes)

        if (mat.flagGem) return

        // Ring
        GTModHandler.addCraftingRecipe(
            mat.get(ring),
            GTModHandler.RecipeBits.BITS_STD,
            arrayOf("h ", "fX", 'X', mat.get(stick)),
        )
        GRecipeBuilder
            .itemInputs(mat.get(ingot), ItemList.Shape_Extruder_Ring.get(0))
            .itemOutputs(mat.get(ring, 4))
            .duration(max(1, mat.mass * 2))
            .eut(6 * volMultiply)
            .addTo(hammerRecipes)

        // Gear
        GTModHandler.addCraftingRecipe(
            mat.get(gearGt),
            GTModHandler.RecipeBits.BITS_STD,
            arrayOf("SPS", "PwP", "SPS", 'P', mat.get(plate), 'S', mat.get(stick)),
        )
        GRecipeBuilder
            .itemInputs(mat.get(ingot, 4), ItemList.Shape_Extruder_Gear.get(0))
            .itemOutputs(mat.get(gearGt))
            .duration(max(1, mat.mass * 5))
            .eut(8 * volMultiply)
            .addTo(extruderRecipes)

        // Small Gear
        GTModHandler.addCraftingRecipe(
            mat.get(OrePrefixes.gearGtSmall),
            GTModHandler.RecipeBits.BITS_STD,
            arrayOf(" S ", "hPx", " S ", 'S', mat.get(stick), 'P', mat.get(plate)),
        )
        GRecipeBuilder
            .itemInputs(mat.get(ingot), ItemList.Shape_Extruder_Small_Gear.get(0))
            .itemOutputs(mat.get(OrePrefixes.gearGtSmall))
            .duration(mat.mass)
            .eut(8 * volMultiply)
            .addTo(extruderRecipes)
    }
}
