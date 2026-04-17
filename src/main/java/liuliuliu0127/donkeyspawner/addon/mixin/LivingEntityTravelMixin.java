package liuliuliu0127.donkeyspawner.addon.mixin;
import liuliuliu0127.donkeyspawner.addon.events.TravelEvent;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3 movement, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 只在客户端世界生效，避免服务端 NPE
        if (!self.level().isClientSide()) return;

        // 确保只处理客户端玩家自身
        if (Minecraft.getInstance().player != self) return;

        TravelEvent event = TravelEvent.get(movement);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancel) {
            ci.cancel();
        }
    }
}
