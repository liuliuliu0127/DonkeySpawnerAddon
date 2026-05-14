package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.mixin.EntityVelocityUpdateS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BetterVelocity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- 击退 ---
    public final Setting<Boolean> knockback = sgGeneral.add(new BoolSetting.Builder()
        .name("knockback")
        .description("Modifies the amount of knockback you take from attacks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> knockbackHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-horizontal")
        .description("How much horizontal knockback you will take.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(knockback::get)
        .build()
    );

    public final Setting<Double> knockbackVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-vertical")
        .description("How much vertical knockback you will take.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(knockback::get)
        .build()
    );

    // --- 爆炸 ---
    public final Setting<Boolean> explosions = sgGeneral.add(new BoolSetting.Builder()
        .name("explosions")
        .description("Modifies your knockback from explosions.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> explosionsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-horizontal")
        .description("How much velocity you will take from explosions horizontally.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(explosions::get)
        .build()
    );

    public final Setting<Double> explosionsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-vertical")
        .description("How much velocity you will take from explosions vertically.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(explosions::get)
        .build()
    );

    // --- 自身风爆 (风弹 / 重锤) ---
    public final Setting<Boolean> selfWindBurst = sgGeneral.add(new BoolSetting.Builder()
        .name("self-wind-burst")
        .description("Modifies your knockback from your own wind bursts (wind charges / mace).")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> selfWindBurstHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("self-wind-burst-horizontal")
        .description("How much horizontal velocity you will take from your own wind bursts.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(selfWindBurst::get)
        .build()
    );

    public final Setting<Double> selfWindBurstVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("self-wind-burst-vertical")
        .description("How much vertical velocity you will take from your own wind bursts.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(selfWindBurst::get)
        .build()
    );

    // --- 液体 ---
    public final Setting<Boolean> liquids = sgGeneral.add(new BoolSetting.Builder()
        .name("liquids")
        .description("Modifies the amount you are pushed by flowing liquids.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> liquidsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-horizontal")
        .description("How much velocity you will take from liquids horizontally.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(liquids::get)
        .build()
    );

    public final Setting<Double> liquidsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-vertical")
        .description("How much velocity you will take from liquids vertically.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(liquids::get)
        .build()
    );

    // --- 实体推挤 ---
    public final Setting<Boolean> entityPush = sgGeneral.add(new BoolSetting.Builder()
        .name("entity-push")
        .description("Modifies the amount you are pushed by entities.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> entityPushAmount = sgGeneral.add(new DoubleSetting.Builder()
        .name("entity-push-amount")
        .description("How much you will be pushed.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(entityPush::get)
        .build()
    );

    // --- 方块 ---
    public final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
        .name("blocks")
        .description("Prevents you from being pushed out of blocks.")
        .defaultValue(true)
        .build()
    );

    // --- 液体下沉 ---
    public final Setting<Boolean> sinking = sgGeneral.add(new BoolSetting.Builder()
        .name("sinking")
        .description("Prevents you from sinking in liquids.")
        .defaultValue(false)
        .build()
    );

    // --- 鱼竿 ---
    public final Setting<Boolean> fishing = sgGeneral.add(new BoolSetting.Builder()
        .name("fishing")
        .description("Prevents you from being pulled by fishing rods.")
        .defaultValue(false)
        .build()
    );

    private int windBurstTimer = 0;

    public BetterVelocity() {
        super(DonkeySpawnerAddon.CATEGORY, "BetterVelocity",
            "Prevents you from being moved by external forces, with custom wind burst handling.");
    }

    @Override
    public void onActivate() {
        windBurstTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        // 处理液体下沉
        if (sinking.get()) {
            if (!mc.options.keyJump.isDown() && !mc.options.keyShift.isDown()) {
                if ((mc.player.isInWater() || mc.player.isInLava()) && mc.player.getDeltaMovement().y < 0) {
                    ((IVec3d) (Object) mc.player.getDeltaMovement()).meteor$setY(0);
                }
            }
        }

        // 检测并更新风爆计时器
        if (selfWindBurst.get()) {
            ItemStack main = mc.player.getMainHandItem();
            ItemStack off  = mc.player.getOffhandItem();

            boolean holdingWindCharge = main.getItem() == Items.WIND_CHARGE || off.getItem() == Items.WIND_CHARGE;
            boolean holdingMace = main.getItem() == Items.MACE || off.getItem() == Items.MACE;
            boolean hasWindBurst = holdingMace && main.getEnchantments()
                .getLevel(mc.player.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.WIND_BURST)) > 0;

            if (holdingWindCharge || hasWindBurst) {
                if (mc.options.keyUse.isDown() || mc.options.keyAttack.isDown()) {
                    windBurstTimer = 10;
                }
            }

            if (windBurstTimer > 0) {
                windBurstTimer--;
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (knockback.get() && event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            Vec3 original = packet.getMovement();
            double velX = (original.x - mc.player.getDeltaMovement().x) * knockbackHorizontal.get();
            double velY = (original.y - mc.player.getDeltaMovement().y) * knockbackVertical.get();
            double velZ = (original.z - mc.player.getDeltaMovement().z) * knockbackHorizontal.get();
            ((IVec3d) (Object) mc.player.getDeltaMovement()).meteor$set(
                mc.player.getDeltaMovement().x + velX,
                mc.player.getDeltaMovement().y + velY,
                mc.player.getDeltaMovement().z + velZ
            );
            event.cancel(); // 阻止原始包生效，避免服务端再次改变速度
        }

        // ---------- 爆炸 (包含自身风爆) ----------
        if (event.packet instanceof ClientboundExplodePacket packet) {
            if (explosions.get() || selfWindBurst.get()) {
                boolean isSelfWind = selfWindBurst.get() && windBurstTimer > 0;

                double horizKb, vertKb;
                if (isSelfWind) {
                    horizKb = selfWindBurstHorizontal.get();
                    vertKb  = selfWindBurstVertical.get();
                } else if (explosions.get()) {
                    horizKb = explosionsHorizontal.get();
                    vertKb  = explosionsVertical.get();
                } else {
                    return;
                }

                Optional<Vec3> optionalKnockback = packet.playerKnockback();
                Vec3 currentVel = mc.player.getDeltaMovement();
                Vec3 newVel;

                if (optionalKnockback.isPresent()) {
                    Vec3 knockback = optionalKnockback.get();
                    newVel = new Vec3(
                        currentVel.x + knockback.x * horizKb,
                        currentVel.y + knockback.y * vertKb,
                        currentVel.z + knockback.z * horizKb
                    );
                } else {
                    Vec3 expCenter = packet.center();
                    Vec3 playerPos = mc.player.position();
                    Vec3 dir = playerPos.subtract(expCenter);
                    double dist = dir.length();
                    double radius = packet.radius();

                    if (dist < radius) {
                        dir = dir.normalize();
                        double strength = 1.0 - dist / radius;
                        newVel = new Vec3(
                            currentVel.x + dir.x * horizKb * strength,
                            currentVel.y + dir.y * vertKb * strength,
                            currentVel.z + dir.z * horizKb * strength
                        );
                    } else {
                        return;
                    }
                }

                mc.player.setDeltaMovement(newVel);
                event.cancel();
            }
        }
    }

    public double getHorizontal(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

    public double getVertical(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }
}