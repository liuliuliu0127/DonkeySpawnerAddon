package liuliuliu0127.donkeyspawner.addon.modules;
import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import liuliuliu0127.donkeyspawner.addon.utils.ElytraControl;
import net.minecraft.world.entity.EntityType;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import net.minecraft.world.phys.AABB;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.*;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;

import java.util.function.Predicate;

import com.mojang.authlib.minecraft.client.MinecraftClient;

public class ElytraSpear extends Module {

    // --- 状态机定义 ---
    public enum State {
        IDLE,
        SEARCHING,
        TARGET_ALIGNED,
        WAITING_FOR_CHARGE,
        FLYING_TO_TARGET, 
        ATTACKING, RETREATING, 
        RETURNING
    }
    // --- 枚举生物类型 ---
    public enum EntityTypeFilter {
        All,
        Hostile,
        Passive
    }

    private State state = State.IDLE;
    private Entity currentTarget;
    private boolean freecamActive = false;

    // --- 设置组 ---
    //private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgAttack = settings.createGroup("Attack");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgLists = settings.createGroup("Lists");
    private final SettingGroup sgDebug = settings.createGroup("Debug");;

    // --- 目标搜索设置 ---
    private final Setting<Double> searchRadiusX = sgTargeting.add(new DoubleSetting.Builder()
        .name("search-radius-horizontal")
        .defaultValue(16.0).range(1.0, 64.0).sliderRange(1.0, 32.0)
        .build()
    );

    private final Setting<Double> searchRadiusY = sgTargeting.add(new DoubleSetting.Builder()
        .name("search-radius-vertical")
        .defaultValue(8.0).range(1.0, 32.0).sliderRange(1.0, 16.0)
        .build()
    );

    private final Setting<EntityTypeFilter> targetType = sgTargeting.add(new EnumSetting.Builder<EntityTypeFilter>()
        .name("target-type")
        .defaultValue(EntityTypeFilter.Hostile)
        .build()
    );

    private final Setting<Boolean> playersOnly = sgTargeting.add(new BoolSetting.Builder()
        .name("players-only")
        .defaultValue(false)
        .build()
    );

    // --- 黑白名单设置 (使用自定义 EntityListSetting 管理) ---
    private final Setting<Boolean> useBlacklist = sgLists.add(new BoolSetting.Builder()
        .name("use-blacklist")
        .defaultValue(false)
        .build()
    );

    private final Setting<Set<EntityType<?>>> excludedEntities = sgLists.add(new EntityTypeListSetting.Builder()
    .name("excluded-entities")
    .description("Entities that will NOT be targeted.")
    .defaultValue(Set.of(
        // 船 (Boats)
        EntityType.OAK_BOAT,
        EntityType.SPRUCE_BOAT,
        EntityType.BIRCH_BOAT,
        EntityType.JUNGLE_BOAT,
        EntityType.ACACIA_BOAT,
        EntityType.DARK_OAK_BOAT,
        EntityType.MANGROVE_BOAT,
        EntityType.CHERRY_BOAT,
        EntityType.BAMBOO_RAFT,
        // 运输船 (Chest Boats)
        EntityType.OAK_CHEST_BOAT,
        EntityType.SPRUCE_CHEST_BOAT,
        EntityType.BIRCH_CHEST_BOAT,
        EntityType.JUNGLE_CHEST_BOAT,
        EntityType.ACACIA_CHEST_BOAT,
        EntityType.DARK_OAK_CHEST_BOAT,
        EntityType.MANGROVE_CHEST_BOAT,
        EntityType.CHERRY_CHEST_BOAT,
        EntityType.BAMBOO_CHEST_RAFT,
        // 其他非攻击实体
        EntityType.EXPERIENCE_ORB,
        EntityType.ITEM,
        EntityType.ARROW,
        EntityType.SPECTRAL_ARROW,
        EntityType.FISHING_BOBBER,
        EntityType.PAINTING,
        EntityType.ITEM_FRAME,
        EntityType.GLOW_ITEM_FRAME
        ))
    .build()
    );


    // --- 攻击设置 ---
    private final Setting<Double> chargeTime = sgAttack.add(new DoubleSetting.Builder()
        .name("charge-time")
        .defaultValue(100.0).range(0.0, 500.0).sliderRange(0.0, 300.0)
        .build()
    );

    private final Setting<Double> attackDistance = sgAttack.add(new DoubleSetting.Builder()
        .name("attack-distance")
        .defaultValue(3.0).range(1.0, 10.0).sliderRange(1.0, 6.0)
        .build()
    );

