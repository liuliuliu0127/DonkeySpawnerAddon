package liuliuliu0127.donkeyspawner.addon;

import liuliuliu0127.donkeyspawner.addon.commands.CommandExample;
import liuliuliu0127.donkeyspawner.addon.hud.HudExample;
import liuliuliu0127.donkeyspawner.addon.modules.ModuleExample;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class DonkeySpawnerAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("DonkeySpawner");
    public static final HudGroup HUD_GROUP = new HudGroup("DonkeySpawner HUD");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor DonkeySpawner Addon");

        // Modules
        Modules.get().add(new ModuleExample());

        // Commands
        Commands.add(new CommandExample());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "liuliuliu0127.donkeyspawner.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-donkeyspawner-addon");
    }
}
