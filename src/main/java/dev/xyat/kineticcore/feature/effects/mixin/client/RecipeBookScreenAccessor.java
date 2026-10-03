package dev.xyat.kineticcore.feature.effects.mixin.client;

//? if >=26.1 {
/*import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 26.1 的配方书界面不再公开配方书组件；配方书展开时不显示状态效果 / 26.1 recipe-book screens keep the recipe book
// private; effects stay hidden while it is open. Older versions list it in unavailable_mixins.
@Mixin(AbstractRecipeBookScreen.class)
public interface RecipeBookScreenAccessor {
    @Accessor("recipeBookComponent")
    RecipeBookComponent<?> kineticcore$getRecipeBookComponent();
}
*///?}
