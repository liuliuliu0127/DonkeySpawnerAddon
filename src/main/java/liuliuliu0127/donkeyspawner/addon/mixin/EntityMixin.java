package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.SpearTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getYRot", at = @At("RETURN"), cancellable = true)
    private void overrideYaw(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide()) return;
        if (self instanceof ServerPlayer && SpearTarget.shouldOverrideServerRotation) {
            cir.setReturnValue(SpearTarget.serverTargetYaw);
        }
    }

    @Inject(method = "getXRot", at = @At("RETURN"), cancellable = true)
    private void overridePitch(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide()) return;
        if (self instanceof ServerPlayer && SpearTarget.shouldOverrideServerRotation) {
            cir.setReturnValue(SpearTarget.serverTargetPitch);
        }
    }
}