    private final Setting<Double> retreatDistance = sgAttack.add(new DoubleSetting.Builder()
        .name("retreat-distance")
        .defaultValue(2.0).range(1.0, 10.0).sliderRange(1.0, 10.0)
        .build()
    );

    // --- 渲染设置 ---
    private final Setting<Boolean> renderTarget = sgRender.add(new BoolSetting.Builder()
        .name("render-target")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> targetColor = sgRender.add(new ColorSetting.Builder()
        .name("target-color")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(renderTarget::get)
        .build()
    );
    // --- DEBUG ---
    private final Setting<Boolean> debug= sgDebug.add(new BoolSetting.Builder()
        .name("Debug Mode")
        .description("Enable debug options for testing.")
        .defaultValue(false)
        .build()
    );
        // 子调试开关（仅在 debugMode 开启时可见）
    private final Setting<Boolean> debugOutput = sgDebug.add(new BoolSetting.Builder()
        .name("DebugOutput")
        .description("Output debug information to the console.")
        .defaultValue(false)
        .visible(debug::get)
        .build()
    );

    // --- 内部变量 ---
    private long chargeStartTime;
    private double freecamX, freecamZ;
    private boolean wasSpearRaised;

    public ElytraSpear() {
        super(DonkeySpawnerAddon.CATEGORY, "ElytraSpear[UNFINISHED]", "Automated spear thrust attacks while elytra flying.");
    }

    @Override
    public void onActivate() {
        //ElytraControl.setRealPlayer(mc.player);
        state = State.IDLE;
        currentTarget = null;
        freecamActive = false;
        wasSpearRaised = false;
    }

    @Override
    public void onDeactivate() {
        //ElytraControl.clearRealPlayer();
        if (freecamActive) {
            disableFreecam();
        }
        state = State.IDLE;
        currentTarget = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 检查前置条件
        boolean elytraFlyActive = Modules.get().isActive(ElytraFly.class);
        boolean flying = mc.player.isFallFlying();
        boolean holdingSpear = isHoldingSpear(mc.player.getMainHandItem());
        boolean conditionsMet = elytraFlyActive && flying && holdingSpear;

        if (!conditionsMet) {
            if (state != State.IDLE) {
                state = State.IDLE;
                currentTarget = null;
                if (freecamActive) disableFreecam();
            }
            return;
        }

        // 状态机主循环
        switch (state) {
            case IDLE:
            case SEARCHING:
                // 搜索目标
                currentTarget = findTarget();
                if (currentTarget != null) {
                    state = State.TARGET_ALIGNED;
                } else {
                    state = State.IDLE;
                }
                break;

            case TARGET_ALIGNED:
                // 检查是否对准目标
                if (!isAimedAt(currentTarget)) {
                    currentTarget = findTarget();
                    if (currentTarget == null) {
                        state = State.IDLE;
                    }
                }
                // 此处可以添加等待玩家举矛的逻辑，进入下一状态
                break;

            default:
                break;
        }
    }

    // --- 辅助方法 ---
    private boolean isHoldingSpear(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.WOODEN_SPEAR ||
            item == Items.STONE_SPEAR ||
            item == Items.IRON_SPEAR ||
            item == Items.GOLDEN_SPEAR ||
            item == Items.DIAMOND_SPEAR ||
            item == Items.NETHERITE_SPEAR ||
            item == Items.COPPER_SPEAR;
}

    private void enableFreecam() {
        //ElytraControl.setRealPlayer(mc.player); // 在切换前保存
        Modules.get().get(Freecam.class).toggle();
        freecamActive = true;
    }

    private void disableFreecam() {
        Modules.get().get(Freecam.class).toggle();
        freecamActive = false;
    }

    private Entity findTarget() {
        
        Minecraft mc = Minecraft.getInstance();
        //DebugOutput("findTarget called, player=" + (mc.player != null) + ", level=" + (mc.level != null));
        if (mc.player == null || mc.level == null) return null;

        double maxDistX = searchRadiusX.get();
        double maxDistY = searchRadiusY.get();
        //DebugOutput("Search radius: X=" + maxDistX + ", Y=" + maxDistY);

        Predicate<Entity> filter = getEntityFilter();
        Entity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        int totalEntities = 0;
        int passedFilter = 0;
        int passedDistance = 0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!filter.test(entity)) continue;

            double dx = entity.getX() - mc.player.getX();
            double dy = entity.getY() - mc.player.getY();
            double dz = entity.getZ() - mc.player.getZ();

            // 检查距离限制
            if (Math.abs(dx) > maxDistX || Math.abs(dz) > maxDistX || Math.abs(dy) > maxDistY) continue;

            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < bestDistance) {
                bestDistance = distSq;
                bestTarget = entity;
            }
        }
        //DebugOutput("Entities: total=" + totalEntities + ", passedFilter=" + passedFilter + ", passedDistance=" + passedDistance);
        //DebugOutput("Best target: " + (bestTarget != null ? bestTarget.getName().getString() : "null"));

        return bestTarget;
    }

    private Predicate<Entity> getEntityFilter() {
        Minecraft mc = Minecraft.getInstance();
        return entity -> {
            // 排除玩家自身
            if (entity == mc.player) return false;
            // 只考虑生物实体（排除物品、经验球等已在 excludedEntities 中过滤）
            if (!(entity instanceof LivingEntity)) return false;

            // 排除名单检查
            Set<EntityType<?>> excluded = excludedEntities.get();
            if (excluded.contains(entity.getType())) {
                return false;
            }

            // 仅玩家模式
            if (playersOnly.get()) {
                return entity instanceof Player;
            }

            // 类型过滤（敌对/被动）
            EntityTypeFilter filter = targetType.get();
            if (filter == EntityTypeFilter.Hostile && !(entity instanceof Monster)) return false;
            if (filter == EntityTypeFilter.Passive && !(entity instanceof Animal)) return false;

            return true;
        };
    }

    private boolean isAimedAt(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        double reach = searchRadiusX.get(); // 使用设置的最大水平搜索距离
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 viewVec = mc.player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach);

        // 构建一个覆盖射线路径的 AABB 用于快速筛选
        AABB searchBox = mc.player.getBoundingBox().expandTowards(viewVec.scale(reach)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            mc.player,
            eyePos,
            endPos,
            searchBox,
            entity -> !entity.isSpectator() && entity.isPickable(),
            reach * reach
        );

        return hit != null && hit.getEntity() == target;
    }

    private void updateFreecamPosition() {
        if (freecamActive) {
            Entity camera = Minecraft.getInstance().getCameraEntity();
            if (camera != null) {
                freecamX = camera.getX();
                freecamZ = camera.getZ();
            }
        }
    }

    // --- 渲染目标 ---
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderTarget.get() || currentTarget == null || !isAimedAt(currentTarget)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 计算透明度（最小80）
        double dist = mc.player.distanceTo(currentTarget);
        Color color = targetColor.get();
        int alpha = (int) (color.a * Math.max(0.0, Math.min(1.0, 1.0 - dist / searchRadiusX.get())));
        alpha = Math.max(alpha, 80);
        Color renderColor = new Color(color.r, color.g, color.b, alpha);

        drawTargetBox(event, currentTarget, renderColor);
    }

    private void drawTargetBox(Render3DEvent event, Entity target, Color color) {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera == null) camera = mc.player;
        if (target == null) return;

        AABB box = target.getBoundingBox();
        double minX = box.minX - camera.getX();
        double minY = box.minY - camera.getY();
        double minZ = box.minZ - camera.getZ();
        double maxX = box.maxX - camera.getX();
        double maxY = box.maxY - camera.getY();
        double maxZ = box.maxZ - camera.getZ();

        PoseStack poseStack = event.matrices;
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderTypes.LINES);

        float r = color.r / 255.0f;
        float g = color.g / 255.0f;
        float b = color.b / 255.0f;
        float a = color.a / 255.0f;

        // 绘制12条边，每条边调用辅助方法，自动设置法线和线宽
        drawLine(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawLine(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawLine(consumer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        drawLine(consumer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        drawLine(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(consumer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawLine(consumer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        drawLine(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawLine(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);

        mc.renderBuffers().bufferSource().endBatch(RenderTypes.LINES);
    }

    private void drawLine(VertexConsumer consumer, Matrix4f matrix,
                        double x1, double y1, double z1,
                        double x2, double y2, double z2,
                        float r, float g, float b, float a) {
        consumer.addVertex(matrix, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(0, 1, 0)   // 必须设置法线
                .setLineWidth(1.0f);  // 必须设置线宽
        consumer.addVertex(matrix, (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(0, 1, 0)
                .setLineWidth(1.0f);
    }

    public void DebugOutput(String message) {
        DebugOutput(message, ChatFormatting.WHITE);
    }
    public void DebugOutput(String message,ChatFormatting color) {
        if (this.debugOutput.get() && this.debug.get()) {
            ChatUtils.sendMsg(Component.literal("[DonkeySpawnerElytraSpearDebug]" + message).withStyle(color));
        }
    }
}
