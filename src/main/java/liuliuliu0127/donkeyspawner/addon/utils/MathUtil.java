package liuliuliu0127.donkeyspawner.addon.utils;
import net.minecraft.client.Minecraft;
public class MathUtil {
    static final Minecraft mc = Minecraft.getInstance();

    // ... (其他方法不变)

    public static float getYaw() {
        // 1.21.1: 获取玩家移动输入的正确方式
        float forward = mc.player.input.getMoveVector().y;
        float side = mc.player.input.getMoveVector().x;
        float yaw = mc.player.yRotO + (mc.player.getViewYRot(1.0F) - mc.player.yRotO) * mc.getFrameTimeNs()/1_000_000_000.0F;
        if (forward == 0.0F && side == 0.0F) return yaw;
        if (forward != 0.0F) {
            if (side >= 1.0F) {
                yaw += ((forward > 0.0F) ? -45 : -135);
                side = 0.0F;
            } else if (side <= -1.0F) {
                yaw += ((forward > 0.0F) ? 45 : 135);
                side = 0.0F;
            }
        } else {
            yaw += side * -90.0F;
        }
        return yaw;
    }

    public static double[] transformStrafe(double speed, boolean autoMove, float yaw) {
        // 1.21.1: 获取玩家移动输入的正确方式
        float forward = mc.player.input.getMoveVector().y;
        float side = mc.player.input.getMoveVector().x;
        //System.out.printf("[MathUtil DEBUG] forward=%.2f, side=%.2f\n", forward, side);
        if (!autoMove) {
            yaw = mc.player.yRotO + (mc.player.getViewYRot(1.0F) - mc.player.yRotO) * mc.getFrameTimeNs()/1_000_000_000.0F;
        } else {
            return new double[]{
                Math.sin(Math.toRadians(yaw)) * speed,
                speed * Math.cos(Math.toRadians(yaw))
            };
        }
        if (forward == 0.0F && side == 0.0F) return new double[]{0.0, 0.0};
        if (forward != 0.0F) {
            if (side >= 1.0F) {
                yaw += ((forward > 0.0F) ? -45 : 45);
                side = 0.0F;
            } else if (side <= -1.0F) {
                yaw += ((forward > 0.0F) ? 45 : -45);
                side = 0.0F;
            }
            if (forward > 0.0F) {
                forward = 1.0F;
            } else if (forward < 0.0F) {
                forward = -1.0F;
            }
        }
        double mx = Math.cos(Math.toRadians((yaw + 90.0F)));
        double mz = Math.sin(Math.toRadians((yaw + 90.0F)));
        double velX = forward * speed * mx + side * speed * mz;
        double velZ = forward * speed * mz - side * speed * mx;
        return new double[]{velX, velZ};
    }
}
