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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DeathScreen;

public class DeathFreecam extends Module {
    public static boolean freecamActive = false;   // 静态字段，供 Mixin 使用
    private boolean savedToggleOnDeath = false;

    public DeathFreecam() {
        super(DonkeySpawnerAddon.CATEGORY, "DeathFreecam",
            "press E on death to toggle freecam, press again to exit");
    }

    @Override
    public void onActivate() {
        freecamActive = false;
        savedToggleOnDeath = false;
    }

    @Override
    public void onDeactivate() {
        if (freecamActive) exitFreecam();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (!isActive()) return;
        if (event.action != KeyAction.Press) return;
        if (event.key() != 69) return; // E 键

        if (mc.screen instanceof DeathScreen || freecamActive) {
            toggleFreecam();
            event.cancel();
        }
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (freecamActive && event.screen instanceof DeathScreen) {
            event.cancel();
        }
    }

    private void toggleFreecam() {
        if (mc.player == null || !mc.player.isDeadOrDying()) return;
        if (freecamActive) exitFreecam();
        else enterFreecam();
    }

    private void enterFreecam() {
        Freecam freecam = Modules.get().get(Freecam.class);
        if (freecam == null || freecam.isActive()) return;

        var setting = freecam.settings.get("toggle-on-death");
        if (setting instanceof BoolSetting bs) {
            savedToggleOnDeath = bs.get();
            bs.set(false);
        }

        freecam.toggle();
        freecamActive = true;
    }

    private void exitFreecam() {
        Freecam freecam = Modules.get().get(Freecam.class);
        if (freecam == null) return;

        if (freecam.isActive()) freecam.toggle();

        var setting = freecam.settings.get("toggle-on-death");
        if (setting instanceof BoolSetting bs) {
            bs.set(savedToggleOnDeath);
        }

        freecamActive = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!freecamActive) return;
        if (mc.player != null && !mc.player.isDeadOrDying()) {
            exitFreecam();
        }
    }
}