package dev.xyat.kineticcore.api.client.gui.overlay;

import dev.xyat.kineticcore.internal.client.overlay.GuiOverlayRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Global overlays drawn above the current screen: tooltips, confirmation dialogs and toasts.
 *
 * <p>Tooltip requests last for one frame, so call them from render code every frame the tooltip should stay
 * visible; the last request of a frame wins. Toasts persist until they expire or are removed. Call these methods on
 * the client thread.
 */
public final class KineticOverlays {
    /**
     * Screen anchor of a toast. Offsets passed to {@link #toast(String, Component, Position, int, int, int)} are
     * relative to it.
     */
    public enum Position {
        TOP_CENTER,
        BOTTOM_CENTER,
        CENTER,
        TOP_LEFT,
        TOP_RIGHT
    }

    /** Visual style of a context-menu row. */
    public enum MenuItemStyle {
        /** Standard actionable row. */
        NORMAL,
        /** Destructive action, drawn in the theme's danger color. */
        DANGER,
        /** Non-interactive divider line. */
        SEPARATOR
    }

    /**
     * One immutable context-menu row. Create rows with the static factories and pass them to the screen's
     * context-menu API.
     */
    public static final class MenuItem {
        private final Component label;
        private final Component detail;
        private final Component tooltip;
        private final Boolean checked;
        private final Runnable action;
        private final boolean enabled;
        private final MenuItemStyle style;

        private MenuItem(
                Component label,
                Component detail,
                Component tooltip,
                Boolean checked,
                Runnable action,
                boolean enabled,
                MenuItemStyle style
        ) {
            this.label = Objects.requireNonNullElse(label, Component.empty());
            this.detail = Objects.requireNonNullElse(detail, Component.empty());
            this.tooltip = Objects.requireNonNullElse(tooltip, Component.empty());
            this.checked = checked;
            this.action = action == null ? () -> { } : action;
            this.style = style == null ? MenuItemStyle.NORMAL : style;
            this.enabled = this.style != MenuItemStyle.SEPARATOR && enabled;
        }

        /**
         * Creates a fully specified row. Prefer the specific factories such as {@link #action(Component, Runnable)}
         * for common cases.
         *
         * @param label row text; {@code null} becomes empty
         * @param detail secondary text shown to the right of the label; {@code null} becomes empty
         * @param tooltip hover text; {@code null} becomes empty
         * @param checked check mark state for toggle rows, or {@code null} for rows without a check mark
         * @param action run when the row is clicked; {@code null} does nothing
         * @param enabled whether the row can be clicked; separators are always disabled
         * @param style row style; {@code null} means {@link MenuItemStyle#NORMAL}
         * @return the new row
         */
        public static MenuItem create(
                Component label,
                Component detail,
                Component tooltip,
                Boolean checked,
                Runnable action,
                boolean enabled,
                MenuItemStyle style
        ) {
            return new MenuItem(label, detail, tooltip, checked, action, enabled, style);
        }

        /** Creates a standard actionable context-menu item without a tooltip. */
        public static MenuItem action(Component label, Runnable action) {
            return action(label, label, action);
        }

        /** Creates a standard actionable context-menu item with an optional tooltip. */
        public static MenuItem action(Component label, Component tooltip, Runnable action) {
            return new MenuItem(label, Component.empty(), tooltip, null, action, true, MenuItemStyle.NORMAL);
        }

        /** Creates a context-menu toggle item with the supplied checked state. */
        public static MenuItem toggle(Component label, Component tooltip, boolean checked, Runnable action) {
            return new MenuItem(label, Component.empty(), tooltip, checked, action, true, MenuItemStyle.NORMAL);
        }

        /** Creates a destructive-action context-menu item without a tooltip. */
        public static MenuItem danger(Component label, Runnable action) {
            return danger(label, label, action);
        }

        /** Creates a destructive-action context-menu item with an optional tooltip. */
        public static MenuItem danger(Component label, Component tooltip, Runnable action) {
            return new MenuItem(label, Component.empty(), tooltip, null, action, true, MenuItemStyle.DANGER);
        }

        /** Creates a disabled context-menu item without a tooltip. */
        public static MenuItem disabled(Component label) {
            return disabled(label, label);
        }

        /** Creates a disabled context-menu item with an optional tooltip. */
        public static MenuItem disabled(Component label, Component tooltip) {
            return new MenuItem(label, Component.empty(), tooltip, null, () -> { }, false, MenuItemStyle.NORMAL);
        }

        /** Creates a non-interactive context-menu separator. */
        public static MenuItem separator() {
            return new MenuItem(Component.empty(), Component.empty(), Component.empty(), null, () -> { }, false, MenuItemStyle.SEPARATOR);
        }

