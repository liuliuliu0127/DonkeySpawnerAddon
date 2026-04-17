package liuliuliu0127.donkeyspawner.addon.utils;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;

public class ElytraControl {
    private static final Minecraft mc = Minecraft.getInstance();

    // 真实玩家引用（用于 Freecam 场景）
    private static LocalPlayer realPlayer = null;

    // 模拟按键状态
    private static boolean forwardHeld = false;
    private static boolean backHeld = false;
    private static boolean leftHeld = false;
    private static boolean rightHeld = false;
    private static boolean jumpHeld = false;
    private static boolean shiftHeld = false;

    // KeyMapping 缓存
    private static KeyMapping keyForward;
    private static KeyMapping keyBack;
    private static KeyMapping keyLeft;
    private static KeyMapping keyRight;
    private static KeyMapping keyJump;
    private static KeyMapping keyShift;

    // ---------- 真实玩家管理 ----------
    public static void setRealPlayer(LocalPlayer player) {
        realPlayer = player;
    }

    public static void clearRealPlayer() {
        realPlayer = null;
        stopAll();
    }

    // ---------- 按键模拟接口 ----------
    public static void setForward(boolean pressed) {
        forwardHeld = pressed;
        applyKeys();
        sendInputPacket();
    }

    public static void setBack(boolean pressed) {
        backHeld = pressed;
        applyKeys();
        sendInputPacket();
    }

    public static void setLeft(boolean pressed) {
        leftHeld = pressed;
        applyKeys();
        sendInputPacket();
    }

    public static void setRight(boolean pressed) {
        rightHeld = pressed;
        applyKeys();
        sendInputPacket();
    }

    public static void setJump(boolean pressed) {
        jumpHeld = pressed;
        applyKeys();
        sendInputPacket();
    }

    public static void setShift(boolean pressed) {
        shiftHeld = pressed;
        applyKeys();
        sendInputPacket();
    }

    public static void stopAll() {
        forwardHeld = backHeld = leftHeld = rightHeld = jumpHeld = shiftHeld = false;
        applyKeys();
        sendInputPacket();
    }

    // ---------- 内部实现 ----------
    private static void initKeys() {
        if (keyForward == null) {
            keyForward = mc.options.keyUp;
            keyBack    = mc.options.keyDown;
            keyLeft    = mc.options.keyLeft;
            keyRight   = mc.options.keyRight;
            keyJump    = mc.options.keyJump;
            keyShift   = mc.options.keyShift;
        }
    }

    private static void applyKeys() {
        initKeys();
        keyForward.setDown(forwardHeld);
        keyBack.setDown(backHeld);
        keyLeft.setDown(leftHeld);
        keyRight.setDown(rightHeld);
        keyJump.setDown(jumpHeld);
        keyShift.setDown(shiftHeld);
    }

    private static void sendInputPacket() {
        LocalPlayer target = (realPlayer != null) ? realPlayer : mc.player;
        if (target == null || target.connection == null) return;

        Input packetInput = new Input(
            forwardHeld,
            backHeld,
            leftHeld,
            rightHeld,
            jumpHeld,
            shiftHeld,
            target.input.keyPresses.sprint()
        );

        target.connection.send(new ServerboundPlayerInputPacket(packetInput));
    }
}
