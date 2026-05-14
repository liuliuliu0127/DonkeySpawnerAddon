package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.BetterVelocity;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = Velocity.class, remap = false)
public class VelocityMixin {

    @Inject(method = "onPacketReceive", at = @At("HEAD"), cancellable = true)
    private void onPacketReceiveHead(PacketEvent.Receive event, CallbackInfo ci) {
        BetterVelocity bv = Modules.get().get(BetterVelocity.class);
        if (bv == null || !bv.isActive()) return;

        // 自身风爆：爆炸包处理（风弹）
        if (event.packet instanceof ClientboundExplodePacket packet) {
            if (bv.selfWindBurst.get() && bv.windBurstTimer > 0) {
                double horiz = bv.selfWindBurstHorizontal.get();
                double vert  = bv.selfWindBurstVertical.get();
                applyExplosionKnockback(packet, horiz, vert);
                ci.cancel();
            }
            return;
        }

        // 矛突刺：彻底放行
        if (event.packet instanceof ClientboundSetEntityMotionPacket) {
            if (bv.spearLunge.get() && bv.isSpearWithLunge()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.getFoodData().getFoodLevel() > 6 && mc.options.keyAttack.isDown()) {
                    ci.cancel();
                }
            }
        }
    }

    private void applyExplosionKnockback(ClientboundExplodePacket packet, double horizKb, double vertKb) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Optional<Vec3> optionalKnockback = packet.playerKnockback();
        Vec3 currentVel = mc.player.getDeltaMovement();
        Vec3 newVel;

        if (optionalKnockback.isPresent()) {
            Vec3 knockback = optionalKnockback.get();
            newVel = new Vec3(
                currentVel.x + knockback.x * horizKb,
                currentVel.y + knockback.y * vertKb,
                currentVel.z + knockback.z * horizKb
            );
        } else {
            Vec3 expCenter = packet.center();
            Vec3 playerPos = mc.player.position();
            Vec3 dir = playerPos.subtract(expCenter);
            double dist = dir.length();
            double radius = packet.radius();
            if (dist < radius) {
                dir = dir.normalize();
                double strength = 1.0 - dist / radius;
                newVel = new Vec3(
                    currentVel.x + dir.x * horizKb * strength,
                    currentVel.y + dir.y * vertKb * strength,
                    currentVel.z + dir.z * horizKb * strength
                );
            } else {
                return;
            }
        }
        ((IVec3d) (Object) currentVel).meteor$set(newVel);
    }
}