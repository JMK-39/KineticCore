package dev.xyat.kineticcore.api.client.widget.input;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import org.jetbrains.annotations.NotNull;
import java.util.function.Predicate;
import static dev.xyat.kineticcore.api.client.widget.KineticWidgets.attachTooltip;

/** 控件实现分组；附属统一从 KineticWidgets 工厂进入。 */
public final class KineticTextFields {
    private KineticTextFields() {}

    public static KineticEditBox createTextField(
            Font font, int x, int y, int width, Component message, Component tooltip
    ) {
        return createTextField(font, x, y, width, message, null, null, tooltip);
    }

    public static KineticEditBox createTextField(
            Font font, int x, int y, int width,
            Component message, Component placeholder, Component tooltip
    ) {
        return createTextField(font, x, y, width, message, placeholder, null, tooltip);
    }

    public static KineticEditBox createTextField(
            Font font, int x, int y, int width,
            Component message, Component placeholder, Predicate<String> validator, Component tooltip
    ) {
        KineticEditBox box = new KineticEditBox(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message
        );
        box.setPlaceholder(placeholder);
        box.setValidator(validator);
        attachTooltip(box, tooltip);
        return box;
    }

    public static MultiLineEditBox createMultiLineTextField(
            Font font, int x, int y, int width, int height,
            Component message, Component placeholder, Component tooltip
    ) {
        MultiLineEditBox box = new KineticMultiLineEditBox(
                font, x, y, width, Math.max(20, height),
                message == null ? Component.empty() : message,
                placeholder == null ? Component.empty() : placeholder
        );
        attachTooltip(box, tooltip);
        return box;
    }

    public static class KineticMultiLineEditBox extends MultiLineEditBox {
        public KineticMultiLineEditBox(
                Font font, int x, int y, int width, int height,
                Component message, Component placeholder
        ) {
            super(font, x, y, width, height, message, placeholder);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            GuiTheme.stateOutline(
                    graphics, getX(), getY(), getWidth(), getHeight(),
                    isFocused(), isHovered(), false
            );
        }
    }

    public static KineticEditBox createCompactTextField(
            Font font, int x, int y, int width, Component message, Component tooltip
    ) {
        return createCompactTextField(font, x, y, width, message, null, null, tooltip);
    }

    public static KineticEditBox createCompactTextField(
            Font font, int x, int y, int width,
            Component message, Component placeholder, Component tooltip
    ) {
        return createCompactTextField(font, x, y, width, message, placeholder, null, tooltip);
    }

    public static KineticEditBox createCompactTextField(
            Font font, int x, int y, int width,
            Component message, Component placeholder, Predicate<String> validator, Component tooltip
    ) {
        KineticEditBox box = new KineticEditBox(
                font, x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT,
                message == null ? Component.empty() : message
        );
        box.setPlaceholder(placeholder);
        box.setValidator(validator);
        attachTooltip(box, tooltip);
        return box;
    }

    public static ValidationEditBox createValidatingCompactTextField(
            Font font, int x, int y, int width, Component message, Component tooltip
    ) {
        ValidationEditBox box = new ValidationEditBox(
                font, x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT,
                message == null ? Component.empty() : message
        );
        attachTooltip(box, tooltip);
        return box;
    }

    public static class KineticEditBox extends EditBox {
        private final Font font;
        private boolean kineticBordered = true;
        private boolean validationError;
        private Predicate<String> validator = ignored -> true;
        private Component placeholder = Component.empty();

        public KineticEditBox(Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
            this.font = font;
        }

        public void setPlaceholder(Component placeholder) {
            this.placeholder = placeholder == null ? Component.empty() : placeholder;
        }

        public Component placeholder() {
            return placeholder;
        }

        @Override
        public void setBordered(boolean bordered) {
            super.setBordered(bordered);
            this.kineticBordered = bordered;
        }

        public boolean isBordered() {
            return kineticBordered;
        }

        public void setValidationError(boolean validationError) {
            this.validationError = validationError;
        }

        public boolean hasValidationError() {
            return validationError;
        }

        public void setValidator(Predicate<String> validator) {
            this.validator = validator == null ? ignored -> true : validator;
        }

        public boolean isValueValid() {
            return validator.test(getValue());
        }

        protected boolean hasBorderError() {
            return validationError || !isValueValid();
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (!isFocused() && getValue().isEmpty() && !placeholder.getString().isBlank()) {
                KineticText.drawScrollingLeft(
                        graphics,
                        font,
                        placeholder,
                        getX() + 5,
                        getY() + (getHeight() - font.lineHeight) / 2,
                        Math.max(0, getWidth() - 10),
                        GuiTheme.current().mutedText(),
                        false
                );
            }
            if (!kineticBordered) return;

            boolean focused = isFocused();
            boolean hovered = isMouseOver(mouseX, mouseY);
            boolean error = hasBorderError();
            if (!focused && !hovered && !error) return;

            GuiTheme.stateOutline(
                    graphics,
                    getX() - 1,
                    getY() - 1,
                    getWidth() + 2,
                    getHeight() + 2,
                    focused,
                    hovered,
                    error
            );
        }
    }

    public static class ValidationEditBox extends KineticEditBox {
        private long errorTime = -1L;

        public ValidationEditBox(Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
        }

        public void showError() {
            errorTime = net.minecraft.Util.getMillis();
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (errorTime > 0L) {
                long elapsed = net.minecraft.Util.getMillis() - errorTime;
                if (elapsed > 1000L) {
                    errorTime = -1L;
                    setHighlightPos(getCursorPosition());
                } else if (elapsed > 200L) {
                    int cycle = (int) ((elapsed - 200L) / 200L);
                    if (cycle == 0 || cycle == 2) {
                        setHighlightPos(0);
                        setCursorPosition(getValue().length());
                    } else {
                        setHighlightPos(getCursorPosition());
                    }
                }
            }
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected boolean hasBorderError() {
            return super.hasBorderError() || errorTime > 0L;
        }
    }
}
