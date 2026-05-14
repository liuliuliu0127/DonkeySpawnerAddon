package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class BetterVelocity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- 自身风爆（风弹） ---
    public final Setting<Boolean> selfWindBurst = sgGeneral.add(new BoolSetting.Builder()
        .name("self-wind-burst").defaultValue(true).build()
    );
    public final Setting<Double> selfWindBurstHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("self-wind-burst-horizontal").defaultValue(1.0).sliderRange(-5, 5).visible(selfWindBurst::get).build()
    );
    public final Setting<Double> selfWindBurstVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("self-wind-burst-vertical").defaultValue(1.0).sliderRange(-5, 5).visible(selfWindBurst::get).build()
    );

    // --- 矛 突刺（Lunge） ---
    public final Setting<Boolean> spearLunge = sgGeneral.add(new BoolSetting.Builder()
        .name("spear-lunge").defaultValue(true).build()
    );

    // --- 重锤自动禁用 Velocity ---
    public final Setting<Boolean> autoDisableForMace = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-velocity-for-mace")
        .description("Automatically disables Velocity when holding a mace with Wind Burst.")
        .defaultValue(false)
        .build()
    );

    public final Setting<DisableCondition> disableCondition = sgGeneral.add(new EnumSetting.Builder<DisableCondition>()
        .name("disable-condition")
        .description("Condition to disable Velocity for mace.")
        .defaultValue(DisableCondition.ATTACK_PRESSED)
        .visible(autoDisableForMace::get)
        .build()
    );

    public enum DisableCondition {
        HOLDING,            // 只要手持风爆重锤就关闭
        ATTACK_PRESSED      // 必须按住左键才关闭
    }

    // 风爆计时器（风弹用）
    public int windBurstTimer = 0;

    // 记录我们是否已经关闭了 Velocity
    private boolean maceDisableActive = false;
    private Module velocityModule = null;

    public BetterVelocity() {
        super(DonkeySpawnerAddon.CATEGORY, "BetterVelocity",
            "(Requires enable Meteor official Velocity) Enhances Meteor official Velocity with spear lunge and self wind burst control.");
    }

    @Override
    public void onActivate() {
        windBurstTimer = 0;
        maceDisableActive = false;
        // 确保 Velocity 处于原始状态（如果之前被我们关掉，这里不自动恢复，交给 onDeactivate 处理）
    }

    @Override
    public void onDeactivate() {
        // 模块关闭时，如果 Velocity 是我们关掉的，恢复它
        if (maceDisableActive) {
            restoreVelocity();
        }
        windBurstTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        // ===== 风爆计时器更新（风弹） =====
        if (selfWindBurst.get()) {
            ItemStack main = mc.player.getMainHandItem();
            boolean holdingWindCharge = main.getItem() == Items.WIND_CHARGE;
            if (holdingWindCharge && mc.options.keyUse.isDown()) {
                windBurstTimer = 20;
            }
            // 注意：重锤的计时器不再用于自身风爆，但为了兼容保留（实际上我们不再用计时器处理重锤风暴，而是直接禁用 Velocity）
            // 可以移除下面这行，但保留也无妨，因为不会影响其他功能
            // else if (hasWindBurst && mc.options.keyAttack.isDown()) { windBurstTimer = 20; }

            if (windBurstTimer > 0) windBurstTimer--;
        }

        // ===== 重锤自动禁用 Velocity =====
        if (autoDisableForMace.get()) {
            ItemStack main = mc.player.getMainHandItem();
            boolean holdingMace = main.getItem() == Items.MACE;
            boolean hasWindBurst = holdingMace && main.getEnchantments()
                .getLevel(mc.player.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.WIND_BURST)) > 0;

            boolean shouldDisable = false;
            if (hasWindBurst) {
                switch (disableCondition.get()) {
                    case HOLDING:
                        shouldDisable = true;
                        break;
                    case ATTACK_PRESSED:
                        shouldDisable = mc.options.keyAttack.isDown();
                        break;
                }
            }

            if (shouldDisable && !maceDisableActive) {
                disableVelocity();
            } else if (!shouldDisable && maceDisableActive) {
                restoreVelocity();
            }
        } else {
            // 功能关闭时，如果之前被我们禁用了，恢复
            if (maceDisableActive) {
                restoreVelocity();
            }
        }
    }

    // ===== 辅助方法 =====
    private void disableVelocity() {
        if (velocityModule == null) {
            velocityModule = Modules.get().get(Velocity.class);
        }
        if (velocityModule != null && velocityModule.isActive()) {
            velocityModule.toggle();
            maceDisableActive = true;
        }
    }

    private void restoreVelocity() {
        if (velocityModule == null) {
            velocityModule = Modules.get().get(Velocity.class);
        }
        if (velocityModule != null && !velocityModule.isActive() && maceDisableActive) {
            velocityModule.toggle();
        }
        maceDisableActive = false;
    }

    // 检测矛的附魔（保持不变）
    public boolean isSpearWithLunge() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandItem();
        if (!stack.has(DataComponents.PIERCING_WEAPON)) return false;
        int level = stack.getEnchantments()
            .getLevel(mc.player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LUNGE));
        return level > 0;
    }
}