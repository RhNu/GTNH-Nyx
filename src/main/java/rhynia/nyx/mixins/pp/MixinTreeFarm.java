package rhynia.nyx.mixins.pp;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTETreeFarmLegacy;

@Mixin(value = MTETreeFarmLegacy.class, priority = 2000, remap = false)
public class MixinTreeFarm {

    @ModifyReturnValue(method = "getTierMultiplier", at = @At("RETURN"))
    private static int getTierMultiplier(int original) {
        return original * 32;
    }
}
