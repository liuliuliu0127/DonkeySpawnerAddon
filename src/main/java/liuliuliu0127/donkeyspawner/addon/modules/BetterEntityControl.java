package liuliuliu0127.donkeyspawner.addon.modules;  // 请根据你的实际包名修改

import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
//import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
//import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl.ActivationMode;

import static org.lwjgl.glfw.GLFW.*;
//import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl.ControlMode;

public class BetterEntityControl extends Module {
    private final SettingGroup sgControl = settings.createGroup("Control");
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgFlight = settings.createGroup("Flight");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    // ---------- 控制模式枚举 ----------
    public enum ControlMode {
        Tradition,   // 传统模式（可自定义下降键）
        HappyGhast   // 快乐恶魂模式（视角方向移动）
    }

    // ---------- 激活模式枚举 ----------
    public enum ActivationMode {
        Immediate,        // 立即生效
        DoubleTapSpace    // 双击空格切换
    }

    // ---------- Control 组 ----------
    private final Setting<Set<EntityType<?>>> entities = sgControl.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Target entities.")
        .filter(entityType -> EntityUtils.isRideable(entityType) && entityType != EntityType.MINECART && entityType != EntityType.LLAMA && entityType != EntityType.TRADER_LLAMA)
        .defaultValue(getAllRideableEntities())
        .build()
    );

    private final Setting<Boolean> spoofSaddle = sgControl.add(new BoolSetting.Builder()
        .name("spoof-saddle*")
        .description("Lets you control rideable entities without them being saddled. Only works on older server versions.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> maxJump = sgControl.add(new BoolSetting.Builder()
        .name("max-jump")
        .description("Sets jump power to maximum.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> lockYaw = sgControl.add(new BoolSetting.Builder()
        .name("lock-yaw")
        .description("Locks the Entity's yaw.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cancelServerPackets = sgControl.add(new BoolSetting.Builder()
        .name("cancel-server-packets")
        .description("Cancels incoming vehicle move packets. WILL desync you from the server if you make an invalid movement.")
        .defaultValue(false)
        .build()
    );

    // ---------- 新增：控制模式 ----------
    private final Setting<ControlMode> controlMode = sgControl.add(new EnumSetting.Builder<ControlMode>()
        .name("control-mode")
        .description("Flight control mode.")
        .defaultValue(ControlMode.Tradition)
        .build()
    );

    // ---------- 新增：激活模式 ----------
    private final Setting<ActivationMode> activationMode = sgControl.add(new EnumSetting.Builder<ActivationMode>()
        .name("activation-mode")
        .description("How to activate the control.")
        .defaultValue(ActivationMode.Immediate)
        .build()
    );

    private final Setting<Boolean> activationMessage = sgControl.add(new BoolSetting.Builder()
        .name("activation-message")
        .description("send a message when toggling")
        .defaultValue(true)
        .visible(() -> activationMode.get() == ActivationMode.DoubleTapSpace)
        .build()
    );

    private final Setting<Integer> dismountResetDelay = sgControl.add(new IntSetting.Builder()
        .name("dismount-reset-delay")
        .description("How many ticks of being dismounted before deactivating DoubleTap mode entity control (to prevent lagback glitches).")
        .defaultValue(10)
        .range(1, 50)
        .sliderRange(1, 200)
        .visible(() -> activationMode.get() == ActivationMode.DoubleTapSpace)
        .build()
    );

    private final Setting<Boolean> persistentUntilDismount = sgControl.add(new BoolSetting.Builder()
        .name("persistent-until-dismount")
        .description("Entity control stays active until you manually dismount (shift) or double-tap space again(to prevent lagback glitches).")
        .defaultValue(true)
        .visible(() -> activationMode.get() == ActivationMode.DoubleTapSpace)
        .build()
    );

    // ---------- Tradition 模式专用：下降键 ----------
    private final Setting<Keybind> descendKey = sgControl.add(new KeybindSetting.Builder()
        .name("descend-key")
        .description("Key used to descend in Tradition mode.")
        .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_CONTROL))
        .visible(() -> controlMode.get() == ControlMode.Tradition)
        .build()
    );

    // ---------- Speed 组 ----------
    private final Setting<Boolean> speed = sgSpeed.add(new BoolSetting.Builder()
        .name("speed")
        .description("Makes you go faster horizontally when riding entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> horizontalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(100)
        .min(0)
        .sliderMax(1000)
        .visible(speed::get)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgSpeed.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Use speed only when standing on a block.")
        .defaultValue(false)
        .visible(speed::get)
        .build()
    );

    private final Setting<Boolean> inWater = sgSpeed.add(new BoolSetting.Builder()
        .name("in-water")
        .description("Use speed when in water.")
        .defaultValue(true)
        .visible(speed::get)
        .build()
    );

    // ---------- Flight 组 ----------
    private final Setting<Boolean> flight = sgFlight.add(new BoolSetting.Builder()
        .name("fly")
        .description("Allows you to fly with entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgFlight.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(20)
        .min(0)
        .sliderMax(50)
        .visible(flight::get)
        .build()
    );

    private final Setting<Double> fallSpeed = sgFlight.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast you will fall in blocks per second. Set to a small value to prevent fly kicks.")
        .defaultValue(0)
        .min(0)
        .visible(flight::get)
        .build()
    );

    private final Setting<Boolean> antiKick = sgFlight.add(new BoolSetting.Builder()
        .name("anti-fly-kick")
        .description("Whether to prevent the server from kicking you for flying.")
        .defaultValue(true)
        .visible(flight::get)
        .build()
    );

    private final Setting<Integer> delay = sgFlight.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay, in ticks, between flying down a bit and return to original position")
        .defaultValue(40)
        .min(1)
        .sliderMax(80)
        .visible(() -> flight.get() && antiKick.get())
        .build()
    );

    public final Setting<Boolean> transparentMount = sgMisc.add(new BoolSetting.Builder()
            .name("transparent-mount[not working now]")
            .description("Makes the entity you are riding transparent to avoid blocking view.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Double> mountAlpha = sgMisc.add(new DoubleSetting.Builder()
            .name("mount-alpha")
            .description("Transparency level of the mounted entity (0 = fully transparent, 1 = fully opaque).")
            .defaultValue(0.3)
            .range(0.0, 1.0)
            .sliderRange(0.0, 1.0)
            .visible(transparentMount::get)
            .build()
    );

    // ---------- 内部状态 ----------
    private int delayLeft;
    private double lastPacketY = Double.MAX_VALUE;
    private boolean sentPacket = false;

    // 双击空格检测相关
    private long lastSpacePressTime = 0;
    private boolean doubleTapActive = false;
    private static final long DOUBLE_TAP_DELAY = 250;

    private boolean lastJumpPressed = false; 

    private int vehicleNullTicks = 0;

    private boolean persistentActive = false;          // 黏性激活标志（true时阻止自动关闭）
    private boolean wasRiding = false;                 // 上一 tick 是否有骑乘实体

    public BetterEntityControl() {
        super(DonkeySpawnerAddon.CATEGORY, "better-entity-control", "Improved Meteor official Entity control for more flexibility");
    }

    private EntityType<?>[] getAllRideableEntities() {
        List<EntityType<?>> list = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.forEach(entityType -> {
            if (EntityUtils.isRideable(entityType) && entityType != EntityType.MINECART && entityType != EntityType.LLAMA && entityType != EntityType.TRADER_LLAMA) {
                list.add(entityType);
            }
        });
        return list.toArray(new EntityType<?>[0]);
    }

    @Override
    public void onActivate() {
        delayLeft = delay.get();
        sentPacket = false;
        lastPacketY = Double.MAX_VALUE;
        doubleTapActive = false;
        lastSpacePressTime = 0;
    }

    @Override
    public void onDeactivate() {
        doubleTapActive = false;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (sentPacket && mc.player.getVehicle() != null) {
            Entity vehicle = mc.player.getVehicle();
            Vec3 newPos = new Vec3(vehicle.getX(), lastPacketY, vehicle.getZ());
            ServerboundMoveVehiclePacket packet = new ServerboundMoveVehiclePacket(newPos, vehicle.getYRot(), vehicle.getXRot(), vehicle.onGround());
            mc.player.connection.send(packet);
            sentPacket = false;
        }
        delayLeft -= 1;
    }

    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.entity;
        if (entity.getControllingPassenger() != mc.player || !entities.get().contains(entity.getType())) return;

        // 检查是否应该控制
        boolean shouldControl = false;
        if (activationMode.get() == ActivationMode.Immediate) {
            shouldControl = true;
        } else if (activationMode.get() == ActivationMode.DoubleTapSpace) {
            shouldControl = doubleTapActive;
        }
        if (!shouldControl) return;

        double velX = entity.getDeltaMovement().x;
        double velY = entity.getDeltaMovement().y;
        double velZ = entity.getDeltaMovement().z;

        // ----- 水平移动（速度加速）-----
        if (speed.get() && (!onlyOnGround.get() || entity.onGround() || entity.isFlyingVehicle()) && (inWater.get() || !entity.isInWater())) {
            Vec3 vel = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());
            velX = vel.x;
            velZ = vel.z;
        }

        // ----- 垂直移动（飞行模式）-----
        if (flight.get()) {
            velY = 0;
            ControlMode mode = controlMode.get();

            if (mode == ControlMode.Tradition) {
                // 传统模式：跳跃上升，自定义键下降
                if (Input.isPressed(mc.options.keyJump)) {
                    velY += verticalSpeed.get() / 20;
                }
                if (descendKey.get().isPressed()) {
                    velY -= verticalSpeed.get() / 20;
                } else {
                    velY -= fallSpeed.get() / 20;
                }
            } else if (mode == ControlMode.HappyGhast) {
                // 快乐恶魂模式：按W/S沿视线方向前后移动，A/D水平侧移，跳跃垂直上升
                Vec3 lookVec = mc.player.getLookAngle(); // 单位向量，包含俯仰
                Vec3 horizontalLook = new Vec3(lookVec.x, 0, lookVec.z).normalize();
                Vec3 left = horizontalLook.cross(new Vec3(0, 1, 0)).normalize(); // 左向量
                Vec3 up = new Vec3(0, 1, 0);

                double moveForward = 0;
                double moveRight = 0;
                double moveUp = 0;

                if (Input.isPressed(mc.options.keyUp)) moveForward += 1;
                if (Input.isPressed(mc.options.keyDown)) moveForward -= 1;
                if (Input.isPressed(mc.options.keyRight)) moveRight += 1;
                if (Input.isPressed(mc.options.keyLeft)) moveRight -= 1;
                if (Input.isPressed(mc.options.keyJump)) moveUp += 1;

                if (moveForward != 0 || moveRight != 0 || moveUp != 0) {
                    // 前后移动：沿视线方向（包括垂直分量）
                    Vec3 forwardVec = lookVec.scale(moveForward);
                    // 左右移动：水平侧移
                    Vec3 rightVec = left.scale(moveRight); // 因为 left 是左向量，取负得右
                    // 垂直移动：直接向上/下
                    Vec3 upVec = up.scale(moveUp);

                    Vec3 moveVec = forwardVec.add(rightVec).add(upVec).normalize();
                    double horizontalSpeedVal = horizontalSpeed.get() / 20;
                    double verticalSpeedVal = verticalSpeed.get() / 20;

                    velX = moveVec.x * horizontalSpeedVal;
                    velY = moveVec.y * verticalSpeedVal;
                    velZ = moveVec.z * horizontalSpeedVal;
                } else {
                    // 没有输入时缓慢减速
                    velX = 0;
                    velY = -fallSpeed.get() / 20;
                    velZ = 0;
                }
            }
        }

        if (lockYaw.get()) entity.setYRot(mc.player.getYRot());
        ((IVec3d) event.movement).meteor$set(velX, velY, velZ);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMoveVehiclePacket packet) || !antiKick.get()) return;

        double currentY = packet.position().y;
        Entity vehicle = mc.player.getVehicle();
        if (delayLeft <= 0 && !sentPacket && shouldFlyDown(currentY) && vehicle != null && !vehicle.onGround() && !vehicle.isFlyingVehicle()) {
            // 修改 Y 坐标，构造新包
            Vec3 newPos = new Vec3(packet.position().x, lastPacketY - 0.03130D, packet.position().z);
            ServerboundMoveVehiclePacket newPacket = new ServerboundMoveVehiclePacket(newPos, packet.yRot(), packet.xRot(), packet.onGround());
            event.cancel(); // 取消原始包
            mc.player.connection.send(newPacket);
            sentPacket = true;
            delayLeft = delay.get();
            return;
        }
        lastPacketY = currentY;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundMoveVehiclePacket && cancelServerPackets.get()) {
            event.cancel();
        }
    }

    private boolean shouldFlyDown(double currentY) {
        if (currentY >= lastPacketY) return true;
        return lastPacketY - currentY < 0.03130D;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // 双击空格检测（仅当激活模式为 DoubleTapSpace 且玩家骑乘目标实体时）
        if (activationMode.get() == ActivationMode.DoubleTapSpace && mc.player.getVehicle() != null && entities.get().contains(mc.player.getVehicle().getType())) {
            boolean jumpPressed = mc.options.keyJump.isDown();
            if (jumpPressed && !lastJumpPressed) {
                long now = System.currentTimeMillis();
                if (now - lastSpacePressTime <= DOUBLE_TAP_DELAY) {
                    doubleTapActive = !doubleTapActive;
                    // 新增：同步黏性标志
                    if (persistentUntilDismount.get()) {
                        persistentActive = doubleTapActive;
                    }
                    // 可选：发送提示消息
                    if(activationMessage.get()){
                        meteordevelopment.meteorclient.utils.player.ChatUtils.sendMsg(
                            Component.literal(doubleTapActive ? "[DonkeySpawner] DoubleTapSpaceMode EntityControl ACTIVATED" : "[DonkeySpawner] DoubleTapSpaceMode EntityControl DEACTIVATED")
                            .withStyle(doubleTapActive ? ChatFormatting.GREEN : ChatFormatting.RED)
                        );
                    }
                }
                lastSpacePressTime = now;
            }
            lastJumpPressed = jumpPressed;
        }
        // 当玩家离开载具时，延迟重置双击激活状态（避免回弹导致的短暂脱离）
        /*if (mc.player.getVehicle() == null) {
            vehicleNullTicks++;
            if (vehicleNullTicks >= dismountResetDelay.get()) { // 连续 2 tick 都没有载具，才认为是真正下马
                doubleTapActive = false;
                vehicleNullTicks = 0;
            }
        } else {
            vehicleNullTicks = 0; // 有载具时重置计数器
        }*/

        // 检测主动下马（Shift 下马）
        boolean currentlyRiding = mc.player.getVehicle() != null;
        boolean shiftPressed = mc.options.keyShift.isDown();

        if (wasRiding && !currentlyRiding && shiftPressed) {
            // 玩家主动按 Shift 下马
            if (persistentUntilDismount.get() && persistentActive) {
                doubleTapActive = false;
                persistentActive = false;
                if (activationMessage.get()) {
                    meteordevelopment.meteorclient.utils.player.ChatUtils.sendMsg(
                        Component.literal("[DonkeySpawner] EntityControl DEACTIVATED due to dismount.")
                        .withStyle(ChatFormatting.RED)
                    );
                }
            }
        }
        wasRiding = currentlyRiding;

        // 原有的延迟重置逻辑（仅在不处于黏性模式时生效）
        if (mc.player.getVehicle() == null) {
            vehicleNullTicks++;
            if (vehicleNullTicks >= dismountResetDelay.get()) {
                // 只有非黏性模式才自动关闭；黏性模式下保持 doubleTapActive = true
                if (!(persistentUntilDismount.get() && persistentActive)) {
                    doubleTapActive = false;
                    persistentActive = false;   // 清理黏性标志
                }
                vehicleNullTicks = 0;
            }
        } else {
            vehicleNullTicks = 0;
        }
    }

    // ----- 外部调用接口（保持与原版兼容）-----
    public boolean spoofSaddle() {
        return isActive() && spoofSaddle.get();
    }

    public boolean maxJump() {
        return isActive() && maxJump.get();
    }

    public boolean cancelJump() {
        // 如果飞行模式开启且模块激活，且玩家骑乘的实体在目标列表中，则取消实体的默认跳跃（因为我们要用跳跃上升）
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return false;
        return isActive() && flight.get() && entities.get().contains(vehicle.getType());
    }
}