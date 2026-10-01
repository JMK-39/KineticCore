package dev.xyat.kineticcore.internal.mixin.api.minecraft.client;

import dev.xyat.kineticcore.internal.client.gui.widget.input.EditBoxScrollAccess;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 读取文本框横向滚动位置，供命令补全弹窗对齐 / Reads an edit box's horizontal scroll for suggestion alignment. */
@Mixin(EditBox.class)
public interface EditBoxAccessor extends EditBoxScrollAccess {
    @Override
    @Accessor("displayPos")
    int kineticcore$getDisplayPos();
}
