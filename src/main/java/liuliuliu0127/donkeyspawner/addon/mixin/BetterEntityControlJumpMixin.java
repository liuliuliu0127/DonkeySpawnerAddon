package liuliuliu0127.donkeyspawner.addon.mixin;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.systems.modules.Modules;
import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayer.class, priority = 1001)
public abstract class BetterEntityControlJumpMixin extends AbstractClientPlayer {
    @Shadow
    public ClientInput input;

    public BetterEntityControlJumpMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    // 修改跳跃力量（maxJump）
    @ModifyReturnValue(method = "getJumpRidingScale", at = @At("RETURN"))
    private float modifyJumpRidingScale(float original) {
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec != null && bec.isActive() && bec.maxJump()) {
            return 1.0F; // 最大跳跃力量
        }
        return original;
    }

    // 取消跳跃（cancelJump）//似乎没生效
    @Inject(method = "sendRidingJump", at = @At("HEAD"), cancellable = true)
    private void onSendRidingJump(CallbackInfo ci) {
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec != null && bec.isActive() && bec.cancelJump()) {
            ci.cancel(); // 阻止发送跳跃包
        }
    }
}