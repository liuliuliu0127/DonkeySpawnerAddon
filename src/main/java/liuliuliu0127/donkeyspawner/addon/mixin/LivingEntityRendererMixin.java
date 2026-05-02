// LivingEntityRendererMixin.java
package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl;
import liuliuliu0127.donkeyspawner.addon.modules.SpearTarget;
//import meteordevelopment.meteorclient.mixin.EntityAccessor;
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
    // ========== 发光：渲染状态提取前打开标志 ==========
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractGlowEnable(T entity, S renderState, float tickDelta, CallbackInfo ci) {
        if (SpearTarget.glowEnabled && entity == SpearTarget.glowTarget) {
            ((EntityAccessor) entity).invokeSetSharedFlag(6, true);
        }
    }

    // ========== 发光：渲染状态提取后关闭标志，并处理原有缩放 ==========
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractGlowDisableAndScale(T entity, S renderState, float tickDelta, CallbackInfo ci) {
        // 关闭发光标志（必须最先执行，因为缩放可能会改变渲染状态，但不影响标志）
        if (SpearTarget.glowEnabled && entity == SpearTarget.glowTarget) {
            ((EntityAccessor) entity).invokeSetSharedFlag(6, false);
        }

        // 原有的缩放逻辑
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec != null && bec.shouldScaleMount() && entity == bec.getMountedEntity()) {
            renderState.scale = bec.getMountScale();
        }
    }
}