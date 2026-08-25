package rhx.lazy.integration.tacz.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rhx.lazy.integration.tacz.TaczInfiniteAmmoState;

@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityAmmoCheck")
public abstract class TaczInfiniteAmmoMixin {
    @Shadow
    @Final
    private LivingEntity shooter;

    @Inject(method = "needCheckAmmo", at = @At("HEAD"), cancellable = true)
    private void lazy$allowInfiniteAmmo(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (TaczInfiniteAmmoState.isEnabled(this.shooter)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
