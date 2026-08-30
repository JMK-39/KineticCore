package dev.xyat.kineticcore.feature.recipebook.mixin.client;

import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class RecipeBookClientMixins {
    @Mixin(ClientRecipeBook.class)
    public static class Logic {
        @Inject(method = "setupCollections", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onSetup(Iterable<Recipe<?>> iterable, RegistryAccess registryAccess, CallbackInfo ci) {
            if (GeneralMechanicsConfig.removeRecipeBook) ci.cancel();
        }
    }

    @Mixin(Screen.class)
    public static class Gui {
        @Unique
        private static final ResourceLocation kineticcore$RECIPE_ICON = new ResourceLocation("textures/gui/recipe_button.png");

        @Inject(
                method = "addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
                at = @At("HEAD"),
                cancellable = true
        )
        private void kineticcore$removeRecipeButton(GuiEventListener widget, CallbackInfoReturnable<GuiEventListener> cir) {
            if (GeneralMechanicsConfig.removeRecipeBook && widget instanceof ImageButton image) {
                ResourceLocation loc = ((ButtonAccess) image).getBtnTexture();
                if (loc != null && loc.equals(kineticcore$RECIPE_ICON)) {
                    cir.setReturnValue(null);
                }
            }
        }
    }
}
