package dev.xyat.kineticcore.internal.client.gui.widget.input;

import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;

import dev.xyat.kineticcore.api.client.gui.widget.KineticTextField;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTextArea;

import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import dev.xyat.kineticcore.internal.client.widget.KineticValidation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;
import dev.xyat.kineticcore.internal.client.gui.text.KineticText;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
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

    /**
     * Keeps vanilla editing, cursor and selection behavior while removing its forced text shadow.
     *
     * <p>The wrapper starts from the target's current transform, so text drawn through {@link #pose()} by
     * vanilla scroll widgets or by font mods that bypass {@code drawString} stays inside the responsive canvas.
     * Scissor calls are forwarded to the target so they keep its canvas conversion and nesting.
     */
    private static final class ShadowlessGraphics extends GuiGraphics {
        private final GuiGraphics target;
        private final Integer valueColor;

        private ShadowlessGraphics(GuiGraphics target) {
            this(target, null);
        }

        private ShadowlessGraphics(GuiGraphics target, Integer valueColor) {
            super(Minecraft.getInstance(), target.bufferSource());
            this.target = target;
            this.valueColor = valueColor;
            PoseStack.Pose source = target.pose().last();
            PoseStack.Pose local = pose().last();
            local.pose().set(source.pose());
            local.normal().set(source.normal());
        }

        private int textColor(int original) {
            return valueColor != null && original == -2039584 ? valueColor : original;
        }

        @Override
        public int drawString(Font font, String text, int x, int y, int color) {
            return super.drawString(font, text, x, y, textColor(color), false);
        }

        @Override
        public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
            return super.drawString(font, text, x, y, textColor(color), false);
        }

        @Override
        public int drawString(Font font, Component text, int x, int y, int color) {
            return super.drawString(font, text, x, y, textColor(color), false);
        }

        @Override
        public void enableScissor(int left, int top, int right, int bottom) {
            target.enableScissor(left, top, right, bottom);
        }

        @Override
        public void disableScissor() {
            target.disableScissor();
        }
    }

    /** Standard multi-line Kinetic text field used by the public widget factories. */
    public static class KineticMultiLineEditBox extends MultiLineEditBox implements InternalControl, KineticTextArea {
        /** Creates the concrete multiline implementation used by Kinetic widget factories. */
        public KineticMultiLineEditBox(
                FactoryAccess access, Font font, int x, int y, int width, int height,
                Component message, Component placeholder
        ) {
            // 占位提示由本类以纯白绘制，原版占位（浅灰）不再使用 / The placeholder is drawn here in pure white instead of
            // vanilla's light-gray placeholder.
            super(font, x, y, width, height, message, Component.empty());
            Objects.requireNonNull(access, "factory access");
            this.font = font;
            this.placeholder = placeholder == null ? Component.empty() : placeholder;
        }

        private final Font font;
        private final Component placeholder;
        private String initialText;

        @Override
        public String textValue() {
            return getValue();
        }

        @Override
        public void setTextValue(String value) {
            setValue(value);
        }

        @Override
        public void limitTextLength(int limit) {
            setCharacterLimit(limit);
        }

        @Override
        public void onTextChange(java.util.function.Consumer<String> listener) {
            setValueListener(listener);
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
            if (initialText == null) initialText = getValue();
            int color = Objects.equals(getValue(), initialText)
                    ? GuiTheme.fieldDefaultText() : GuiTheme.fieldModifiedText();
            super.renderWidget(new ShadowlessGraphics(graphics, color), mouseX, mouseY, partialTick);
            if (!isFocused() && getValue().isEmpty() && !placeholder.getString().isBlank()) {
                String plain = net.minecraft.ChatFormatting.stripFormatting(placeholder.getString());
                graphics.drawWordWrap(font, Component.literal(plain == null ? "" : plain),
                        getX() + 4, getY() + 4, Math.max(1, getWidth() - 8), GuiTheme.fieldPlaceholderText());
            }
            GuiTheme.stateOutline(
                    graphics, getX(), getY(), getWidth(), getHeight(),
                    isFocused(), isHovered(), false
            );
        }
    }

    /** Standard single-line Kinetic text field used by the public widget factories. */
    public static class KineticEditBox extends EditBox implements InternalControl, KineticTextField {
        private final Font font;
        private boolean textEditable;
        private boolean validationError;
        private long validationErrorStartMillis = -1L;
        private Predicate<String> validator = ignored -> true;
        private Component placeholder = Component.empty();
        private String defaultText = null;
        private boolean defaultFromFirstShownText;
        private java.util.function.Function<String, Integer> valueColor = null;

        /** Factory-only constructor used by standard Kinetic widget factories. */
        public KineticEditBox(FactoryAccess access, Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
            Objects.requireNonNull(access, "factory access");
            this.font = font;
            setTextColor(GuiTheme.fieldText());
            setTextColorUneditable(GuiTheme.fieldMutedText());
            setTextEditable(true);
        }

        @Override
        public void setDefaultText(String defaultText) {
            this.defaultText = defaultText;
            this.defaultFromFirstShownText = false;
        }

        @Override
        public void useFirstShownTextAsDefault() {
            this.defaultText = null;
            this.defaultFromFirstShownText = true;
        }

        @Override
        public String defaultText() {
            return defaultText;
        }

        @Override
        public void setValueColor(java.util.function.Function<String, Integer> valueColor) {
            this.valueColor = valueColor;
        }

        /**
         * 内容颜色：自定义规则优先；否则等于默认值为青色，其它（已修改）为绿色。
         * Value color: invalid values are red; otherwise a custom rule wins, then cyan or green.
         */
        private int resolveValueColor(String value) {
            if (hasBorderError()) return GuiTheme.fieldErrorText();
            if (defaultFromFirstShownText) {
                defaultText = value;
                defaultFromFirstShownText = false;
            }
            if (valueColor != null) {
                try {
                    Integer custom = valueColor.apply(value);
                    if (custom != null) return custom;
                } catch (RuntimeException ignored) {
                    // 业务颜色回调出错时回退到标准规则 / Fall back to the standard rule when the callback fails.
                }
            }
            return defaultText != null && defaultText.equals(value) ? GuiTheme.fieldDefaultText() : GuiTheme.fieldModifiedText();
        }

        // ---- 公共 API（稳定名称）委托给原版 EditBox / Public API (stable names) delegating to vanilla EditBox ----
        @Override
        public String textValue() {
            return getValue();
        }

        @Override
        public void setTextValue(String value) {
            setValue(value);
        }

        @Override
        public void onTextChange(java.util.function.Consumer<String> responder) {
            setResponder(responder);
        }

        @Override
        public void limitTextLength(int maxLength) {
            setMaxLength(maxLength);
        }

        @Override
        public void filterText(Predicate<String> filter) {
            setFilter(filter);
        }

        @Override
        public void formatText(java.util.function.BiFunction<String, Integer, net.minecraft.util.FormattedCharSequence> formatter) {
            setFormatter(formatter);
        }

        @Override
        public int cursorIndex() {
            return getCursorPosition();
        }

        @Override
        public void setCursorIndex(int position) {
            setCursorPosition(position);
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
                    KineticTheme.Surface.FIELD,
                    focused,
                    hovered,
                    error
            );

            int contentX = InputTextLayout.textLeft(fieldX);
            int contentY = InputTextLayout.textTop(fieldY, fieldHeight, font.lineHeight);
            int contentWidth = InputTextLayout.textWidth(fieldWidth);

            // Let vanilla keep cursor/selection/scroll semantics, but render only the text layer.
            // Bounds are restored immediately so hit testing and business layout always see the
            // API-level field rectangle rather than the temporary text rectangle.
            setTextColor(resolveValueColor(getValue()));
            super.setBordered(false);
            setX(contentX);
            setY(contentY);
            setWidth(contentWidth);
            try {
                super.renderWidget(new ShadowlessGraphics(graphics), mouseX, mouseY, partialTick);
            } finally {
                setWidth(fieldWidth);
                setX(fieldX);
                setY(fieldY);
                super.setBordered(true);
            }

            if (!focused && getValue().isEmpty() && !placeholder.getString().isBlank()) {
                // 占位提示统一纯白：去掉文本自带颜色与格式码 / Placeholder is always pure white: strip styles and codes.
                String plain = net.minecraft.ChatFormatting.stripFormatting(placeholder.getString());
                KineticText.drawScrollingLeft(
                        graphics,
                        font,
                        Component.literal(plain == null ? "" : plain),
                        contentX,
                        contentY,
                        contentWidth,
                        GuiTheme.fieldPlaceholderText(),
                        false
                );
            }
        }
    }


}
