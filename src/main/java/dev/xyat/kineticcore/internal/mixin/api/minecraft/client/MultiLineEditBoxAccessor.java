package dev.xyat.kineticcore.internal.mixin.api.minecraft.client;

//? if >=26.1 {
/*import dev.xyat.kineticcore.internal.client.gui.widget.input.MultiLineTextColorAccess;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

// 26.1 的多行文本框在构建时固定文字颜色，这里用于按“已修改”状态切换颜色 / 26.1 fixes a multi-line box's text
// color when it is built; this switches it for the modified state. Older versions list it in unavailable_mixins.
@Mixin(MultiLineEditBox.class)
public interface MultiLineEditBoxAccessor extends MultiLineTextColorAccess {
    @Override
    @Mutable
    @Accessor("textColor")
    void kineticcore$setTextColor(int color);
}
*///?}
