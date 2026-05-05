package liuliuliu0127.donkeyspawner.addon.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;

public class MeteorTextFix extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 对应 TextRendererMixin 的功能：强制使用 VanillaTextRenderer
    public final Setting<Boolean> forceVanilla = sgGeneral.add(new BoolSetting.Builder()
        .name("force-vanilla")
        .description("Force using Vanilla text renderer instead of custom one.")
        .defaultValue(true)
        .build()
    );

    // 对应 VanillaTextRendererMixin 的功能：强制独立缩放
    public final Setting<Boolean> forceScaleIndividually = sgGeneral.add(new BoolSetting.Builder()
        .name("force-scale-individually")
        .description("Force scale each character individually when using Vanilla renderer.")
        .defaultValue(true)
        .build()
    );

    public MeteorTextFix() {
        super(
            DonkeySpawnerAddon.CATEGORY,  // 你的 addon 定义的 Category
            "MeteorTextFix",
            "(Restart the game to activate)Overrides Meteor's text rendering behavior to fix compatibility issues."
        );
    }
}
