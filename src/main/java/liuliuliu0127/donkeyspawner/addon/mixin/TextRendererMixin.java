package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.MeteorTextFix;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TextRenderer.class, remap = false)
public interface TextRendererMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void onGet(CallbackInfoReturnable<TextRenderer> cir) {
        MeteorTextFix module = Modules.get().get(MeteorTextFix.class);
        if (module != null && module.isActive() && module.forceVanilla.get()) {
            cir.setReturnValue(VanillaTextRenderer.INSTANCE);
        }
        // 条件不满足时不干预，原方法正常执行
    }
}