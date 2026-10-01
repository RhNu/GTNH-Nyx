package rhynia.nyx.common.recipe

import gregtech.api.enums.Materials
import gregtech.api.recipe.RecipeMaps
import rhynia.nyx.api.enums.ref.Tier
import rhynia.nyx.api.recipe.RecipePool
import rhynia.nyx.api.recipe.dsl.withRecipeMap
import rhynia.nyx.common.material.NyxMaterials
import rhynia.nyx.config.ConfigRecipe

class GTAddonRecipes : RecipePool() {
    override fun loadRecipes() {
        withRecipeMap(RecipeMaps.electrolyzerRecipes) {
            newRecipe {
                input {
                    +Materials.Redstone.getDust(32)
                }
                output {
                    +NyxMaterials.Restone.getDust(24)
                }
                durSec(4)
                eut(Tier.LV)
            }
            newRecipe {
                input {
                    +Materials.Redstone.getBlocks(16)
                }
                output {
                    +NyxMaterials.Restone.getDust(64)
                    +NyxMaterials.Restone.getDust(64)
                }
                durSec(8)
                eut(Tier.MV)
            }
        }
    }
}

class GTEasyWirelessRecipes : RecipePool() {
    override fun loadRecipes() {
        if (!ConfigRecipe.RECIPE_EASY_WIRELESS) return
        withRecipeMap(RecipeMaps.assemblerRecipes) {
            val time = 4 // seconds
            for (tier in Tier.G_COMMON) {
                newRecipe {
                    itemInputs(
                        tier.getEnergyHatch(1),
                        tier.getCircuit(1),
                        tier.getComponent(Tier.Component.Emitter, 1),
                        tier.getComponent(Tier.Component.Sensor, 1),
                        ic(22),
                    )
                    fluidInputs(tier.getIngotSolder(1))
                    itemOutputs(tier.getEnergyWireless(1))
                    eut(tier)
                    durSec(time)
                }
                newRecipe {
                    itemInputs(
                        tier.getDynamoHatch(1),
                        tier.getCircuit(1),
                        tier.getComponent(Tier.Component.Emitter, 1),
                        tier.getComponent(Tier.Component.Sensor, 1),
                        ic(22),
                    )
                    fluidInputs(tier.getIngotSolder(1))
                    itemOutputs(tier.getDynamoWireless(1))
                    eut(tier)
                    durSec(time)
                }
            }
            for (tier in Tier.G_MEDIUM) {
                newRecipe {
                    itemInputs(
                        tier.getEnergyHatch4A(1),
                        tier.getCircuit(2),
                        tier.getComponent(Tier.Component.Emitter, 1),
                        tier.getComponent(Tier.Component.Sensor, 1),
                        ic(22),
                    )
                    fluidInputs(tier.getIngotSolder(2))
                    itemOutputs(tier.getEnergyWireless4A(1))
                    eut(tier)
                    durSec(time)
                }
                newRecipe {
                    itemInputs(
                        tier.getEnergyHatch16A(1),
                        tier.getCircuit(2),
                        tier.getComponent(Tier.Component.Emitter, 1),
                        tier.getComponent(Tier.Component.Sensor, 1),
                        ic(22),
                    )
                    fluidInputs(tier.getIngotSolder(4))
                    itemOutputs(tier.getEnergyWireless16A(1))
                    eut(tier)
                    durSec(time)
                }
                newRecipe {
                    itemInputs(
                        tier.getEnergyHatch64A(1),
                        tier.getCircuit(2),
                        tier.getComponent(Tier.Component.Emitter, 1),
                        tier.getComponent(Tier.Component.Sensor, 1),
                        ic(22),
                    )
                    fluidInputs(tier.getIngotSolder(8))
                    itemOutputs(tier.getEnergyWireless64A(1))
                    eut(tier)
                    durSec(time)
                }
            }
            // Wireless laser tunnels only exist at UXV.
            Tier.UXV.let { tier ->
                newRecipeIter(1..7) { lvl ->
                    itemInputs(
                        tier.getLaserTarget(lvl, 1),
                        tier.getCircuit(3),
                        tier.getComponent(Tier.Component.Emitter, 2),
                        tier.getComponent(Tier.Component.Sensor, 2),
                        ic(22),
                    )
                    fluidInputs(tier.getIngotSolder(16))
                    itemOutputs(tier.getLaserEnergyWireless(lvl, 1))
                    eut(tier)
                    durSec(time)
                }
            }
        }
    }
}
