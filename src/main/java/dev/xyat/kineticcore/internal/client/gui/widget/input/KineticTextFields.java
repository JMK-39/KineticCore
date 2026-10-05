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
import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;
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

    //? if <26.1 {
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
        public int drawString(@NotNull Font font, @NotNull String text, int x, int y, int color) {
            return super.drawString(font, text, x, y, textColor(color), false);
        }

        @Override
        public int drawString(@NotNull Font font, @NotNull FormattedCharSequence text, int x, int y, int color) {
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
    //?}

    //? if <26.1 {
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
    //?} else {
    /*// Standard multi-line Kinetic text field used by the public widget factories. On 26.1 MultiLineEditBox can only be
    // built through its builder, so this field wraps one and keeps the methods of the subclass used before.
    public static class KineticMultiLineEditBox extends net.minecraft.client.gui.components.AbstractWidget
            implements InternalControl, KineticTextArea {
        private final Font font;
        private final Component placeholder;
        private final MultiLineEditBox box;
        private String initialText;

        public KineticMultiLineEditBox(
                FactoryAccess access, Font font, int x, int y, int width, int height,
                Component message, Component placeholder
        ) {
            super(x, y, width, height, message);
            Objects.requireNonNull(access, "factory access");
            this.font = font;
            this.placeholder = placeholder == null ? Component.empty() : placeholder;
            // The placeholder is drawn here in pure white instead of the light-gray vanilla placeholder.
            this.box = MultiLineEditBox.builder().setX(x).setY(y).setTextShadow(false)
                    .setTextColor(GuiTheme.fieldDefaultText()).build(font, width, height, message);
        }

        public String getValue() {
            return box.getValue();
        }

        public void setValue(String value) {
            box.setValue(value);
        }

        public void setCharacterLimit(int limit) {
            box.setCharacterLimit(limit);
        }

        public void setValueListener(java.util.function.Consumer<String> listener) {
            box.setValueListener(listener);
        }

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

        @Override
        public void setEnabled(boolean enabled) {
            this.active = enabled;
        }

        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        @Override
        public boolean isEnabled() {
            return this.active;
        }

        @Override
        public boolean isVisible() {
            return this.visible;
        }

        private MultiLineEditBox box() {
            box.setX(getX());
            box.setY(getY());
            box.setWidth(getWidth());
            box.setHeight(getHeight());
            box.active = active;
            box.visible = visible;
            return box;
        }

        @Override
        protected void extractWidgetRenderState(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (initialText == null) initialText = getValue();
            int color = Objects.equals(getValue(), initialText)
                    ? GuiTheme.fieldDefaultText() : GuiTheme.fieldModifiedText();
            ((MultiLineTextColorAccess) box)
                    .kineticcore$setTextColor(color);
            box().extractRenderState(graphics, mouseX, mouseY, partialTick);
            if (!isFocused() && getValue().isEmpty() && !placeholder.getString().isBlank()) {
                String plain = net.minecraft.ChatFormatting.stripFormatting(placeholder.getString());
                VanillaGuiDraw.wordWrap(graphics, font, Component.literal(plain == null ? "" : plain),
                        getX() + 4, getY() + 4, Math.max(1, getWidth() - 8), GuiTheme.fieldPlaceholderText());
            }
            GuiTheme.stateOutline(
                    graphics, getX(), getY(), getWidth(), getHeight(),
                    isFocused(), isHovered(), false
            );
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            return box().mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
            return box().mouseReleased(event);
        }

        @Override
        public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
            return box().mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return box().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            return box().keyPressed(event);
        }

        @Override
        public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
            return box().charTyped(event);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            box.setFocused(focused);
        }

        @Override
        protected void updateWidgetNarration(@NotNull net.minecraft.client.gui.narration.NarrationElementOutput output) {
            box.updateWidgetNarration(output);
        }
    }
    *///?}

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

        /**
         * 原版只按“字段 X + 前缀宽度”计算字符位置，没有计入文字内边距和横向滚动，长命令时补全弹窗会错位。
         * 这里按实际绘制位置计算（滚出左边的字符归到文字起点），命令补全与用法提示因此始终对齐。
         * Vanilla ignores the text inset and horizontal scroll, so suggestions drift on long commands; this returns the
         * drawn position (characters scrolled off the left map to the text start).
         */
        @Override
        public int getScreenX(int charIndex) {
            String value = getValue();
            int left = InputTextLayout.textLeft(getX());
            if (charIndex <= 0 || charIndex > value.length()) return left;
            int start = 0;
            if ((Object) this instanceof EditBoxScrollAccess accessor) {
                start = Math.max(0, Math.min(accessor.kineticcore$getDisplayPos(), value.length()));
            }
            if (charIndex <= start) return left;
            return left + font.width(value.substring(start, charIndex));
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

        // A field that is not being edited shows its text from the start. Vanilla keeps the view scrolled to the
        // cursor, which is left at the end after a value is set or picked, so a long value showed only its end.
        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (!focused) showStart();
        }

        @Override
        public void setValue(String value) {
            super.setValue(value);
            if (!isFocused()) showStart();
        }

        /** Moves the cursor and the view back to the first character, without a selection. */
        protected final void showStart() {
            setCursorPosition(0);
            setHighlightPos(0);
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

        //? if >=26.1 {
        /*// Since 1.21.9 input arrives as events. The field and its subclasses keep the coordinate and key-code methods;
        // the events are forwarded to them, and they reach the vanilla behavior through super.
        private net.minecraft.client.input.MouseButtonEvent currentMouseEvent;
        private boolean currentDoubleClick;

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            currentMouseEvent = event;
            currentDoubleClick = doubleClick;
            return mouseClicked(event.x(), event.y(), event.button());
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            return keyPressed(event.key(), event.scancode(), event.modifiers());
        }

        @Override
        public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
            boolean handled = false;
            for (char character : Character.toChars(event.codepoint())) handled |= charTyped(character, 0);
            return handled;
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int modifiers = currentMouseEvent == null ? 0 : currentMouseEvent.modifiers();
            return super.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY,
                    new net.minecraft.client.input.MouseButtonInfo(button, modifiers)), currentDoubleClick);
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return super.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers));
        }

        public boolean charTyped(char codePoint, int modifiers) {
            return super.charTyped(new net.minecraft.client.input.CharacterEvent(codePoint));
        }
        *///?}

        private void renderVanillaText(
GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            //? if >=26.1 {
            /*setTextShadow(false);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            *///?} else {
            super.renderWidget(new ShadowlessGraphics(graphics), mouseX, mouseY, partialTick);
            //?}
        }

        //? if >=26.1 {
        /*// 26.1 keeps a list of formatters; the field installs one that applies the latest formatText function.
        private java.util.function.BiFunction<String, Integer, net.minecraft.util.FormattedCharSequence> formatter;

        @Override
        public void formatText(java.util.function.BiFunction<String, Integer, net.minecraft.util.FormattedCharSequence> formatter) {
            if (this.formatter == null) addFormatter((text, offset) -> this.formatter.apply(text, offset));
            this.formatter = formatter;
        }
        *///?} else {
        @Override
        public void formatText(java.util.function.BiFunction<String, Integer, net.minecraft.util.FormattedCharSequence> formatter) {
            setFormatter(formatter);
        }
        //?}

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
                renderVanillaText(graphics, mouseX, mouseY, partialTick);
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
