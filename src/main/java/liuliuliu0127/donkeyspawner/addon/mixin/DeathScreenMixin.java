package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.DeathFreecam;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    /**
     * freecam 激活时，完全跳过死亡界面的渲染。
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (DeathFreecam.freecamActive) {
            ci.cancel();
        }
    }

    /**
     * freecam 激活时，禁用鼠标点击（防止误触按钮）。
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(CallbackInfoReturnable<Boolean> cir) {
        if (DeathFreecam.freecamActive) {
            cir.setReturnValue(false);
        }
    }
}