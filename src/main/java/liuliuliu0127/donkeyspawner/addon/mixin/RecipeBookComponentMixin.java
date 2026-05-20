package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.DonkeySpawnerTMI;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jspecify.annotations.Nullable;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {

    @Shadow @Nullable private RecipeCollection lastRecipeCollection;

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At("RETURN"), cancellable = true)
    private void onMouseClickedReturn(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        DonkeySpawnerTMI module = Modules.get().get(DonkeySpawnerTMI.class);
        if (module == null || !module.shouldOpenWorkbench()) return;

        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof InventoryScreen)) return;

        if (this.lastRecipeCollection != null && !this.lastRecipeCollection.hasCraftable()) {
            DonkeySpawnerTMI.openCraftingScreen();
            cir.setReturnValue(true);
        }
    }
}