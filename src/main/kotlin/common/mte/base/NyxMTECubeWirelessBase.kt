package rhynia.nyx.common.mte.base

/*
import com.gtnewhorizon.structurelib.structure.IStructureDefinition
import com.gtnewhorizon.structurelib.structure.IStructureElement
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment
import com.gtnewhorizon.structurelib.structure.StructureDefinition
import com.gtnewhorizon.structurelib.structure.StructureUtility
import gregtech.api.enums.HatchElement
import gregtech.api.interfaces.IHatchElement
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.util.GTUtility
import gregtech.api.util.HatchElementBuilder
import gregtech.api.util.MultiblockTooltipBuilder
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import org.jetbrains.annotations.ApiStatus

abstract class NyxMTECubeWirelessBase<T : NyxMTEWirelessBase<T>> : NyxMTEWirelessBase<T> {
    protected constructor(
        aId: Int,
        aName: String,
    ) : super(aId, aName)

    protected constructor(aName: String) : super(aName)

    protected companion object {
        const val OFFSET_H = 1
        const val OFFSET_V = 1
        const val OFFSET_D = 0

        val STRUCTURE_CUBE_SHAPE =
            arrayOf(
                arrayOf("CCC", "CCC", "CCC"),
                arrayOf("C~C", "CBC", "CCC"),
                arrayOf("CCC", "CCC", "CCC"),
            )
    }

    /** The block and corresponding meta used for the casing of the machine. */
    protected abstract val sCasingBlock: Pair<Block, Int>

    /** The index of the casing texture, usually calculated from the block and meta. */
    protected open val sCasingIndex: Int by lazy {
        GTUtility.getCasingTextureIndex(sCasingBlock.first, sCasingBlock.second)
    }

    /** The block used for the core of the machine, leave null if no core block is needed. */
    protected open val sCoreBlock: Pair<Block, Int>?
        @ApiStatus.OverrideOnly get() = null

    /**
     * The core block element used in structure definition, if this one is overridden, sCoreBlock is
     * no more needed
     */
    protected open val sCoreBlockEl: IStructureElement<T>
        get() =
            sCoreBlock?.let { StructureUtility.ofBlock(it.first, it.second) }
                ?: StructureUtility.isAir()

    /** The hatches allowed on the casings */
    protected open val sCasingHatch: Array<IHatchElement<in T>>
        get() =
            arrayOf(
                HatchElement.InputBus,
                HatchElement.InputHatch,
                HatchElement.OutputBus,
                HatchElement.OutputHatch,
                HatchElement.Energy.or(HatchElement.ExoticEnergy),
                HatchElement.Dynamo.or(ExoticDynamo),
                AdditionHatch,
            )

    final override val sControllerBlock: Pair<Block, Int>
        get() = sCasingBlock

    override fun checkMachine(
        aBaseMetaTileEntity: IGregTechTileEntity?,
        aStack: ItemStack?,
    ): Boolean {
        removeMaintenance()
        return checkPiece(STRUCTURE_PIECE_MAIN, OFFSET_H, OFFSET_V, OFFSET_D)
    }

    override fun construct(
        stackSize: ItemStack?,
        hintsOnly: Boolean,
    ) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, OFFSET_H, OFFSET_V, OFFSET_D)
    }

    override fun survivalConstruct(
        stackSize: ItemStack,
        elementBudget: Int,
        env: ISurvivalBuildEnvironment,
    ): Int {
        if (mMachine) return -1
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            OFFSET_H,
            OFFSET_V,
            OFFSET_D,
            elementBudget,
            env,
            false,
            true,
        )
    }

    final override fun genStructureDefinition(): IStructureDefinition<T> =
        StructureDefinition
            .builder<T>()
            .addShape(STRUCTURE_PIECE_MAIN, StructureUtility.transpose(STRUCTURE_CUBE_SHAPE))
            .addElement('B', sCoreBlockEl)
            .addElement(
                'C',
                HatchElementBuilder
                    .builder<T>()
                    .atLeast(*sCasingHatch)
                    .adder { c, t, i -> c.addToMachineList(t, i.toInt()) }
                    .hint(1)
                    .casingIndex(sCasingIndex)
                    .buildAndChain(sCasingBlock.first, sCasingBlock.second),
            ).build()

    protected fun MultiblockTooltipBuilder.beginStructureCube(): MultiblockTooltipBuilder = this.beginStructureBlock(3, 3, 3, false)
}
*/
