package dev.xyat.kineticcore.internal.client.screen;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/** Keeps Screen focus and control focus synchronized for Kinetic Screen implementations. */
public final class KineticScreenFocus {
    private final Screen screen;

    public KineticScreenFocus(Screen screen) {
        this.screen = screen;
    }

    public void focusControl(GuiEventListener control) {
        if (control == null) {
            clearControlFocus();
            return;
        }
        // A hidden or disabled widget must not take keyboard focus from the
        // input field the user was editing.
        if (control instanceof AbstractWidget widget && (!widget.visible || !widget.active)) return;
        GuiEventListener current = screen.getFocused();
        if (current == control) {
            if (!control.isFocused()) control.setFocused(true);
            return;
        }
        try {
            if (current != null) current.setFocused(false);
            screen.setFocused(control);
            control.setFocused(true);
        } catch (RuntimeException | Error failure) {
            // Addon-provided widgets may throw after mutating their own focus
            // flag. Restore both the host's reference and each widget's flag.
            try {
                control.setFocused(false);
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
            }
            try {
                screen.setFocused(current);
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
            }
            if (current != null) {
                try {
                    current.setFocused(true);
                } catch (RuntimeException | Error cleanupFailure) {
                    if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    public void blurControl(GuiEventListener control) {
        if (control == null) return;
        try {
            control.setFocused(false);
        } catch (RuntimeException | Error failure) {
            // The widget may have already cleared its flag before throwing.
            // Do not leave a dead reference in the owning Screen.
            try {
                if (screen.getFocused() == control) screen.setFocused(null);
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        if (screen.getFocused() == control) {
            screen.setFocused(null);
        }
    }

    public void clearControlFocus() {
        GuiEventListener current = screen.getFocused();
        if (current != null) {
            blurControl(current);
        } else {
            screen.setFocused(null);
        }
    }

    public boolean isControlFocused(GuiEventListener control) {
        return control != null && screen.getFocused() == control && control.isFocused();
    }

    public void synchronizeControlState() {
        GuiEventListener current = screen.getFocused();
        if (current == null) return;
        if (!current.isFocused()) {
            screen.setFocused(null);
            return;
        }
        if (current instanceof AbstractWidget widget && (!widget.visible || !widget.active)) {
            blurControl(current);
        }
    }
}
