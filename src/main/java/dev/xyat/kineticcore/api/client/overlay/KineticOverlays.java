package dev.xyat.kineticcore.api.client.overlay;

import dev.xyat.kineticcore.internal.client.overlay.GuiOverlayRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/** Public facade for external Kinetic overlay requests and global toasts. */
public final class KineticOverlays {
    /** Supported position values exposed by this API. */
    public enum Position {
        TOP_CENTER,
        BOTTOM_CENTER,
        CENTER,
        TOP_LEFT,
        TOP_RIGHT
    }

    /** Supported menu item style values exposed by this API. */
    public enum MenuItemStyle {
        NORMAL,
        DANGER,
        SEPARATOR
    }

    /** Public API type for menu item. */
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
         * Performs the create API operation.
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

        /**
         * Returns the label.
         */
        public Component label() {
            return label;
        }

        /**
         * Performs the detail API operation.
         */
        public Component detail() {
            return detail;
        }

        /**
         * Returns the tooltip.
         */
        public Component tooltip() {
            return tooltip;
        }

        /**
         * Performs the checked API operation.
         */
        public Boolean checked() {
            return checked;
        }

        /**
         * Performs the action API operation.
         */
        public Runnable action() {
            return action;
        }

        /**
         * Enables d.
         */
        public boolean enabled() {
            return enabled;
        }

        /**
         * Performs the style API operation.
         */
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
     * Requests formatted tooltip.
     */
    public static void requestFormattedTooltip(List<FormattedCharSequence> lines, int mouseX, int mouseY) {
        GuiOverlayRuntime.requestFormattedTooltip(lines, mouseX, mouseY);
    }

    /**
     * Requests item tooltip.
     */
    public static void requestItemTooltip(ItemStack stack, int mouseX, int mouseY) {
        GuiOverlayRuntime.requestItemTooltip(stack, mouseX, mouseY);
    }

    /**
     * Performs the open current dialog API operation.
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
     * Performs the toast API operation.
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

    /**
     * Removes toast.
     */
    public static void removeToast(String id) {
        GuiOverlayRuntime.removeToast(id);
    }

    /**
     * Performs the clear toasts API operation.
     */
    public static void clearToasts() {
        GuiOverlayRuntime.clearToasts();
    }
}
