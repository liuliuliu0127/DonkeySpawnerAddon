package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DeathScreen;
import org.lwjgl.glfw.GLFW;

public class DeathFreecam extends Module {
    public static boolean freecamActive = false;

    private DeathScreen savedDeathScreen = null;
    private boolean savedToggleOnDeath = false;
    private boolean savedClickToPath = false;

    private double lastMouseX, lastMouseY;
    private boolean mouseInitialized = false;

    public DeathFreecam() {
        super(DonkeySpawnerAddon.CATEGORY, "DeathFreecam",
            "press E on death to enter freecam instead of death screen");
    }

    @Override
    public void onActivate() {
        freecamActive = false;
        savedDeathScreen = null;
    }

    @Override
    public void onDeactivate() {
        if (freecamActive) exitFreecam();
    }

    // 全局按键：E 键切换
    @EventHandler
    private void onKey(KeyEvent event) {
        if (!isActive()) return;
        if (event.action != KeyAction.Press) return;
        if (event.key() != 69) return; // E

        if (mc.screen instanceof DeathScreen || freecamActive) {
            toggleFreecam();
            event.cancel();
        }
    }

    // 阻止死亡界面在 freecam 期间被自动重开
    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (freecamActive && event.screen instanceof DeathScreen) {
            event.cancel();
        }
    }

    public void toggleFreecam() {
        if (mc.player == null || !mc.player.isDeadOrDying()) return;
        if (freecamActive) exitFreecam();
        else enterFreecam();
    }

    private void enterFreecam() {
        Freecam freecam = Modules.get().get(Freecam.class);
        if (freecam == null || freecam.isActive()) return;

        // 防止 Freecam 因死亡自动关闭
        var setting = freecam.settings.get("toggle-on-death");
        var setting2 = freecam.settings.get("click-to-path");
        if (setting instanceof BoolSetting bs) {
            savedToggleOnDeath = bs.get();
            bs.set(false);
        }
        if (setting2 instanceof BoolSetting bs) {
            savedClickToPath = bs.get();
            bs.set(false);
        }

        // 保存死亡界面，然后清空屏幕 → Freecam 立刻获得鼠标焦点
        if (mc.screen instanceof DeathScreen) {
            savedDeathScreen = (DeathScreen) mc.screen;
        }
        mc.setScreen(null);

        // 强制锁定鼠标并隐藏指针
        long window = mc.getWindow().handle();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

        // 初始化鼠标位置
        double[] xpos = new double[1], ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);
        lastMouseX = xpos[0];
        lastMouseY = ypos[0];
        mouseInitialized = true;

        freecam.toggle();
        freecamActive = true;
    }

    private void exitFreecam() {
        Freecam freecam = Modules.get().get(Freecam.class);
        if (freecam == null) return;

        // 恢复鼠标可见
        long window = mc.getWindow().handle();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        mouseInitialized = false;

        if (freecam.isActive()) freecam.toggle();

        // 恢复 toggleOnDeath
        var setting = freecam.settings.get("toggle-on-death");
        if (setting instanceof BoolSetting bs) {
            bs.set(savedToggleOnDeath);
        }

        // 恢复 clickToPath
        var setting2 = freecam.settings.get("click-to-path");
        if (setting2 instanceof BoolSetting bs) {
            bs.set(savedClickToPath);
        }

        // 恢复死亡界面
        if (savedDeathScreen != null && mc.player != null && mc.player.isDeadOrDying()) {
            mc.setScreen(savedDeathScreen);
            savedDeathScreen = null;
        }

        freecamActive = false;
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        if (!freecamActive || !mouseInitialized) return;
        Freecam freecam = Modules.get().get(Freecam.class);
        if (freecam == null || !freecam.isActive()) return;

        long window = mc.getWindow().handle();
        double[] xpos = new double[1], ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);

        double dx = xpos[0] - lastMouseX;
        double dy = ypos[0] - lastMouseY;
        if (dx != 0 || dy != 0) {
            double sensitivity = mc.options.sensitivity().get();
            freecam.changeLookDirection(dx * sensitivity, dy * sensitivity);
        }
        lastMouseX = xpos[0];
        lastMouseY = ypos[0];
    }

    // 复活自动退出
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!freecamActive) return;
        if (mc.player != null && !mc.player.isDeadOrDying()) {
            exitFreecam();
            if (mc.screen instanceof DeathScreen) mc.setScreen(null);
        }
    }
}