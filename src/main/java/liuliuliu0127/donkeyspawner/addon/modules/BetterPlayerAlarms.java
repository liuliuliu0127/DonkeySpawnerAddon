package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
//import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.ChatFormatting;

import java.util.*;

public class BetterPlayerAlarms extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgJoin = settings.createGroup("Player Joining");
    private final SettingGroup sgLeave = settings.createGroup("Player Leaving");
    private final SettingGroup sgEnterRD = settings.createGroup("Player Entering Render Distance");
    private final SettingGroup sgLeaveRD = settings.createGroup("Player Leaving Render Distance");
    private final SettingGroup sgGamemode = settings.createGroup("Gamemode Change");

    // ==================== General ====================
    private final Setting<Boolean> showGamemodeInChat = sgGeneral.add(new BoolSetting.Builder()
        .name("show-gamemode-in-chat")
        .description("show gamemode of the players in alarm messages in the chat.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> alarmOnJoin = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-join")
        .description("Play alarm when a player joins the server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> alarmOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-leave")
        .description("Play alarm when a player leaves the server.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> alarmOnEnterRD = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-enter-render-distance")
        .description("Play alarm when a player enters your render distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> alarmOnLeaveRD = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-leave-render-distance")
        .description("Play alarm when a player leaves your render distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> alarmOnGamemodeChange = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-gamemode-change")
        .description("Play alarm when a player changes gamemode.")
        .defaultValue(false)
        .build()
    );

    // ==================== Join Settings ====================
    private final Setting<Boolean> useJoinList = sgJoin.add(new BoolSetting.Builder()
        .name("use-names-list")
        .description("Only alarm for specific players when they join.")
        .defaultValue(false)
        .visible(alarmOnJoin::get)
        .build()
    );

    private final Setting<List<String>> joinNames = sgJoin.add(new StringListSetting.Builder()
        .name("names")
        .description("Player names to watch for on join.")
        .defaultValue(List.of("ssy_", "e_2" , "山水圆"))
        .visible(() -> alarmOnJoin.get() && useJoinList.get())
        .build()
    );

    private final Setting<Integer> joinRings = sgJoin.add(new IntSetting.Builder()
        .name("rings")
        .description("How many times the alarm will ring.")
        .defaultValue(5).min(1).sliderRange(1, 10)
        .visible(alarmOnJoin::get)
        .build()
    );

    private final Setting<Integer> joinRingDelay = sgJoin.add(new IntSetting.Builder()
        .name("ring-delay")
        .description("Delay between rings in ticks.")
        .defaultValue(20).min(1).sliderRange(1, 100)
        .visible(alarmOnJoin::get)
        .build()
    );

    private final Setting<Double> joinVolume = sgJoin.add(new DoubleSetting.Builder()
        .name("volume")
        .description("Volume of the alarm.")
        .defaultValue(1.0).sliderRange(0.0, 1.0)
        .visible(alarmOnJoin::get)
        .build()
    );

    private final Setting<Double> joinPitch = sgJoin.add(new DoubleSetting.Builder()
        .name("pitch")
        .description("Pitch of the alarm.")
        .defaultValue(1.0).sliderRange(0.5, 2.0)
        .visible(alarmOnJoin::get)
        .build()
    );

    private final Setting<List<SoundEvent>> joinSound = sgJoin.add(new SoundEventListSetting.Builder()
        .name("sound")
        .description("Sound to play.")
        .defaultValue(SoundEvents.BELL_BLOCK)
        .visible(alarmOnJoin::get)
        .build()
    );

    private final Setting<Boolean> joinChatMessage = sgJoin.add(new BoolSetting.Builder()
        .name("chat-message")
        .description("Show a message in chat when a player joins.")
        .defaultValue(true)
        .visible(alarmOnJoin::get)
        .build()
    );

    private final Setting<String> joinChatText = sgJoin.add(new StringSetting.Builder()
        .name("chat-text")
        .description("Chat message when a player joins. Use {name} and {gamemode} as placeholders.")
        .defaultValue("{name} ({gamemode}) joined the server!")
        .visible(() -> alarmOnJoin.get() && joinChatMessage.get())
        .build()
    );

    // ==================== Leave Settings ====================
    private final Setting<Boolean> useLeaveList = sgLeave.add(new BoolSetting.Builder()
        .name("use-names-list")
        .description("Only alarm for specific players when they leave.")
        .defaultValue(false)
        .visible(alarmOnLeave::get)
        .build()
    );

    private final Setting<List<String>> leaveNames = sgLeave.add(new StringListSetting.Builder()
        .name("names")
        .description("Player names to watch for on leave.")
        .defaultValue(List.of("ssy_", "e_2" , "山水圆"))
        .visible(() -> alarmOnLeave.get() && useLeaveList.get())
        .build()
    );

    private final Setting<Integer> leaveRings = sgLeave.add(new IntSetting.Builder()
        .name("rings")
        .defaultValue(3).min(1).sliderRange(1, 10)
        .visible(alarmOnLeave::get)
        .build()
    );

    private final Setting<Integer> leaveRingDelay = sgLeave.add(new IntSetting.Builder()
        .name("ring-delay")
        .defaultValue(20).min(1).sliderRange(1, 100)
        .visible(alarmOnLeave::get)
        .build()
    );

    private final Setting<Double> leaveVolume = sgLeave.add(new DoubleSetting.Builder()
        .name("volume").defaultValue(1.0).sliderRange(0.0, 1.0)
        .visible(alarmOnLeave::get)
        .build()
    );

    private final Setting<Double> leavePitch = sgLeave.add(new DoubleSetting.Builder()
        .name("pitch").defaultValue(1.0).sliderRange(0.5, 2.0)
        .visible(alarmOnLeave::get)
        .build()
    );

    private final Setting<List<SoundEvent>> leaveSound = sgLeave.add(new SoundEventListSetting.Builder()
        .name("sound").defaultValue(SoundEvents.ANVIL_LAND)
        .visible(alarmOnLeave::get)
        .build()
    );

    private final Setting<Boolean> leaveChatMessage = sgLeave.add(new BoolSetting.Builder()
        .name("chat-message").defaultValue(true)
        .visible(alarmOnLeave::get)
        .build()
    );

    private final Setting<String> leaveChatText = sgLeave.add(new StringSetting.Builder()
        .name("chat-text").defaultValue("{name} left the server!")
        .visible(() -> alarmOnLeave.get() && leaveChatMessage.get())
        .build()
    );

    // ==================== Enter RD Settings ====================
    private final Setting<Boolean> useEnterRDList = sgEnterRD.add(new BoolSetting.Builder()
        .name("use-names-list").defaultValue(false)
        .visible(alarmOnEnterRD::get)
        .build()
    );

    private final Setting<List<String>> enterRDNames = sgEnterRD.add(new StringListSetting.Builder()
        .name("names").defaultValue(List.of("ssy_", "e_2" , "山水圆"))
        .visible(() -> alarmOnEnterRD.get() && useEnterRDList.get())
        .build()
    );

    private final Setting<Integer> enterRDRings = sgEnterRD.add(new IntSetting.Builder()
        .name("rings").defaultValue(2).min(1).sliderRange(1, 10)
        .visible(alarmOnEnterRD::get)
        .build()
    );

    private final Setting<Integer> enterRDRingDelay = sgEnterRD.add(new IntSetting.Builder()
        .name("ring-delay").defaultValue(20).min(1).sliderRange(1, 100)
        .visible(alarmOnEnterRD::get)
        .build()
    );

    private final Setting<Double> enterRDVolume = sgEnterRD.add(new DoubleSetting.Builder()
        .name("volume").defaultValue(1.0).sliderRange(0.0, 1.0)
        .visible(alarmOnEnterRD::get)
        .build()
    );

    private final Setting<Double> enterRDPitch = sgEnterRD.add(new DoubleSetting.Builder()
        .name("pitch").defaultValue(1.0).sliderRange(0.5, 2.0)
        .visible(alarmOnEnterRD::get)
        .build()
    );

    private final Setting<List<SoundEvent>> enterRDSound = sgEnterRD.add(new SoundEventListSetting.Builder()
        .name("sound").defaultValue(SoundEvents.ANVIL_DESTROY)
        .visible(alarmOnEnterRD::get)
        .build()
    );

    private final Setting<Boolean> enterRDChatMessage = sgEnterRD.add(new BoolSetting.Builder()
        .name("chat-message").defaultValue(true)
        .visible(alarmOnEnterRD::get)
        .build()
    );

    private final Setting<String> enterRDChatText = sgEnterRD.add(new StringSetting.Builder()
        .name("chat-text").defaultValue("{name} ({gamemode}) entered render distance!")
        .visible(() -> alarmOnEnterRD.get() && enterRDChatMessage.get())
        .build()
    );

    // ==================== Leave RD Settings ====================
    private final Setting<Boolean> useLeaveRDList = sgLeaveRD.add(new BoolSetting.Builder()
        .name("use-names-list").defaultValue(false)
        .visible(alarmOnLeaveRD::get)
        .build()
    );

    private final Setting<List<String>> leaveRDNames = sgLeaveRD.add(new StringListSetting.Builder()
        .name("names").defaultValue(List.of("ssy_", "e_2" , "山水圆"))
        .visible(() -> alarmOnLeaveRD.get() && useLeaveRDList.get())
        .build()
    );

    private final Setting<Integer> leaveRDRings = sgLeaveRD.add(new IntSetting.Builder()
        .name("rings").defaultValue(2).min(1).sliderRange(1, 10)
        .visible(alarmOnLeaveRD::get)
        .build()
    );

    private final Setting<Integer> leaveRDRingDelay = sgLeaveRD.add(new IntSetting.Builder()
        .name("ring-delay").defaultValue(20).min(1).sliderRange(1, 100)
        .visible(alarmOnLeaveRD::get)
        .build()
    );

    private final Setting<Double> leaveRDVolume = sgLeaveRD.add(new DoubleSetting.Builder()
        .name("volume").defaultValue(1.0).sliderRange(0.0, 1.0)
        .visible(alarmOnLeaveRD::get)
        .build()
    );

    private final Setting<Double> leaveRDPitch = sgLeaveRD.add(new DoubleSetting.Builder()
        .name("pitch").defaultValue(1.0).sliderRange(0.5, 2.0)
        .visible(alarmOnLeaveRD::get)
        .build()
    );

    private final Setting<List<SoundEvent>> leaveRDSound = sgLeaveRD.add(new SoundEventListSetting.Builder()
        .name("sound").defaultValue(SoundEvents.BELL_BLOCK)
        .visible(alarmOnLeaveRD::get)
        .build()
    );

    private final Setting<Boolean> leaveRDChatMessage = sgLeaveRD.add(new BoolSetting.Builder()
        .name("chat-message").defaultValue(true)
        .visible(alarmOnLeaveRD::get)
        .build()
    );

    private final Setting<String> leaveRDChatText = sgLeaveRD.add(new StringSetting.Builder()
        .name("chat-text").defaultValue("{name} ({gamemode}) left render distance!")
        .visible(() -> alarmOnLeaveRD.get() && leaveRDChatMessage.get())
        .build()
    );

    // ==================== Gamemode Change Settings ====================
    private final Setting<Boolean> useGamemodeList = sgGamemode.add(new BoolSetting.Builder()
        .name("use-names-list").defaultValue(false)
        .visible(alarmOnGamemodeChange::get)
        .build()
    );

    private final Setting<List<String>> gamemodeNames = sgGamemode.add(new StringListSetting.Builder()
        .name("names").defaultValue(List.of("ssy_", "e_2" , "山水圆"))
        .visible(() -> alarmOnGamemodeChange.get() && useGamemodeList.get())
        .build()
    );

    private final Setting<Integer> gamemodeRings = sgGamemode.add(new IntSetting.Builder()
        .name("rings").defaultValue(3).min(1).sliderRange(1, 10)
        .visible(alarmOnGamemodeChange::get)
        .build()
    );

    private final Setting<Integer> gamemodeRingDelay = sgGamemode.add(new IntSetting.Builder()
        .name("ring-delay").defaultValue(20).min(1).sliderRange(1, 100)
        .visible(alarmOnGamemodeChange::get)
        .build()
    );

    private final Setting<Double> gamemodeVolume = sgGamemode.add(new DoubleSetting.Builder()
        .name("volume").defaultValue(1.0).sliderRange(0.0, 1.0)
        .visible(alarmOnGamemodeChange::get)
        .build()
    );

    private final Setting<Double> gamemodePitch = sgGamemode.add(new DoubleSetting.Builder()
        .name("pitch").defaultValue(1.0).sliderRange(0.5, 2.0)
        .visible(alarmOnGamemodeChange::get)
        .build()
    );

    private final Setting<List<SoundEvent>> gamemodeSound = sgGamemode.add(new SoundEventListSetting.Builder()
        .name("sound").defaultValue(SoundEvents.NOTE_BLOCK_PLING.value())
        .visible(alarmOnGamemodeChange::get)
        .build()
    );

    private final Setting<Boolean> gamemodeChatMessage = sgGamemode.add(new BoolSetting.Builder()
        .name("chat-message").defaultValue(true)
        .visible(alarmOnGamemodeChange::get)
        .build()
    );

    private final Setting<String> gamemodeChatText = sgGamemode.add(new StringSetting.Builder()
        .name("chat-text").defaultValue("{name} changed gamemode: {old_gamemode} -> {new_gamemode}")
        .visible(() -> alarmOnGamemodeChange.get() && gamemodeChatMessage.get())
        .build()
    );

    // ==================== State ====================
    private final Set<UUID> playersSpottedRD = new HashSet<>();          // 进入过渲染距离的玩家
    private final Map<UUID, GameType> gamemodeCache = new HashMap<>();   // 游戏模式缓存
    private final Set<UUID> playersInRender = new HashSet<>();           // 当前在渲染距离内的玩家

    // 响铃状态
    private static class RingState { int ticks; int ringsLeft; boolean active; }
    private final RingState joinRing = new RingState();
    private final RingState leaveRing = new RingState();
    private final RingState enterRDRing = new RingState();
    private final RingState leaveRDRing = new RingState();
    private final RingState gamemodeRing = new RingState();

    private final Set<UUID> alarmedJoinPlayers = new HashSet<>();  // 已触发过上线报警的玩家
    private boolean initialJoinCheckDone = false;                  // 是否已完成初始扫描

    public BetterPlayerAlarms() {
        super(DonkeySpawnerAddon.CATEGORY, "BetterPlayerAlarms",
            "Enhanced player alarms: join/leave server, enter/leave render distance, gamemode changes. Works across multiple worlds.");
    }

    @Override
    public void onActivate() {
        playersSpottedRD.clear();
        gamemodeCache.clear();
        playersInRender.clear();
        alarmedJoinPlayers.clear();          // 新增
        initialJoinCheckDone = false;        // 新增
        resetRingState(joinRing);
        resetRingState(leaveRing);
        resetRingState(enterRDRing);
        resetRingState(leaveRDRing);
        resetRingState(gamemodeRing);
    }

    private void resetRingState(RingState rs) { rs.ticks = 0; rs.ringsLeft = 0; rs.active = false; }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null) return;

        // 处理所有响铃
        handleRing(joinRing, () -> playSound(joinVolume.get(), joinPitch.get(), joinSound.get()));
        handleRing(leaveRing, () -> playSound(leaveVolume.get(), leavePitch.get(), leaveSound.get()));
        handleRing(enterRDRing, () -> playSound(enterRDVolume.get(), enterRDPitch.get(), enterRDSound.get()));
        handleRing(leaveRDRing, () -> playSound(leaveRDVolume.get(), leaveRDPitch.get(), leaveRDSound.get()));
        handleRing(gamemodeRing, () -> playSound(gamemodeVolume.get(), gamemodePitch.get(), gamemodeSound.get()));

        // --- 初始主动检测：你上线时检查已在线的目标玩家 ---
        if (alarmOnJoin.get() && !initialJoinCheckDone && mc.player.connection != null) {
            for (var entry : mc.player.connection.getListedOnlinePlayers()) {
                UUID id = entry.getProfile().id();
                String name = entry.getProfile().name();
                if (!alarmedJoinPlayers.contains(id) && shouldAlarm(useJoinList.get(), joinNames.get(), name)) {
                    startRing(joinRing, joinRings.get(), joinRingDelay.get());
                    sendChat(joinChatMessage.get(), joinChatText.get(), name, id, ChatFormatting.RED);
                    alarmedJoinPlayers.add(id);
                }
            }
            initialJoinCheckDone = true;
        }

        // 渲染距离检测
        if (alarmOnEnterRD.get() || alarmOnLeaveRD.get()) {
            Set<UUID> currentInRender = new HashSet<>();
            for (var entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Player && entity != mc.player) {
                    currentInRender.add(entity.getUUID());
                }
            }

            if (alarmOnEnterRD.get()) {
                for (UUID id : currentInRender) {
                    if (!playersInRender.contains(id)) {
                        String name = getPlayerName(id);
                        if (name != null && shouldAlarm(useEnterRDList.get(), enterRDNames.get(), name)) {
                            startRing(enterRDRing, enterRDRings.get(), enterRDRingDelay.get());
                            sendChat(enterRDChatMessage.get(), enterRDChatText.get(), name, id, ChatFormatting.DARK_RED);
                        }
                    }
                }
            }

            if (alarmOnLeaveRD.get()) {
                for (UUID id : playersInRender) {
                    if (!currentInRender.contains(id)) {
                        String name = getPlayerName(id);
                        if (name != null && shouldAlarm(useLeaveRDList.get(), leaveRDNames.get(), name)) {
                            startRing(leaveRDRing, leaveRDRings.get(), leaveRDRingDelay.get());
                            sendChat(leaveRDChatMessage.get(), leaveRDChatText.get(), name, id, ChatFormatting.DARK_GREEN);
                        }
                    }
                }
            }

            playersInRender.clear();
            playersInRender.addAll(currentInRender);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.level == null || mc.player == null) return;

        // --- Player Join ---
        if (alarmOnJoin.get() && event.packet instanceof ClientboundPlayerInfoUpdatePacket packet) {
            if (packet.actions().contains(Action.ADD_PLAYER)) {
                for (Entry entry : packet.entries()) {
                    String name = entry.profile().name();
                    // 更新游戏模式缓存
                    gamemodeCache.put(entry.profileId(), entry.gameMode());
                    if (!alarmedJoinPlayers.contains(entry.profileId()) && shouldAlarm(useJoinList.get(), joinNames.get(), name)) {
                        startRing(joinRing, joinRings.get(), joinRingDelay.get());
                        sendChat(joinChatMessage.get(), joinChatText.get(), name, entry.profileId(), ChatFormatting.RED);
                        alarmedJoinPlayers.add(entry.profileId());
                    }
                }
            }
        }

        // --- Player Leave ---
        if (alarmOnLeave.get() && event.packet instanceof ClientboundPlayerInfoRemovePacket packet) {
            for (UUID id : packet.profileIds()) {
                String name = getPlayerName(id);
                if (name == null) name = id.toString();
                if (shouldAlarm(useLeaveList.get(), leaveNames.get(), name)) {
                    startRing(leaveRing, leaveRings.get(), leaveRingDelay.get());
                    sendChat(leaveChatMessage.get(), leaveChatText.get(), name , id ,ChatFormatting.GREEN);
                }
                gamemodeCache.remove(id);
                playersInRender.remove(id);
                alarmedJoinPlayers.remove(id);
            }
        }

        // --- Gamemode Change ---
        if (alarmOnGamemodeChange.get() && event.packet instanceof ClientboundPlayerInfoUpdatePacket packet) {
            if (packet.actions().contains(Action.UPDATE_GAME_MODE)) {
                for (Entry entry : packet.entries()) {
                    UUID id = entry.profileId();
                    GameType newMode = entry.gameMode();
                    GameType oldMode = gamemodeCache.getOrDefault(id, newMode);
                    if (oldMode != newMode) {
                        gamemodeCache.put(id, newMode);
                        String name = getPlayerName(id);
                        if (name != null && shouldAlarm(useGamemodeList.get(), gamemodeNames.get(), name)) {
                            startRing(gamemodeRing, gamemodeRings.get(), gamemodeRingDelay.get());
                            if (gamemodeChatMessage.get()) {
                                String msg = gamemodeChatText.get()
                                    .replace("{name}", name)
                                    .replace("{old_gamemode}", oldMode.getName())
                                    .replace("{new_gamemode}", newMode.getName());
                                ChatUtils.sendMsg(Component.literal(msg).withStyle(ChatFormatting.YELLOW));
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== Helpers ====================

    private boolean shouldAlarm(boolean useList, List<String> names, String playerName) {
        if (!useList) return true;
        return names.stream().anyMatch(n -> n.equalsIgnoreCase(playerName));
    }

    private void startRing(RingState rs, int rings, int delay) {
        rs.ringsLeft = rings;
        rs.ticks = 0;
        rs.active = true;
    }

    private void handleRing(RingState rs, Runnable play) {
        if (!rs.active || rs.ringsLeft <= 0) { rs.active = false; return; }
        if (rs.ticks <= 0) {
            play.run();
            rs.ticks = (rs.ringsLeft == 1) ? 0 : switchRingDelay(rs); // 最后一次不延迟
            rs.ringsLeft--;
            if (rs.ringsLeft <= 0) rs.active = false;
        } else {
            rs.ticks--;
        }
    }

    private int switchRingDelay(RingState rs) {
        if (rs == joinRing) return joinRingDelay.get();
        if (rs == leaveRing) return leaveRingDelay.get();
        if (rs == enterRDRing) return enterRDRingDelay.get();
        if (rs == leaveRDRing) return leaveRDRingDelay.get();
        if (rs == gamemodeRing) return gamemodeRingDelay.get();
        return 20;
    }

    private void sendChat(boolean enabled, String template, String name, UUID playerId, ChatFormatting color) {
        if (!enabled) return;
        String msg = template.replace("{name}", name);
        if (showGamemodeInChat.get() && playerId != null) {
            msg = msg.replace("{gamemode}", getGamemodeName(playerId));
        } else {
            // 如果模板中有 {gamemode} 但不显示，则移除占位符，避免残留
            msg = msg.replace("{gamemode}", "");
        }
        ChatUtils.sendMsg(Component.literal(msg).withStyle(color));
    }
    // 保留无 UUID 的兼容方法
    private void sendChat(boolean enabled, String template, String name) {
        sendChat(enabled, template, name, null, ChatFormatting.YELLOW);
    }
    // 保留带颜色的兼容方法（无 UUID）
    private void sendChat(boolean enabled, String template, String name, ChatFormatting color) {
        sendChat(enabled, template, name, null, color);
    }

    private void playSound(double vol, double pitch, List<SoundEvent> sounds) {
        if (mc.player == null || mc.level == null || sounds.isEmpty()) return;
        SoundEvent sound = sounds.get(0);
        mc.level.playLocalSound(mc.player.blockPosition(), sound, SoundSource.PLAYERS, (float) vol, (float) pitch, false);
    }

    private String getPlayerName(UUID id) {
        if (mc.level == null) return null;
        Player player = mc.level.getPlayerByUUID(id);
        if (player != null) return player.getGameProfile().name();
        // fallback: try to get from network player list
        if (mc.player != null && mc.player.connection != null) {
            var entry = mc.player.connection.getPlayerInfo(id);
            if (entry != null && entry.getProfile() != null) return entry.getProfile().name();
        }
        return null;
    }

    private String getGamemodeName(UUID id) {
        GameType mode = gamemodeCache.get(id);
        if (mode != null) return mode.getName();
        // 如果缓存中没有，尝试从网络信息获取
        if (mc.player != null && mc.player.connection != null) {
            var info = mc.player.connection.getPlayerInfo(id);
            if (info != null) {
                mode = info.getGameMode();
                if (mode != null) return mode.getName();
            }
        }
        return "Unknown";
    }
}