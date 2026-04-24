// LivingEntityRendererMixin.java
package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(T entity, S renderState, float tickDelta, CallbackInfo ci) {
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec != null && bec.shouldScaleMount() && entity == bec.getMountedEntity()) {
            if (entity == bec.getMountedEntity()) {
                renderState.scale = bec.getMountScale(); // 直接覆盖缩放值
            }
        }
    }
}