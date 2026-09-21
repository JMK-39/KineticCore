package dev.xyat.kineticcore.api.client.widget.input;

import dev.xyat.kineticcore.api.client.widget.KineticControl;
import dev.xyat.kineticcore.internal.client.widget.KineticValidation;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.FactoryAccess;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Standard Kinetic text-field implementations returned by screen helpers and {@code KineticWidgets}.
 * Add-ons should create these controls through those factories so placeholder, tooltip, focus, and theme behavior
 * remain consistent.
 */
public final class KineticTextFields {
    private KineticTextFields() {}

    /** Standard multi-line Kinetic text field used by the public widget factories. */
    public static class KineticMultiLineEditBox extends MultiLineEditBox implements KineticControl {
        /** Creates the concrete multiline implementation used by Kinetic widget factories. */
        public KineticMultiLineEditBox(
                FactoryAccess access, Font font, int x, int y, int width, int height,
                Component message, Component placeholder
        ) {
            super(font, x, y, width, height, message, placeholder);
            Objects.requireNonNull(access, "factory access");
        }

        /** Sets whether this multiline field accepts interaction. */
        @Override
        public void setEnabled(boolean enabled) {
            this.active = enabled;
        }

        /** Shows or hides this multiline field. */
        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        /** Returns whether this multiline field currently accepts interaction. */
        @Override
        public boolean isEnabled() {
            return this.active;
        }

        /** Returns whether this multiline field is currently visible. */
        @Override
        public boolean isVisible() {
            return this.visible;
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

    /** Standard single-line Kinetic text field used by the public widget factories. */
    public static class KineticEditBox extends EditBox implements KineticControl {
        private final Font font;
        private boolean textEditable;
        private boolean validationError;
        private long validationErrorStartMillis = -1L;
        private Predicate<String> validator = ignored -> true;
        private Component placeholder = Component.empty();

        /** Factory-only constructor used by standard Kinetic widget factories. */
        public KineticEditBox(FactoryAccess access, Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
            Objects.requireNonNull(access, "factory access");
            this.font = font;
            setTextColor(GuiTheme.current().text());
            setTextColorUneditable(GuiTheme.current().mutedText());
            setTextEditable(true);
        }

        /** Sets display-only placeholder text; the placeholder never becomes the field value. */
        public void setPlaceholder(Component placeholder) {
            this.placeholder = placeholder == null ? Component.empty() : placeholder;
        }

        /** Returns the current display-only placeholder component. */
        public Component placeholder() {
            return placeholder;
        }

        @Override
        public final void setBordered(boolean bordered) {
            super.setBordered(true);
        }


        @Override
        public final void setEditable(boolean editable) {
            super.setEditable(editable);
            this.textEditable = editable;
        }

        /** Sets whether the text value itself may be edited while keeping the control visible. */
        public void setTextEditable(boolean editable) {
            setEditable(editable);
        }

        /** Returns whether the text value itself may currently be edited. */
        public boolean isTextEditable() {
            return textEditable;
        }

        /** Sets whether this text field accepts interaction. */
        public void setEnabled(boolean enabled) {
            this.active = enabled;
        }

        /** Sets whether this text field participates in rendering and hit testing. */
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        /** Returns whether this text field currently accepts interaction. */
        public boolean isEnabled() {
            return this.active;
        }

        /** Returns whether this text field currently participates in rendering and hit testing. */
        public boolean isVisible() {
            return this.visible;
        }

        /** Sets an explicit validation-error state in addition to the configured validator result. */
        public void setValidationError(boolean validationError) {
            this.validationError = validationError;
        }

        /** Returns whether an explicit validation-error state is currently set. */
        public boolean hasValidationError() {
            return validationError;
        }

        /** Starts the standard one-second transient validation-error feedback without changing the field value. */
        public void flashValidationError() {
            validationErrorStartMillis = Util.getMillis();
        }

        /** Replaces the raw-value validator; {@code null} accepts every value. */
        public void setValidator(Predicate<String> validator) {
            this.validator = validator == null ? ignored -> true : validator;
        }

        /** Returns whether the current raw input satisfies the configured validator. */
        public boolean isValueValid() {
            return KineticValidation.accepts(validator, getValue());
        }

        private void updateTransientValidationFeedback() {
            if (validationErrorStartMillis < 0L) return;
            long elapsed = Util.getMillis() - validationErrorStartMillis;
            if (elapsed > 1000L) {
                validationErrorStartMillis = -1L;
                setHighlightPos(getCursorPosition());
                return;
            }
            if (elapsed <= 200L) return;
            int cycle = (int) ((elapsed - 200L) / 200L);
            if (cycle == 0 || cycle == 2) {
                setHighlightPos(0);
                setCursorPosition(getValue().length());
            } else {
                setHighlightPos(getCursorPosition());
            }
        }

        /** Returns whether the field should render its error border from explicit, transient, or validator state. */
        protected boolean hasBorderError() {
            return validationError || validationErrorStartMillis >= 0L || !isValueValid();
        }

        /**
         * Renders all single-line Kinetic inputs through one API-owned content layout.
         * Add-ons must not draw their own input text/placeholder offsets: horizontal padding,
         * vertical centering, field surface and the single outline are owned here.
         */
        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            updateTransientValidationFeedback();

            int fieldX = getX();
            int fieldY = getY();
            int fieldWidth = getWidth();
            int fieldHeight = getHeight();
            boolean focused = isFocused();
            boolean hovered = isMouseOver(mouseX, mouseY);
            boolean error = hasBorderError();

            GuiTheme.stateSurface(
                    graphics,
                    fieldX,
                    fieldY,
                    fieldWidth,
                    fieldHeight,
                    GuiTheme.Surface.FIELD,
                    focused,
                    hovered,
                    error
            );

            int contentX = fieldX + 4;
            int contentY = fieldY + Math.max(0, Math.round((fieldHeight - font.lineHeight) / 2.0F));
            int contentWidth = Math.max(1, fieldWidth - 8);

            // Let vanilla keep cursor/selection/scroll semantics, but render only the text layer.
            // Bounds are restored immediately so hit testing and business layout always see the
            // API-level field rectangle rather than the temporary text rectangle.
            super.setBordered(false);
            setX(contentX);
            setY(contentY);
            setWidth(contentWidth);
            try {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            } finally {
                setWidth(fieldWidth);
                setX(fieldX);
                setY(fieldY);
                super.setBordered(true);
            }

            if (!focused && getValue().isEmpty() && !placeholder.getString().isBlank()) {
                KineticText.drawScrollingLeft(
                        graphics,
                        font,
                        placeholder,
                        contentX,
                        contentY,
                        contentWidth,
                        GuiTheme.current().mutedText(),
                        false
                );
            }
        }
    }


}
