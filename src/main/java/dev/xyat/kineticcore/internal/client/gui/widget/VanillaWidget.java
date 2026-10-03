package dev.xyat.kineticcore.internal.client.gui.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import dev.xyat.kineticcore.internal.client.gui.GuiInputCompat;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
*///?}

/**
 * Base of KineticCore widgets that handle input themselves. They override the coordinate methods
 * ({@code mouseClicked(double, double, int)}, {@code keyPressed(int, int, int)} and so on) on every version. Since
 * 1.21.9 vanilla delivers input as events, so on 26.1 this class forwards the events to those methods and gives them
 * the vanilla behavior as their super implementation.
 */
public abstract class VanillaWidget extends AbstractWidget {
    protected VanillaWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    //? if >=26.1 {
    /*private MouseButtonEvent currentMouseEvent;
    private boolean currentDoubleClick;

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        currentMouseEvent = event;
        currentDoubleClick = doubleClick;
        return mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        currentMouseEvent = event;
        return mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        currentMouseEvent = event;
        return mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        boolean handled = false;
        for (char character : Character.toChars(event.codepoint())) handled |= charTyped(character, 0);
        return handled;
    }

    private MouseButtonEvent mouseEventAt(double mouseX, double mouseY, int button) {
        int modifiers = currentMouseEvent == null ? GuiInputCompat.currentModifiers() : currentMouseEvent.modifiers();
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, modifiers));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseEventAt(mouseX, mouseY, button), currentDoubleClick);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseEventAt(mouseX, mouseY, button));
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseEventAt(mouseX, mouseY, button), dragX, dragY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codePoint));
    }
    *///?}
}
