package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class PacketEat extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> deSync = sgGeneral.add(new BoolSetting.Builder()
        .name("de-sync")
        .description("Continuously sends interaction packets to desync the eating animation.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> noRelease = sgGeneral.add(new BoolSetting.Builder()
        .name("no-release")
        .description("Cancels the release item packet so the server thinks you are still eating.")
        .defaultValue(true)
        .build()
    );

    public PacketEat() {
        super(DonkeySpawnerAddon.CATEGORY, "Packet Eat", "allows you to eat without interruption by sending packets to the server.");
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (mc.player == null) return;

        if (deSync.get() && mc.player.isUsingItem()) {
            ItemStack activeStack = mc.player.getUseItem();

            if (activeStack.has(DataComponents.FOOD) || activeStack.has(DataComponents.POTION_CONTENTS)) {
                InteractionHand hand = mc.player.getUsedItemHand();
                mc.player.connection.send(
                    new ServerboundUseItemPacket(hand, 0, mc.player.getYRot(), mc.player.getXRot())
                );
            }
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;

        if (noRelease.get() && event.packet instanceof ServerboundPlayerActionPacket packet) {
            if (packet.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
                ItemStack activeStack = mc.player.getUseItem();
                if (activeStack.has(DataComponents.FOOD) || activeStack.has(DataComponents.POTION_CONTENTS)) {
                    event.cancel(); // 阻止释放通知发送到服务器
                }
            }
        }
    }
}