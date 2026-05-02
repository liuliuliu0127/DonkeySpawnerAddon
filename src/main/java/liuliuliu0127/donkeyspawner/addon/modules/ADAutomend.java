package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;

public class ADAutomend extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> inventorySwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Inventory Switch")
        .description("use xp bottles in inventory when there's no xp bottle in hotbar")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxHeight = sgGeneral.add(new IntSetting.Builder()
        .name("Above range Without Blocks")
        .description("Maximun height of check range above the player without any blocks if there's no block near the player")
        .defaultValue(5)
        .min(1)
        .sliderMax(10)
        .build()
    );

    private boolean lastNoBottle = false;

    public ADAutomend() {
        super(DonkeySpawnerAddon.CATEGORY, "ADAutomend", "auto mend when you press A + D");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!(mc.options.keyLeft.isDown() && mc.options.keyRight.isDown()) 
            || mc.options.keyUp.isDown() || mc.options.keyDown.isDown()) {
            lastNoBottle = false;
            return;
        }

        // 查找经验瓶，根据 inventorySwitch 决定搜索范围
        FindItemResult exp;
        if (inventorySwitch.get()) {
            exp = InvUtils.find(stack -> stack.getItem() == Items.EXPERIENCE_BOTTLE, 0, 35);
        } else {
            exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        }

        if (!exp.found()) {
            if (!lastNoBottle) {
                String msg = inventorySwitch.get()
                    ? "[DonkeySpawnerAddon] ADAutomend: NO XP bottle in inventory!"
                    : "[DonkeySpawnerAddon] ADAutomend: NO XP bottle in hotbar!";
                ChatUtils.sendMsg(Component.literal(msg).withStyle(ChatFormatting.RED));
                lastNoBottle = true;
            }
            return;
        }
        lastNoBottle = false;

        // 确定投掷方向
        double pitch=0;
        boolean canThrow = false;

        if (hasBlockBelow(2.0)) {
            canThrow = true;
            pitch = 90.0; // 向下
        } else if (hasBlockAbove(1.5)) {
            canThrow = true;
            pitch = -90.0; // 向上
        } else if (isAboveClear() && isPlayerStill()) {
            canThrow = true;
            pitch = -90.0; // 向上投掷
        }

        if (!canThrow) return;

        // 投掷
        Rotations.rotate(mc.player.getYRot(), pitch, () -> {
            if (exp.getHand() != null) {
                mc.gameMode.useItem(mc.player, exp.getHand());
                return;
            }

            if (inventorySwitch.get() && exp.slot() >= 9) {
                // 从背包移动到当前手持槽
                final int backpackSlot = exp.slot();
                final int hotbarSlot = mc.player.getInventory().getSelectedSlot();
                InvUtils.move().from(backpackSlot).to(hotbarSlot);

                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);

                // 如果投掷后还有剩余，移回背包
                ItemStack remain = mc.player.getInventory().getItem(hotbarSlot);
                if (!remain.isEmpty() && remain.getItem() == Items.EXPERIENCE_BOTTLE) {
                    InvUtils.move().from(hotbarSlot).to(backpackSlot);
                }
            } else {
                // 快捷栏内交换，投掷，换回（与 EXPThrower 一致）
                InvUtils.swap(exp.slot(), true);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                InvUtils.swapBack();
            }
        });
    }

    /** 检查正下方一格内是否有能砸到经验瓶的方块 */
    private boolean hasBlockBelow(double range) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 target = eyePos.add(0, -range, 0);
        BlockHitResult hit = mc.level.clip(new ClipContext(eyePos, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK &&
               hit.getBlockPos().getY() >= mc.player.blockPosition().getY() - 1;
    }

    /** 检查正上方一格内是否有方块 */
    private boolean hasBlockAbove(double range) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 target = eyePos.add(0, range, 0);
        BlockHitResult hit = mc.level.clip(new ClipContext(eyePos, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK &&
               hit.getBlockPos().getY() <= mc.player.blockPosition().getY() + 1;
    }

    /** 检查从头顶到 maxHeight 格高度之间是否有方块 */
    private boolean isAboveClear() {
        int startY = mc.player.blockPosition().getY() + 1;
        int maxY = mc.player.blockPosition().getY() + maxHeight.get();
        for (int y = startY; y <= maxY; y++) {
            BlockPos pos = new BlockPos(mc.player.blockPosition().getX(), y, mc.player.blockPosition().getZ());
            BlockState state = mc.level.getBlockState(pos);
            if (state.isCollisionShapeFullBlock(mc.level, pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPlayerStill() {
        return mc.player.getDeltaMovement().lengthSqr() < 0.001;
    }
}