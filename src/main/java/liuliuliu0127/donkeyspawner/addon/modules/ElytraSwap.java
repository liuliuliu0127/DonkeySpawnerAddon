package liuliuliu0127.donkeyspawner.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
//import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.ResourceKey;

import net.minecraft.world.entity.EquipmentSlot;
//import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;


import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import liuliuliu0127.donkeyspawner.addon.utils.Timer;

public class ElytraSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDebug = settings.createGroup("InfELytraDebug");

    // --- 核心功能开关 ---
    private final Setting<Boolean> smartElytraSwap = sgGeneral.add(new BoolSetting.Builder()
            .name("Smart Elytra Swap")
            .description("When enabled, automatically swap elytra")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> smartSwapBack = sgGeneral.add(new BoolSetting.Builder()
            .name("Smart Swap Back")
            .description("When enabled, automatically swap back to chestplate by autoarmor(enable meteor autoarmor)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoReplaceElytra = sgGeneral.add(new BoolSetting.Builder()
            .name("Auto Replace Elytra")
            .description("When enabled, automatically replace the elytra when its durability is about to run out.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> replaceDurabilityThreshold = sgGeneral.add(new IntSetting.Builder()
            .name("replaceDurabilityThreshold")
            .description("Replace the elytra when its durability is at or below this value. (Only effective if Auto Replace Elytra is enabled)")
            .defaultValue(2)
            .range(1, 50)
            .sliderRange(1, 20)
            .visible(autoReplaceElytra::get)
            .build()
    );

    private final Setting<Boolean> infiniteDurability = sgGeneral.add(new BoolSetting.Builder()
            .name("InfElytra")
            .description("Periodically reset elytra durability loss(Not work in liquid)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoInfElytra = sgGeneral.add(new BoolSetting.Builder()
            .name("SmartInfElytra")
            .description("Automatically enable InfElytra when flying straight for a while, and disable when turning.")
            .defaultValue(true)
            .visible(infiniteDurability::get)
            .build()
    );

    private final Setting<InfiniteDurabilityMode> infiniteDurabilityMode = sgGeneral.add(new EnumSetting.Builder<InfiniteDurabilityMode>()
            .name("InfElytra Mode")
            .description("Select the method to reset elytra durability.")
            .defaultValue(InfiniteDurabilityMode.DoubleClickSlot)
            .visible(() -> infiniteDurability.get())
            .build()
    );

    private final Setting<Integer> infiniteDurabilityInterval = sgGeneral.add(new IntSetting.Builder()
            .name("Reset Interval (ms)")
            .description("Time between durability resets.")
            .defaultValue(800)
            .range(400, 2000)
            .sliderRange(400, 1500)
            .visible(() -> infiniteDurability.get())
            .build()
    );

    private final Setting<Integer> autoInfDirectionTime = sgGeneral.add(new IntSetting.Builder()
            .name("SmartInf Direction Time (ticks)")
            .description("How long to fly straight before auto-enabling InfElytra.")
            .defaultValue(60)
            .range(20, 200)
            .sliderRange(20, 200)
            .visible(() -> infiniteDurability.get() && autoInfElytra.get())
            .build()
    );

    private final Setting<Double> autoInfDirectionTolerance = sgGeneral.add(new DoubleSetting.Builder()
            .name("SmartInf Direction Tolerance")
            .description("Maximum allowed horizontal movement direction change (degrees) to be considered 'straight'.")
            .defaultValue(5.0)
            .range(1.0, 30.0)
            .sliderRange(1.0, 15.0)
            .visible(() -> infiniteDurability.get() && autoInfElytra.get())
            .build()
    );
    private final Setting<Double> autoInfMoveTolerance = sgGeneral.add(new DoubleSetting.Builder()
            .name("SmartInf Move Tolerance")
            .description("Maximum allowed movespeed to be considered 'straight'.")
            .defaultValue(0.01)
            .range(0.0, 0.1)
            .sliderRange(0.0, 0.1)
            .visible(() -> infiniteDurability.get() && autoInfElytra.get())
            .build()
    );

    private final Setting<FireWorkHandlerMode> fireWorkHandler = sgGeneral.add(new EnumSetting.Builder<FireWorkHandlerMode>()
        .name("FireWork Handler")
        .description("If causes bug,how to handle conflict between infinite durability and firework boost.")
        .defaultValue(FireWorkHandlerMode.None)
        .visible(infiniteDurability::get)
        .build()
    );

    private final Setting<Boolean> infiniteDurabilityItemSync = sgGeneral.add(new BoolSetting.Builder()
            .name("InfElytraSynctoServers")
            .description("(might not working)Keep sync with server to avoid some bugs")
            .defaultValue(true)
            .visible(infiniteDurability::get)
            .build()
    );

    private final Setting<Boolean> enhancedStuckRecovery = sgGeneral.add(new BoolSetting.Builder()
            .name("Enhanced Stuck Recovery")
            .description("(For InfElytra)Auto-open inventory and re-equip elytra if stuck in air without elytra for too long.")
            .defaultValue(true)
            .visible(infiniteDurability::get)
            .build()
    );

    private final Setting<Integer> enhancedStuckThreshold = sgGeneral.add(new IntSetting.Builder()
                .name("Enhanced Stuck Recovery Detection(tick)")
                .description("detect if players stucked in air")
                .defaultValue(100)// 5秒（20 tick/秒）
                .range(20, 400)
                .sliderRange(20, 400)
                .visible(()->enhancedStuckRecovery.get())
                .build()
        );


    // --- 低耐久报警 ---
    private final Setting<Boolean> lowDurabilityWarning = sgGeneral.add(new BoolSetting.Builder()
            .name("Low Durability Warning")
            .description("When enabled, display a warning when the elytra's durability is low.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> warningThreshold = sgGeneral.add(new IntSetting.Builder()
            .name("Warning Threshold")
            .description("Elytra durability below this value will trigger a warning.")
            .defaultValue(20)
            .range(5, 100)
            .sliderRange(5, 50)
            .visible(lowDurabilityWarning::get)
            .build()
    );

    private final Setting<Boolean> onlyWhenLastElytra = sgGeneral.add(new BoolSetting.Builder()
            .name("Only When Last Elytra")
            .description("Only trigger the low durability warning when there are no other elytras in the inventory.")
            .defaultValue(true)
            .visible(lowDurabilityWarning::get)
            .build()
    );

    // --- 官方 AutoArmor 联动开关 ---
    private final Setting<Boolean> autoManageAutoArmor = sgGeneral.add(new BoolSetting.Builder()
            .name("Auto Manage AutoArmor")
            .description("When enabled, automatically enable the ignore-elytra option in the official AutoArmor module when this module is activated, and restore it when deactivated.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> debugMode = sgDebug.add(new BoolSetting.Builder()
            .name("Debug Mode")
            .description("Enable debug options for testing.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> debugOutput = sgDebug.add(new BoolSetting.Builder()
            .name("DebugOutPut")
            .description("DebugOutPut")
            .defaultValue(false)
            .visible(() -> debugMode.get())  // 仅在 Debug Mode 开启时可见
            .build()
    );

    private final Setting<Double> StuckYmotionThreshold =sgDebug.add(new DoubleSetting.Builder()
            .name("Stuck in Air Y Motion Threshold")
            .description("detect stucked in air Y motion Threshold")
            .defaultValue(0.01D)
            .range(0.0D, 0.1D)
            .sliderRange(0.0D, 0.1D)
            .visible(()->enhancedStuckRecovery.get())
            .build()
    );

    private final Setting<Boolean> forceStuckTest = sgDebug.add(new BoolSetting.Builder()
            .name("Force Stuck Elytra (Test)")
            .description("Simulate elytra stuck on cursor for testing recovery.")
            .defaultValue(false)
            .visible(() -> debugMode.get())
            .build()
    );

    // --- 状态变量 ---
    //private int resetDelayTicks = 0;
    //private long lastElytraResetTime = 0;
    private boolean wearingChestplate = false;
    private int consecutiveFailures = 0;
    private boolean wasElytraFlyActive = false;
    private boolean pendingChestUnlock = false;
    private int lastWarnedDurability = -1;
    private int brokenElytraCount = 0;
    private boolean jumpPressedLastTick = false;
    private boolean pauseInfiniteDurability = false; // 用于 PriorFirework
    private Boolean tempEasyTakeoff = null;//修复关闭easytakeoff时infelytra的BUG
    private int stuckTicks = 0;
    private float lastMoveDirection = Float.NaN;
    private int directionStableTicks = 0;
    //private boolean autoInfElytraEnabled = false;
    //private volatile boolean skipResetThisTick = false;
    private final Timer resetTimer = new Timer();
    //private boolean autoInfTriggered = false;
    //private boolean skipNextPeriodicReset = false;

    private boolean forceDisableInfElytra = false;

    // --- 无限鞘翅耐久功能 ---
    public enum InfiniteDurabilityMode {
        SilentMove,       // 静默移动鞘翅
        SilentMoveSafe,   // 静默移动鞘翅（安全版，使用 quickSwap）
        DoubleClickSlot,  // 双击胸甲槽位
        ChestplateSwap    // 胸甲切换（需背包有安全胸甲）
    }

    public enum FireWorkHandlerMode {
        None,              // 不处理
        PriorFirework,     // 烟花优先：检测到烟花加速时暂停无限耐久
        PriorInfElytra     // 无限耐久优先：暂停 ElytraFly 的自动使用烟花
    }
    public enum NoElytraAction {
        NOTHING,        // 什么都不做（赌命）
        STOP_FLYING,    // 强制结束飞行
        WARNING_ONLY    // 仅警告
    }
    public enum TimerState{
        IDLE,
        CHECK,
        ENABLEFUNC
    }
    TimerState shouldExecute = TimerState.IDLE;

    public ElytraSwap() {
        super(DonkeySpawnerAddon.CATEGORY, "ElytraSwap", "Better elytra swapping for DonkeySpawner ElytraFly and Meteor official Auto Armor");
    }

    public void requestChestplateSwap() {
        pendingChestUnlock = true;
    }

    public void setForceDisableInfElytra(boolean disable) {
        this.forceDisableInfElytra = disable;
        // 可选：立即应用 ignore-elytra 覆盖（如果已有 applyIgnoreElytraOverride）
        if (disable) {
            setAutoArmorIgnoreElytra(true);
        } else {
            // 恢复需要根据原始值，建议使用 applyIgnoreElytraOverride
            applyIgnoreElytraOverride();
        }
    }

    // 提供直接设置 ignore-elytra 的方法（如果还没有 public 版本）
    public void setAutoArmorIgnoreElytra(boolean enable) {
        if (!autoManageAutoArmor.get()) return;
        Module autoArmor = Modules.get().get("auto-armor");
        if (autoArmor != null && autoArmor.isActive()) {
            Setting<Boolean> ignoreSetting = (Setting<Boolean>) autoArmor.settings.get("ignore-elytra");
            if (ignoreSetting != null) {
                ignoreSetting.set(enable);
            }
        }
    }

    private void applyIgnoreElytraOverride() {
        if (!forceDisableInfElytra) return;
        Module autoArmor = Modules.get().get("auto-armor");
        if (autoArmor == null || !autoArmor.isActive()) return;
        Setting<Boolean> ignoreSetting = (Setting<Boolean>) autoArmor.settings.get("ignore-elytra");
        if (ignoreSetting != null) {
            ignoreSetting.set(true);
        }
    }

    @Override
    public void onActivate() {
        pendingChestUnlock = false;
        brokenElytraCount = 0;
        lastWarnedDurability = -1;
        jumpPressedLastTick = false;
        wearingChestplate = false;
        consecutiveFailures = 0;
        resetTimer.reset();                // ✅ 必须存在
        lastMoveDirection = Float.NaN;     // ✅ 必须存在
        directionStableTicks = 0;          // ✅ 必须存在
    }

    @Override
    public void onDeactivate() {
        if (!smartSwapBack.get()) {
            setAutoArmorIgnoreElytra(false);
        }
        pendingChestUnlock = false;
        pauseInfiniteDurability = false;

    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null ||
            mc.player.getInventory() == null ||
            mc.player.connection == null ||
            mc.player.containerMenu == null ||
            mc.player.gameMode() == null) {
            return;
        }

        // --- 与 ElytraFly 联动的智能换装 ---
        Module elytrafly = Modules.get().get(ElytraFly.class);
        boolean elytraflyActive = elytrafly != null && elytrafly.isActive();

        if (smartElytraSwap.get() && elytraflyActive) {
            handleElytraSwapOnElytraFlyActive();
        }

        // 处理 ElytraFly 关闭时的恢复逻辑
        if (wasElytraFlyActive && !elytraflyActive) {
            pendingChestUnlock = smartSwapBack.get();
        }
        wasElytraFlyActive = elytraflyActive;

        if (pendingChestUnlock && (mc.player.onGround()||mc.player.isPassenger())) {
            setAutoArmorIgnoreElytra(false);
            pendingChestUnlock = false;
        } else if (pendingChestUnlock && (!mc.player.onGround()&&!mc.player.isPassenger())) {
            ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            boolean elytraBroken = chest.has(DataComponents.GLIDER) && (chest.getMaxDamage() - chest.getDamageValue()) <= 1;
            if (elytraBroken) {
                setAutoArmorIgnoreElytra(false);
                pendingChestUnlock = false;
            }
        }

        // 检测玩家从飞行状态落地，主动换回胸甲
        if ((mc.player.onGround()||mc.player.isPassenger()) && !mc.player.isFallFlying()) {
            if (smartSwapBack.get() && !forceDisableInfElytra) {
                pendingChestUnlock = true;
            }
        }

        // 获取 ElytraFly 实例
        ElytraFly elytraflyInstance = null;
        Module elytraflyModule = Modules.get().get(ElytraFly.class);
        if (elytraflyModule instanceof ElytraFly) {
            elytraflyInstance = (ElytraFly) elytraflyModule;
        }

        // 烟花处理器：检测烟花加速状态，根据选项决定暂停哪一方
        if (infiniteDurability.get() && !mc.player.isInWater() && !mc.player.isInLava()) {
            boolean boosting = (elytraflyInstance != null) && elytraflyInstance.isBoost();
            FireWorkHandlerMode mode = fireWorkHandler.get();
            if (mode == FireWorkHandlerMode.PriorFirework) {
                pauseInfiniteDurability = boosting;
            } else if (mode == FireWorkHandlerMode.PriorInfElytra) {
                if (elytraflyInstance != null) {
                    Setting<Boolean> autoUseSetting = (Setting<Boolean>) elytraflyInstance.settings.get("VerticalTakeoff");
                    if (autoUseSetting != null) {
                        autoUseSetting.set(!boosting); // boosting时设为 false，否则 true
                    }
                }
            }
        }

        // --- 无限鞘翅耐久：周期性重置（AutoInf 仅作为内部阀门）---
        if (this.infiniteDurability.get() && mc.player.isFallFlying() 
                && !mc.player.isInWater() && !mc.player.isInLava() 
                && !pauseInfiniteDurability && elytraflyActive) {
            
            TimerState currentState = TimerState.IDLE;
            
            // 1. 如果开启了自动模式，则每个 tick 更新方向稳定性（持续跟踪）
            if (autoInfElytra.get() && elytraflyActive) {
                Vec3 vel = mc.player.getDeltaMovement();
                double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                boolean meetsCondition = false;
                
                if (hSpeed < autoInfMoveTolerance.get()) {
                    meetsCondition = true;
                } else {
                    float currentDir = (float) Math.toDegrees(Math.atan2(-vel.x, vel.z));
                    if (!Float.isNaN(lastMoveDirection)) {
                        float diff = Math.abs(currentDir - lastMoveDirection);
                        diff = Math.min(diff, 360 - diff);
                        meetsCondition = (diff <= autoInfDirectionTolerance.get());
                    }
                    lastMoveDirection = currentDir;
                }
                
                if (meetsCondition) {
                    directionStableTicks++;
                    if (directionStableTicks >= autoInfDirectionTime.get()) {
                        currentState = TimerState.ENABLEFUNC;
                    } else {
                        currentState = TimerState.CHECK;
                    }
                } else {
                    directionStableTicks = 0;
                    currentState = TimerState.IDLE;
                }
                
                if (debugMode.get() && debugOutput.get() && mc.player.tickCount % 10 == 0) {
                    DebugOutput("State: " + currentState.name() + ", stable=" + directionStableTicks + "/" + autoInfDirectionTime.get(), ChatFormatting.GRAY);
                }
            }
            
            // --- 自动控制 ElytraFly 的 easyTakeoff 开关 ---
            Module elytraflyModule2 = Modules.get().get(ElytraFly.class);
            if (elytraflyModule instanceof ElytraFly elytrafly2) {
                Setting<Boolean> easyTakeoffSetting = (Setting<Boolean>) elytrafly.settings.get("easyTakeoff");
                if (easyTakeoffSetting != null) {
                    boolean shouldEnable = false;
                    if (infiniteDurability.get()) {
                        if (autoInfElytra.get()) {
                            // 自动模式：仅当状态为 ENABLEFUNC 时启用
                            shouldEnable = (currentState == TimerState.ENABLEFUNC);
                        } else {
                            // 手动模式：平飞状态（飞行中且无垂直输入）
                            boolean isFlyingFlat = mc.player.isFallFlying() && !mc.options.keyJump.isDown() && !mc.options.keyShift.isDown();
                            shouldEnable = isFlyingFlat && elytraflyActive;
                        }
                    }
                    
                    // 需要启用且当前未启用 -> 记录原始值并启用
                    if (shouldEnable && !easyTakeoffSetting.get()) {
                        if (tempEasyTakeoff == null) {
                            tempEasyTakeoff = easyTakeoffSetting.get(); // 记录原始值
                        }
                        easyTakeoffSetting.set(true);
                    } 
                    // 不需要启用且之前记录过原始值 -> 恢复
                    else if (!shouldEnable && tempEasyTakeoff != null) {
                        easyTakeoffSetting.set(tempEasyTakeoff);
                        tempEasyTakeoff = null;
                    }
                    
                    // 落地时强制恢复（防止状态残留）
                    if ((mc.player.onGround()||mc.player.isPassenger()) && tempEasyTakeoff != null) {
                        easyTakeoffSetting.set(tempEasyTakeoff);
                        tempEasyTakeoff = null;
                    }
                }
            }

            // 2. 计时器触发（固定周期）
            if (resetTimer.passed(this.infiniteDurabilityInterval.get())) {
                resetTimer.reset();
                
                // 决定本次是否执行重置：手动模式直接执行；自动模式仅当状态为 ENABLEFUNC 时执行
                boolean shouldExecute = (!autoInfElytra.get() || (currentState == TimerState.ENABLEFUNC))&& elytraflyActive;
                
                if (debugMode.get() && debugOutput.get()) {
                    String status = shouldExecute ? "EXECUTE" : "SKIP";
                    DebugOutput("Timer triggered, " + status, shouldExecute ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
                }
                
                if (shouldExecute) {
                    switch (infiniteDurabilityMode.get()) {
                        case SilentMove -> resetElytraSilentMove();
                        case SilentMoveSafe -> resetElytraSilentMoveSafe();
                        case DoubleClickSlot -> resetElytraDoubleClickSlot();
                        case ChestplateSwap -> resetElytraChestplateSwap();
                    }
                }
            }
        }

        // 强制卡手测试按钮
        if (forceStuckTest.get()) {
            simulateElytraStuckOnCursor();
            forceStuckTest.set(false);
        }

        // 加强版卡手恢复：检测长时间卡空、无鞘翅、生存/极限模式
        if (enhancedStuckRecovery.get() && mc.player != null && (!mc.player.onGround()&&!mc.player.isPassenger())) {
            boolean validGameMode = mc.player.gameMode().isSurvival();
            if (validGameMode) {
                ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
                boolean hasElytraEquipped = chest.has(DataComponents.GLIDER);
                boolean isUpright = !mc.player.isFallFlying(); 
                boolean isStuckInAir = Math.abs(mc.player.getDeltaMovement().y) < StuckYmotionThreshold.get();

                if (!hasElytraEquipped && isUpright && isStuckInAir) {
                    stuckTicks++;
                    if (stuckTicks >= enhancedStuckThreshold.get()) {
                        performEnhancedRecovery();
                        stuckTicks = 0;
                    }
                } else {
                    stuckTicks = 0;
                }
            }
        } else {
            stuckTicks = 0;
        }

        // 低耐久报警
        if (lowDurabilityWarning.get() && mc.player.isFallFlying()) {
            checkLowDurabilityWarning();
        }

        // 胸甲锁定检查：如果等待落地解锁且未落地，则跳过本次替换
        if (pendingChestUnlock && (!mc.player.onGround()&&!mc.player.isPassenger())) {
            return;
        }

        // 智能鞘翅切换核心逻辑（低耐久自动替换）
        if (smartElytraSwap.get() && mc.player.isFallFlying()) {
            evaluateAndSwapElytra();
        }
    }
/* 
    private boolean isBoostActive() {
        // 与 ElytraFly.isBoost() 逻辑一致，但独立实现避免循环依赖
        if (mc.player == null || mc.level == null) return false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof FireworkRocketEntity rocket) {
                if (rocket.getOwner() != null && rocket.getOwner().is(mc.player)) {
                    return true;
                }
            }
        }
        return false;
    }*/

    public void DebugOutput(String message) {
        DebugOutput(message, ChatFormatting.WHITE);
    }
    public void DebugOutput(String message,ChatFormatting color) {
        if (this.debugOutput.get() && this.debugMode.get()) {
            ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraFlyDebugOutput]" + message).withStyle(color));
        }
    }

    private void performEnhancedRecovery() {
        if (mc.player == null || mc.player.gameMode() == null || mc.player.containerMenu == null) return;
        ForceInvSyncPro();

        ItemStack carried = mc.player.containerMenu.getCarried();
        int elytraSlot = -1;

        // 1. 优先鼠标
        if (!carried.isEmpty() && carried.getCount() > 0 && carried.has(DataComponents.GLIDER)) {
            int durability = carried.getMaxDamage() - carried.getDamageValue();
            if (durability > 1 && !hasBindingCurse(carried)) {
                elytraSlot = -999;
            }
        }

        // 2. 背包查找
        if (elytraSlot == -1) {
            int size = mc.player.getInventory().getContainerSize();
            for (int i = 0; i < size; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty() || stack.getCount() == 0 || !stack.has(DataComponents.GLIDER)) continue;
                int durability = stack.getMaxDamage() - stack.getDamageValue();
                if (durability > 1 && !hasBindingCurse(stack)) {
                    elytraSlot = i;
                    break;
                }
            }
        }

        if (elytraSlot == -1) {
            ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] No available elytra found. Unable to recover。").withStyle(ChatFormatting.RED));
            return;
        }

        ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Detected stuck in air. Try recovering...").withStyle(ChatFormatting.YELLOW));

        ItemStack beforeChest = mc.player.getItemBySlot(EquipmentSlot.CHEST).copy();

        if (elytraSlot == -999) {
            mc.player.setItemSlot(EquipmentSlot.CHEST, carried);
            mc.player.containerMenu.setCarried(ItemStack.EMPTY);
        } else {
            InvUtils.move().from(elytraSlot).toArmor(EquipmentSlot.CHEST.getIndex());
        }
        mc.player.getInventory().setChanged();
        ForceInvSyncPro();
        // 验证结果
        ItemStack currentChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean success = currentChest.has(DataComponents.GLIDER) && currentChest.getCount() > 0;
        if (!success) {
            // 尝试回退
            if (!beforeChest.isEmpty()) {
                mc.player.setItemSlot(EquipmentSlot.CHEST, beforeChest);
            }
            ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Auto recover chest slot failed,please check manually").withStyle(ChatFormatting.RED));
            return;
        }

        if (!mc.player.isFallFlying()) {
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            mc.player.startFallFlying();
        }

        ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Auto Recovery finished,Requipped elytra....").withStyle(ChatFormatting.GREEN));
    }

    private void simulateElytraStuckOnCursor() {
        if (mc.player == null) return;
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.has(DataComponents.GLIDER)) {
            ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] DEBUGTEST FAILED:Failed to equip the Elytra").withStyle(ChatFormatting.RED));
            return;
        }

        // 将鞘翅从胸甲槽位移到鼠标上
        mc.player.containerMenu.setCarried(chest.copy());
        mc.player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        mc.player.getInventory().setChanged();

        // 如果玩家在飞行，尝试停止飞行以模拟真实卡手后的状态
        if (mc.player.isFallFlying()) {
            mc.player.stopFallFlying();
        }

        ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Elytra-stuck-on-cursor BUG triggerd for Enhanced Recovery test!").withStyle(ChatFormatting.YELLOW));
    }

    private void ForceSyncInv(){
        if(infiniteDurabilityItemSync.get()) mc.player.getInventory().setChanged();
    }
    private void ForceInvSyncPro() {
        if (mc.player == null || mc.player.connection == null || !infiniteDurabilityItemSync.get()) return;
        // 发送一个无实际移动的完整移动包，强制服务端同步玩家状态（含物品栏）
        mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            mc.player.getYRot(), mc.player.getXRot(),
            mc.player.onGround()||mc.player.isPassenger(), false
        ));
    }
    /**
     * 尝试一次将鼠标上的鞘翅放回胸甲。
     * @param carried 鼠标上的鞘翅堆
     * @return 恢复是否成功（操作后鼠标上不再有鞘翅）
     */
    private boolean tryRecoverElytra(ItemStack carried) {
        // 保存当前胸甲槽位的物品（用于失败回滚）
        ItemStack beforeChest = mc.player.getItemBySlot(EquipmentSlot.CHEST).copy();

        // 执行恢复操作
        mc.player.containerMenu.setCarried(ItemStack.EMPTY);
        mc.player.setItemSlot(EquipmentSlot.CHEST, carried);
        mc.player.getInventory().setChanged();

        // 验证恢复是否真正成功
        ItemStack currentCarried = mc.player.containerMenu.getCarried();
        ItemStack currentChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean cursorCleared = currentCarried.isEmpty() || !currentCarried.has(DataComponents.GLIDER);
        boolean chestHasElytra = currentChest.has(DataComponents.GLIDER) && currentChest.getCount() > 0;

        if (cursorCleared && chestHasElytra) {
            return true;
        }

        // 恢复失败：尝试回滚（若胸甲槽位被破坏）
        if (!chestHasElytra && !beforeChest.isEmpty()) {
            mc.player.setItemSlot(EquipmentSlot.CHEST, beforeChest);
        }
        // 若鼠标仍有鞘翅，尝试再次清空
        if (!cursorCleared) {
            mc.player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        mc.player.getInventory().setChanged();
        return false;
    }

    // === 无限耐久四种实现 ===

    private void resetElytraSilentMove() {
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.has(DataComponents.GLIDER)) return;

        int tempSlot = findEmptyTempSlot();
        InvUtils.move().fromArmor(EquipmentSlot.CHEST.getIndex()).to(tempSlot);
        handleStuckElytraOnCursor();
        ForceSyncInv();
        InvUtils.move().from(tempSlot).toArmor(EquipmentSlot.CHEST.getIndex());
        handleStuckElytraOnCursor();
        ForceSyncInv();

        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        if (mc.player.isFallFlying()) {
            mc.player.startFallFlying();
            mc.player.setYRot(mc.player.getYRot());
            mc.player.setXRot(mc.player.getXRot());
        }
    }

    private void resetElytraSilentMoveSafe() {
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.has(DataComponents.GLIDER)) return;

        int tempSlot = mc.player.getInventory().getSelectedSlot();
        InvUtils.quickSwap().fromArmor(EquipmentSlot.CHEST.getIndex()).to(tempSlot);
        ForceSyncInv();
        InvUtils.quickSwap().fromArmor(EquipmentSlot.CHEST.getIndex()).to(tempSlot);
        ForceSyncInv();

        handleStuckElytraOnCursor();
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        if (mc.player.isFallFlying()) {
            mc.player.startFallFlying();
            mc.player.setYRot(mc.player.getYRot());
            mc.player.setXRot(mc.player.getXRot());
        }
    }

    private void resetElytraDoubleClickSlot() {
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.has(DataComponents.GLIDER)) return;

        int chestSlot = 6;
        int syncId = mc.player.containerMenu.containerId;
        if(mc.gameMode==null) return;
        mc.gameMode.handleInventoryMouseClick(syncId, chestSlot, 0, ClickType.PICKUP, mc.player);
        ForceSyncInv();
        mc.gameMode.handleInventoryMouseClick(syncId, chestSlot, 0, ClickType.PICKUP, mc.player);
        ForceSyncInv();

        handleStuckElytraOnCursor();
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        if (mc.player.isFallFlying()) {
            mc.player.startFallFlying();
            mc.player.setYRot(mc.player.getYRot());
            mc.player.setXRot(mc.player.getXRot());
        }
        
    }

    private void resetElytraChestplateSwap() {
        if (wearingChestplate) {
            if (!switchBackToElytra()) {
                consecutiveFailures++;
                if (consecutiveFailures >= 3) {
                    ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Chestplate swap failed repeatedly. Disabled.").withStyle(ChatFormatting.RED));
                    infiniteDurability.set(false);
                }
            } else {
                consecutiveFailures = 0;
            }
        } else {
            if (!switchToChestplate()) {
                consecutiveFailures++;
                if (consecutiveFailures >= 3) {
                    ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Chestplate swap failed repeatedly. Disabled.").withStyle(ChatFormatting.RED));
                    infiniteDurability.set(false);
                }
            } else {
                consecutiveFailures = 0;
            }
        }
        wearingChestplate = !wearingChestplate;

        handleStuckElytraOnCursor();

        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        if (mc.player.isFallFlying()) {
            mc.player.startFallFlying();
            mc.player.setYRot(mc.player.getYRot());
            mc.player.setXRot(mc.player.getXRot());
        }
    }

    private boolean switchToChestplate() {
        ItemStack currentChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!currentChest.has(DataComponents.GLIDER)) return false;

        int chestSlot = findSafeChestplate();
        if (chestSlot == -1) return false;

        int tempSlot = findEmptyTempSlot();
        InvUtils.move().fromArmor(EquipmentSlot.CHEST.getIndex()).to(tempSlot);
        InvUtils.move().from(chestSlot).toArmor(EquipmentSlot.CHEST.getIndex());
        handleStuckElytraOnCursor();

        //mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        return true;
    }

    private boolean switchBackToElytra() {
        ItemStack currentChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (currentChest.has(DataComponents.GLIDER)) return false;

        int elytraSlot = findElytraInInventory();
        if (elytraSlot == -1) return false;

        int chestSlot = findCurrentChestplateSlot();
        if (chestSlot == -1) chestSlot = mc.player.getInventory().getSelectedSlot();

        InvUtils.move().fromArmor(EquipmentSlot.CHEST.getIndex()).to(chestSlot);
        InvUtils.move().from(elytraSlot).toArmor(EquipmentSlot.CHEST.getIndex());
        handleStuckElytraOnCursor();
        //mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        return true;
    }

    private int findSafeChestplate() {
        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isChestplate(stack) && !stack.has(DataComponents.GLIDER) && !hasBindingCurse(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findElytraInInventory() {
        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (mc.player.getInventory().getItem(i).has(DataComponents.GLIDER)) {
                return i;
            }
        }
        return -1;
    }

    private int findCurrentChestplateSlot() {
        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isChestplate(stack) && !stack.has(DataComponents.GLIDER)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isChestplate(ItemStack stack) {
        var eq = stack.get(DataComponents.EQUIPPABLE);
        return eq != null && eq.slot() == EquipmentSlot.CHEST;
    }

    private boolean hasBindingCurse(ItemStack stack) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(Enchantments.BINDING_CURSE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测并处理鼠标上卡住的鞘翅。
     * 如果鼠标持有鞘翅，则将其放回胸甲槽位，并同步物品栏。
     */
    private void handleStuckElytraOnCursor() {
        // 1. 主动同步，清理幽灵物品
        ForceInvSyncPro();

        ItemStack carried = mc.player.containerMenu.getCarried();
        if (carried.isEmpty() || carried.getCount() == 0 || !carried.has(DataComponents.GLIDER)) {
            return;
        }

        int durability = carried.getMaxDamage() - carried.getDamageValue();
        if (durability <= 1 || hasBindingCurse(carried)) {
            mc.player.containerMenu.setCarried(ItemStack.EMPTY);
            mc.player.getInventory().setChanged();
            if (debugMode.get() && debugOutput.get()) {
                ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Elytra on cursor is not availabie,abandonded...").withStyle(ChatFormatting.RED));
            }
            return;
        }

        // 多次尝试恢复
        boolean recovered = false;
        for (int i = 0; i < 3; i++) {
            if (tryRecoverElytra(carried)) {
                recovered = true;
                break;
            }
            ForceInvSyncPro();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
            carried = mc.player.containerMenu.getCarried();
        }

        if (recovered) {
            if (debugMode.get() && debugOutput.get()) {
                ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Hand stuck bug recovery succeeded").withStyle(ChatFormatting.GREEN));
            }
        } else {
            ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Hand stuck bug recovery failed, please put the elytra in your chest slot manually").withStyle(ChatFormatting.RED));
        }
    }

    
    private int findEmptyTempSlot() {
        // 优先快捷栏空位
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }
        // 其次背包空位
        int size = mc.player.getInventory().getContainerSize();
        for (int i = 9; i < size; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }
        // 无空位则使用当前手持槽位（覆盖）
        return mc.player.getInventory().getSelectedSlot();
    }

    // === 原有功能方法 ===
    private void handleElytraSwapOnElytraFlyActive() {
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearingValidElytra = chest.has(DataComponents.GLIDER) && (chest.getMaxDamage() - chest.getDamageValue()) > 1;
        if (wearingValidElytra) {
            setAutoArmorIgnoreElytra(true);
            return;
        }

        if (!mc.player.onGround()&&!mc.player.isPassenger()) {
            setAutoArmorIgnoreElytra(true);
            int bestSlot = findBestElytraSlot();
            if (bestSlot != -1) {
                InvUtils.move().from(bestSlot).toArmor(EquipmentSlot.CHEST.getIndex());
                ItemStack after = mc.player.getItemBySlot(EquipmentSlot.CHEST);
                if (!after.has(DataComponents.GLIDER) || (after.getMaxDamage() - after.getDamageValue()) <= 1) {
                    int retry = findBestElytraSlot();
                    if (retry != -1) 
                        InvUtils.move().from(retry).toArmor(EquipmentSlot.CHEST.getIndex());
                }
            }
        } else {
            boolean jumpPressed = mc.options.keyJump.isDown();
            if (jumpPressed && !jumpPressedLastTick && !mc.player.isPassenger()) {
                performElytraEquip();
            }
            jumpPressedLastTick = jumpPressed;
        }
    }

    private void performElytraEquip() {
        setAutoArmorIgnoreElytra(true);
        int bestSlot = findBestElytraSlot();
        if (bestSlot != -1) {
            InvUtils.move().from(bestSlot).toArmor(EquipmentSlot.CHEST.getIndex());
        }
    }

    private void evaluateAndSwapElytra() {
        if (this.infiniteDurability.get()) return; // 无限耐久开启时跳过自动替换

        ItemStack currentChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearingValidElytra = currentChest.has(DataComponents.GLIDER) && (currentChest.getMaxDamage() - currentChest.getDamageValue()) > 1;

        if (!wearingValidElytra) {
            int bestSlot = findBestElytraSlot();
            if (bestSlot != -1) {
                InvUtils.move().from(bestSlot).toArmor(EquipmentSlot.CHEST.getIndex());
            }
            return;
        }

        if (!autoReplaceElytra.get()) return;

        int currentDurability = currentChest.getMaxDamage() - currentChest.getDamageValue();
        if (currentDurability > replaceDurabilityThreshold.get()) return;

        int bestSlot = findBestElytraSlot();
        if (bestSlot != -1) {
            ItemStack candidate = mc.player.getInventory().getItem(bestSlot);
            int candidateDurability = candidate.getMaxDamage() - candidate.getDamageValue();
            boolean shouldReplace = candidateDurability > replaceDurabilityThreshold.get();
            if (shouldReplace) {
                boolean wasFlying = mc.player.isFallFlying();
                InvUtils.move().from(bestSlot).toArmor(EquipmentSlot.CHEST.getIndex());
                ItemStack afterChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
                if (!afterChest.has(DataComponents.GLIDER) || (afterChest.getMaxDamage() - afterChest.getDamageValue()) <= 1) {
                    int retrySlot = findBestElytraSlot();
                    if (retrySlot != -1) {
                        InvUtils.move().from(retrySlot).toArmor(EquipmentSlot.CHEST.getIndex());
                    }
                }
                if (wasFlying && !mc.player.isFallFlying()) {
                    mc.player.startFallFlying();
                }
                brokenElytraCount++;
                int remaining = countElytraInInventory();
                ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Replaced old elytra with new one. broken elytras: " + brokenElytraCount + ", Remaining elytras: " + remaining).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    private int findBestElytraSlot() {
        List<Integer> elytraSlots = new ArrayList<>();
        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (i == 38 || i == 40) continue;
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.has(DataComponents.GLIDER)) {
                int durability = stack.getMaxDamage() - stack.getDamageValue();
                if (durability <= 1 || hasEnchantment(stack, Enchantments.BINDING_CURSE)) continue;
                elytraSlots.add(i);
            }
        }
        if (elytraSlots.isEmpty()) return -1;
        return elytraSlots.stream()
                .max(Comparator.comparingInt(slot -> getElytraScore(mc.player.getInventory().getItem(slot))))
                .orElse(-1);
    }

    private boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> key) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(key)) {
                return true;
            }
        }
        return false;
    }

    private int getElytraScore(ItemStack stack) {
        int score = stack.getMaxDamage() - stack.getDamageValue();
        if (hasEnchantment(stack, Enchantments.MENDING)) score += 500;
        if (hasEnchantment(stack, Enchantments.UNBREAKING)) score += 200;
        return score;
    }

    private void checkLowDurabilityWarning() {
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.has(DataComponents.GLIDER)) {
            lastWarnedDurability = -1;
            return;
        }

        int rem = chest.getMaxDamage() - chest.getDamageValue();
        if (rem > warningThreshold.get()) {
            lastWarnedDurability = -1;
            return;
        }

        if (onlyWhenLastElytra.get()) {
            if (countElytraInInventory() > 1) {
                lastWarnedDurability = -1;
                return;
            }
        }

        boolean warn = false;
        if (rem <= 10) {
            if (lastWarnedDurability == -1 || rem != lastWarnedDurability) {
                warn = true;
            }
        } else {
            int currentTen = rem / 10;
            int lastTen = lastWarnedDurability / 10;
            if (lastWarnedDurability == -1 || currentTen < lastTen) {
                warn = true;
            }
        }

        if (warn) {
            lastWarnedDurability = rem;
            ChatUtils.sendMsg(Component.literal("[DonkeySpawner ElytraSwap] Warning: Low elytra durability! Remaining: " + rem).withStyle(ChatFormatting.RED));
        }
    }

    private int countElytraInInventory() {
        int count = 0;
        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (mc.player.getInventory().getItem(i).has(DataComponents.GLIDER)) count++;
        }
        return count;
    }
}