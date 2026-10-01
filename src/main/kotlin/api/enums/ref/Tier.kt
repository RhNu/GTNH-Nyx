package rhynia.nyx.api.enums.ref

import gregtech.api.enums.GTValues
import gregtech.api.enums.ItemList
import gregtech.api.enums.Materials
import gregtech.api.enums.Mods.BartWorks
import gregtech.api.enums.Mods.GTPlusPlus
import gregtech.api.enums.Mods.GoodGenerator
import gregtech.api.enums.OrePrefixes
import gregtech.api.interfaces.IItemContainer
import gregtech.api.util.GTModHandler
import gregtech.api.util.GTOreDictUnificator
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import org.jetbrains.annotations.Range
import rhynia.nyx.ModLogger
import rhynia.nyx.api.enums.RecipeValues
import rhynia.nyx.api.enums.RecipeValues.INGOT
import rhynia.nyx.api.util.getItem
import rhynia.nyx.common.NyxItemList
import rhynia.nyx.common.item.NyxDebugItem
import tectech.thing.CustomItemList

/** Enum class for tiered components and materials. */
@Suppress("UNUSED", "SpellCheckingInspection")
enum class Tier(
    private val material: Materials,
    val recipeVol: Long,
) {
    ULV(Materials.ULV, RecipeValues.RECIPE_ULV),
    LV(Materials.LV, RecipeValues.RECIPE_LV),
    MV(Materials.MV, RecipeValues.RECIPE_MV),
    HV(Materials.HV, RecipeValues.RECIPE_HV),
    EV(Materials.EV, RecipeValues.RECIPE_EV),
    IV(Materials.IV, RecipeValues.RECIPE_IV),
    LuV(Materials.LuV, RecipeValues.RECIPE_LuV),
    ZPM(Materials.ZPM, RecipeValues.RECIPE_ZPM),
    UV(Materials.UV, RecipeValues.RECIPE_UV),
    UHV(Materials.UHV, RecipeValues.RECIPE_UHV),
    UEV(Materials.UEV, RecipeValues.RECIPE_UEV),
    UIV(Materials.UIV, RecipeValues.RECIPE_UIV),
    UMV(Materials.UMV, RecipeValues.RECIPE_UMV),
    UXV(Materials.UXV, RecipeValues.RECIPE_UXV),
    MAX(Materials.MAX, RecipeValues.RECIPE_MAX),
    ;

    /** Enum class for tiered components. */
    enum class Component {
        ElectricMotor,
        ElectricPiston,
        ElectricPump,
        RobotArm,
        ConveyorModule,
        Emitter,
        Sensor,
        FieldGenerator,
        ;

        fun ofTier(tier: Tier): IItemContainer? =
            when (this) {
                ElectricMotor ->
                    when (tier) {
                        LV -> ItemList.Electric_Motor_LV
                        MV -> ItemList.Electric_Motor_MV
                        HV -> ItemList.Electric_Motor_HV
                        EV -> ItemList.Electric_Motor_EV
                        IV -> ItemList.Electric_Motor_IV
                        LuV -> ItemList.Electric_Motor_LuV
                        ZPM -> ItemList.Electric_Motor_ZPM
                        UV -> ItemList.Electric_Motor_UV
                        UHV -> ItemList.Electric_Motor_UHV
                        UEV -> ItemList.Electric_Motor_UEV
                        UIV -> ItemList.Electric_Motor_UIV
                        UMV -> ItemList.Electric_Motor_UMV
                        UXV -> ItemList.Electric_Motor_UXV
                        MAX -> ItemList.Electric_Motor_MAX
                        else -> null
                    }
                ElectricPiston ->
                    when (tier) {
                        LV -> ItemList.Electric_Piston_LV
                        MV -> ItemList.Electric_Piston_MV
                        HV -> ItemList.Electric_Piston_HV
                        EV -> ItemList.Electric_Piston_EV
                        IV -> ItemList.Electric_Piston_IV
                        LuV -> ItemList.Electric_Piston_LuV
                        ZPM -> ItemList.Electric_Piston_ZPM
                        UV -> ItemList.Electric_Piston_UV
                        UHV -> ItemList.Electric_Piston_UHV
                        UEV -> ItemList.Electric_Piston_UEV
                        UIV -> ItemList.Electric_Piston_UIV
                        UMV -> ItemList.Electric_Piston_UMV
                        UXV -> ItemList.Electric_Piston_UXV
                        MAX -> ItemList.Electric_Piston_MAX
                        else -> null
                    }
                ElectricPump ->
                    when (tier) {
                        LV -> ItemList.Electric_Pump_LV
                        MV -> ItemList.Electric_Pump_MV
                        HV -> ItemList.Electric_Pump_HV
                        EV -> ItemList.Electric_Pump_EV
                        IV -> ItemList.Electric_Pump_IV
                        LuV -> ItemList.Electric_Pump_LuV
                        ZPM -> ItemList.Electric_Pump_ZPM
                        UV -> ItemList.Electric_Pump_UV
                        UHV -> ItemList.Electric_Pump_UHV
                        UEV -> ItemList.Electric_Pump_UEV
                        UIV -> ItemList.Electric_Pump_UIV
                        UMV -> ItemList.Electric_Pump_UMV
                        UXV -> ItemList.Electric_Pump_UXV
                        MAX -> ItemList.Electric_Pump_MAX
                        else -> null
                    }
                RobotArm ->
                    when (tier) {
                        LV -> ItemList.Robot_Arm_LV
                        MV -> ItemList.Robot_Arm_MV
                        HV -> ItemList.Robot_Arm_HV
                        EV -> ItemList.Robot_Arm_EV
                        IV -> ItemList.Robot_Arm_IV
                        LuV -> ItemList.Robot_Arm_LuV
                        ZPM -> ItemList.Robot_Arm_ZPM
                        UV -> ItemList.Robot_Arm_UV
                        UHV -> ItemList.Robot_Arm_UHV
                        UEV -> ItemList.Robot_Arm_UEV
                        UIV -> ItemList.Robot_Arm_UIV
                        UMV -> ItemList.Robot_Arm_UMV
                        UXV -> ItemList.Robot_Arm_UXV
                        MAX -> ItemList.Robot_Arm_MAX
                        else -> null
                    }
                ConveyorModule ->
                    when (tier) {
                        LV -> ItemList.Conveyor_Module_LV
                        MV -> ItemList.Conveyor_Module_MV
                        HV -> ItemList.Conveyor_Module_HV
                        EV -> ItemList.Conveyor_Module_EV
                        IV -> ItemList.Conveyor_Module_IV
                        LuV -> ItemList.Conveyor_Module_LuV
                        ZPM -> ItemList.Conveyor_Module_ZPM
                        UV -> ItemList.Conveyor_Module_UV
                        UHV -> ItemList.Conveyor_Module_UHV
                        UEV -> ItemList.Conveyor_Module_UEV
                        UIV -> ItemList.Conveyor_Module_UIV
                        UMV -> ItemList.Conveyor_Module_UMV
                        UXV -> ItemList.Conveyor_Module_UXV
                        MAX -> ItemList.Conveyor_Module_MAX
                        else -> null
                    }
                Emitter ->
                    when (tier) {
                        LV -> ItemList.Emitter_LV
                        MV -> ItemList.Emitter_MV
                        HV -> ItemList.Emitter_HV
                        EV -> ItemList.Emitter_EV
                        IV -> ItemList.Emitter_IV
                        LuV -> ItemList.Emitter_LuV
                        ZPM -> ItemList.Emitter_ZPM
                        UV -> ItemList.Emitter_UV
                        UHV -> ItemList.Emitter_UHV
                        UEV -> ItemList.Emitter_UEV
                        UIV -> ItemList.Emitter_UIV
                        UMV -> ItemList.Emitter_UMV
                        UXV -> ItemList.Emitter_UXV
                        MAX -> ItemList.Emitter_MAX
                        else -> null
                    }
                Sensor ->
                    when (tier) {
                        LV -> ItemList.Sensor_LV
                        MV -> ItemList.Sensor_MV
                        HV -> ItemList.Sensor_HV
                        EV -> ItemList.Sensor_EV
                        IV -> ItemList.Sensor_IV
                        LuV -> ItemList.Sensor_LuV
                        ZPM -> ItemList.Sensor_ZPM
                        UV -> ItemList.Sensor_UV
                        UHV -> ItemList.Sensor_UHV
                        UEV -> ItemList.Sensor_UEV
                        UIV -> ItemList.Sensor_UIV
                        UMV -> ItemList.Sensor_UMV
                        UXV -> ItemList.Sensor_UXV
                        MAX -> ItemList.Sensor_MAX
                        else -> null
                    }
                FieldGenerator ->
                    when (tier) {
                        LV -> ItemList.Field_Generator_LV
                        MV -> ItemList.Field_Generator_MV
                        HV -> ItemList.Field_Generator_HV
                        EV -> ItemList.Field_Generator_EV
                        IV -> ItemList.Field_Generator_IV
                        LuV -> ItemList.Field_Generator_LuV
                        ZPM -> ItemList.Field_Generator_ZPM
                        UV -> ItemList.Field_Generator_UV
                        UHV -> ItemList.Field_Generator_UHV
                        UEV -> ItemList.Field_Generator_UEV
                        UIV -> ItemList.Field_Generator_UIV
                        UMV -> ItemList.Field_Generator_UMV
                        UXV -> ItemList.Field_Generator_UXV
                        MAX -> ItemList.Field_Generator_MAX
                        else -> null
                    }
            }
    }

    enum class Hatch {
        Dynamo,
        Energy,
        Energy4A,
        Energy16A,
        Energy64A,
        LaserEnergy,
        LaserDynamo,
        WirelessDynamo,
        WirelessEnergy,
        WirelessEnergy4A,
        WirelessEnergy16A,
        WirelessEnergy64A,
        WirelessLaser,
    }

    val voltage: Long
        get() = GTValues.V[ordinal]

    val voltageRecipe: Long
        get() = GTValues.VP[ordinal]

    val superConductor: SuperConductorPart
        get() =
            when (this) {
                ULV,
                LV,
                    -> {
                    ModLogger.error(
                        "$this tier is too low for standard Super Conductor material! Using MV instead.",
                    )
                    SuperConductorPart.MV
                }

                UXV,
                MAX,
                    -> {
                    ModLogger.error(
                        "$this tier is too high for standard Super Conductor material! Using UMV instead.",
                    )
                    SuperConductorPart.UMV
                }

                else -> SuperConductorPart.entries[this.ordinal - 2]
            }

    val solderMaterial: SolderMaterial
        get() =
            when (this) {
                ULV,
                LV,
                MV,
                HV,
                EV,
                    -> SolderMaterial.T1

                UEV,
                UIV,
                UMV,
                UXV,
                MAX,
                    -> SolderMaterial.T3

                else -> SolderMaterial.T2
            }

    private val fallbackStack: ItemStack
        get() = NyxItemList.TestItem01.get(1)

    private fun fail(vararg info: String): ItemStack =
        "Attempting to get ${info.joinToString(", ")}, but it doesn't exist!".let {
            ModLogger.error(it)
            NyxDebugItem.reportInfo(it)
        }

    fun getSolder(amount: Int): FluidStack = solderMaterial.getFluidStack(amount)

    fun getIngotSolder(amount: Int): FluidStack = solderMaterial.getFluidStack(amount * INGOT)

    fun getCircuit(amount: Int): ItemStack = GTOreDictUnificator.get(OrePrefixes.circuit, material, amount.toLong())

    fun getCircuitWrap(amount: Int): ItemStack = BundleChip.entries[ordinal].getItemStack(amount)

    fun getComponent(
        component: Component,
        amount: Int,
    ): ItemStack =
        if (this == ULV) {
            fail("ULV component of ${component.name}")
        } else {
            component.ofTier(this)?.get(amount.toLong()) ?: fail("${component.name} of $this")
        }

    fun getCoil(amount: Int): ItemStack =
        when (this) {
            ULV -> ItemList.ULV_Coil.get(amount.toLong())
            LV -> ItemList.LV_Coil.get(amount.toLong())
            MV -> ItemList.MV_Coil.get(amount.toLong())
            HV -> ItemList.HV_Coil.get(amount.toLong())
            EV -> ItemList.EV_Coil.get(amount.toLong())
            IV -> ItemList.IV_Coil.get(amount.toLong())
            LuV -> ItemList.LuV_Coil.get(amount.toLong())
            ZPM -> ItemList.ZPM_Coil.get(amount.toLong())
            UV -> ItemList.UV_Coil.get(amount.toLong())
            UHV -> ItemList.UHV_Coil.get(amount.toLong())
            UEV,
            UIV,
            UMV,
            UXV,
            MAX,
                -> fail("$this coil")
        }

    fun getComponentAssemblyCasing(amount: Int): ItemStack =
        if (this == ULV) {
            fail("ULV component assembly casing")
        } else {
            GoodGenerator.getItem(
                "componentAssemblylineCasing",
                amount,
                this.ordinal - 1,
            ) {
                fail("Component Assemblyline Casing $this not found! Using fallback.")
            }
        }

    fun getGlass(amount: Int): ItemStack =
        when (this) {
            ULV,
            LV,
            MV,
            HV,
                -> GTModHandler.getModItem(BartWorks.ID, "BW_GlasBlocks", amount.toLong(), 0)

            UMV,
            UXV,
            MAX,
                -> GTModHandler.getModItem(BartWorks.ID, "BW_GlasBlocks2", amount.toLong())

            else ->
                BartWorks.getItem(
                    "BW_GlasBlocks",
                    amount,
                    this.ordinal - 3,
                ) {
                    fail("Glass $this not found! Using fallback.")
                }
        }

    fun getBufferCore(amount: Int): ItemStack =
        when (this) {
            UHV,
            UEV,
            UIV,
            UMV,
            UXV,
            MAX,
                -> GTModHandler.getModItem(GTPlusPlus.ID, "item.itemBufferCore10", amount.toLong())

            else ->
                GTPlusPlus.getItem(
                    "item.itemBufferCore${this.ordinal + 1}",
                    amount,
                ) {
                    fail("BufferCore $this not found! Using fallback.")
                }
        }

    fun getCasing(amount: Int): ItemStack =
        when (this) {
            ULV -> ItemList.Casing_ULV.get(amount.toLong())
            LV -> ItemList.Casing_LV.get(amount.toLong())
            MV -> ItemList.Casing_MV.get(amount.toLong())
            HV -> ItemList.Casing_HV.get(amount.toLong())
            EV -> ItemList.Casing_EV.get(amount.toLong())
            IV -> ItemList.Casing_IV.get(amount.toLong())
            LuV -> ItemList.Casing_LuV.get(amount.toLong())
            ZPM -> ItemList.Casing_ZPM.get(amount.toLong())
            UV -> ItemList.Casing_UV.get(amount.toLong())
            UHV -> fail("Casing_UHV")
            UEV -> ItemList.Casing_UEV.get(amount.toLong())
            UIV -> ItemList.Casing_UIV.get(amount.toLong())
            UMV -> ItemList.Casing_UMV.get(amount.toLong())
            UXV -> ItemList.Casing_UXV.get(amount.toLong())
            MAX -> ItemList.Casing_MAX.get(amount.toLong())
        }

    fun getHull(amount: Int): ItemStack =
        when (this) {
            ULV -> ItemList.Hull_ULV.get(amount.toLong())
            LV -> ItemList.Hull_LV.get(amount.toLong())
            MV -> ItemList.Hull_MV.get(amount.toLong())
            HV -> ItemList.Hull_HV.get(amount.toLong())
            EV -> ItemList.Hull_EV.get(amount.toLong())
            IV -> ItemList.Hull_IV.get(amount.toLong())
            LuV -> ItemList.Hull_LuV.get(amount.toLong())
            ZPM -> ItemList.Hull_ZPM.get(amount.toLong())
            UV -> ItemList.Hull_UV.get(amount.toLong())
            UHV -> fail("Hull_UHV")
            UEV -> ItemList.Hull_UEV.get(amount.toLong())
            UIV -> ItemList.Hull_UIV.get(amount.toLong())
            UMV -> ItemList.Hull_UMV.get(amount.toLong())
            UXV -> ItemList.Hull_UXV.get(amount.toLong())
            MAX -> ItemList.Hull_MAX.get(amount.toLong())
        }

    fun getHatch(
        hatch: Hatch,
        amount: Int,
    ) = when (hatch) {
        Hatch.Dynamo -> getDynamoHatch(amount)
        Hatch.Energy -> getEnergyHatch(amount)
        Hatch.Energy4A -> getEnergyHatch4A(amount)
        Hatch.Energy16A -> getEnergyHatch16A(amount)
        Hatch.Energy64A -> getEnergyHatch64A(amount)
        Hatch.LaserEnergy -> getLaserTarget(1, amount)
        Hatch.LaserDynamo -> getLaserTarget(1, amount)
        Hatch.WirelessDynamo -> getDynamoWireless(amount)
        Hatch.WirelessEnergy -> getEnergyWireless(amount)
        Hatch.WirelessEnergy4A -> getEnergyWireless4A(amount)
        Hatch.WirelessEnergy16A -> getEnergyWireless16A(amount)
        Hatch.WirelessEnergy64A -> getEnergyWireless64A(amount)
        Hatch.WirelessLaser -> getLaserEnergyWireless(1, amount)
    }

    fun getDynamoHatch(amount: Int): ItemStack =
        when (this) {
            ULV -> ItemList.Hatch_Dynamo_ULV.get(amount.toLong())
            LV -> ItemList.Hatch_Dynamo_LV.get(amount.toLong())
            MV -> ItemList.Hatch_Dynamo_MV.get(amount.toLong())
            HV -> ItemList.Hatch_Dynamo_HV.get(amount.toLong())
            EV -> ItemList.Hatch_Dynamo_EV.get(amount.toLong())
            IV -> ItemList.Hatch_Dynamo_IV.get(amount.toLong())
            LuV -> ItemList.Hatch_Dynamo_LuV.get(amount.toLong())
            ZPM -> ItemList.Hatch_Dynamo_ZPM.get(amount.toLong())
            UV -> ItemList.Hatch_Dynamo_UV.get(amount.toLong())
            UHV -> ItemList.Hatch_Dynamo_UHV.get(amount.toLong())
            UEV -> ItemList.Hatch_Dynamo_UEV.get(amount.toLong())
            UIV -> ItemList.Hatch_Dynamo_UIV.get(amount.toLong())
            UMV -> ItemList.Hatch_Dynamo_UMV.get(amount.toLong())
            UXV -> ItemList.Hatch_Dynamo_UXV.get(amount.toLong())
            MAX -> fail("Hatch_Dynamo_MAX")
        }

    fun getEnergyHatch(amount: Int): ItemStack =
        when (this) {
            ULV -> ItemList.Hatch_Energy_ULV.get(amount.toLong())
            LV -> ItemList.Hatch_Energy_LV.get(amount.toLong())
            MV -> ItemList.Hatch_Energy_MV.get(amount.toLong())
            HV -> ItemList.Hatch_Energy_HV.get(amount.toLong())
            EV -> ItemList.Hatch_Energy_EV.get(amount.toLong())
            IV -> ItemList.Hatch_Energy_IV.get(amount.toLong())
            LuV -> ItemList.Hatch_Energy_LuV.get(amount.toLong())
            ZPM -> ItemList.Hatch_Energy_ZPM.get(amount.toLong())
            UV -> ItemList.Hatch_Energy_UV.get(amount.toLong())
            UHV -> ItemList.Hatch_Energy_UHV.get(amount.toLong())
            UEV -> ItemList.Hatch_Energy_UEV.get(amount.toLong())
            UIV -> ItemList.Hatch_Energy_UIV.get(amount.toLong())
            UMV -> ItemList.Hatch_Energy_UMV.get(amount.toLong())
            UXV -> ItemList.Hatch_Energy_UXV.get(amount.toLong())
            MAX -> fail("Hatch_Energy_MAX")
        }

    fun getEnergyHatch4A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_energyMulti4_EV.get(amount.toLong())
            IV -> CustomItemList.eM_energyMulti4_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_energyMulti4_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_energyMulti4_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_energyMulti4_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_energyMulti4_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_energyMulti4_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_energyMulti4_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_energyMulti4_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_energyMulti4_UXV.get(amount.toLong())
            else -> fail("$this 4A energy hatch")
        }

    fun getEnergyHatch16A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_energyMulti16_EV.get(amount.toLong())
            IV -> CustomItemList.eM_energyMulti16_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_energyMulti16_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_energyMulti16_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_energyMulti16_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_energyMulti16_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_energyMulti16_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_energyMulti16_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_energyMulti16_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_energyMulti16_UXV.get(amount.toLong())
            else -> fail("$this 16A energy hatch")
        }

    fun getEnergyHatch64A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_energyMulti64_EV.get(amount.toLong())
            IV -> CustomItemList.eM_energyMulti64_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_energyMulti64_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_energyMulti64_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_energyMulti64_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_energyMulti64_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_energyMulti64_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_energyMulti64_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_energyMulti64_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_energyMulti64_UXV.get(amount.toLong())
            else -> fail("$this 64A energy hatch")
        }

    fun getDynamoHatch4A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_dynamoMulti4_EV.get(amount.toLong())
            IV -> CustomItemList.eM_dynamoMulti4_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_dynamoMulti4_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_dynamoMulti4_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_dynamoMulti4_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_dynamoMulti4_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_dynamoMulti4_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_dynamoMulti4_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_dynamoMulti4_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_dynamoMulti4_UXV.get(amount.toLong())
            else -> fail("$this 4A dynamo hatch")
        }

    fun getDynamoHatch16A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_dynamoMulti16_EV.get(amount.toLong())
            IV -> CustomItemList.eM_dynamoMulti16_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_dynamoMulti16_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_dynamoMulti16_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_dynamoMulti16_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_dynamoMulti16_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_dynamoMulti16_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_dynamoMulti16_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_dynamoMulti16_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_dynamoMulti16_UXV.get(amount.toLong())
            else -> fail("$this 16A dynamo hatch")
        }

    fun getDynamoHatch64A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_dynamoMulti64_EV.get(amount.toLong())
            IV -> CustomItemList.eM_dynamoMulti64_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_dynamoMulti64_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_dynamoMulti64_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_dynamoMulti64_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_dynamoMulti64_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_dynamoMulti64_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_dynamoMulti64_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_dynamoMulti64_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_dynamoMulti64_UXV.get(amount.toLong())
            else -> fail("$this 64A dynamo hatch")
        }

    fun getDynamoWireless(amount: Int): ItemStack =
        when (this) {
            ULV -> ItemList.Wireless_Dynamo_Energy_ULV.get(amount.toLong())
            LV -> ItemList.Wireless_Dynamo_Energy_LV.get(amount.toLong())
            MV -> ItemList.Wireless_Dynamo_Energy_MV.get(amount.toLong())
            HV -> ItemList.Wireless_Dynamo_Energy_HV.get(amount.toLong())
            EV -> ItemList.Wireless_Dynamo_Energy_EV.get(amount.toLong())
            IV -> ItemList.Wireless_Dynamo_Energy_IV.get(amount.toLong())
            LuV -> ItemList.Wireless_Dynamo_Energy_LuV.get(amount.toLong())
            ZPM -> ItemList.Wireless_Dynamo_Energy_ZPM.get(amount.toLong())
            UV -> ItemList.Wireless_Dynamo_Energy_UV.get(amount.toLong())
            UHV -> ItemList.Wireless_Dynamo_Energy_UHV.get(amount.toLong())
            UEV -> ItemList.Wireless_Dynamo_Energy_UEV.get(amount.toLong())
            UIV -> ItemList.Wireless_Dynamo_Energy_UIV.get(amount.toLong())
            UMV -> ItemList.Wireless_Dynamo_Energy_UMV.get(amount.toLong())
            UXV -> ItemList.Wireless_Dynamo_Energy_UXV.get(amount.toLong())
            MAX -> ItemList.Wireless_Dynamo_Energy_MAX.get(amount.toLong())
        }

    fun getEnergyWireless(amount: Int): ItemStack =
        when (this) {
            ULV -> ItemList.Wireless_Hatch_Energy_ULV.get(amount.toLong())
            LV -> ItemList.Wireless_Hatch_Energy_LV.get(amount.toLong())
            MV -> ItemList.Wireless_Hatch_Energy_MV.get(amount.toLong())
            HV -> ItemList.Wireless_Hatch_Energy_HV.get(amount.toLong())
            EV -> ItemList.Wireless_Hatch_Energy_EV.get(amount.toLong())
            IV -> ItemList.Wireless_Hatch_Energy_IV.get(amount.toLong())
            LuV -> ItemList.Wireless_Hatch_Energy_LuV.get(amount.toLong())
            ZPM -> ItemList.Wireless_Hatch_Energy_ZPM.get(amount.toLong())
            UV -> ItemList.Wireless_Hatch_Energy_UV.get(amount.toLong())
            UHV -> ItemList.Wireless_Hatch_Energy_UHV.get(amount.toLong())
            UEV -> ItemList.Wireless_Hatch_Energy_UEV.get(amount.toLong())
            UIV -> ItemList.Wireless_Hatch_Energy_UIV.get(amount.toLong())
            UMV -> ItemList.Wireless_Hatch_Energy_UMV.get(amount.toLong())
            UXV -> ItemList.Wireless_Hatch_Energy_UXV.get(amount.toLong())
            MAX -> ItemList.Wireless_Hatch_Energy_MAX.get(amount.toLong())
        }

    fun getEnergyWireless4A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_energyWirelessMulti4_EV.get(amount.toLong())
            IV -> CustomItemList.eM_energyWirelessMulti4_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_energyWirelessMulti4_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_energyWirelessMulti4_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_energyWirelessMulti4_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_energyWirelessMulti4_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_energyWirelessMulti4_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_energyWirelessMulti4_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_energyWirelessMulti4_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_energyWirelessMulti4_UXV.get(amount.toLong())
            MAX -> CustomItemList.eM_energyWirelessMulti4_MAX.get(amount.toLong())
            else -> fail("$this 4A energy wireless")
        }

    fun getEnergyWireless16A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_energyWirelessMulti16_EV.get(amount.toLong())
            IV -> CustomItemList.eM_energyWirelessMulti16_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_energyWirelessMulti16_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_energyWirelessMulti16_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_energyWirelessMulti16_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_energyWirelessMulti16_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_energyWirelessMulti16_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_energyWirelessMulti16_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_energyWirelessMulti16_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_energyWirelessMulti16_UXV.get(amount.toLong())
            MAX -> CustomItemList.eM_energyWirelessMulti16_MAX.get(amount.toLong())
            else -> fail("$this 16A energy wireless")
        }

    fun getEnergyWireless64A(amount: Int): ItemStack =
        when (this) {
            EV -> CustomItemList.eM_energyWirelessMulti64_EV.get(amount.toLong())
            IV -> CustomItemList.eM_energyWirelessMulti64_IV.get(amount.toLong())
            LuV -> CustomItemList.eM_energyWirelessMulti64_LuV.get(amount.toLong())
            ZPM -> CustomItemList.eM_energyWirelessMulti64_ZPM.get(amount.toLong())
            UV -> CustomItemList.eM_energyWirelessMulti64_UV.get(amount.toLong())
            UHV -> CustomItemList.eM_energyWirelessMulti64_UHV.get(amount.toLong())
            UEV -> CustomItemList.eM_energyWirelessMulti64_UEV.get(amount.toLong())
            UIV -> CustomItemList.eM_energyWirelessMulti64_UIV.get(amount.toLong())
            UMV -> CustomItemList.eM_energyWirelessMulti64_UMV.get(amount.toLong())
            UXV -> CustomItemList.eM_energyWirelessMulti64_UXV.get(amount.toLong())
            MAX -> CustomItemList.eM_energyWirelessMulti64_MAX.get(amount.toLong())
            else -> fail("$this 64A energy wireless")
        }

    fun getLaserTarget(
        tier:
        @Range(from = 1, to = 7)
        Int,
        amount: Int,
    ): ItemStack {
        val target = laserEnergyTargetOf(this, tier) ?: return fail("$this level $tier laser target")
        return target.get(amount.toLong())
    }

    fun getLaserSource(
        tier:
        @Range(from = 1, to = 7)
        Int,
        amount: Int,
    ): ItemStack {
        val source = laserDynamoSourceOf(this, tier) ?: return fail("$this level $tier laser source")
        return source.get(amount.toLong())
    }

    fun getLaserEnergyWireless(
        tier:
        @Range(from = 1, to = 7)
        Int,
        amount: Int,
    ): ItemStack =
        when (this) {
            UXV -> {
                val item = laserEnergyWirelessUXVOf(tier)
                if (item == null) {
                    fail("$this level $tier laser wireless energy")
                } else {
                    item.get(amount.toLong())
                }
            }

            else -> fail("$this level $tier laser wireless energy")
        }

    companion object {
        val G_ALL: Array<Tier>
            get() = entries.toTypedArray()
        val G_COMMON: Array<Tier>
            get() = entries.filter { it != ULV && it != MAX }.toTypedArray()
        val G_MEDIUM: Array<Tier>
            get() = arrayOf(IV, LuV, ZPM, UV, UHV, UEV, UIV, UMV, UXV)

        private fun laserEnergyTargetOf(
            tier: Tier,
            sub: Int,
        ): CustomItemList? =
            when (tier) {
                IV ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_IV
                        else -> null
                    }
                LuV ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_LuV
                        2 -> CustomItemList.eM_energyTunnel2_LuV
                        else -> null
                    }
                ZPM ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_ZPM
                        2 -> CustomItemList.eM_energyTunnel2_ZPM
                        3 -> CustomItemList.eM_energyTunnel3_ZPM
                        else -> null
                    }
                UV ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_UV
                        2 -> CustomItemList.eM_energyTunnel2_UV
                        3 -> CustomItemList.eM_energyTunnel3_UV
                        4 -> CustomItemList.eM_energyTunnel4_UV
                        else -> null
                    }
                UHV ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_UHV
                        2 -> CustomItemList.eM_energyTunnel2_UHV
                        3 -> CustomItemList.eM_energyTunnel3_UHV
                        4 -> CustomItemList.eM_energyTunnel4_UHV
                        5 -> CustomItemList.eM_energyTunnel5_UHV
                        else -> null
                    }
                UEV ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_UEV
                        2 -> CustomItemList.eM_energyTunnel2_UEV
                        3 -> CustomItemList.eM_energyTunnel3_UEV
                        4 -> CustomItemList.eM_energyTunnel4_UEV
                        5 -> CustomItemList.eM_energyTunnel5_UEV
                        6 -> CustomItemList.eM_energyTunnel6_UEV
                        else -> null
                    }
                UIV ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_UIV
                        2 -> CustomItemList.eM_energyTunnel2_UIV
                        3 -> CustomItemList.eM_energyTunnel3_UIV
                        4 -> CustomItemList.eM_energyTunnel4_UIV
                        5 -> CustomItemList.eM_energyTunnel5_UIV
                        6 -> CustomItemList.eM_energyTunnel6_UIV
                        7 -> CustomItemList.eM_energyTunnel7_UIV
                        else -> null
                    }
                UMV ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_UMV
                        2 -> CustomItemList.eM_energyTunnel2_UMV
                        3 -> CustomItemList.eM_energyTunnel3_UMV
                        4 -> CustomItemList.eM_energyTunnel4_UMV
                        5 -> CustomItemList.eM_energyTunnel5_UMV
                        6 -> CustomItemList.eM_energyTunnel6_UMV
                        7 -> CustomItemList.eM_energyTunnel7_UMV
                        8 -> CustomItemList.eM_energyTunnel8_UMV
                        else -> null
                    }
                UXV ->
                    when (sub) {
                        1 -> CustomItemList.eM_energyTunnel1_UXV
                        2 -> CustomItemList.eM_energyTunnel2_UXV
                        3 -> CustomItemList.eM_energyTunnel3_UXV
                        4 -> CustomItemList.eM_energyTunnel4_UXV
                        5 -> CustomItemList.eM_energyTunnel5_UXV
                        6 -> CustomItemList.eM_energyTunnel6_UXV
                        7 -> CustomItemList.eM_energyTunnel7_UXV
                        8 -> CustomItemList.eM_energyTunnel8_UXV
                        9 -> CustomItemList.eM_energyTunnel9_UXV
                        else -> null
                    }
                else -> null
            }

        private fun laserDynamoSourceOf(
            tier: Tier,
            sub: Int,
        ): CustomItemList? =
            when (tier) {
                IV ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_IV
                        else -> null
                    }
                LuV ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_LuV
                        2 -> CustomItemList.eM_dynamoTunnel2_LuV
                        else -> null
                    }
                ZPM ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_ZPM
                        2 -> CustomItemList.eM_dynamoTunnel2_ZPM
                        3 -> CustomItemList.eM_dynamoTunnel3_ZPM
                        else -> null
                    }
                UV ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_UV
                        2 -> CustomItemList.eM_dynamoTunnel2_UV
                        3 -> CustomItemList.eM_dynamoTunnel3_UV
                        4 -> CustomItemList.eM_dynamoTunnel4_UV
                        else -> null
                    }
                UHV ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_UHV
                        2 -> CustomItemList.eM_dynamoTunnel2_UHV
                        3 -> CustomItemList.eM_dynamoTunnel3_UHV
                        4 -> CustomItemList.eM_dynamoTunnel4_UHV
                        5 -> CustomItemList.eM_dynamoTunnel5_UHV
                        else -> null
                    }
                UEV ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_UEV
                        2 -> CustomItemList.eM_dynamoTunnel2_UEV
                        3 -> CustomItemList.eM_dynamoTunnel3_UEV
                        4 -> CustomItemList.eM_dynamoTunnel4_UEV
                        5 -> CustomItemList.eM_dynamoTunnel5_UEV
                        6 -> CustomItemList.eM_dynamoTunnel6_UEV
                        else -> null
                    }
                UIV ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_UIV
                        2 -> CustomItemList.eM_dynamoTunnel2_UIV
                        3 -> CustomItemList.eM_dynamoTunnel3_UIV
                        4 -> CustomItemList.eM_dynamoTunnel4_UIV
                        5 -> CustomItemList.eM_dynamoTunnel5_UIV
                        6 -> CustomItemList.eM_dynamoTunnel6_UIV
                        7 -> CustomItemList.eM_dynamoTunnel7_UIV
                        else -> null
                    }
                UMV ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_UMV
                        2 -> CustomItemList.eM_dynamoTunnel2_UMV
                        3 -> CustomItemList.eM_dynamoTunnel3_UMV
                        4 -> CustomItemList.eM_dynamoTunnel4_UMV
                        5 -> CustomItemList.eM_dynamoTunnel5_UMV
                        6 -> CustomItemList.eM_dynamoTunnel6_UMV
                        7 -> CustomItemList.eM_dynamoTunnel7_UMV
                        8 -> CustomItemList.eM_dynamoTunnel8_UMV
                        else -> null
                    }
                UXV ->
                    when (sub) {
                        1 -> CustomItemList.eM_dynamoTunnel1_UXV
                        2 -> CustomItemList.eM_dynamoTunnel2_UXV
                        3 -> CustomItemList.eM_dynamoTunnel3_UXV
                        4 -> CustomItemList.eM_dynamoTunnel4_UXV
                        5 -> CustomItemList.eM_dynamoTunnel5_UXV
                        6 -> CustomItemList.eM_dynamoTunnel6_UXV
                        7 -> CustomItemList.eM_dynamoTunnel7_UXV
                        8 -> CustomItemList.eM_dynamoTunnel8_UXV
                        9 -> CustomItemList.eM_dynamoTunnel9_UXV
                        else -> null
                    }
                else -> null
            }

        private fun laserEnergyWirelessUXVOf(sub: Int): CustomItemList? =
            when (sub) {
                1 -> CustomItemList.eM_energyWirelessTunnel1_UXV
                2 -> CustomItemList.eM_energyWirelessTunnel2_UXV
                3 -> CustomItemList.eM_energyWirelessTunnel3_UXV
                4 -> CustomItemList.eM_energyWirelessTunnel4_UXV
                5 -> CustomItemList.eM_energyWirelessTunnel5_UXV
                6 -> CustomItemList.eM_energyWirelessTunnel6_UXV
                7 -> CustomItemList.eM_energyWirelessTunnel7_UXV
                8 -> CustomItemList.eM_energyWirelessTunnel8_UXV
                9 -> CustomItemList.eM_energyWirelessTunnel9_UXV
                else -> null
            }
    }
}
