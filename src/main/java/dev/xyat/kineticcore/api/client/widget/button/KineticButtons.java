package dev.xyat.kineticcore.api.client.widget.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import static dev.xyat.kineticcore.api.client.widget.KineticWidgets.attachTooltip;

/** 控件实现分组；附属统一从 KineticWidgets 工厂进入。 */
public final class KineticButtons {
    private KineticButtons() {}

    public static Button createButton(
            int x, int y, int width, Component text, Component tooltip, Button.OnPress action
    ) {
        return createButton(x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT, text, tooltip, action);
    }

    public static Button createCompactButton(
            int x, int y, int width, Component text, Component tooltip, Button.OnPress action
    ) {
        return createButton(x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT, text, tooltip, action);
    }

    public static TextureButton createTextureButton(
            int x,
            int y,
            int width,
            int height,
            ResourceLocation texture,
            int u,
            int v,
            int hoverVOffset,
            int textureWidth,
            int textureHeight,
            Component narration,
            Component tooltip,
            Button.OnPress action
    ) {
        TextureButton button = new TextureButton(
                x, y, width, height, texture, u, v, hoverVOffset, textureWidth, textureHeight,
                narration == null ? Component.empty() : narration,
                action == null ? ignored -> { } : action
        );
        attachTooltip(button, tooltip);
        return button;
    }

