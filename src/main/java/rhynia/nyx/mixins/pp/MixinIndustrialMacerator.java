package rhynia.nyx.mixins.pp;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import gregtech.api.logic.ProcessingLogic;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.MTEIndustrialMaceratorLegacy;

@SuppressWarnings("SpellCheckingInspection")
@Mixin(value = MTEIndustrialMaceratorLegacy.class, priority = 2000, remap = false)
public class MixinIndustrialMacerator {

    @ModifyReturnValue(method = "createProcessingLogic", at = @At("RETURN"))
    private ProcessingLogic injectProcessingLogic(ProcessingLogic o) {
        return o.setSpeedBonus(1.0F / 16.0F)
            .setEuModifier(1.0F / 20.0F);
    }

    @ModifyReturnValue(method = "getMaxParallelRecipes", at = @At("RETURN"))
    private int parallel(int o) {
        return o * 128;
    }
}
