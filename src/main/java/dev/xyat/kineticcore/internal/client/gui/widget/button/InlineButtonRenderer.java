package dev.xyat.kineticcore.internal.client.gui.widget.button;

import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Draws the standard Kinetic button look for self-drawn rows (buttons embedded in custom list rows). */
public final class InlineButtonRenderer {
    private static final PaintButton BUTTON = new PaintButton();

    private InlineButtonRenderer() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int width, int height, Component text,
                              boolean hovered, boolean active, boolean error) {
        BUTTON.setX(x);
        BUTTON.setY(y);
        BUTTON.setWidth(Math.max(1, width));
        BUTTON.setPaintHeight(Math.max(1, height));
        BUTTON.setMessage(text == null ? Component.empty() : text);
        BUTTON.active = active;
        BUTTON.visible = true;
        int mouse = hovered ? 0 : -100000;
        BUTTON.render(graphics, hovered ? x + 1 : mouse, hovered ? y + 1 : mouse, 0F);
        if (error) GuiTheme.stateOutline(graphics, x, y, width, height, false, hovered, true);
    }

    private static final class PaintButton extends Button {
        private PaintButton() {
            super(0, 0, 1, 1, Component.empty(), ignored -> { }, DEFAULT_NARRATION);
        }

        private void setPaintHeight(int height) {
            this.height = height;
        }
    }
}
