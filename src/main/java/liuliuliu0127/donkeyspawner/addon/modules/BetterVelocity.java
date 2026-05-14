package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

public class BetterVelocity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- 自身风爆 ---
    public final Setting<Boolean> selfWindBurst = sgGeneral.add(new BoolSetting.Builder()
        .name("self-wind-burst")
        .defaultValue(true)
        .build()
    );
    public final Setting<Double> selfWindBurstHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("self-wind-burst-horizontal")
        .defaultValue(0)
        .sliderRange(-5, 5)
        .visible(selfWindBurst::get)
        .build()
    );
    public final Setting<Double> selfWindBurstVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("self-wind-burst-vertical")
        .defaultValue(1)
        .sliderRange(-5, 5)
        .visible(selfWindBurst::get)
        .build()
    );
    public final Setting<Boolean> selfWindBurstDebug = sgGeneral.add(new BoolSetting.Builder()
        .name("self-wind-Debug")
        .defaultValue(false)
        .visible(selfWindBurst::get)
        .build()
    );
    public final Setting<Integer> selfWindBurstTimerSet = sgGeneral.add(new IntSetting.Builder()
        .name("self-wind-timer-set(tick)")
        .description("Completely cancels explosion knockback when self wind burst is active.")
        .defaultValue(10)
        .sliderRange(0, 100)
        .visible(() -> selfWindBurst.get() && selfWindBurstDebug.get())
        .build()
    );
    // --- 矛 突刺（Lunge） ---
    public final Setting<Boolean> spearLunge = sgGeneral.add(new BoolSetting.Builder()
        .name("spear-lunge")
        .defaultValue(true)
        .build()
    );

    // 风爆计时器
    public int windBurstTimer = 0;
    //public Vec3 pendingVelocity = null;
    private boolean lastAttackPressed = false;

    public BetterVelocity() {
        super(DonkeySpawnerAddon.CATEGORY, "BetterVelocity",
            "(Requires enable Meteor officia Velocity)Enhances Meteor official Velocity with spear lunge and self windcharge burst control.");
    }

    @Override
    public void onActivate() {
        windBurstTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        // 风爆计时器更新
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
                if (holdingWindCharge) {
                    if (mc.options.keyUse.isDown()) windBurstTimer = selfWindBurstTimerSet.get();   // 风弹只响应右键
                } else if (hasWindBurst) {
                    if (mc.options.keyAttack.isDown()) windBurstTimer = selfWindBurstTimerSet.get(); // 重锤只响应左键
                }
            }
            if (windBurstTimer > 0) windBurstTimer--;
        }

        // --- 新增：应用延迟的重锤风暴速度 ---
        //if (pendingVelocity != null) {
        //    ((IVec3d) (Object) mc.player.getDeltaMovement()).meteor$set(pendingVelocity);
        //    pendingVelocity = null;
        //}
        
    }

    /** 检查玩家手持的矛是否有 Lunge 附魔 */
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