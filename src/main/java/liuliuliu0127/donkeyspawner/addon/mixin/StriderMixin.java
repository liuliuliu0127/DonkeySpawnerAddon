package liuliuliu0127.donkeyspawner.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Strider.class)
public abstract class StriderMixin {
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void overrideGetControllingPassenger(CallbackInfoReturnable<LivingEntity> cir) {
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec != null && bec.isActive() && bec.spoofSaddle()) {
            Strider strider = (Strider) (Object) this;
            if (strider.getFirstPassenger() instanceof Player player) {
                // 直接返回该玩家作为控制器，绕过诡异菌钓竿检查
                cir.setReturnValue(player);
            }
        }
    }
}