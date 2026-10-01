package rhynia.nyx.init.mixin

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager
import gregtech.api.enums.Mods
import gregtech.api.enums.Mods.AppliedEnergistics2
import gregtech.api.enums.Mods.BartWorks
import rhynia.nyx.ModLogger
import rhynia.nyx.config.ConfigDebug
import rhynia.nyx.config.ConfigMachine
import rhynia.nyx.config.ConfigMixin
import rhynia.nyx.config.ConfigRecipe

/** Store all late mixins, will be loaded by MixinManager */
@Suppress("UNUSED", "SpellCheckingInspection")
enum class MixinEntry(
    actions: MixinBuilder.() -> Unit,
) {
    PortableCell({
        ofClasses("ae.MixinPortableCell")
        toMod(AppliedEnergistics2)
        condition = ConfigMixin::MIXIN_AE_PORTABLE_CELL
    }),
    QuantumCell({
        ofClasses("ae.MixinQuantumCell")
        toMod(AppliedEnergistics2)
        condition = ConfigMixin::MIXIN_AE_QUANTUM_CELL
    }),

    MegaMultiBase({
        ofClasses("bw.MixinMegaMultiBase")
        toMod(BartWorks)
        condition = ConfigMixin::MIXIN_BW_MEGA_NO_AIR_CHECK
    }),
    ;

    private val builder = MixinBuilder().apply(actions)

    val modId: String =
        builder.targetModId ?: throw IllegalStateException("Target mod ID not set for $name")
    val mixinClasses: List<String> = builder.classes
    val condition: (() -> Boolean) = builder.condition ?: { true }

    companion object {
        fun findLateMixins(loadedMods: Set<String>): List<String> {
            ConfigurationManager.registerConfig(ConfigMachine::class.java)
            ConfigurationManager.registerConfig(ConfigRecipe::class.java)
            ConfigurationManager.registerConfig(ConfigMixin::class.java)
            ConfigurationManager.registerConfig(ConfigDebug::class.java)

            val mixinsToLoad = mutableListOf<String>()
            val mininsNotLoad = mutableListOf<String>()
            MixinEntry.entries.forEach {
                if (it.modId in loadedMods && (it.condition())) {
                    mixinsToLoad.addAll(it.mixinClasses)
                } else {
                    mininsNotLoad.addAll(it.mixinClasses)
                }
            }
            ModLogger.info("Mixins to load: $mixinsToLoad")
            if (mininsNotLoad.isNotEmpty()) ModLogger.info("Mixins not load: $mininsNotLoad")
            return mixinsToLoad
        }
    }

    private class MixinBuilder {
        val classes = mutableListOf<String>()
        var targetModId: String? = null
        var condition: (() -> Boolean)? = null

        /**
         * Add mixin classes to the builder
         *
         * Note that the class name is under the package `rhynia.nyx.mixins`, the prefix is NOT needed
         */
        fun ofClasses(vararg mixinClasses: String): MixinBuilder =
            apply {
                classes.addAll(mixinClasses)
            }

        /** Convert Mods to mod ID */
        fun toMod(targetMod: Mods) = apply { targetModId = targetMod.ID }

        /** Extra condition checked when loading the mixin */
        fun applyIf(ai: () -> Boolean) = apply { condition = ai }
    }
}
