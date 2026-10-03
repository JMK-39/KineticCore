package dev.xyat.kineticcore.internal.client.gui.widget.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Base of KineticCore's buttons, which draw in {@link #renderButton}. Up to 1.21.1 a button draws in renderWidget;
 * on 26.1 AbstractButton's render method is final and calls extractContents instead.
 */
public abstract class VanillaButton extends Button {
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
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
        *///?} else {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        //?}
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
