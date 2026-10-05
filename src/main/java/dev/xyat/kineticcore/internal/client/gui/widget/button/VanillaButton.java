package dev.xyat.kineticcore.internal.client.gui.widget.button;

import dev.xyat.kineticcore.internal.client.gui.text.KineticText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Base of KineticCore's buttons, which draw in {@link #renderButton}. Up to 1.21.1 a button draws in renderWidget;
 * on 26.1 AbstractButton's render method is final and calls extractContents instead.
 */
public abstract class VanillaButton extends Button {
    /** Smallest gap between a label and the button's outer edge: the 2 px frame plus 2 px of space. */
    private static final int LABEL_PADDING = 4;

    protected VanillaButton(int x, int y, int width, int height, Component message, Button.OnPress onPress,
                            Button.CreateNarration createNarration) {
        super(x, y, width, height, message, onPress, createNarration);
    }

    /** Draws the button; the default is the vanilla look. */
    protected void renderButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderVanillaButton(graphics, mouseX, mouseY, partialTick);
    }

    /** The vanilla button look: sprite and centred label. */
    protected final void renderVanillaButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if >=26.1 {
        /*extractDefaultSprite(graphics);
        drawLabel(graphics, (active ? 0xFFFFFF : 0xA0A0A0) | net.minecraft.util.Mth.ceil(alpha * 255.0F) << 24);
        *///?} else {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        //?}
    }

    //? if <26.1 {
    // Vanilla draws the label 2 px from the edge, on the frame's bevel; Kinetic labels keep clear of the frame.
    @Override
    public void renderString(GuiGraphics graphics, net.minecraft.client.gui.Font font, int color) {
        drawLabel(graphics, color);
    }
    //?}

    /** Centred, vertically centred label that keeps its padding, scrolls when long and ellipsizes when far too long. */
    private void drawLabel(GuiGraphics graphics, int color) {
        Component message = getMessage();
        if (message == null || message.getString().isEmpty()) return;
        KineticText.drawScrollingCentered(graphics, Minecraft.getInstance().font, message, getX() + getWidth() / 2,
                getY() + (getHeight() - 8 + 1) / 2, getWidth() - LABEL_PADDING * 2, color, true);
    }

    //? if >=26.1 {
    /*@Override
    protected final void extractContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderButton(graphics, mouseX, mouseY, partialTick);
    }
    *///?} else {
    @Override
    protected final void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderButton(graphics, mouseX, mouseY, partialTick);
    }
    //?}
}
