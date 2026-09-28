package dev.xyat.kineticcore.api.client.gui.widget;

import dev.xyat.kineticcore.api.client.gui.input.CharInput;
import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.internal.client.gui.page.CustomControlSupport;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * 附属自定义控件的基类：页面负责注册、渲染、输入分发、焦点与提示，子类只实现绘制与输入逻辑。用
 * {@code ui.add(new MyControl(...))} 注册。
 * Base class for addon-defined controls. The page owns registration, rendering, input dispatch, focus and
 * tooltips; subclasses only implement drawing and input. Register with {@code ui.add(new MyControl(...))}.
 */
public abstract class KineticCustomControl implements KineticControl {
    static {
        CustomControlSupport.install(new Access());
    }

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean visible = true;
    private boolean enabled = true;
    private Supplier<Component> tooltip;
    private Object widget;

    /** 创建控件 / Creates the control. */
    protected KineticCustomControl(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    @Override
    public final int controlX() {
        return x;
    }

    @Override
    public final int controlY() {
        return y;
    }

    @Override
    public final int controlWidth() {
        return width;
    }

    @Override
    public final int controlHeight() {
        return height;
    }

    @Override
    public final void moveControlX(int x) {
        this.x = x;
    }

    @Override
    public final void moveControlY(int y) {
        this.y = y;
    }

    @Override
    public final void resizeControlWidth(int width) {
        this.width = Math.max(0, width);
    }

    /** 修改高度 / Changes the height. */
    public final void resizeControlHeight(int height) {
        this.height = Math.max(0, height);
    }

    @Override
    public final boolean controlVisible() {
        return visible;
    }

    @Override
    public final void setControlVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public final void setTooltip(Component tooltip) {
        this.tooltip = tooltip == null ? null : () -> tooltip;
    }

    @Override
    public final void setTooltip(Supplier<Component> tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public final boolean controlHovered() {
        return CustomControlSupport.isHovered(widget);
    }

    @Override
    public final boolean controlFocused() {
        return CustomControlSupport.isFocused(widget);
    }

    /** 绘制控件（仅在可见时调用）/ Draws the control; only called while visible. */
    protected abstract void render(KineticGraphics graphics, int mouseX, int mouseY, float partialTick);

    /**
     * 播放原版按钮点击音效，供自绘按钮在点击时调用。
     * Plays the vanilla button click sound; call it from a self-drawn button's click handler.
     */
    protected static void playClickSound() {
        dev.xyat.kineticcore.internal.client.gui.UiSounds.buttonClick();
    }

    /** 控件范围内的按下 / Press inside the control. */
    protected boolean onMouseClick(MouseInput input) {
        return false;
    }

    /** 松开 / Release. */
    protected boolean onMouseRelease(MouseInput input) {
        return false;
    }

    /** 拖动 / Drag. */
    protected boolean onMouseDrag(MouseDragInput input) {
        return false;
    }

    /** 控件范围内的滚轮 / Wheel inside the control. */
    protected boolean onMouseScroll(ScrollInput input) {
        return false;
    }

    /** 拥有焦点时的按键 / Key press while focused. */
    protected boolean onKeyPress(KeyInput input) {
        return false;
    }

    /** 拥有焦点时的字符输入 / Character input while focused. */
    protected boolean onCharTyped(CharInput input) {
        return false;
    }

    /** 是否可获得键盘焦点 / Whether the control can take keyboard focus. */
    protected boolean isFocusable() {
        return false;
    }

    /** 焦点变化 / Focus changed. */
    protected void onFocusChanged(boolean focused) {
    }

    /** 每 tick 调用 / Called every tick. */
    protected void onTick() {
    }

    /** 无障碍朗读文本 / Narration text. */
    protected Component narration() {
        return Component.empty();
    }

    private static final class Access implements CustomControlSupport.Accessor {
        @Override
        public void attach(KineticCustomControl control, Object widget) {
            control.widget = widget;
        }

        @Override
        public Object widget(KineticCustomControl control) {
            return control.widget;
        }

        @Override
        public Supplier<Component> tooltip(KineticCustomControl control) {
            return control.tooltip;
        }

        @Override
        public void render(KineticCustomControl control, KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
            control.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean onMouseClick(KineticCustomControl control, MouseInput input) {
            return control.onMouseClick(input);
        }

        @Override
        public boolean onMouseRelease(KineticCustomControl control, MouseInput input) {
            return control.onMouseRelease(input);
        }

        @Override
        public boolean onMouseDrag(KineticCustomControl control, MouseDragInput input) {
            return control.onMouseDrag(input);
        }

        @Override
        public boolean onMouseScroll(KineticCustomControl control, ScrollInput input) {
            return control.onMouseScroll(input);
        }

        @Override
        public boolean onKeyPress(KineticCustomControl control, KeyInput input) {
            return control.onKeyPress(input);
        }

        @Override
        public boolean onCharTyped(KineticCustomControl control, CharInput input) {
            return control.onCharTyped(input);
        }

        @Override
        public boolean isFocusable(KineticCustomControl control) {
            return control.isFocusable();
        }

        @Override
        public void onFocusChanged(KineticCustomControl control, boolean focused) {
            control.onFocusChanged(focused);
        }

        @Override
        public void onTick(KineticCustomControl control) {
            control.onTick();
        }

        @Override
        public Component narration(KineticCustomControl control) {
            return control.narration();
        }
    }
}
