package liuliuliu0127.donkeyspawner.addon.modules;  

//import liuliuliu0127.donkeyspawner.addon.utils.MathUtil;
import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
//import meteordevelopment.meteorclient.events.entity.RenderLivingEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
//import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.item.Item;
//import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
//import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.world.item.Items;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;

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

    private final SettingGroup sgAutoPlane = settings.createGroup("AutoPlane");

    private final Setting<Boolean> autoPlane = sgAutoPlane.add(new BoolSetting.Builder()
            .name("AutoPlane")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> autoPlaneY = sgAutoPlane.add(new IntSetting.Builder()
            .name("autoPlaneY")
            .range(-1000, 4000)
            .sliderRange(0, 400)
            .defaultValue(257)
            .visible(autoPlane::get)
            .build()
    );

    private final Setting<String> destinationX = sgAutoPlane.add(new StringSetting.Builder()
            .name("DestinationX")
            .defaultValue("0")
            .visible(autoPlane::get)
            .build()
    );

    private final Setting<String> destinationZ = sgAutoPlane.add(new StringSetting.Builder()
            .name("DestinationZ")
            .defaultValue("0")
            .visible(autoPlane::get)
            .build()
    );

    private final Setting<Boolean> toggleAutoPlane = sgAutoPlane.add(new BoolSetting.Builder()
            .name("toggleAutoPlane")
            .defaultValue(true)
            .visible(autoPlane::get)
            .build()
    );

    private final Setting<Boolean> playerDodge = sgAutoPlane.add(new BoolSetting.Builder()
            .name("playerDodge")
            .defaultValue(false)
            .visible(autoPlane::get)
            .build()
    );
    private final Setting<Boolean> antiFallDamage = sgMisc.add(new BoolSetting.Builder()
        .name("anti mount fall damage")
        .description("(not 100% work)Try to prevent your mount from taking fall damage when landing.")
        .defaultValue(true)
        .build()
    );

    // 新增：排除列表（船、快乐恶魂等）
    private final Setting<Set<EntityType<?>>> fallProtectionExcludes = sgMisc.add(new EntityTypeListSetting.Builder()
        .name("fall-protection-excludes")
        .description("Entities excluded from fall damage protection.")
        .defaultValue(Set.of(
            EntityType.BAMBOO_RAFT, EntityType.BAMBOO_CHEST_RAFT,
            EntityType.BIRCH_BOAT, EntityType.BIRCH_CHEST_BOAT,
            EntityType.CHERRY_BOAT, EntityType.CHERRY_CHEST_BOAT,
            EntityType.DARK_OAK_BOAT, EntityType.DARK_OAK_CHEST_BOAT,
            EntityType.ACACIA_BOAT, EntityType.ACACIA_CHEST_BOAT,
            EntityType.JUNGLE_BOAT, EntityType.JUNGLE_CHEST_BOAT,
            EntityType.HAPPY_GHAST
        ))
        .visible(antiFallDamage::get)
        .build()
    );

    private final Setting<Double> fallSafetyDistance = sgMisc.add(new DoubleSetting.Builder()
        .name("fall safety distance")
        .description("Distance above ground to start pulling up.")
        .defaultValue(2.5)
        .min(0.5)
        .sliderRange(0.5, 10)
        .visible(antiFallDamage::get)
        .build()
    );
    // 自动落地水总开关
    private final Setting<Boolean> autoWaterBucket = sgMisc.add(new BoolSetting.Builder()
        .name("auto water bucket")
        .description("Place water/powder snow to reset mount fall damage.")
        .defaultValue(true)
        .visible(antiFallDamage::get)        // 只在开启防摔时可见
        .build());

    // 静默切换（切换桶时不显示在快捷栏）
    private final Setting<Boolean> waterSilentSwitch = sgMisc.add(new BoolSetting.Builder()
        .name("switch back")
        .description("switch back when using the bucket.")
        .defaultValue(true)
        .visible(autoWaterBucket::get)
        .build());

    // 允许使用整个背包的桶（而非仅快捷栏）
    private final Setting<Boolean> waterInventorySwitch = sgMisc.add(new BoolSetting.Builder()
        .name("inventory switch")
        .description("Use buckets from inventory, not just hotbar.")
        .defaultValue(true)
        .visible(autoWaterBucket::get)
        .build());

    public final Setting<Boolean> scaleMount = sgMisc.add(new BoolSetting.Builder()
            .name("Scale Mount")
            .description("Makes the entity you are riding smaller to avoid blocking view.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Double> mountScale = sgMisc.add(new DoubleSetting.Builder()
            .name("Scale Size")
            .description("Scale level of the mounted entity.")
            .defaultValue(0.5)
            .range(0.0, 1.0)
            .sliderRange(0.0, 1.0)
            .visible(scaleMount::get)
            .build()
    );

    private final Setting<Boolean> scaleMountWithoutActivation = sgMisc.add(new BoolSetting.Builder()
            .name("Always Scale Mount")
            .description("When in DoubleTap mode, scale the mounted entity even if entity control is not activated (requires scale-mount=true).")
            .defaultValue(false)
            .visible(() -> scaleMount.get() && activationMode.get() == ActivationMode.DoubleTapSpace)
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

    private int waterState = 0;              // 0=空闲，1=已放水等待入水，2=收回中
    private int waterSlot = -1;              // 水桶所在的物品栏槽位
    private int prevSlot = -1;               // 切换前玩家的手持槽位
    private BlockPos waterPos = null;        // 放置的位置
    private int waterTimer = 0;              // 简单计时器（防止卡死）

    private boolean lastJumpPressed = false; 

    private int vehicleNullTicks = 0;

    private boolean persistentActive = false;          // 黏性激活标志（true时阻止自动关闭）
    private boolean wasRiding = false;                 // 上一 tick 是否有骑乘实体
    private Entity lastVehicle;

    public boolean forcePause = false;
    private Vec3 customMotion = null;
    private boolean resetFallDistance = true;//DEBUG的遗留产物

    private int waterSlotBackup = -1;   // 记录桶原本的背包槽位（用于归还）

    public void setForcePause(boolean pause) {
        this.forcePause = pause;
        if (!pause) this.customMotion = null;
    }

    public void applyCustomMotion(Vec3 motion) {
        if (forcePause) this.customMotion = motion;
    }

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
        if (lastVehicle != null && antiFallDamage.get() && resetFallDistance
            && !fallProtectionExcludes.get().contains(lastVehicle.getType())) {
            lastVehicle.fallDistance = 0;
        }
        lastVehicle = null;
        doubleTapActive = false;
    }

    public boolean isMountScaleEnabled() {
        return isActive() && scaleMount.get();
    }

    public float getMountScale() {
        return mountScale.get().floatValue();
    }

    public Entity getMountedEntity() {
        return mc.player != null ? mc.player.getVehicle() : null;
    }

    public boolean shouldScaleMount() {
        if (!isActive()) return false;
        if (!scaleMount.get()) return false;
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return false;
        if (!entities.get().contains(vehicle.getType())) return false;

        if (activationMode.get() == ActivationMode.Immediate) {
            return true; // 立即模式，骑乘即缩放
        } else { // DoubleTapSpace 模式
            // 如果未激活，但允许未激活时缩放，则返回 true
            if (!doubleTapActive && scaleMountWithoutActivation.get()) {
                return true;
            }
            return doubleTapActive;
        }
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
        // 落地水状态机（非骑乘或超时则放弃）
        // 落地水状态机（非骑乘或超时则放弃）
        if (waterState == 1) {
            Entity vehicle = mc.player.getVehicle();
            if (vehicle != null && entities.get().contains(vehicle.getType())) {
                tryRetrieveWater(vehicle);
                waterTimer++;
                if (waterTimer > 10) {
                    // 超时未收回则强制收回（万一水没生成）
                    if (waterPos != null) {
                        // 直接尝试收回
                        tryRetrieveWater(vehicle);
                    }
                    waterState = 0;
                    waterTimer = 0;
                }
            } else {
                waterState = 0; // 坐骑丢失
            }
        }
    }

    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.entity;
        if (entity.getControllingPassenger() != mc.player || !entities.get().contains(entity.getType())) return;
        if (forcePause) {//矛光环要用----------------------
            if (customMotion != null) {
                // 应用外部传入的移动向量
                ((IVec3d) event.movement).meteor$set(customMotion.x, customMotion.y, customMotion.z);
                if (lockYaw.get()) entity.setYRot(mc.player.getYRot());
            }
            return; // 跳过原有的按键移动逻辑
        }//矛光环要用------------------
        

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

        // 自动导航（如果未开启飞行模式，也强制水平移动）
        float autoYaw = calcAutoMoveYaw();
        boolean isAutoMoving = autoYaw != -999.0F;
        if (isAutoMoving) {
            double speedVal = horizontalSpeed.get() / 20.0;   // 每秒转每 tick
            // 重要: 必须将 autoYaw + 90° 才能得到正确的速度方向
            double rad = Math.toRadians(autoYaw + 90.0);
            double motionX = Math.cos(rad) * speedVal;
            double motionZ = Math.sin(rad) * speedVal;
            velX = motionX;
            velZ = motionZ;
            velY = 0;
            ((IVec3d) event.movement).meteor$set(velX, velY, velZ);
            return;
        }

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
        // ---------- 防摔保护（绝对防摔+落地水） ----------
        if (antiFallDamage.get() && velY < 0.0) {
            if (!fallProtectionExcludes.get().contains(entity.getType())) {
                Double surfaceY = getGroundSurfaceY(entity);
                if (surfaceY != null) {
                    double mountBottom = entity.getBoundingBox().minY;
                    double distToGround = mountBottom - surfaceY;
                    if (distToGround < fallSafetyDistance.get() && distToGround > 0.0) {
                        if (autoWaterBucket.get() && waterState == 0 && prepareWaterBucket()) {
                            if (tryStartWaterLanding(entity)) {
                                velY = 0.0;
                                ((IVec3d) event.movement).meteor$set(velX, velY, velZ);
                                return;
                            }
                        }
                        // 后备拉升
                        velY = 0.0;
                        entity.fallDistance = 0;
                    }
                }
            }
        }
        ((IVec3d) event.movement).meteor$set(velX, velY, velZ);
    }

    /**
     * 尝试启动落地水流程：找到桶、切换到手上、放置。
     * @return 是否成功启动（已放水）
     */
    private boolean tryStartWaterLanding(Entity vehicle) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return false;

        // 找到下方固体方块（用于贴水）
        BlockPos solidGround = findSolidGroundBelow(vehicle);
        if (solidGround == null) return false;

        // 水将生成在固体方块上方
        waterPos = solidGround.above();
        if (!mc.level.getBlockState(waterPos).isAir() && !mc.level.getBlockState(waterPos).canBeReplaced()) {
            return false;
        }

        // 此时手上已是水桶（prepareWaterBucket 成功），旋转视角到 solidGround 的顶面，然后直接使用水桶
        Rotations.rotate(
            Rotations.getYaw(solidGround),
            Rotations.getPitch(solidGround),
            10,                 // 优先级，避免与其他旋转冲突时可调高
            true,               // 是否返回原视角（true = 完成后恢复）
            () -> {
                // 确保手持水桶（因为旋转回调可能在稍后的 tick 执行，但通常立即）
                if (mc.player.getInventory().getSelectedSlot() != waterSlot) {
                    InvUtils.swap(waterSlot, waterSilentSwitch.get());
                }
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);  // 直接右键放水
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        );

        waterState = 1;   // 进入等待收回状态
        waterTimer = 0;
        return true;
    }

    /** 从实体脚底向下找第一个固体方块（不关心完整度，只要能放水即可） */
    private BlockPos findSolidGroundBelow(Entity entity) {
    if (mc.level == null) return null;
        int startY = Mth.floor(entity.getBoundingBox().minY - 0.1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int centerX = Mth.floor(entity.getX());
        int centerZ = Mth.floor(entity.getZ());
        for (int y = startY; y > startY - 10; y--) {
            pos.set(centerX, y, centerZ);
            BlockState state = mc.level.getBlockState(pos);
            if (!state.isAir() && state.isCollisionShapeFullBlock(mc.level, pos)) {
                return pos.immutable();
            }
        }
        return null;
    }
    /**
     * 从背包中找到水/细雪桶，切换到手上，并返回是否成功。
     * 如果开启 waterInventorySwitch，会搜索整个背包，必要时自动将桶移动到快捷栏。
     */
    private boolean prepareWaterBucket() {
        if (mc.player == null) return false;
        int maxSlot = waterInventorySwitch.get() ? 36 : 9;
        int bucketSlot = -1;
        for (int i = 0; i < maxSlot; i++) {
            String key = BuiltInRegistries.ITEM.getKey(mc.player.getInventory().getItem(i).getItem()).getPath();
            if (key.equals("water_bucket") || key.equals("powder_snow_bucket")) {
                bucketSlot = i;
                break;
            }
        }
        if (bucketSlot == -1) return false;

        prevSlot = mc.player.getInventory().getSelectedSlot();
        waterSlotBackup = bucketSlot;   // 记载桶的原始位置

        // 如果桶在背包深层（>=9）且开启了 InventorySwitch，先移到快捷栏
        if (bucketSlot >= 9 && waterInventorySwitch.get()) {
            // 找个空的快捷栏槽位
            int emptyHotbar = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).isEmpty()) {
                    emptyHotbar = i;
                    break;
                }
            }
            int targetSlot = (emptyHotbar != -1) ? emptyHotbar : prevSlot;
            InvUtils.move().from(bucketSlot).to(targetSlot);
            waterSlot = targetSlot;   // 桶现在在这里
        } else {
            waterSlot = bucketSlot;   // 桶就在快捷栏
        }

        // 静默切换到桶所在的快捷栏槽位
        InvUtils.swap(waterSlot, waterSilentSwitch.get());
        return true;
    }

    /**
     * 等待坐骑入水后收回空桶。
     * 应在 onPreTick 中调用（见后文）。
     */
    private void tryRetrieveWater(Entity vehicle) {
        if (mc.player == null || mc.level == null || waterPos == null) return;

        BlockState current = mc.level.getBlockState(waterPos);
        if (current.getFluidState().isEmpty()) {
            return; // 还没生成水源
        }

        // 寻找空桶
        int emptyBucketSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.BUCKET) {
                emptyBucketSlot = i;
                break;
            }
        }
        if (emptyBucketSlot == -1) return; // 没有空桶

        final int bucketSlot = emptyBucketSlot;
        Rotations.rotate(
            Rotations.getYaw(waterPos),
            Rotations.getPitch(waterPos),
            10,
            true,
            () -> {
                InvUtils.swap(bucketSlot, waterSilentSwitch.get());
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); // 收回水
                mc.player.swing(InteractionHand.MAIN_HAND);
                InvUtils.swapBack();  // 恢复原手持物品
            }
        );

        // 重置状态
        waterState = 0;
        waterSlot = -1;
        waterSlotBackup = -1;
        waterPos = null;
        waterTimer = 0;
    }
    /**
     * 获取实体下方最近的非空气方块表面高度（考虑半砖等不完整形状）。
     * @return 表面 y 坐标，若脚下全是空气则返回 null
     */
    private Double getGroundSurfaceY(Entity entity) {
        if (mc.level == null || entity == null) return null;
        AABB box = entity.getBoundingBox();
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);
        int startY = Mth.floor(box.minY - 0.05);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double highestSurface = Double.NEGATIVE_INFINITY;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // 向下最多找 5 格，通常足够
                for (int y = startY; y > startY - 5; y--) {
                    pos.set(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (!state.isAir()) {
                        VoxelShape shape = state.getCollisionShape(mc.level, pos);
                        if (!shape.isEmpty()) {
                            double surfaceY = y + shape.max(Direction.Axis.Y);
                            if (surfaceY > highestSurface) {
                                highestSurface = surfaceY;
                            }
                        }
                        break; // 找到第一个非空气方块就停止本列
                    }
                }
            }
        }
        return highestSurface == Double.NEGATIVE_INFINITY ? null : highestSurface;
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
            // 清零坠落伤害（坐骑已经安全落地，以防万一）
            if (lastVehicle != null && antiFallDamage.get() && resetFallDistance
                && !fallProtectionExcludes.get().contains(lastVehicle.getType())) {
                lastVehicle.fallDistance = 0;
            }
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
            // 新增：根据设置决定是否清零坠落距离
            if (lastVehicle != null && antiFallDamage.get() && resetFallDistance
                    && !fallProtectionExcludes.get().contains(lastVehicle.getType())) {
                lastVehicle.fallDistance = 0;
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
        Entity currentVehicle = mc.player.getVehicle();
        if (currentVehicle != null) {
            lastVehicle = currentVehicle;
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

    private float[] getLegitRotations(Vec3 vec) {
        Vec3 eyesPos = mc.player.getEyePosition();
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
        return new float[] {
            mc.player.getYHeadRot() + Mth.wrapDegrees(yaw - mc.player.getYHeadRot()),
            mc.player.getXRot() + Mth.wrapDegrees(pitch - mc.player.getXRot())
        };
    }

    private float calcAutoMoveYaw() {
        float yaw = -999.0F;
        if (autoPlane.get() && mc.player.getY() > autoPlaneY.get() && !isMoveBindPress() && !mc.options.keyJump.isDown()) {
            Double x = null;
            Double z = null;
            try {
                x = Double.valueOf(destinationX.get());
                z = Double.valueOf(destinationZ.get());
            } catch (NumberFormatException ignored) {}
            if (x == null || z == null) return -999.0F;
            Vec3 destination = new Vec3(x, mc.player.getY(), z);
            if (Math.sqrt(mc.player.distanceToSqr(destination)) > 40.0D) {
                float[] rotations = getLegitRotations(destination);
                yaw = rotations[0];
            } else if (toggleAutoPlane.get()) {
                autoPlane.set(false);
                // 修复: 关闭后立即返回 -999，不再继续执行 playerDodge
                return -999.0F;
            }
        }
        if (!autoPlane.get()) return -999.0F;
        // 修复: 只有当 autoPlane 仍为 true 且 yaw 无效时才尝试 playerDodge
        if (autoPlane.get() && playerDodge.get() && yaw == -999.0F && !isMoveBindPress() && !mc.options.keyJump.isDown()) {
            List<AbstractClientPlayer> players = mc.level.players().stream()
                    .filter(p -> mc.player.distanceTo(p) <= 16.0F && !mc.player.equals(p) && !Friends.get().shouldAttack(p))
                    .collect(Collectors.toList());
            players.sort(Comparator.comparingDouble(mc.player::distanceTo));
            if (!players.isEmpty()) {
                float[] rotations = getLegitRotations(players.get(0).position());
                yaw = rotations[0] + 180.0F;
            }
        }
        return yaw;
    }

    private boolean isMoveBindPress() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    // 添加矛光环方法
    public boolean isControlActive() {
        if (!isActive()) return false;
        if (activationMode.get() == ActivationMode.Immediate) return true;
        return doubleTapActive; // 直接访问自己的字段
    }
}