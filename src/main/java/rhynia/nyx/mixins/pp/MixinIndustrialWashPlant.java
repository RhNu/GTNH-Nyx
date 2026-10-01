package rhynia.nyx.mixins.pp;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import gregtech.api.logic.ProcessingLogic;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.MTEIndustrialWashPlantLegacy;

@Mixin(value = MTEIndustrialWashPlantLegacy.class, priority = 2000, remap = false)
public class MixinIndustrialWashPlant {

    @ModifyReturnValue(method = "createProcessingLogic", at = @At("RETURN"))
    private ProcessingLogic injectProcessingLogic(ProcessingLogic o) {
        return o.setSpeedBonus(1F / 10F)
            .setEuModifier(1.0F / 2.0F);
    }

    @ModifyReturnValue(method = "getMaxParallelRecipes", at = @At("RETURN"))
    private int getMaxParallelRecipes(int o) {
        return o * 4;
    }
}
