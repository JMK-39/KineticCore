package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 颜色色块 / Color swatch. */
public abstract class ColorSwatchBuilder extends ControlBuilder<ColorSwatchBuilder, KineticColorSwatch> {
    /** 颜色 / Color. */
    protected final int rgb;
    /** 点击回调 / Click action. */
    protected Runnable onClick = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ColorSwatchBuilder(int x, int y, int width, int rgb) {
        super(x, y, width);
        this.rgb = rgb;
    }

    @Override
    protected final ColorSwatchBuilder self() {
        return this;
    }

    /** 点击回调 / Click action. */
    public final ColorSwatchBuilder onClick(Runnable action) {
        this.onClick = action;
        return this;
    }
}
