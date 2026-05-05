package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.MeteorTextFix;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VanillaTextRenderer.class, remap = false)
public class VanillaTextRendererMixin {

    @Shadow
    private boolean scaleIndividually;   // 建议设为 private 更安全

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(String text, double x, double y, Color color, boolean shadow,
                          CallbackInfoReturnable<Double> cir) {
        MeteorTextFix module = Modules.get().get(MeteorTextFix.class);
        if (module != null && module.isActive() && module.forceScaleIndividually.get()) {
            this.scaleIndividually = true;
        }
        // 否则不修改 scaleIndividually，保留原值
    }
}