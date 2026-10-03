package dev.xyat.kineticcore.internal.mixin.api.recipebook.client;

import dev.xyat.kineticcore.internal.runtime.KineticCommonHookRuntime;
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
        //? if >=26.1 {
        /*// 26.1 builds the client collections from the recipes the server sent.
        @Inject(method = "rebuildCollections", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onSetup(CallbackInfo ci) {
        *///?} else {
        @Inject(method = "setupCollections", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onSetup(Iterable<Recipe<?>> iterable, RegistryAccess registryAccess, CallbackInfo ci) {
        //?}
            if (KineticCommonHookRuntime.recipeBookRemovalEnabled()) ci.cancel();
        }
    }

    @Mixin(Screen.class)
    public static class Gui {
        @Unique
        private static final ResourceLocation kineticcore$RECIPE_ICON = ResourceLocation.withDefaultNamespace("textures/gui/recipe_button.png");

        @Inject(
                method = "addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
                at = @At("HEAD"),
                cancellable = true
        )
        private void kineticcore$removeRecipeButton(GuiEventListener widget, CallbackInfoReturnable<GuiEventListener> cir) {
            if (KineticCommonHookRuntime.recipeBookRemovalEnabled() && widget instanceof ImageButton image) {
                //? if >=1.20.2 {
                /*if (((ButtonAccess) image).getBtnSprites() == net.minecraft.client.gui.screens.recipebook.RecipeBookComponent.RECIPE_BUTTON_SPRITES) {
                    cir.setReturnValue(null);
                }
                *///?} else {
                ResourceLocation loc = ((ButtonAccess) image).getBtnTexture();
                if (loc != null && loc.equals(kineticcore$RECIPE_ICON)) {
                    cir.setReturnValue(null);
                }
                //?}
            }
        }
    }
}
