package liuliuliu0127.donkeyspawner.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

// 必须声明为 abstract，与官方完全一致
@Mixin(value = Mob.class, priority = 1001)
public abstract class BetterEntityControlSpoofSaddleMixin {

    @ModifyReturnValue(method = "isSaddled", at = @At("RETURN"))  // 用 isSaddled！
    private boolean overrideSaddleCheck(boolean original) {
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec != null && bec.isActive() && bec.spoofSaddle()) {
            return true;
        }
        return original;
    }
}