package dev.xyat.kineticcore.api.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class HighZButton extends Button {
    private final int zLevel;

    public HighZButton(int x, int y, int w, int h, Component msg, OnPress onPress, Tooltip tooltip) {
        this(x, y, w, h, msg, onPress, tooltip, 200);
    }

    public HighZButton(int x, int y, int w, int h, Component msg, OnPress onPress, Tooltip tooltip, int zLevel) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
        if (tooltip != null) this.setTooltip(tooltip);
        this.zLevel = zLevel;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics g, int mx, int my, float pt) {
        g.pose().pushPose();
        g.pose().translate(0, 0, zLevel);
        super.renderWidget(g, mx, my, pt);
        g.pose().popPose();
    }
}