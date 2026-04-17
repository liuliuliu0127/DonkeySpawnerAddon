package liuliuliu0127.donkeyspawner.addon.utils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedList;
import java.util.Queue;

public class LagBackDetectUtil {
    private static final double MIN_LAGBACK_DISTANCE = 1.0;      // 最小拉回距离（格）
    private static final double MAX_SPEED_TOLERANCE = 2.0;       // 速度容忍系数
    private static final long COOLDOWN_MS = 500;                // 触发冷却（毫秒）
    private static final int SUSPICIOUS_COUNT_THRESHOLD = 3;    // 短时间内多次触发视为异常

    private boolean enabled = false;
    private LagbackCallback callback;
    private long lastTriggerTime = 0;
    private final Queue<Long> recentTriggers = new LinkedList<>();

    public interface LagbackCallback {
        void onLagbackDetected(LagbackInfo info);
    }

    public record LagbackInfo(Vec3 fromPos, Vec3 toPos, double distance, boolean suspicious, long timestamp) {}

    public LagBackDetectUtil() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            recentTriggers.clear();
        }
    }

    public void setCallback(LagbackCallback callback) {
        this.callback = callback;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!enabled || MeteorClient.mc.player == null) return;
        if (!(event.packet instanceof ClientboundPlayerPositionPacket packet)) return;

        // ✅ 正确获取服务器位置：通过 change().position()
        Vec3 serverPos = packet.change().position();
        Vec3 currentPos = MeteorClient.mc.player.position();
        Vec3 currentVel = MeteorClient.mc.player.getDeltaMovement();

        double distance = serverPos.distanceTo(currentPos);
        if (distance < MIN_LAGBACK_DISTANCE) return;

        double speed = Math.sqrt(currentVel.x * currentVel.x + currentVel.z * currentVel.z);
        double expectedMax = Math.max(1.2, speed * MAX_SPEED_TOLERANCE + 0.5);

        boolean isLagback = distance > expectedMax;

        if (!isLagback && speed > 0.1) {
            Vec3 moveDir = currentVel.normalize();
            Vec3 serverDir = serverPos.subtract(currentPos).normalize();
            double dot = moveDir.dot(serverDir);
            isLagback = dot < -0.5 && distance > 0.8;
        }

        if (!isLagback) return;

        long now = System.currentTimeMillis();
        if (now - lastTriggerTime < COOLDOWN_MS) return;
        lastTriggerTime = now;

        recentTriggers.offer(now);
        if (recentTriggers.size() > SUSPICIOUS_COUNT_THRESHOLD) {
            recentTriggers.poll();
        }

        boolean suspicious = false;
        if (recentTriggers.size() >= SUSPICIOUS_COUNT_THRESHOLD) {
            long oldest = recentTriggers.peek();
            suspicious = (now - oldest) < 2000;
        }

        if (callback != null) {
            callback.onLagbackDetected(new LagbackInfo(currentPos, serverPos, distance, suspicious, now));
        }
    }
}
