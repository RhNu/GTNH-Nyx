package rhynia.nyx.common.mte.prod

import com.gtnewhorizons.modularui.api.widget.Widget
import com.gtnewhorizons.modularui.common.widget.ButtonWidget
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn
import com.gtnewhorizons.modularui.common.widget.SlotWidget
import com.gtnewhorizons.modularui.common.widget.TextWidget
import gregtech.api.GregTechAPI
import gregtech.api.enums.GTValues
import gregtech.api.gui.modularui.GTUITextures
import gregtech.api.interfaces.metatileentity.IMetaTileEntity
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.logic.ProcessingLogic
import gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock
import gregtech.api.recipe.RecipeMap
import gregtech.api.recipe.check.CheckRecipeResult
import gregtech.api.recipe.check.CheckRecipeResultRegistry
import gregtech.api.util.GTRecipe
import gregtech.api.util.GTUtility
import gregtech.api.util.MultiblockTooltipBuilder
import gregtech.api.util.OverclockCalculator
import gregtech.api.util.ParallelHelper
import java.util.stream.Stream
import kotlin.math.log10
import kotlin.math.pow
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumChatFormatting.AQUA
import net.minecraft.util.EnumChatFormatting.DARK_RED
import net.minecraft.util.EnumChatFormatting.WHITE
import net.minecraftforge.common.util.ForgeDirection
import rhynia.nyx.ModLogger
import rhynia.nyx.api.enums.CheckRecipeResultRef
import rhynia.nyx.api.enums.CommonString
import rhynia.nyx.api.process.NyxProcessingLogic
import rhynia.nyx.api.util.localize
import rhynia.nyx.api.util.localized
import rhynia.nyx.common.mte.base.NyxMTECubeBase
import rhynia.nyx.common.mte.proxy.AlgaeAdapter
import rhynia.nyx.common.mte.proxy.MassFabricatorAdapter
import rhynia.nyx.common.mte.proxy.ProxyCache
import rhynia.nyx.common.mte.proxy.ProxyMachine
import rhynia.nyx.common.mte.proxy.ProxyMachineRegistry
import rhynia.nyx.common.mte.proxy.ProxyModes
import rhynia.nyx.common.mte.proxy.ProxyPolicy
import rhynia.nyx.common.mte.proxy.ProxyStrategy
import rhynia.nyx.common.mte.proxy.RockBreakerAdapter
import rhynia.nyx.common.mte.proxy.TreeAdapter
import rhynia.nyx.common.mte.proxy.proxyPower
import rhynia.nyx.common.mte.proxy.proxyPowerParallel

class NyxProxy : NyxMTECubeBase<NyxProxy> {
    constructor(
        aId: Int,
        aName: String,
    ) : super(aId, aName)

    constructor(aName: String) : super(aName)

    override fun newMetaEntity(aTileEntity: IGregTechTileEntity?): IMetaTileEntity = NyxProxy(mName)

    private val pModes = ProxyModes<RecipeMap<*>> { it.unlocalizedName }
    private var pMachine: ProxyMachine? = null
    private var pRevision = 0
    private var pControllerStackSize: Int = 0
    private val pRockBreaker by lazy { RockBreakerAdapter() }

    private fun nextProxyMode() {
        if (pMachine?.strategy == ProxyStrategy.ROCK_BREAKER) pRockBreaker.nextMode() else pModes.next()
        pRevision++
    }

    private val currentModeName: String?
        get() =
            if (pMachine?.strategy == ProxyStrategy.ROCK_BREAKER) pRockBreaker.modeName
            else pModes.current?.let { localize(it.unlocalizedName) }

    override val rMaxParallel: Int
        get() = LogarithmicMapper[pControllerStackSize]

    override fun getRecipeMap(): RecipeMap<*>? {
        updateRecipeContainer()
        return pModes.current
    }

    // Persisted recipe locks bypass strategy matching and cannot be safe for dynamic recipes.
    override fun supportsSingleRecipeLocking(): Boolean = false

    override fun getAvailableRecipeMaps(): Collection<RecipeMap<*>?> = emptyList()

    override fun onScrewdriverRightClick(
        side: ForgeDirection?,
        aPlayer: EntityPlayer?,
        aX: Float,
        aY: Float,
        aZ: Float,
        aTool: ItemStack?,
    ) {
        super.onScrewdriverRightClick(side, aPlayer, aX, aY, aZ, aTool)
        nextProxyMode()
    }

    override fun createProcessingLogic(): ProcessingLogic =
        object : NyxProcessingLogic() {
            private var revision = -1
            private val massFabricator by lazy { MassFabricatorAdapter() }
            private val algae by lazy { AlgaeAdapter() }
            private val trees by lazy { ProxyCache<Class<*>, TreeAdapter>(2) { it } }

