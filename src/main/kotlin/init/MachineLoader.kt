package rhynia.nyx.init

import gregtech.api.GregTechAPI
import gregtech.api.interfaces.metatileentity.IMetaTileEntity
import rhynia.nyx.DevEnv
import rhynia.nyx.MOD_NAME
import rhynia.nyx.api.interfaces.Loader
import rhynia.nyx.common.ItemList
import rhynia.nyx.common.NyxItemList
import rhynia.nyx.common.mte.prod.NyxConverter
import rhynia.nyx.common.mte.prod.NyxCopier
import rhynia.nyx.common.mte.prod.NyxProxy
import rhynia.nyx.common.mte.sing.NyxInjector
import rhynia.nyx.config.ConfigDebug
import rhynia.nyx.config.ConfigMachine
import java.io.File

object MachineLoader : Loader {
    /** Size of the ID block reserved after [ConfigMachine.MTE_ID_OFFSET]. */
    private const val RESERVED_ID_COUNT = 50

    private val offset by lazy { ConfigMachine.MTE_ID_OFFSET }

    override fun load() {
        if (ConfigDebug.DEBUG_PRINT_MTE_IDS || DevEnv) printMteIds()
        checkOccupation()
        initialiseMachineClass()
    }

    private fun checkOccupation() {
        val range = (offset + 1)..(offset + RESERVED_ID_COUNT)
        val checked = mutableListOf<Pair<Int, String>>()
        for (i in range) {
            if (GregTechAPI.METATILEENTITIES[i] != null) {
                val mte = GregTechAPI.METATILEENTITIES[i]
                if (mte != null) checked.add(i to "${mte.localName}(${mte.javaClass.name})")
            }
        }
        if (checked.isNotEmpty()) {
            throw IllegalStateException(
                "ID $range preserved by $MOD_NAME is occupied by\n ${checked.joinToString(", \n")
                    { "${it.first}: ${it.second}" }}",
            )
        }
    }

    private fun initialiseMachineClass() {
        NyxItemList.ControllerCopier.register(NyxCopier(offset + 1, "nyx.machine.copier"), ConfigMachine.MTE_COPIER)
        NyxItemList.ControllerProxy.register(NyxProxy(offset + 2, "nyx.machine.proxy"), ConfigMachine.MTE_PROXY)
        NyxItemList.ControllerConverter.register(NyxConverter(offset + 3, "nyx.machine.converter"), ConfigMachine.MTE_CONVERTER)

        NyxItemList.MachineInjector.register(NyxInjector(offset + 31, "nyx.machine.injector", 14), ConfigMachine.MTE_INJECTOR)
    }

    private fun printMteIds() {
        buildList {
            GregTechAPI.METATILEENTITIES.forEachIndexed { i, mte ->
                if (mte != null) add(i to "${mte.localName},${mte.javaClass.name}")
            }
            sortBy { it.first }
        }.let { list ->
            File("loaded_mte_ids.csv").writeText(
                "id,localName,className" +
                    list.joinToString("\n") { "${it.first},${it.second}" },
            )
        }
    }

    private fun ItemList.register(
        mte: IMetaTileEntity,
        condition: Boolean,
    ) {
        if (condition) {
            this.set(mte.getStackForm(1))
        }
    }
}
