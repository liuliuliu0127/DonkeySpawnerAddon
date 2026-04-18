package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class TridentElytra extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoEquipElytra = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-equip-elytra")
            .description("Automatically equip elytra when using riptide trident.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> speedThreshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed-threshold")
            .description("Speed (m/s) below which ElytraFly will take over after boost.")
            .defaultValue(2.0)
            .range(0.5, 5.0)
            .build()
    );

    private final Setting<Boolean> manualTakeoverOnMove = sgGeneral.add(new BoolSetting.Builder()
            .name("manual-takeover-on-move")
            .description("Pressing any movement key instantly takes over with ElytraFly.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableGravityDuringBoost = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-gravity")
            .description("Disable gravity during trident boost.")
            .defaultValue(false)
            .build()
    );

    // 状态变量
    private boolean isActive = false;          // 是否处于活跃期（蓄力或冲刺中）
    private boolean hasSwappedElytra = false;  // 是否已经换过鞘翅（避免重复）
    private ElytraFly elytraFly = null;
    private ElytraSwap elytraSwap = null;

    public TridentElytra() {
        super(DonkeySpawnerAddon.CATEGORY, "TridentElytra", "Integrate riptide trident with elytra flight.");
    }

    @Override
    public void onActivate() {
        elytraFly = Modules.get().get(ElytraFly.class);
        elytraSwap = Modules.get().get(ElytraSwap.class);
    }

    @Override
    public void onDeactivate() {
        if (elytraFly != null) elytraFly.setForcePause(false);
        if (elytraSwap != null) {
            elytraSwap.setForceDisableInfElytra(false);
            if (mc.player != null && mc.player.onGround()) {
                elytraSwap.requestChestplateSwap();
                if (mc.player.isFallFlying()) {
                    mc.player.stopFallFlying();
                }
            }
        }
        isActive = false;
        hasSwappedElytra = false;
        if (mc.player != null) {
            Objects.requireNonNull(mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue(0.08D);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        boolean usingTrident = isUsingRiptideTrident();

        if (usingTrident && !isActive) {
            startBoost();
        } else if (!usingTrident && isActive) {
            // 松开右键，但可能还在冲刺中，等待结束条件
            // 不立即结束，让 handleActive 处理
        }

        if (isActive) {
            handleActive();
        }
    }

    private boolean isUsingRiptideTrident() {
        if (!mc.player.isUsingItem()) return false;
        ItemStack main = mc.player.getMainHandItem();
        if (!main.is(Items.TRIDENT)) return false;
        var enchants = main.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return false;
        boolean hasRiptide = false;
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE)) {
                hasRiptide = true;
                break;
            }
        }
        if (!hasRiptide) return false;
        // 原版激流三叉戟使用条件：雨天（露天）或水中
        return mc.level.isRaining() || mc.player.isInWaterOrRain();
    }

    private void startBoost() {
        // 暂停 ElytraFly 和 ElytraSwap 的无限耐久
        if (elytraFly != null) elytraFly.setForcePause(true);
        if (elytraSwap != null) elytraSwap.setForceDisableInfElytra(true);

        isActive = true;
        hasSwappedElytra = false;
    }

    private void handleActive() {
        // 1. 离地且未换鞘翅 -> 换鞘翅并开始滑翔
        if (autoEquipElytra.get() && !hasSwappedElytra && !mc.player.onGround()) {
            // 先让 AutoArmor 忽略鞘翅（避免抢回）
            if (elytraSwap != null) elytraSwap.setAutoArmorIgnoreElytra(true);
            // 换鞘翅
            if (!isElytraEquipped()) {
                int slot = findValidElytraInInventory();
                if (slot != -1) {
                    InvUtils.move().from(slot).toArmor(EquipmentSlot.CHEST.getIndex());
                }
            }
            // 开始滑翔
            if (isElytraEquipped() && !mc.player.isFallFlying()) {
                mc.player.startFallFlying();
            }
            hasSwappedElytra = true;
        }

        // 2. 检查结束条件
        boolean shouldEnd = false;

        // 条件A：落地
        if (mc.player.onGround()) {
            shouldEnd = true;
        }

        // 条件B：松开右键且速度低于阈值
        if (!mc.player.isUsingItem()) {
            double speed = mc.player.getDeltaMovement().length();
            if (speed < speedThreshold.get()) {
                shouldEnd = true;
            }
        }

        // 条件C：手动接管（按下任意移动键）
        if (manualTakeoverOnMove.get() && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() ||
                mc.options.keyLeft.isDown() || mc.options.keyRight.isDown())) {
            shouldEnd = true;
        }

        if (shouldEnd) {
            endBoost();
            return;
        }

        // 3. 可选：禁用重力（只在下降时）
        if (disableGravityDuringBoost.get()) {
            Vec3 vel = mc.player.getDeltaMovement();
            if (vel.y < 0) {
                Objects.requireNonNull(mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue(0.0D);
            } else {
                Objects.requireNonNull(mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue(0.08D);
            }
        }
    }

    private void endBoost() {
        // 恢复平飞模块和无限耐久
        if (elytraFly != null) elytraFly.setForcePause(false);
        if (elytraSwap != null) {
            elytraSwap.setForceDisableInfElytra(false);
            if (mc.player.onGround()) {
                elytraSwap.requestChestplateSwap();  // 触发换回胸甲
                if (mc.player.isFallFlying()) {
                    mc.player.stopFallFlying();
                }
            }
        }
        isActive = false;
        hasSwappedElytra = false;
        // 恢复重力
        Objects.requireNonNull(mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue(0.08D);
    }

    private boolean isElytraEquipped() {
        return mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER);
    }

    private int findValidElytraInInventory() {
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.has(DataComponents.GLIDER)) {
                int durability = stack.getMaxDamage() - stack.getDamageValue();
                if (durability > 1 && !hasBindingCurse(stack)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean hasBindingCurse(ItemStack stack) {
        var enchants = stack.getEnchantments();
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE)) {
                return true;
            }
        }
        return false;
    }
}