            private fun voltageTier(): Int = GTUtility.getTierExtended(availableVoltage).coerceIn(0, GTValues.V.lastIndex)

            private fun actualMockEUt(): Long =
                when (pMachine?.strategy) {
                    ProxyStrategy.TREE -> GTValues.VP[voltageTier().coerceAtLeast(1)]
                    ProxyStrategy.ALGAE -> algae.actualEUt(voltageTier())
                    else -> 0
                }

            private fun discountedMockEUt(): Long = kotlin.math.ceil(actualMockEUt() * euModifier).toLong()

            override fun process(): CheckRecipeResult {
                if (!updateRecipeContainer()) return CheckRecipeResultRef.NO_RECIPE_MAP_SET
                // Also reject old saved locks. They must not bypass a changed controller or deny policy.
                isRecipeLocked = false
                if (revision != pRevision || pMachine?.strategy != ProxyStrategy.DEFAULT) {
                    lastRecipe = null
                    activeDualInv = null
                    dualInvWithPatternToRecipeCache.clear()
                    revision = pRevision
                }
                setEuModifier(rEuModifier)
                setSpeedBonus(rTimeModifier)
                setOverclock(rOverclockType)
                return super.process()
            }

            override fun findRecipeMatches(map: RecipeMap<*>?): Stream<GTRecipe> {
                return when (pMachine?.strategy) {
                    ProxyStrategy.MASS_FABRICATOR ->
                        massFabricator.findRecipe(inputItems ?: emptyArray(), inputFluids ?: emptyArray())
                            ?.let { Stream.of(it) } ?: Stream.empty()
                    ProxyStrategy.TREE -> {
                        val machineClass = pMachine?.source?.javaClass ?: return Stream.empty()
                        val sourceClass = generateSequence<Class<*>>(machineClass) { it.superclass }
                            .firstOrNull { it.name in ProxyPolicy.mockMachineTypeNames.getValue(ProxyStrategy.TREE) }
                            ?: return Stream.empty()
                        trees.getOrCreate(sourceClass) { TreeAdapter(sourceClass) }
                            ?.findRecipes(voltageTier().coerceAtLeast(1), inputItems ?: emptyArray())
                            ?.stream() ?: Stream.empty()
                    }
                    ProxyStrategy.ALGAE -> algae.findRecipes(voltageTier(), inputItems ?: emptyArray()).stream()
                    ProxyStrategy.ROCK_BREAKER -> pRockBreaker.findRecipes(inputItems ?: emptyArray()).stream()
                    ProxyStrategy.DEFAULT -> super.findRecipeMatches(map)
                    else -> Stream.empty()
                }
            }

            override fun validateRecipe(recipe: GTRecipe): CheckRecipeResult {
                if (pMachine?.strategy == ProxyStrategy.TREE || pMachine?.strategy == ProxyStrategy.ALGAE) {
                    val cost = discountedMockEUt()
                    if (cost <= 0 || cost > proxyPower(availableVoltage, availableAmperage)) {
                        return CheckRecipeResultRegistry.insufficientPower(cost.coerceAtLeast(1))
                    }
                }
                return super.validateRecipe(recipe)
            }

            override fun createParallelHelper(recipe: GTRecipe): ParallelHelper {
                val helper = super.createParallelHelper(recipe)
                return when (pMachine?.strategy) {
                    ProxyStrategy.MASS_FABRICATOR -> helper
                        .setAvailableEUt(proxyPower(availableVoltage, availableAmperage))
                        .setMaxParallelCalculator(massFabricator::maxParallel)
                    ProxyStrategy.TREE, ProxyStrategy.ALGAE -> helper
                        .setAvailableEUt(proxyPower(availableVoltage, availableAmperage))
                        .setMaxParallelCalculator { r, maximum, fluids, items ->
                            val bound = minOf(
                                proxyPowerParallel(
                                    proxyPower(availableVoltage, availableAmperage), discountedMockEUt(), maximum,
                                ),
                                proxyPowerParallel(Long.MAX_VALUE, actualMockEUt(), maximum),
                            )
                            r.maxParallelCalculatedByInputs(bound, fluids, *items)
                        }
                    ProxyStrategy.ROCK_BREAKER -> helper
                        .setAvailableEUt(proxyPower(availableVoltage, availableAmperage))
                        .setMaxParallelCalculator(pRockBreaker::maxParallel)
                        .setInputConsumer(pRockBreaker::consumeInputs)
                    else -> helper
                }
            }

