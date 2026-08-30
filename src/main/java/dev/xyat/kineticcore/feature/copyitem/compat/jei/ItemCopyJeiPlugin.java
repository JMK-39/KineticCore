package dev.xyat.kineticcore.feature.copyitem.compat.jei;

import dev.xyat.kineticcore.KineticCore;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class ItemCopyJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return new ResourceLocation(KineticCore.MODID, "copyitem");
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static ItemStack getHoveredItemStack() {
        if (runtime == null) {
            return ItemStack.EMPTY;
        }

        ItemStack ingredientStack = runtime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        if (ingredientStack != null && !ingredientStack.isEmpty()) {
            return ingredientStack.copy();
        }

        ItemStack bookmarkStack = runtime.getBookmarkOverlay().getItemStackUnderMouse();
        if (bookmarkStack != null && !bookmarkStack.isEmpty()) {
            return bookmarkStack.copy();
        }

        return ItemStack.EMPTY;
    }
}