    public static void renderTextureButtonIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            ResourceLocation texture,
            int u,
            int v,
            int hoverVOffset,
            int textureWidth,
            int textureHeight,
            boolean hovered
    ) {
        if (graphics == null || texture == null) return;
        graphics.blit(
                texture,
                x,
                y,
                u,
                hovered ? v + hoverVOffset : v,
                width,
                height,
                textureWidth,
                textureHeight
        );
    }

    public static MenuButton createMenuButton(
            Component text,
            boolean enabled,
            boolean danger,
            Button.OnPress action
    ) {
        MenuButton button = new MenuButton(
                0, 0, 1, KineticScreen.STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                action == null ? ignored -> { } : action,
                danger
        );
        button.active = enabled;
        return button;
    }

    private static Button createButton(
            int x, int y, int width, int height, Component text, Component tooltip, Button.OnPress action
    ) {
        StateButton button = new StateButton(
                x,
                y,
                width,
                height,
                text == null ? Component.empty() : text,
                pressed -> { if (action != null) action.onPress(pressed); }
        );
        attachTooltip(button, tooltip);
        return button;
    }

    public static void setButtonSelected(Button button, boolean selected) {
        if (button instanceof StateButton stateButton) {
            stateButton.setSelected(selected);
        }
    }

    public static void setButtonError(Button button, boolean error) {
        if (button instanceof StateButton stateButton) {
            stateButton.setError(error);
        }
    }

    public static boolean isButtonSelected(Button button) {
        return button instanceof StateButton stateButton && stateButton.isSelectedState();
    }

    public static boolean isButtonError(Button button) {
        return button instanceof StateButton stateButton && stateButton.isErrorState();
    }

    public static ToggleButton createToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return createToggleButton(
                x, y, width, value, onText, offText, tooltip,
                ignored -> true, responder
        );
    }

    public static ToggleButton createToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder
    ) {
        ToggleButton button = new ToggleButton(
                x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                value,
                onText == null ? Component.empty() : onText,
                offText == null ? Component.empty() : offText,
                validator,
                responder
        );
        attachTooltip(button, tooltip);
        return button;
    }

    public static ColorSwatchButton createColorSwatchButton(
            int x,
            int y,
            int rgb,
            Component tooltip,
            Runnable action
    ) {
        ColorSwatchButton button = new ColorSwatchButton(
                x, y, KineticScreen.COMPACT_CONTROL_HEIGHT, rgb,
                ignored -> { if (action != null) action.run(); }
        );
        attachTooltip(button, tooltip);
        return button;
    }

    public static ColorPreviewButton createColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        ColorPreviewButton button = new ColorPreviewButton(
                x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT, color,
                text == null ? Component.empty() : text,
                ignored -> { if (action != null) action.run(); }
        );
        attachTooltip(button, tooltip);
        return button;
    }

    public static HighZButton createHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        return createHighZButton(
                x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                text, tooltip, zLevel, action
        );
    }

    public static HighZButton createCompactHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        return createHighZButton(
                x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT,
                text, tooltip, zLevel, action
        );
    }

    private static HighZButton createHighZButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        HighZButton button = new HighZButton(
                x, y, width, height,
                text == null ? Component.empty() : text,
                action == null ? ignored -> { } : action,
                null,
                zLevel
        );
        attachTooltip(button, tooltip);
        return button;
    }

    public static final class TextureButton extends Button {
        private final ResourceLocation texture;
        private final int u;
        private final int v;
        private final int hoverVOffset;
        private final int textureWidth;
        private final int textureHeight;

        private TextureButton(
                int x,
                int y,
                int width,
                int height,
                ResourceLocation texture,
                int u,
                int v,
                int hoverVOffset,
                int textureWidth,
                int textureHeight,
                Component narration,
                Button.OnPress onPress
        ) {
            super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
            this.texture = Objects.requireNonNull(texture, "texture");
            this.u = u;
            this.v = v;
            this.hoverVOffset = hoverVOffset;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }


        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderTextureButtonIcon(
                    graphics,
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    texture,
                    u,
                    v,
                    hoverVOffset,
                    textureWidth,
                    textureHeight,
                    isHoveredOrFocused()
            );
        }

        @Override
        public void updateWidgetNarration(@NotNull NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    public static class StateButton extends Button {
        private boolean selected;
        private boolean error;

        public StateButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress
        ) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public void setError(boolean error) {
            this.error = error;
        }

        public boolean isSelectedState() {
            return selected;
        }

        public boolean isErrorState() {
            return error;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (selected || error) {
                GuiTheme.stateOutline(
                        graphics,
                        getX(),
                        getY(),
                        getWidth(),
                        getHeight(),
                        selected,
                        isHovered(),
                        error
                );
            }
        }
    }

    public static class ColorSwatchButton extends Button {
        private int rgb;

        public ColorSwatchButton(int x, int y, int size, int rgb, OnPress onPress) {
            super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
            this.rgb = rgb & 0xFFFFFF;
        }

        public void setRgb(int rgb) {
            this.rgb = rgb & 0xFFFFFF;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            int inset = Math.max(4, Math.min(getWidth(), getHeight()) / 4);
            graphics.fill(
                    getX() + inset,
                    getY() + inset,
                    getX() + getWidth() - inset,
                    getY() + getHeight() - inset,
                    0xFF000000 | rgb
            );
        }
    }

    public static class ColorPreviewButton extends StateButton {
        private int rgb;

        public ColorPreviewButton(int x, int y, int width, int height, int rgb, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress);
            this.rgb = rgb & 0xFFFFFF;
        }

        public void setRgb(int rgb) {
            this.rgb = rgb & 0xFFFFFF;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            int size = Math.min(12, Math.max(8, getHeight() - 8));
            int x = getX() + 6;
            int y = getY() + (getHeight() - size) / 2;
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFFFFFFFF);
            graphics.fill(x, y, x + size, y + size, 0xFF000000 | rgb);
        }
    }

    public static class MenuButton extends StateButton {
        public MenuButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean danger) {
            super(x, y, width, height, message, onPress);
            setError(danger);
        }

        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }
    }

    public static class HighZButton extends StateButton {
        private final int zLevel;

        public HighZButton(int x, int y, int w, int h, Component msg, OnPress onPress, Tooltip tooltip) {
            this(x, y, w, h, msg, onPress, tooltip, 200);
        }

        public HighZButton(int x, int y, int w, int h, Component msg, OnPress onPress, Tooltip tooltip, int zLevel) {
            super(x, y, w, h, msg, onPress);
            if (tooltip != null) this.setTooltip(tooltip);
            this.zLevel = zLevel;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mx, int my, float pt) {
            g.pose().pushPose();
            g.pose().translate(0, 0, zLevel);
            try {
                super.renderWidget(g, mx, my, pt);
            } finally {
                g.pose().popPose();
            }
        }
    }

    public static class ToggleButton extends StateButton {
        private boolean value;
        private final Component onText;
        private final Component offText;
        private final Predicate<Boolean> validator;
        private final Consumer<Boolean> responder;

        public ToggleButton(
                int x,
                int y,
                int width,
                int height,
                boolean value,
                Component onText,
                Component offText,
                Predicate<Boolean> validator,
                Consumer<Boolean> responder
        ) {
            super(x, y, width, height, value ? onText : offText, ignored -> { });
            this.value = value;
            this.onText = Objects.requireNonNullElse(onText, Component.empty());
            this.offText = Objects.requireNonNullElse(offText, Component.empty());
            this.validator = validator == null ? ignored -> true : validator;
            this.responder = responder == null ? ignored -> { } : responder;
            setError(!this.validator.test(value));
        }

        public boolean value() {
            return value;
        }

        public void setValue(boolean value) {
            this.value = value;
            setMessage(value ? onText : offText);
            setError(!validator.test(value));
        }

        @Override
        public void onPress() {
            setValue(!value);
            responder.accept(value);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }
}