            override fun createOverclockCalculator(recipe: GTRecipe): OverclockCalculator =
                if (pMachine?.strategy == ProxyStrategy.MASS_FABRICATOR) {
                    massFabricator.createOverclockCalculator(
                        recipe,
                        (pMachine?.source as? MTETieredMachineBlock)?.mTier?.toInt() ?: 1,
                        availableVoltage,
                        availableAmperage,
                        euModifier,
                        speedBoost,
                    )
                } else if (pMachine?.strategy == ProxyStrategy.TREE || pMachine?.strategy == ProxyStrategy.ALGAE) {
                    OverclockCalculator.ofNoOverclock(actualMockEUt(), recipe.mDuration)
                        .setEUtDiscount(euModifier)
                        .setDurationModifier(speedBoost)
                } else {
                    super.createOverclockCalculator(recipe)
                }

            init {
                setMaxParallelSupplier(::rMaxParallel)
            }
        }

    private fun updateRecipeContainer(): Boolean {
        val machine = ProxyMachineRegistry.resolve(controllerSlot)
        val previous = pMachine
        if (machine?.source !== previous?.source || machine?.strategy != previous?.strategy ||
            machine?.maps != previous?.maps
        ) {
            pRevision++
            pModes.replace(machine?.maps ?: emptyList())
            if (ModLogger.isDebugEnabled) {
                ModLogger.debug("Proxy strategy: ${machine?.strategy}, map: ${pModes.current?.unlocalizedName}")
            }
        }
        pMachine = machine
        pControllerStackSize = if (machine == null) 0 else controllerSlot?.stackSize ?: 0
        return machine != null
    }

    override val sCasingBlock: Pair<Block, Int>
        get() = GregTechAPI.sBlockCasings2 to 0

    override fun createTooltip(): MultiblockTooltipBuilder =
        MultiblockTooltipBuilder()
            .addMachineTypeLocalized()
            .beginStructureCube()
            .toolTipFinisher(CommonString.NyxGigaFac)

    override fun drawTexts(
        screenElements: DynamicPositionedColumn,
        inventorySlot: SlotWidget?,
    ) {
        screenElements.widget(
            TextWidget
                .dynamicString {
                    "${WHITE}${"nyx.common.current"
                        .localized()}: ${currentModeName?.let { AQUA.toString() + it } ?: "${DARK_RED}?"}"
                },
        )
        super.drawTexts(screenElements, inventorySlot)
    }

    override fun addRowUIWidgets(): List<Widget> =
        listOf(
            ButtonWidget()
                .setOnClick { _, _ -> updateRecipeContainer() }
                .setPlayClickSound(true)
                .setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_BUTTON_ARROW_GREEN_UP)
                .setSize(16, 16)
                .addTooltip(localize("nyx.machine.proxy.gui.t.1"))
                .setTooltipShowUpDelay(TOOLTIP_DELAY),
            ButtonWidget()
                .setOnClick { _, _ ->
                    nextProxyMode()
                }
                .setPlayClickSound(true)
                .setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_BUTTON_CHECKMARK)
                .setSize(16, 16)
                .addTooltip(localize("nyx.machine.proxy.gui.t.0"))
                .setTooltipShowUpDelay(TOOLTIP_DELAY),
        )

    override fun loadNBTData(aNBT: NBTTagCompound) {
        super.loadNBTData(aNBT)
        updateRecipeContainer()
        pModes.restore(aNBT.getInteger("pMode"), aNBT.getString("pModeName"))
        if (pMachine?.strategy == ProxyStrategy.ROCK_BREAKER) {
            pRockBreaker.restoreMode(aNBT.getInteger("pRockMode"), aNBT.getString("pRockModeId"))
        }
        pRevision++
    }

    override fun saveNBTData(aNBT: NBTTagCompound) {
        super.saveNBTData(aNBT)
        aNBT.setInteger("pMode", pModes.index)
        pModes.current?.let { aNBT.setString("pModeName", it.unlocalizedName) }
        if (pMachine?.strategy == ProxyStrategy.ROCK_BREAKER) {
            aNBT.setInteger("pRockMode", pRockBreaker.modeIndex)
            pRockBreaker.modeId?.let { aNBT.setString("pRockModeId", it) }
        }
    }

    object LogarithmicMapper {
        private val mappingCache: IntArray by lazy { initMapping() }

        private fun initMapping(): IntArray {
            val cache = IntArray(65)

            val factor = log10(Int.MAX_VALUE.toDouble()) / log10(64.0)

            for (i in 0..64) {
                if (i <= 1) {
                    cache[i] = 1
                } else if (i == 64) {
                    cache[i] = Int.MAX_VALUE
                } else {
                    val value = i.toDouble().pow(factor).toLong()
                    cache[i] = value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                }
            }

            return cache
        }

        operator fun get(i: Int) = mappingCache[i.coerceIn(0, 64)]
    }

}
