package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.DonkeySpawnerTMI;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(RecipeCollection.class)
public class RecipeCollectionMixin {

    @Shadow private Set<RecipeDisplayId> selected;

    @Inject(method = "hasAnySelected", at = @At("HEAD"), cancellable = true)
    private void onHasAnySelected(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(DonkeySpawnerTMI.class).isActive()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getSelectedRecipes", at = @At("HEAD"), cancellable = true)
    private void onGetSelectedRecipes(RecipeCollection.CraftableStatus status, CallbackInfoReturnable<List<RecipeDisplayEntry>> cir) {
        if (Modules.get().get(DonkeySpawnerTMI.class).isActive()) {
            if (this.selected.isEmpty()) {
                cir.setReturnValue(new ArrayList<>(((RecipeCollection) (Object) this).getRecipes()));
            }
        }
    }
}