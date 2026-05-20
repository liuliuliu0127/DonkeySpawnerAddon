package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;

public class DonkeySpawnerTMI extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showAllRecipes = sgGeneral.add(new BoolSetting.Builder()
        .name("unlock-all-recipes")
        .description("unlock all recipes in the recipe book, even those that haven't been unlocked in survival.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> openWorkbench = sgGeneral.add(new BoolSetting.Builder()
        .name("open-workbench")
        .description("Open the workbench screen when clicking on a workbench recipe in the inventory.")
        .defaultValue(true)
        .visible(showAllRecipes::get)
        .build()
    );

    public DonkeySpawnerTMI() {
        super(DonkeySpawnerAddon.CATEGORY, "DonkeySpawnerTMI",
            "unlock all recipes in the recipe book, even those that haven't been unlocked in survival.");
    }

    public boolean shouldShowAllRecipes() {
        return isActive() && showAllRecipes.get();
    }

    public boolean shouldOpenWorkbench() {
        return isActive() && openWorkbench.get();
    }

    /** 供 Mixin 调用，打开原版工作台界面 */
    public static void openCraftingScreen() {
        Minecraft mc = Minecraft.getInstance();
        DonkeySpawnerTMI module = Modules.get().get(DonkeySpawnerTMI.class);
        if (module == null || !module.shouldOpenWorkbench() || mc.player == null) return;

        mc.setScreen(new CraftingScreen(
            new CraftingMenu(0, mc.player.getInventory()),
            mc.player.getInventory(),
            Component.translatable("container.crafting")
        ));
    }
}