package dev.xyat.kineticcore.api.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;

/** 保持 Screen 焦点与控件自身焦点一致，统一清理旧控件。 */
public final class KineticScreenFocus {
    private final Screen screen;
    KineticScreenFocus(Screen screen) { this.screen = screen; }

    public static void focus(Screen screen, GuiEventListener control) {
        if (screen == null) return;
        new KineticScreenFocus(screen).focusControl(control);
    }

    public static void blur(Screen screen, GuiEventListener control) {
        if (screen == null) return;
        new KineticScreenFocus(screen).blurControl(control);
    }

    public static void clear(Screen screen) {
        if (screen == null) return;
        new KineticScreenFocus(screen).clearControlFocus();
    }

    public static boolean isFocused(Screen screen, GuiEventListener control) {
        return screen != null && new KineticScreenFocus(screen).isControlFocused(control);
    }

    public final void focusControl(GuiEventListener control) {
        if (control == null) {
            clearControlFocus();
            return;
        }
        GuiEventListener current = screen.getFocused();
        if (current != null && current != control) current.setFocused(false);
        screen.setFocused(control);
        control.setFocused(true);
    }

    public final void blurControl(GuiEventListener control) {
        if (control == null) return;
        control.setFocused(false);
        if (screen.getFocused() == control) screen.setFocused(null);
    }

    public final void clearControlFocus() {
        GuiEventListener current = screen.getFocused();
        if (current != null) current.setFocused(false);
        screen.setFocused(null);
    }

    public final boolean isControlFocused(GuiEventListener control) {
        return control != null && screen.getFocused() == control && control.isFocused();
    }
}
