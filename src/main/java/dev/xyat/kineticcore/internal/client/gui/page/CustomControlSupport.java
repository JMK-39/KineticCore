package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.input.CharInput;
import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import dev.xyat.kineticcore.api.client.gui.widget.KineticCustomControl;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Supplier;

/** Internal support for {@link KineticCustomControl}: accessor bridge and the vanilla widget adapter. */
public final class CustomControlSupport {
    /** Hook access implemented privately inside {@link KineticCustomControl}. */
    public interface Accessor {
        void attach(KineticCustomControl control, Object widget);

        Object widget(KineticCustomControl control);

        Supplier<Component> tooltip(KineticCustomControl control);

        void render(KineticCustomControl control, KineticGraphics graphics, int mouseX, int mouseY, float partialTick);

        boolean onMouseClick(KineticCustomControl control, MouseInput input);

        boolean onMouseRelease(KineticCustomControl control, MouseInput input);

        boolean onMouseDrag(KineticCustomControl control, MouseDragInput input);

        boolean onMouseScroll(KineticCustomControl control, ScrollInput input);

        boolean onKeyPress(KineticCustomControl control, KeyInput input);

        boolean onCharTyped(KineticCustomControl control, CharInput input);

        boolean isFocusable(KineticCustomControl control);

        void onFocusChanged(KineticCustomControl control, boolean focused);

        void onTick(KineticCustomControl control);

        Component narration(KineticCustomControl control);
    }

    private static volatile Accessor accessor;

    private CustomControlSupport() {
    }

    public static void install(Accessor installed) {
        accessor = Objects.requireNonNull(installed, "accessor");
    }

    private static Accessor access() {
        return Objects.requireNonNull(accessor, "KineticCustomControl accessor was not installed");
    }

    /** Creates (or returns) the widget adapter of a custom control. */
    public static Widget adapt(KineticCustomControl control) {
        Object existing = access().widget(control);
        if (existing instanceof Widget widget) return widget;
        if (existing != null) throw new IllegalStateException("Custom control is already attached: " + control);
        Widget widget = new Widget(control);
        access().attach(control, widget);
        return widget;
    }

    /** Resolves any public control to its internal widget-backed control. */
    public static InternalControl widget(KineticControl control) {
        if (control instanceof InternalControl internal) return internal;
        if (control instanceof KineticCustomControl custom && access().widget(custom) instanceof Widget widget) return widget;
        throw new IllegalArgumentException("Control is not registered with a Kinetic page: " + control);
    }

    /** Maps an internal widget back to the control addons know. */
    public static KineticControl publicControl(KineticControl control) {
        return control instanceof Widget widget ? widget.control : control;
    }

    public static boolean isHovered(Object widget) {
        return widget instanceof Widget adapter && adapter.isHovered();
    }

    public static boolean isFocused(Object widget) {
        return widget instanceof Widget adapter && adapter.isFocused();
    }

    /** Ticks a custom control's widget adapter. */
    public static void tick(Widget widget) {
        access().onTick(widget.control);
    }

    /** Vanilla widget adapter hosting one custom control. */
    public static final class Widget extends AbstractWidget implements InternalControl {
        private int pressedButton = -1;
        private final KineticCustomControl control;

        private Widget(KineticCustomControl control) {
            super(control.controlX(), control.controlY(), control.controlWidth(), control.controlHeight(), Component.empty());
            this.control = control;
        }

        public KineticCustomControl control() {
            return control;
        }

        /** Current tooltip supplier of the custom control. */
        public Component tooltip() {
            Supplier<Component> supplier = access().tooltip(control);
            return supplier == null ? null : supplier.get();
        }

        private void sync() {
            super.setX(control.controlX());
            super.setY(control.controlY());
            super.setWidth(control.controlWidth());
            this.height = control.controlHeight();
            this.visible = control.controlVisible();
            this.active = control.isEnabled();
        }

        @Override
        public void setX(int x) {
            control.moveControlX(x);
            super.setX(x);
        }

        @Override
        public void setY(int y) {
            control.moveControlY(y);
            super.setY(y);
        }

        @Override
        public void setWidth(int width) {
            control.resizeControlWidth(width);
            super.setWidth(width);
        }

        @Override
        public void setVisible(boolean visible) {
            control.setControlVisible(visible);
            this.visible = visible;
        }

        @Override
        public void setEnabled(boolean enabled) {
            control.setEnabled(enabled);
            this.active = enabled;
        }

        @Override
        public boolean isVisible() {
            return control.controlVisible();
        }

        @Override
        public boolean isEnabled() {
            return control.isEnabled();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            sync();
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            access().render(control, GuiGraphicsAdapter.wrap(graphics), mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            sync();
            return visible && mouseX >= getX() && mouseY >= getY()
                    && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            sync();
            if (!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
            boolean handled = access().onMouseClick(control, InputRecords.mouse(mouseX, mouseY, button));
            if (handled) pressedButton = button;
            return handled;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            sync();
            if (button == pressedButton) pressedButton = -1;
            if (!active || !visible) return false;
            return access().onMouseRelease(control, InputRecords.mouse(mouseX, mouseY, button));
        }

        /**
         * 按下后尚未收到松开的按键（原版只把松开发给鼠标下的子控件，拖出控件外松开会丢失）。
         * The button pressed on this control and not yet released (vanilla only sends releases to the child under
         * the pointer, so a release outside the control would otherwise be lost).
         */
        boolean isPressed(int button) {
            return pressedButton == button;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            sync();
            if (!active || !visible) return false;
            return access().onMouseDrag(control, InputRecords.drag(mouseX, mouseY, button, dragX, dragY));
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            sync();
            if (!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
            return access().onMouseScroll(control, InputRecords.scroll(mouseX, mouseY, delta));
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!active || !visible || !isFocused()) return false;
            return access().onKeyPress(control, InputRecords.key(keyCode, scanCode, modifiers));
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (!active || !visible || !isFocused()) return false;
            return access().onCharTyped(control, InputRecords.character(codePoint, modifiers));
        }

        @Override
        public void setFocused(boolean focused) {
            boolean next = focused && access().isFocusable(control);
            if (next == isFocused()) return;
            super.setFocused(next);
            access().onFocusChanged(control, next);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            Component narration = access().narration(control);
            if (narration != null && !narration.getString().isBlank()) {
                output.add(NarratedElementType.TITLE, narration);
            }
        }
    }
}
