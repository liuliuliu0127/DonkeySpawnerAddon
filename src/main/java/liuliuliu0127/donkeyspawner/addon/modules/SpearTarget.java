package liuliuliu0127.donkeyspawner.addon.modules;
import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import liuliuliu0127.donkeyspawner.addon.utils.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
//import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.misc.Keybind;
//import meteordevelopment.meteorclient.utils.misc.input.Input;
//import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
//import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.lwjgl.glfw.GLFW.*;

/**
 * SpearTarget – 瞄准辅助模块（专为 1.21.11 的矛打造）。
 * 手持矛（Spear）并按住右键时，自动平滑旋转看向符合条件的实体（Always 旋转模式）。
 * 仅选择距离在 [minRange, maxRange] 之间且直接可见的实体，触发后持续瞄准直到松开右键或目标无效。
 */
public class SpearTarget extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgDebug = settings.createGroup("DEBUG");

    // ----- 通用设置 -----
    private final Setting<Boolean> targetGlow = sgGeneral.add(new BoolSetting.Builder()
        .name("target-highlight")
        //.description("为目标实体添加原版发光轮廓（仅本地可见）")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoResetCharge = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-reset-charge")
        .description("[VERY BUGGY]Auto recharge spear")
        .defaultValue(false)
        .build()
    );

    private final Setting<ChargeStage> resetStage = sgGeneral.add(new EnumSetting.Builder<ChargeStage>()
        .name("reset-stage")
        .description("Last charge stage you want to keep")
        .defaultValue(ChargeStage.KeepEngaged)
        .visible(autoResetCharge::get)
        .build()
    );

    private final Setting<Integer> customResetTick = sgGeneral.add(new IntSetting.Builder()
        .name("custom-reset-tick")
        .description("custom recharge tick setting")
        .defaultValue(20)
        .min(1)
        .max(300)
        .sliderMax(100)
        .visible(() -> autoResetCharge.get() && resetStage.get() == ChargeStage.CustomTick)
        .build()
    );
    private final Setting<Boolean> enableManualLock = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-force-lock")
        .description("lock your view to the target")
        .defaultValue(false)
        .build()
    );

    private final Setting<Keybind> lockKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("lock-key")
        .defaultValue(Keybind.fromKey(GLFW_KEY_CAPS_LOCK))
        .visible(enableManualLock::get)
        .build()
    );

    private final Setting<LockMode> lockMode = sgGeneral.add(new EnumSetting.Builder<LockMode>()
        .name("lock-mode")
        .defaultValue(LockMode.Hold)
        .visible(enableManualLock::get)
        .build()
    );
    
    private final Setting<RotationMethod> rotationMethod = sgGeneral.add(new EnumSetting.Builder<RotationMethod>()
        .name("rotation-method")
        .description("How to rotate towards target. Meteor uses the internal Rotations system, Direct sends packets directly.")
        .defaultValue(RotationMethod.Meteor)  
        .build()
    );
    private final Setting<Double> rotationSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotation-speed")
        .description("Speed factor for smooth rotation (0.0 - 1.0). Higher = faster.")
        .defaultValue(0.6)
        .min(0.1)
        .max(1.0)
        .sliderMax(1.0)
        .visible(() -> rotationMethod.get() == RotationMethod.Direct)
        .build()
    );

    private final Setting<Double> maxTurnDegrees = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-turn-per-tick")
        .description("Maximum degrees the camera can turn per tick (prevents snapping).")
        .defaultValue(60.0)
        .min(10)
        .max(180)
        .sliderMax(180)
        .visible(() -> rotationMethod.get() == RotationMethod.Direct)
        .build()
    );

    private final Setting<Boolean> aimCompensation = sgGeneral.add(new BoolSetting.Builder()
        .name("aim-compensation")
        .description("Fix aiming for high speed or large horizonal target offset")
        .defaultValue(false)
        .build()
    );

    private final Setting<CompensationMode> compensationMode = sgGeneral.add(new EnumSetting.Builder<CompensationMode>()
        .name("compensation-mode")
        //.description("Legacy：角度放大；Trajectory：轨迹拦截")
        .defaultValue(CompensationMode.Legacy)
        .visible(aimCompensation::get)
        .build()
    );

    private final Setting<Double> compensationFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("compensation-factor")
        //.description("补偿强度系数。")
        .defaultValue(0.5)
        .min(0.0)
        .max(5.0)
        .sliderMax(2.0)
        .visible(() -> aimCompensation.get() && compensationMode.get() == CompensationMode.Legacy)
        .build()
    );

    private final Setting<Double> maxCompScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-compensation-scale")
        //.description("最大放大倍数，防止角度过度偏离。")
        .defaultValue(3.0)
        .min(1.0)
        .max(10.0)
        .sliderMax(5.0)
        .visible(() -> aimCompensation.get() && compensationMode.get() == CompensationMode.Legacy)
        .build()
    );

    private final Setting<Double> horizontalMoveLimit = sgGeneral.add(new DoubleSetting.Builder()
        .name("attack yaw limit")
        .description("Max degrees the target yaw can differ from your movement direction. 180 = no limit.")
        .defaultValue(60.0)
        .min(0.0)
        .max(180.0)
        .sliderMax(180.0)
        .visible(() -> aimCompensation.get() && compensationMode.get() == CompensationMode.Legacy)
        .build()
    );

    private final Setting<Double> verticalMoveLimit = sgGeneral.add(new DoubleSetting.Builder()
        .name("attack pitch limit")
        .description("Max degrees the target pitch can differ from your movement pitch. 180 = no limit.")
        .defaultValue(60.0)
        .min(0.0)
        .max(180.0)
        .sliderMax(180.0)
        .visible(() -> aimCompensation.get() && compensationMode.get() == CompensationMode.Legacy)
        .build()
    );

    private final Setting<Double> trajectoryMaxAngle = sgGeneral.add(new DoubleSetting.Builder()
        .name("trajectory-max-angle")
        //.description("视线与目标运动轨迹的最大允许夹角（度）")
        .defaultValue(45.0)
        .min(1.0)
        .max(90.0)
        .sliderMax(90.0)
        .visible(() -> aimCompensation.get() && compensationMode.get() == CompensationMode.Trajectory)
        .build()
    );

    private final Setting<Double> trajectoryMinDist = sgGeneral.add(new DoubleSetting.Builder()
        .name("trajectory-min-dist")
        //.description("瞄准点距玩家的最小距离（格）")
        .defaultValue(2.0)
        .min(0.0)
        .max(10.0)
        .sliderMax(5.0)
        .visible(() -> aimCompensation.get() && compensationMode.get() == CompensationMode.Trajectory)
        .build()
    );

    private final Setting<Double> trajectoryMaxDist = sgGeneral.add(new DoubleSetting.Builder()
        .name("trajectory-max-dist")
        //.description("瞄准点距玩家的最大距离（格）")
        .defaultValue(4.5)
        .min(1.0)
        .max(10.0)
        .sliderMax(6.0)
        .visible(() -> aimCompensation.get() && compensationMode.get() == CompensationMode.Trajectory)
        .build()
    );

    private final Setting<Double> minRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-range")
        .description("Min range for entities to be selected as target")
        .defaultValue(2.0)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Double> maxRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-range")
        .description("Max range for entities to be selected as target")
        .defaultValue(20.0)
        .min(1)
        .sliderMax(50)
        .build()
    );

    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("Pause Baritone path finding when targeting")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnLag = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-lag")
        .description("Pause when server lags")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnCA = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-CA")
        .description("Pause when CrystalAura works")
        .defaultValue(true)
        .build()
    );

    // ----- 目标筛选设置 -----
    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to be targeted")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<Boolean> limitByMovement = sgTargeting.add(new BoolSetting.Builder()
        .name("sort-limit-by-movement-yaw-pitch")
        .description("Only target entities within a certain angle of your movement direction.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> movementHorizontalLimit = sgTargeting.add(new DoubleSetting.Builder()
        .name("pitch-sort-limit")
        .description("Max horizontal angle difference from your movement direction to be selected.")
        .defaultValue(90.0)
        .min(0)
        .max(180)
        .sliderMax(180)
        .visible(limitByMovement::get)
        .build()
    );

    private final Setting<Double> movementVerticalLimit = sgTargeting.add(new DoubleSetting.Builder()
        .name("yaw-sort-limit")
        .description("Max vertical angle difference from your movement direction to be selected.")
        .defaultValue(90.0)
        .min(0)
        .max(180)
        .sliderMax(180)
        .visible(limitByMovement::get)
        .build()
    );

    private final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How to sort targets")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder()
        .name("range")
        .description("Range for searching targets")
        .defaultValue(20.0)
        .min(0)
        .sliderMax(50)
        .build()
    );

    private final Setting<KillAura.EntityAge> passiveMobAgeFilter = sgTargeting.add(new EnumSetting.Builder<KillAura.EntityAge>()
        .name("passive mob age filter")
        .description("passive mob age filter")
        .defaultValue(KillAura.EntityAge.Adult)
        .build()
    );

    private final Setting<KillAura.EntityAge> hostileMobAgeFilter = sgTargeting.add(new EnumSetting.Builder<KillAura.EntityAge>()
        .name("hostile mob age filter")
        .description("hostile mob age filter")
        .defaultValue(KillAura.EntityAge.Both)
        .build()
    );

    private final Setting<Boolean> ignoreNamed = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-named")
        .description("ignore named")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignorePassive = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore passive")
        .description("ignore passive(only attack netural entities that want to attack you)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreTamed = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-tamed")
        .description("ignoretamed")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> debugMode = sgDebug.add(new BoolSetting.Builder()
        .name("Debug Mode")
        .description("toggle Debug Mode")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> debugOutput = this.sgDebug.add(new BoolSetting.Builder()
        .name("DebugOutput")
        .description("Output debug information to the console.")
        .defaultValue(false)
        .visible(this.debugMode::get)
        .build());
    private final Setting<Boolean> packetCancel = sgDebug.add(new BoolSetting.Builder()
        .name("packet-cancel")
        .description("packet cancel for test")
        .defaultValue(false)
        .visible(this.debugMode::get)
        .build()
    );

    private final Setting<Boolean> mixinMethod = sgDebug.add(new BoolSetting.Builder()
        .name("mixin-method")
        .description("mixin for test")
        .defaultValue(false)
        .visible(this.debugMode::get)
        .build()
    );
    private final Setting<TriggerMode> rotationTrigger = sgDebug.add(new EnumSetting.Builder<TriggerMode>()
        .name("rotation-trigger")
        .description("When to start rotating: Hold = just holding spear, Use = holding right click")
        .defaultValue(TriggerMode.Use)   
        .visible(debugMode::get)
        .build()
    );

    // 在类内定义枚举
    public enum TriggerMode {
        Hold,
        Use
    }

    public enum RotationMethod {
        Meteor,   // 使用meteor Rotations.rotate（一次性请求，会受使用物品等限制）
        Direct//,    // 使用平滑插值 + 直接发包（持续、不受使用限制）
        //VehicleSnap   // 骑乘专用：瞬间旋转坐骑，不影响玩家视角
    }

    public enum LockMode {
        Hold,
        Toggle,
        Always,
        AlwaysAsPassenger
    }

    public enum ChargeStage {
        KeepEngaged,   // 第一阶段
        KeepTired,     // 第二阶段
        KeepDisengaged,// 第三阶段
        CustomTick     // 自定义 tick
    }

    public enum CompensationMode {
        Legacy,
        Trajectory
    }

    // ----- 状态变量 -----
    private Entity currentTarget = null;
    private boolean aiming = false;
    private boolean wasPathing = false;
    private float currentYaw, currentPitch;
    private Vec3 lastPos = null;
    //private float serverYaw, serverPitch;   // 估算服务端的当前朝向
    // 静态字段，用于服务端 Mixin 获取角度
    public static float serverTargetYaw;
    public static float serverTargetPitch;
    public static boolean shouldOverrideServerRotation;
    //private int retryDelayCounter = 0;
    //private int resetRetryTimer = 0;
    //private int releaseWaitTimer = 0;   // 释放后的等待计时
    //private int retryCounter = 0;       // 重试次数

    private boolean manualLockActive = false;
    private boolean lastLockKeyState = false;
    private final Timer chargeTimer = new Timer();
    private boolean chargeTimerStarted = false;

    private final Timer releaseTimer = new Timer();   // 控制释放后的等待
    private final Timer retryTimer = new Timer();      // 控制重试间隔
    private boolean isInReleaseWait = false;

    public static Entity glowTarget = null;
    public static boolean glowEnabled = false;


    //private int chargeTimer = 0;

    //private long chargeStartTime = 0;

    public SpearTarget() {
        super(DonkeySpawnerAddon.CATEGORY, "SpearTarget", "Make your spear more easy to use,not woring when riding except force view lock!");
    }

    @Override
    public void onDeactivate() {
        currentTarget = null;
        stopAiming();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundMoveVehiclePacket && aiming && mc.player.isPassenger() && debugMode.get() && packetCancel.get()) {
            event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        //DebugOutput("Tick - isHoldingSpear: " + isHoldingSpear() + ", keyUse: " + mc.options.keyUse.isDown());
        if (mc.player == null || !mc.player.isAlive() || PlayerUtils.getGameMode() == GameType.SPECTATOR) {
            //DebugOutput("Exited at: player dead or spectator", ChatFormatting.RED);
            stopAiming();
            return;
        }

        if (TickRate.INSTANCE.getTimeSinceLastTick() >= 1f && pauseOnLag.get()) {
            //DebugOutput("Exited at: server lag", ChatFormatting.RED);
            stopAiming();
            return;
        }
        if (pauseOnCA.get() && Modules.get().get(CrystalAura.class).isActive() && Modules.get().get(CrystalAura.class).kaTimer > 0) {
            //DebugOutput("Exited at: CrystalAura active", ChatFormatting.RED);
            stopAiming();
            return;
        }

        // 必须手持矛（SpearItem，1.21.11 新增）
        if (!isHoldingSpear()) {
            //DebugOutput("Exited at: not holding spear", ChatFormatting.RED);
            stopAiming();
            return;
        }

        // ----- 蓄力阶段重置功能 (基于 Timer，释放后延迟200ms) -----
        if (autoResetCharge.get() && isHoldingSpear()) {
            int maxMs = getChargeResetMs();

            if (mc.player.isUsingItem()) {
                // 如果不在等待阶段，确保计时器已启动
                if (!isInReleaseWait && !chargeTimerStarted) {
                    chargeTimer.reset();
                    chargeTimerStarted = true;
                }
                // 检查是否达到阈值，且不在等待阶段
                if (maxMs > 0 && chargeTimer.passed(maxMs) && !isInReleaseWait) {
                    // 释放物品
                    mc.player.releaseUsingItem();
                    chargeTimerStarted = false;
                    chargeTimer.reset();
                    stopAiming();

                    // 进入等待阶段
                    isInReleaseWait = true;
                    releaseTimer.reset();
                }
            } else {
                // 不在使用物品时
                if (!isInReleaseWait) {
                    chargeTimerStarted = false;
                    chargeTimer.reset();
                }
            }

            // 等待阶段：200ms 后直接重新蓄力
            if (isInReleaseWait && releaseTimer.passed(200)) {
                // 强制设置按键状态并开始使用物品
                mc.options.keyUse.setDown(true);
                mc.player.startUsingItem(InteractionHand.MAIN_HAND);
                isInReleaseWait = false;

                // 如果成功开始蓄力，重置计时器
                if (mc.player.isUsingItem()) {
                    chargeTimer.reset();
                    chargeTimerStarted = true;
                }
            }
        } else {
            chargeTimerStarted = false;
            chargeTimer.reset();
            isInReleaseWait = false; 
        }

        if(debugMode.get()){
            if (rotationTrigger.get() == TriggerMode.Use) {
                if (!mc.options.keyUse.isDown()) {
                    //DebugOutput("Exited at: trigger Use but keyUse not down (debugMode)", ChatFormatting.RED);
                    stopAiming();
                    return;
                }
            }
        } else {
            if (!mc.options.keyUse.isDown()) {
                //DebugOutput("Exited at: keyUse not down (non-debugMode)", ChatFormatting.RED);
                stopAiming();
                return;
            }
        }

        Vec3 movementVec = Vec3.ZERO;
        if (lastPos != null) {
            movementVec = mc.player.position().subtract(lastPos);
        }
        lastPos = mc.player.position();

        // 如果当前已有目标，先检查其是否依然有效（死亡、超出 maxRange、不再可见等）
        if (currentTarget != null) {
            if (!entityCheck(currentTarget) || distanceTo(currentTarget) > maxRange.get()) {
                currentTarget = null; // 失效后下次 tick 重新寻找
            } else if (limitByMovement.get()) {// --- 新增：移动方向角度限制，若超出则放弃目标 ---
                Vec3 velocity = movementVec;//mc.player.getDeltaMovement();
                if (velocity.lengthSqr() > 0.01) {
                    Vec3 movePos = mc.player.position().add(velocity);
                    float moveYaw = (float) Rotations.getYaw(movePos);
                    double horizLen = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                    float movePitch = (float) -Math.toDegrees(Math.atan2(velocity.y, horizLen));

                    float entityYaw = (float) Rotations.getYaw(currentTarget);
                    float entityPitch = (float) Rotations.getPitch(currentTarget, Target.Body);

                    float yawDiff = Mth.wrapDegrees(entityYaw - moveYaw);
                    float pitchDiff = entityPitch - movePitch;

                    float limitH = movementHorizontalLimit.get().floatValue();
                    float limitV = movementVerticalLimit.get().floatValue();
                    // 临时调试输出（测试后可删除）
                    //DebugOutput("Move yaw: " + moveYaw + " entity yaw: " + entityYaw + " diff: " + yawDiff + " limitH: " + limitH);
                    //DebugOutput("Move pitch: " + movePitch + " entity pitch: " + entityPitch + " diff: " + pitchDiff + " limitV: " + limitV);

                    if (Math.abs(yawDiff) > limitH || Math.abs(pitchDiff) > limitV) {
                        //DebugOutput("Target out of angle limit, discarding.");
                        currentTarget = null;   // 放弃目标，触发重新搜索
                    }
                }
            }
        }

        // 若没有有效目标，从范围内重新选取
        if (currentTarget == null) {
            List<Entity> candidates = new ArrayList<>();
            for (Entity entity : mc.level.entitiesForRendering()) {
                double dist = distanceTo(entity);
                if (dist < minRange.get() || dist > maxRange.get()) {
                    //DebugOutput("Entity " + entity.getName().getString() + " rejected: dist=" + dist + " not in [" + minRange.get() + "," + maxRange.get() + "]");
                    continue;
                }
                if (!entityCheck(entity)) {
                    //DebugOutput("Entity " + entity.getName().getString() + " rejected: entityCheck failed (visible? " + PlayerUtils.canSeeEntity(entity) + ")");
                    continue;
                }
                //DebugOutput("Entity " + entity.getName().getString() + " ACCEPTED, dist=" + dist);
                // --- 新增：移动方向角度限制 ---
                if (limitByMovement.get()) {
                    Vec3 velocity = movementVec;
                    if (velocity.lengthSqr() > 0.01) { // 只在真正移动时生效
                        // 计算移动方向角度（使用 Rotations 确保坐标系一致）
                        Vec3 movePos = mc.player.position().add(velocity);
                        float moveYaw = (float) Rotations.getYaw(movePos);
                        double horizLen = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                        float movePitch = (float) -Math.toDegrees(Math.atan2(velocity.y, horizLen));

                        // 实体相对玩家的角度
                        float entityYaw = (float) Rotations.getYaw(entity);
                        float entityPitch = (float) Rotations.getPitch(entity, Target.Body);

                        float yawDiff = Mth.wrapDegrees(entityYaw - moveYaw);
                        float pitchDiff = entityPitch - movePitch;

                        float limitH = movementHorizontalLimit.get().floatValue();
                        float limitV = movementVerticalLimit.get().floatValue();

                        if (Math.abs(yawDiff) > limitH || Math.abs(pitchDiff) > limitV) {
                            //DebugOutput("Entity " + entity.getName().getString() + " rejected by movement angle limit");
                            continue; // 跳过此实体
                        }
                    }
                }
                // --- 结束新增 ---
                candidates.add(entity);
            }

            if (candidates.isEmpty()) {
                //DebugOutput("No candidates found in range", ChatFormatting.RED);
                stopAiming();
                return;
            }

            // 按优先级排序并只保留第一个（maxTargets 固定为 1）
            TargetUtils.getList(candidates, this::entityCheck, priority.get(), 1);
            if (candidates.isEmpty()) {
                //DebugOutput("No candidates after priority filter", ChatFormatting.RED);
                stopAiming();
                return;
            }
            currentTarget = candidates.get(0);
            //DebugOutput("New target: " + currentTarget.getName().getString());
        }

        // 原始目标角度
        float targetYaw = (float) Rotations.getYaw(currentTarget);
        float targetPitch = (float) Rotations.getPitch(currentTarget, Target.Body);

        
        float compensatedYaw = targetYaw;
        float compensatedPitch = targetPitch;

        if (aimCompensation.get() && currentTarget != null) {
            if (compensationMode.get() == CompensationMode.Legacy) {
                // ========== Legacy：原有角度放大 ==========
                double horizontalLength = Math.sqrt(movementVec.x * movementVec.x + movementVec.z * movementVec.z);
                Vec3 movePos = mc.player.position().add(movementVec);
                float moveYaw = (float) Rotations.getYaw(movePos);
                float movePitch = (float) -Math.toDegrees(Math.atan2(movementVec.y, horizontalLength));

                float yawDiff = Mth.wrapDegrees(targetYaw - moveYaw);
                float pitchDiff = targetPitch - movePitch;
                double angleDiff = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

                Vec3 playerVel = movementVec;
                Vec3 targetVel = currentTarget.getDeltaMovement();
                double relativeSpeed = targetVel.subtract(playerVel).length();

                double scale = 1.0 + compensationFactor.get() * angleDiff * relativeSpeed;
                if (scale > maxCompScale.get()) scale = maxCompScale.get();

                compensatedYaw = moveYaw + yawDiff * (float) scale;
                compensatedPitch = movePitch + pitchDiff * (float) scale;

                // Legacy 移动方向限制（同时限制原始和补偿角度）
                double horizLen2 = Math.sqrt(movementVec.x * movementVec.x + movementVec.z * movementVec.z);
                float moveYaw2 = (float) Rotations.getYaw(mc.player.position().add(movementVec));
                float movePitch2 = (float) -Math.toDegrees(Math.atan2(movementVec.y, horizLen2));
                float limitH = horizontalMoveLimit.get().floatValue();
                float limitV = verticalMoveLimit.get().floatValue();

                // 限制原始角度
                float rawYawDiff = Mth.wrapDegrees(targetYaw - moveYaw2);
                float rawPitchDiff = targetPitch - movePitch2;
                rawYawDiff = Mth.clamp(rawYawDiff, -limitH, limitH);
                rawPitchDiff = Mth.clamp(rawPitchDiff, -limitV, limitV);
                targetYaw = moveYaw2 + rawYawDiff;
                targetPitch = movePitch2 + rawPitchDiff;

                // 限制补偿角度
                float compYawDiff = Mth.wrapDegrees(compensatedYaw - moveYaw2);
                float compPitchDiff = compensatedPitch - movePitch2;
                compYawDiff = Mth.clamp(compYawDiff, -limitH, limitH);
                compPitchDiff = Mth.clamp(compPitchDiff, -limitV, limitV);
                compensatedYaw = moveYaw2 + compYawDiff;
                compensatedPitch = movePitch2 + compPitchDiff;

            } else if (compensationMode.get() == CompensationMode.Trajectory) {
                Vec3 vRel = currentTarget.getDeltaMovement().subtract(movementVec);
                Vec3 playerEye = mc.player.getEyePosition();
                if (mc.player.isSwimming() || mc.player.isVisuallyCrawling()) {
                    double correctedY = mc.player.getY() + mc.player.getEyeHeight(Pose.STANDING);
                    playerEye = new Vec3(playerEye.x, correctedY, playerEye.z);
                }
                Vec3 targetCenter = currentTarget.getBoundingBox().getCenter();
                Vec3 m = targetCenter.subtract(playerEye);   // C - O

                boolean compensated = false;   // 标记是否成功设置补偿角度

                // 原有尝试逻辑
                if (vRel.lengthSqr() >= 0.0001) {
                    // 目标在靠近时才尝试
                    if (vRel.dot(m) < 0) {
                        double currentDist = m.length();
                        if (currentDist >= trajectoryMinDist.get()) {
                            Vec3 u = vRel.normalize();
                            double projLen = m.dot(u);
                            Vec3 closestOnLine = playerEye.add(u.scale(projLen));
                            double d = targetCenter.distanceTo(closestOnLine);

                            double angleMax = Math.toRadians(trajectoryMaxAngle.get());
                            double sinMax = Math.sin(angleMax);
                            double tMinAngle = (sinMax > 1e-6) ? (d / sinMax) : 0;
                            double tIdeal = Math.max(trajectoryMinDist.get(), tMinAngle);
                            double calcT = (tIdeal <= trajectoryMaxDist.get()) ? tIdeal : trajectoryMaxDist.get();

                            if (calcT >= trajectoryMinDist.get() && calcT <= trajectoryMaxDist.get()) {
                                double a = vRel.lengthSqr();
                                double b = 2 * vRel.dot(m);
                                double c = m.lengthSqr() - calcT * calcT;
                                double disc = b * b - 4 * a * c;
                                if (disc >= 0) {
                                    double sqrtD = Math.sqrt(disc);
                                    double s1 = (-b - sqrtD) / (2 * a);
                                    double s2 = (-b + sqrtD) / (2 * a);
                                    double s = Math.min(s1, s2);
                                    if (s < 0) s = Math.max(s1, s2);
                                    if (s >= 0) {
                                        Vec3 aimPoint = targetCenter.add(vRel.scale(s));
                                        if (aimPoint.subtract(playerEye).dot(m) > 0) {
                                            Vec3 aimDir = aimPoint.subtract(playerEye).normalize();
                                            compensatedYaw   = (float) Rotations.getYaw(playerEye.add(aimDir.scale(3.0)));
                                            compensatedPitch = (float) Rotations.getPitch(playerEye.add(aimDir.scale(3.0)));
                                            compensated = true;   // 成功
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 失败补偿：强制使用最大空间角方向（双方向择优）
                if (!compensated && vRel.lengthSqr() >= 0.0001 && vRel.dot(m) < 0) {
                    Vec3 u = vRel.scale(-1.0).normalize(); 
                    double proj = m.dot(u);
                    Vec3 mPerp = m.subtract(u.scale(proj));
                    if (mPerp.lengthSqr() > 1e-6) {
                        mPerp = mPerp.normalize();
                        double cosA = Math.cos(Math.toRadians(trajectoryMaxAngle.get()));
                        double sinA = Math.sin(Math.toRadians(trajectoryMaxAngle.get()));
                        Vec3 dir1 = u.scale(cosA).add(mPerp.scale(sinA));
                        Vec3 dir2 = u.scale(cosA).add(mPerp.scale(-sinA));
                        // 选择与目标方向点积更大的方向（保证指向目标）
                        Vec3 dir = (dir1.dot(m) > dir2.dot(m)) ? dir1 : dir2;
                        dir = dir.normalize();
                        compensatedYaw   = (float) Rotations.getYaw(playerEye.add(dir.scale(3.0)));
                        compensatedPitch = (float) Rotations.getPitch(playerEye.add(dir.scale(3.0)));
                    }
                }
            }
        }

        // 开始瞄准（如果尚未）
        if (!aiming) {
            aiming = true;
            if (rotationMethod.get() == RotationMethod.Direct) {
                this.currentYaw = mc.player.getYRot();
                this.currentPitch = mc.player.getXRot();
            }
            if (pauseOnCombat.get() && PathManagers.get().isPathing()) {
                PathManagers.get().pause();
                wasPathing = true;
            }
        }

        // 更新服务端覆盖状态（骑乘且瞄准时启用）
        if (mc.player.isPassenger() && aiming && currentTarget != null && debugMode.get() && mixinMethod.get()) {
            shouldOverrideServerRotation = true;
            serverTargetYaw = targetYaw;
            serverTargetPitch = targetPitch;
        } else {
            shouldOverrideServerRotation = false;
        }

        // 手动锁定按键检测
        if (enableManualLock.get() && aiming && currentTarget != null) {
            boolean keyDown = lockKey.get().isPressed();
            if(lockMode.get() == LockMode.Always){
                manualLockActive = true;
            }else if(lockMode.get() == LockMode.AlwaysAsPassenger){
                if(mc.player.isPassenger()) {
                    manualLockActive = true;
                }
            }else if (lockMode.get() == LockMode.Hold) {
                manualLockActive = keyDown;
            } else {
                if (keyDown && !lastLockKeyState) {
                    manualLockActive = !manualLockActive;
                }
                lastLockKeyState = keyDown;
            }
            // 如果手动锁定生效，强制设置视角
            if (manualLockActive) {
                mc.player.setYRot(targetYaw);
                mc.player.setXRot(targetPitch);
            }
        } else {
            manualLockActive = false;
            lastLockKeyState = false;
        }

        // 分流旋转方法
        if (rotationMethod.get() == RotationMethod.Meteor) {
            Rotations.rotate(compensatedYaw, compensatedPitch);
        } else {
            // Direct 平滑发包（仅步行时）
            float yawDiff = Mth.wrapDegrees(compensatedYaw - this.currentYaw);
            float maxTurn = maxTurnDegrees.get().floatValue();
            float turn = Mth.clamp(yawDiff, -maxTurn, maxTurn);
            float speed = rotationSpeed.get().floatValue();
            this.currentYaw += turn * speed;
            this.currentPitch += (compensatedPitch - this.currentPitch) * speed;
            //ridinghandler(currentYaw,currentPitch);
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                this.currentYaw, this.currentPitch,
                mc.player.onGround(), mc.player.horizontalCollision
            ));
        }
        // 同步高亮目标
        SpearTarget.glowTarget = this.currentTarget;
        SpearTarget.glowEnabled = this.targetGlow.get();
    }

    private void stopAiming() {
        if (!aiming) {
            //DebugOutput("stopAiming called but aiming already false (no target?)", ChatFormatting.YELLOW);
            return;
        }
        //DebugOutput("stopAiming called, aiming was true", ChatFormatting.RED);
        aiming = false;
        currentTarget = null;
        //DebugOutput("Target lost or invalidated");
        if (wasPathing) {
            PathManagers.get().resume();
            wasPathing = false;
        }
        //DebugOutput("stopAiming called, aiming was: " + aiming, ChatFormatting.RED);
        shouldOverrideServerRotation = false;
        manualLockActive = false;
        lastLockKeyState = false;
        chargeTimerStarted = false;
        chargeTimer.reset();
        isInReleaseWait = false;
        //isInRetry = false;
        releaseTimer.reset();
        retryTimer.reset();
        SpearTarget.glowTarget = null;
        SpearTarget.glowEnabled = false;
    }

    /**
     * 目标过滤逻辑（使用 Mojang 映射的实体类）。
     * 要求实体必须直接可见（无 wallsRange），不可穿墙。
     */
    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity())) return false;
        if ((entity instanceof LivingEntity living && living.isDeadOrDying()) || !entity.isAlive()) return false;

        // 距玩家直线距离检查（使用 range 设定值）
        AABB hitbox = entity.getBoundingBox();
        if (!PlayerUtils.isWithin(
            Mth.clamp(mc.player.getX(), hitbox.minX, hitbox.maxX),
            Mth.clamp(mc.player.getY(), hitbox.minY, hitbox.maxY),
            Mth.clamp(mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
            range.get()
        )) return false;

        if (!entities.get().contains(entity.getType())) return false;
        if (ignoreNamed.get() && entity.hasCustomName()) return false;

        // 必须直接可见（不可穿墙）
        if (!PlayerUtils.canSeeEntity(entity)) return false;

        if (ignoreTamed.get()) {
            if (entity instanceof TamableAnimal tameable
                && tameable.getOwner() != null
                && tameable.getOwner().getUUID().equals(mc.player.getUUID())
            ) return false;
        }
        if (ignorePassive.get()) {
            if (entity instanceof EnderMan enderman && !enderman.isAngry()) return false;
            if ((entity instanceof Piglin || entity instanceof ZombifiedPiglin || entity instanceof Wolf)
                && !((Mob) entity).isAggressive()) return false;
        }
        if (entity instanceof Player player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
            if (player instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit) return false;
        }
        if (entity instanceof LivingEntity living) {
            // 敌对生物年龄筛选
            if (entity instanceof Zombie || entity instanceof Piglin
                || entity instanceof Hoglin || entity instanceof Zoglin) {
                return switch (hostileMobAgeFilter.get()) {
                    case Baby -> living.isBaby();
                    case Adult -> !living.isBaby();
                    case Both -> true;
                };
            }
            // 被动生物年龄筛选（AgeableMob 替代原 PassiveEntity）
            if (entity instanceof AgeableMob ageable && !(entity instanceof Frog || entity instanceof Parrot)) {
                return switch (passiveMobAgeFilter.get()) {
                    case Baby -> ageable.isBaby();
                    case Adult -> !ageable.isBaby();
                    case Both -> true;
                };
            }
        }
        return true;
    }

    private boolean isHoldingSpear() {
        Item item = mc.player.getMainHandItem().getItem();
        var key = BuiltInRegistries.ITEM.getResourceKey(item);
        return key.isPresent() && key.get().toString().endsWith("spear]");
    }

    private double distanceTo(Entity entity) {
        double dx = mc.player.getX() - entity.getX();
        double dy = mc.player.getY() - entity.getY();
        double dz = mc.player.getZ() - entity.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public Entity getTarget() {
        return currentTarget;
    }

    @Override
    public String getInfoString() {
        if (currentTarget != null) return EntityUtils.getName(currentTarget);
        return null;
    }

    private int getChargeResetMs() {
        Item item = mc.player.getMainHandItem().getItem();
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();

        int ms;
        switch (itemId) {
            case "minecraft:wooden_spear":
                ms = switch (resetStage.get()) {
                    case KeepEngaged   -> 5000;   // 5s
                    case KeepTired      -> 10000;  // 10s
                    case KeepDisengaged -> 15000;  // 15s
                    case CustomTick     -> customResetTick.get() * 50;
                };
                break;
            case "minecraft:golden_spear":
                ms = switch (resetStage.get()) {
                    case KeepEngaged   -> 3500;   // 3.5s
                    case KeepTired      -> 8500;   // 8.5s
                    case KeepDisengaged -> 13750;  // 13.75s
                    case CustomTick     -> customResetTick.get() * 50;
                };
                break;
            case "minecraft:stone_spear":
                ms = switch (resetStage.get()) {
                    case KeepEngaged   -> 4500;   // 4.5s
                    case KeepTired      -> 9000;   // 9s
                    case KeepDisengaged -> 13750;  // 13.75s
                    case CustomTick     -> customResetTick.get() * 50;
                };
                break;
            case "minecraft:copper_spear":
                ms = switch (resetStage.get()) {
                    case KeepEngaged   -> 4000;   // 4s
                    case KeepTired      -> 8250;   // 8.25s
                    case KeepDisengaged -> 12500;  // 12.5s
                    case CustomTick     -> customResetTick.get() * 50;
                };
                break;
            case "minecraft:iron_spear":
                ms = switch (resetStage.get()) {
                    case KeepEngaged   -> 2500;   // 2.5s
                    case KeepTired      -> 6750;   // 6.75s
                    case KeepDisengaged -> 11250;  // 11.25s
                    case CustomTick     -> customResetTick.get() * 50;
                };
                break;
            case "minecraft:diamond_spear":
                ms = switch (resetStage.get()) {
                    case KeepEngaged   -> 3000;   // 3s
                    case KeepTired      -> 6500;   // 6.5s
                    case KeepDisengaged -> 10000;  // 10s
                    case CustomTick     -> customResetTick.get() * 50;
                };
                break;
            case "minecraft:netherite_spear":
                ms = switch (resetStage.get()) {
                    case KeepEngaged   -> 2500;   // 2.5s
                    case KeepTired      -> 5500;   // 5.5s
                    case KeepDisengaged -> 8750;   // 8.75s
                    case CustomTick     -> customResetTick.get() * 50;
                };
                break;
            default:
                return -1;
        }
        return ms;
    }
    public void DebugOutput(String message) {
        DebugOutput(message, ChatFormatting.WHITE);
    }
    public void DebugOutput(String message,ChatFormatting color) {
        if (this.debugOutput.get() && this.debugMode.get()) {
            ChatUtils.sendMsg(Component.literal("[DonkeySpawnerSpearTargetDebug]" + message).withStyle(color));
        }
    }
}