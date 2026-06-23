package liuliuliu0127.donkeyspawner.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pig.class)
public abstract class PigMixin {
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void overrideGetControllingPassenger(CallbackInfoReturnable<LivingEntity> cir) {
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec != null && bec.isActive() && bec.spoofSaddle()) {
            Pig pig = (Pig) (Object) this;
            // 检查是否有乘客，且乘客是玩家
            if (pig.getFirstPassenger() instanceof Player player) {
                // 直接返回该玩家作为控制器，绕过钓竿检查
                cir.setReturnValue(player);
            }
        }
    }
}