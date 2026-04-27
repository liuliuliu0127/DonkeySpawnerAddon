package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import java.util.*;

public class SpearAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAttack = settings.createGroup("Attack");

    // --- 通用 ---
    private final Setting<Set<EntityType<?>>> targetEntities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("target-entities").description("Entities to attack.").onlyAttackable().build());

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends").defaultValue(true).build());

    private final Setting<Double> maxRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-range").defaultValue(256).min(0).sliderRange(0, 512).build());

    // --- 攻击参数 ---
    private final Setting<Double> aboveHeight = sgAttack.add(new DoubleSetting.Builder()
        .name("above-height").description("Height above target center to hover before diving.")
        .defaultValue(10.0).min(3).sliderRange(3, 50).build());

    private final Setting<Double> aboveTriggerDistance = sgAttack.add(new DoubleSetting.Builder()
        .name("above-trigger-distance").description("Distance to hover point to start diving.")
        .defaultValue(2.0).min(0.5).sliderRange(0.5, 10).build());

    private final Setting<Double> horizontalSpeed = sgAttack.add(new DoubleSetting.Builder()
        .name("horizontal-speed").defaultValue(2.0).min(0.1).sliderRange(0.1, 10).build());

    private final Setting<Double> verticalSpeed = sgAttack.add(new DoubleSetting.Builder()
        .name("vertical-speed").defaultValue(3.0).min(0.1).sliderRange(0.1, 10).build());

    private final Setting<Integer> attackInterval = sgAttack.add(new IntSetting.Builder()
        .name("attack-interval").description("Ticks between two dives.").defaultValue(20).min(0).sliderRange(0, 60).build());

    private final Setting<Boolean> stopOnHit = sgAttack.add(new BoolSetting.Builder()
        .name("stop-on-hit").description("Stop movement when hitting target.").defaultValue(true).build());

    // --- 防摔保护 ---
    private final Setting<Boolean> fallProtection = sgAttack.add(new BoolSetting.Builder()
        .name("fall-protection").defaultValue(true).build());

    private final Setting<Set<EntityType<?>>> fallProtectionExcludes = sgAttack.add(new EntityTypeListSetting.Builder()
        .name("fall-protection-excludes").description("Entities excluded from fall protection (e.g. boats, happy ghast).")
        .defaultValue(Set.of(EntityType.BAMBOO_RAFT, EntityType.BAMBOO_CHEST_RAFT,
            EntityType.BIRCH_BOAT, EntityType.BIRCH_CHEST_BOAT, EntityType.CHERRY_BOAT, EntityType.CHERRY_CHEST_BOAT,
            EntityType.DARK_OAK_BOAT, EntityType.DARK_OAK_CHEST_BOAT, EntityType.ACACIA_BOAT, EntityType.ACACIA_CHEST_BOAT,
            EntityType.JUNGLE_BOAT, EntityType.JUNGLE_CHEST_BOAT, EntityType.HAPPY_GHAST))
        .visible(fallProtection::get).build());

    // --- 状态 ---
    private Entity killTarget;
    private int intervalTimer;
    private boolean diving;   // true: 俯冲中；false: 前往头顶/悬停

    public SpearAura() {
        super(DonkeySpawnerAddon.CATEGORY, "SpearAura", "Automatically spear entities from above while riding.");
    }

    @Override public void onActivate() { reset(); }
    @Override public void onDeactivate() { reset(); }

    private void reset() {
        killTarget = null;
        intervalTimer = 0;
        diving = false;
        restoreBEC();
    }

    private boolean isUsingSpear() {
        if (mc.player == null) return false;
        Item item = mc.player.getUseItem().getItem();
        return BuiltInRegistries.ITEM.getKey(item).getPath().contains("spear");
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        if (!isUsingSpear()) {
            killTarget = null;
            intervalTimer = 0;
            diving = false;
            restoreBEC();
            return;
        }

        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) {
            killTarget = null; restoreBEC(); return;
        }

        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec == null || !bec.isActive() || !bec.isControlActive()) {
            killTarget = null; restoreBEC(); return;
        }

        // 锁定目标
        if (killTarget == null || !killTarget.isAlive() || !isValidTarget(killTarget)) {
            killTarget = findTarget(vehicle);
            if (killTarget == null) {
                restoreBEC(); return;
            }
            diving = false;
            intervalTimer = 0;
        }

        // 暂停 BetterEntityControl 自主移动
        bec.setForcePause(true);

        Vec3 targetCenter = killTarget.getBoundingBox().getCenter();
        Vec3 mountPos = vehicle.position();

        // 头顶悬停点
        Vec3 hoverPos = new Vec3(targetCenter.x, targetCenter.y + aboveHeight.get(), targetCenter.z);

        // 如果处于攻击间隔，悬停并等待
        if (intervalTimer > 0) {
            intervalTimer--;
            // 悬停意味着移动到 hoverPos 并保持（不下降）
            Vec3 toHover = hoverPos.subtract(mountPos);
            if (toHover.length() > 0.5) {
                Vec3 move = toHover.normalize().scale(Math.min(toHover.length(), Math.max(horizontalSpeed.get(), verticalSpeed.get())));
                applyMotion(bec, vehicle, move);
            } else {
                vehicle.setDeltaMovement(Vec3.ZERO);
            }
            rotateToTarget(targetCenter);
            return;
        }

        // 判断是否到达头顶就位
        double horizDist = new Vec3(mountPos.x - hoverPos.x, 0, mountPos.z - hoverPos.z).length();
        double vertDist = mountPos.y - hoverPos.y;  // 正->在上方
        boolean atHover = horizDist < aboveTriggerDistance.get() && Math.abs(vertDist) < aboveTriggerDistance.get();

        if (!atHover) {
            // 前往悬停点
            diving = false;
            Vec3 toHover = hoverPos.subtract(mountPos);
            Vec3 move = toHover.normalize().scale(Math.max(horizontalSpeed.get(), verticalSpeed.get()));
            applyMotion(bec, vehicle, move);
            rotateToTarget(targetCenter);
            return;
        }

        // 已就位 → 俯冲攻击
        diving = true;
        Vec3 motion = new Vec3(0, -verticalSpeed.get(), 0); // 垂直向下

        // 水平修正：让准星对准目标（即坐骑水平位置尽可能靠近目标正上方）
        double dx = targetCenter.x - mountPos.x;
        double dz = targetCenter.z - mountPos.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        if (hDist > 0.1) {
            Vec3 horizCorrection = new Vec3(dx, 0, dz).normalize().scale(Math.min(horizontalSpeed.get(), hDist));
            motion = motion.add(horizCorrection);
        }

        // 防摔保护（不拉升排除列表内的实体）
        if (fallProtection.get() && !fallProtectionExcludes.get().contains(vehicle.getType()) && motion.y < 0) {
            BlockPos ground = getGroundPos(vehicle);
            if (ground != null) {
                double groundY = ground.getY() + 1.0;
                double mountBottom = vehicle.getBoundingBox().minY;
                double distGround = mountBottom - groundY;
                if (distGround < 2.5) {
                    double up = 1.0;
                    motion = new Vec3(motion.x, Math.max(motion.y, 0), motion.z).add(0, up, 0).normalize().scale(verticalSpeed.get());
                }
            }
        }

        applyMotion(bec, vehicle, motion);
        // 低头 90°，yaw 暂时无所谓，但可统一朝向目标
        rotateToTarget(targetCenter);

        // 命中检测
        if (mc.player.getBoundingBox().inflate(0.3).intersects(killTarget.getBoundingBox())) {
            if (stopOnHit.get()) {
                vehicle.setDeltaMovement(Vec3.ZERO);
            }
            intervalTimer = attackInterval.get();  // 攻击后停顿
            diving = false;
        }
    }

    // ---------- 移动 ----------
    private void applyMotion(BetterEntityControl bec, Entity vehicle, Vec3 motion) {
        bec.applyCustomMotion(motion);
        vehicle.setDeltaMovement(motion);
        vehicle.hurtMarked = true;
    }

    // ---------- 视角 ----------
    private void rotateToTarget(Vec3 targetCenter) {
        if (mc.player == null) return;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 dir = targetCenter.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90f;
        mc.player.setYRot(yaw);
        mc.player.setYHeadRot(yaw);
        // 不动 pitch，因为我们会在需要时设置为 -90°（低头90度）
        mc.player.setXRot(-90f);  // 完全低头
    }

    // ---------- 目标搜索（忽略坐骑） ----------
    private Entity findTarget(Entity vehicle) {
        if (mc.player == null || mc.level == null) return null;
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() != vehicle && isValidTarget(hit.getEntity()))
            return hit.getEntity();

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getViewVector(1f);
        AABB box = mc.player.getBoundingBox().expandTowards(look.scale(maxRange.get())).inflate(1.0);
        List<Entity> list = mc.level.getEntities(mc.player, box,
            e -> e instanceof LivingEntity && e.isAlive() && e != mc.player && e != vehicle && isValidTarget(e));
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingDouble(e -> eye.distanceToSqr(e.getBoundingBox().getCenter())));
        return list.get(0);
    }

    // ---------- 地面检测 ----------
    private BlockPos getGroundPos(Entity entity) {
        if (mc.level == null) return null;
        int y = Mth.floor(entity.getBoundingBox().minY - 0.1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(entity.getBoundingBox().minX); x <= Mth.floor(entity.getBoundingBox().maxX); x++)
            for (int z = Mth.floor(entity.getBoundingBox().minZ); z <= Mth.floor(entity.getBoundingBox().maxZ); z++) {
                pos.set(x, y, z);
                BlockState state = mc.level.getBlockState(pos);
                if (!state.isAir() && state.isCollisionShapeFullBlock(mc.level, pos)) return pos.immutable();
            }
        return null;
    }

    // ---------- BEC 恢复 ----------
    private void restoreBEC() {
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec != null) bec.setForcePause(false);
    }

    // ---------- 有效性检查 ----------
    private boolean isValidTarget(Entity e) {
        if (e == null) return false;
        if (e instanceof Player p && ignoreFriends.get() && Friends.get().isFriend(p)) return false;
        boolean inList = targetEntities.get().contains(e.getType());
        return targetEntities.get().isEmpty() || inList;  // 无实体时攻击所有，否则只攻击列表内
    }
}