        /** Returns the row text; never {@code null}. */
        public Component label() {
            return label;
        }

        /** Returns the secondary text shown to the right of the label; empty when unset. */
        public Component detail() {
            return detail;
        }

        /** Returns the hover text; empty when unset. */
        public Component tooltip() {
            return tooltip;
        }

        /**
         * Returns the check mark state.
         *
         * @return {@code true} or {@code false} for toggle rows, or {@code null} for rows without a check mark
         */
        public Boolean checked() {
            return checked;
        }

        /** Returns the click action; never {@code null}. */
        public Runnable action() {
            return action;
        }

        /** Returns whether the row can be clicked. Always {@code false} for separators. */
        public boolean enabled() {
            return enabled;
        }

        /** Returns the row style; never {@code null}. */
        public MenuItemStyle style() {
            return style;
        }
    }

    private KineticOverlays() {
    }

    /** Requests one unwrapped tooltip line at the supplied screen position. */
    public static void requestTooltip(Component line, int mouseX, int mouseY) {
        GuiOverlayRuntime.requestTooltip(line, mouseX, mouseY);
    }

    /** Requests unwrapped tooltip lines at the supplied screen position. */
    public static void requestTooltip(List<? extends Component> lines, int mouseX, int mouseY) {
        GuiOverlayRuntime.requestTooltip(lines, mouseX, mouseY);
    }

    /** Requests one tooltip line wrapped to the supplied maximum width. */
    public static void requestTooltip(Component line, int maxWidth, int mouseX, int mouseY) {
        GuiOverlayRuntime.requestTooltip(line, maxWidth, mouseX, mouseY);
    }

    /** Requests tooltip lines wrapped to the supplied maximum width. */
    public static void requestTooltip(List<? extends Component> lines, int maxWidth, int mouseX, int mouseY) {
        GuiOverlayRuntime.requestTooltip(lines, maxWidth, mouseX, mouseY);
    }

    /**
     * Requests pre-formatted tooltip lines for this frame, for text that was already split or styled.
     *
     * @param lines lines to show; {@code null} elements are skipped and an empty request is ignored
     * @param mouseX anchor X in screen coordinates
     * @param mouseY anchor Y in screen coordinates
     */
    public static void requestFormattedTooltip(List<FormattedCharSequence> lines, int mouseX, int mouseY) {
        GuiOverlayRuntime.requestFormattedTooltip(lines, mouseX, mouseY);
    }

    /**
     * Requests the standard item tooltip for this frame, including lines added by other mods.
     *
     * @param stack item to describe; copied, and ignored when {@code null} or empty
     * @param mouseX anchor X in screen coordinates
     * @param mouseY anchor Y in screen coordinates
     */
    public static void requestItemTooltip(ItemStack stack, int mouseX, int mouseY) {
        GuiOverlayRuntime.requestItemTooltip(stack, mouseX, mouseY);
    }

    /**
     * Opens the standard confirmation dialog on the current Kinetic screen.
     *
     * @param title dialog title
     * @param message dialog body
     * @param confirmText confirm button text
     * @param cancelText cancel button text
     * @param onConfirm run after the player confirms
     * @param onCancel run after the player cancels or closes the dialog
     * @return {@code true} if a Kinetic screen is open and showed the dialog; {@code false} otherwise, in which
     *   case neither callback runs
     */
    public static boolean openCurrentDialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        return GuiOverlayRuntime.openCurrentDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
    }

    /** Shows a transient toast using the default toast identity. */
    public static void toast(Component message) {
        if (message == null) return;
        toast(message.getString(), message, Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    /** Shows or replaces a transient toast identified by the supplied id. */
    public static void toast(String id, Component message) {
        toast(id, message, Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    /**
     * Shows a toast, replacing an active toast with the same id at the same position.
     *
     * @param id identity used for replacement and {@link #removeToast(String)}; {@code null} never replaces
     * @param message toast text; {@code null} does nothing
     * @param position screen anchor; {@code null} means {@link Position#BOTTOM_CENTER}
     * @param durationMs display time in milliseconds, at least 1
     * @param offsetX horizontal offset from the anchor in screen pixels
     * @param offsetY vertical offset from the anchor in screen pixels
     */
    public static void toast(
            String id,
            Component message,
            Position position,
            int durationMs,
            int offsetX,
            int offsetY
    ) {
        GuiOverlayRuntime.toast(id, message, position, durationMs, offsetX, offsetY);
    }

    /** Removes every active toast with this id at any position; {@code null} does nothing. */
    public static void removeToast(String id) {
        GuiOverlayRuntime.removeToast(id);
    }

    /** Removes every active toast. */
    public static void clearToasts() {
        GuiOverlayRuntime.clearToasts();
    }
}
