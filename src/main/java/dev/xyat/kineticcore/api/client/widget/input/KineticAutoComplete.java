package dev.xyat.kineticcore.api.client.widget.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import org.jetbrains.annotations.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.GridScrollController;
import static dev.xyat.kineticcore.api.client.widget.KineticWidgets.attachTooltip;

/** 控件实现分组；附属统一从 KineticWidgets 工厂进入。 */
public final class KineticAutoComplete {
    private KineticAutoComplete() {}

    public static AutoCompleteBox createAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        return createAutoCompleteField(
                font, x, y, width, message, null, dictionarySupplier, tooltip
        );
    }

    public static AutoCompleteBox createAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        AutoCompleteBox box = new AutoCompleteBox(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier
        );
        box.setPlaceholder(placeholder);
        attachTooltip(box, tooltip);
        return box;
    }

    public static NumericAutoCompleteBox createIntegerAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return createIntegerAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, null, tooltip);
    }

    public static NumericAutoCompleteBox createIntegerAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = integer(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier,
                allowNegative, minValue, maxValue, validator
        );
        attachTooltip(box, tooltip);
        return box;
    }

    public static NumericAutoCompleteBox createLongAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return createLongAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, null, tooltip);
    }

    public static NumericAutoCompleteBox createLongAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = longInteger(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier,
                allowNegative, minValue, maxValue, validator
        );
        attachTooltip(box, tooltip);
        return box;
    }

    public static NumericAutoCompleteBox createDecimalAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return createDecimalAutoCompleteField(font, x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, null, tooltip);
    }

    public static NumericAutoCompleteBox createDecimalAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = decimal(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier,
                allowNegative, minValue, maxValue, validator
        );
        attachTooltip(box, tooltip);
        return box;
    }

    public static class AutoCompleteBox extends KineticEditBox {
        private final Supplier<List<String>> dictionarySupplier;
        private List<String> suggestions = new ArrayList<>();
        private Consumer<String> externalResponder;
        private Consumer<String> selectionResponder;

        private final GridScrollController suggestionScroll =
                new GridScrollController();

        private int selectedIndex = -1;
        private int maxSuggestionWidth = 0;
        private static final int MAX_VISIBLE = 8;

        public AutoCompleteBox(Font font, int x, int y, int width, int height, Component message, Supplier<List<String>> dictionarySupplier) {
            super(font, x, y, width, height, message);
            this.dictionarySupplier = dictionarySupplier;
            this.setMaxLength(1024);
            this.setBordered(true);

            super.setResponder(val -> {
                this.updateSuggestions(val);
                if (this.externalResponder != null) this.externalResponder.accept(val);
            });
        }

        @Override
        public void setResponder(@NotNull Consumer<String> responder) {
            this.externalResponder = responder;
        }

        public void setSelectionResponder(Consumer<String> responder) {
            this.selectionResponder = responder;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (super.isMouseOver(mouseX, mouseY)) return true;
            if (isFocused() && !suggestions.isEmpty()) {
                int x = this.getX() - 4;
                int y = this.getY() + this.getHeight() + 4;
                int listH = Math.min(suggestions.size(), MAX_VISIBLE) * 12;
                int w = this.maxSuggestionWidth + (suggestions.size() > MAX_VISIBLE ? 10 : 0);
                return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + listH;
            }
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused) updateSuggestions(this.getValue());
            else clearSuggestions();
        }

        public void clearSuggestions() {
            this.suggestions.clear();
            this.suggestionScroll.reset();
            this.selectedIndex = -1;
            this.maxSuggestionWidth = 0;
        }

        public void loadSuggestions() {
            updateSuggestions(this.getValue());
        }

        private void updateSuggestions(String input) {
            List<String> allItems = dictionarySupplier.get();
            if (input.isEmpty()) {
                suggestions = new ArrayList<>(allItems);
            } else {
                String lower = input.toLowerCase();
                suggestions = allItems.stream().filter(s -> s.toLowerCase().contains(lower)).collect(Collectors.toList());
            }

            Font font = Minecraft.getInstance().font;
            int currentMax = this.width;
            for (String s : suggestions) {
                int w = font.width(s) + 10;
                if (w > currentMax) currentMax = w;
            }
            this.maxSuggestionWidth = currentMax;
            suggestionScroll.reset();
            suggestionScroll.update(
                    suggestions.size(),
                    MAX_VISIBLE
            );
            selectedIndex = -1;
        }

        public boolean handleMouseScrolled(double delta) {
            if (!isFocused() || suggestions.isEmpty()) {
                return false;
            }

            suggestionScroll.update(
                    suggestions.size(),
                    MAX_VISIBLE
            );

            return suggestionScroll.scroll(delta);
        }

        public boolean handleKeyPressed(int keyCode) {
            if (!isFocused()) return false;
            if (!suggestions.isEmpty()) {
                if (keyCode == 265) {
                    selectedIndex = (selectedIndex <= 0) ? suggestions.size() - 1 : selectedIndex - 1;
                    ensureVisible(); return true;
                }
                if (keyCode == 264) {
                    selectedIndex = (selectedIndex >= suggestions.size() - 1) ? 0 : selectedIndex + 1;
                    ensureVisible(); return true;
                }
                if (keyCode == 257 || keyCode == 335) {
                    if (selectedIndex >= 0 && selectedIndex < suggestions.size()) {
                        selectItem(selectedIndex); return true;
                    }
                }
            }
            return super.keyPressed(keyCode, 0, 0);
        }

        private void ensureVisible() {
            suggestionScroll.update(
                    suggestions.size(),
                    MAX_VISIBLE
            );

            if (selectedIndex < suggestionScroll.offset()) {
                suggestionScroll.setOffset(selectedIndex);
            }

            if (selectedIndex
                    >= suggestionScroll.offset() + MAX_VISIBLE) {
                suggestionScroll.setOffset(
                        selectedIndex - MAX_VISIBLE + 1
                );
            }
        }

        private void selectItem(int index) {
            String val = normalizeValue(suggestions.get(index));
            this.setValue(val);
            this.setCursorPosition(this.getValue().length());
            this.clearSuggestions();
            if (this.selectionResponder != null) {
                this.selectionResponder.accept(val);
            }
        }

        public void renderSuggestions(GuiGraphics gui, int mouseX, int mouseY) {
            if (!isFocused() || suggestions.isEmpty()) return;

            int x = this.getX() - 4;
            int y = this.getY() + this.getHeight() + 4;
            int itemH = 12;
            int w = this.maxSuggestionWidth;
            int visibleCount = Math.min(suggestions.size(), MAX_VISIBLE);
            int totalH = visibleCount * itemH;

            suggestionScroll.update(
                    suggestions.size(),
                    MAX_VISIBLE
            );

            int firstIndex = suggestionScroll.smoothIndexOffset();
            int visualShift = suggestionScroll.visualShift(itemH);
            int rowsToRender = Math.min(
                    visibleCount + (visualShift > 0 ? 1 : 0),
                    suggestions.size() - firstIndex
            );

            gui.pose().pushPose();
            gui.pose().translate(0, 0, 600);
            gui.fill(x, y, x + w, y + totalH, 0xFF0A0A0A);
            gui.renderOutline(x, y, w, totalH, 0xFF555555);
            gui.enableScissor(x, y, x + w, y + totalH);

            for (int i = 0; i < rowsToRender; i++) {
                int index = firstIndex + i;
                int top = y + (i * itemH) - visualShift;

                gui.fill(x + 1, top, x + w - 1, top + itemH, (index % 2 == 0) ? 0xFF1C1C1C : 0xFF0A0A0A);

                boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY < y + totalH
                        && mouseY >= top && mouseY < top + itemH;
                boolean selected = index == selectedIndex;
                if (hovered || selected) {
                    gui.fill(x + 1, top, x + w - 1, top + itemH, 0xFF202020);
                }
                GuiTheme.stateOutline(gui, x + 1, top, w - 2, itemH, selected, hovered, false);

                String[] parts = suggestions.get(index).split(" - ", 2);
                Font font = Minecraft.getInstance().font;
                gui.drawString(font, parts[0], x + 4, top + 2, GuiTheme.current().text(), false);
                if (parts.length > 1) {
                    int detailX = x + 4 + font.width(parts[0]);
                    gui.drawString(font, " - " + parts[1], detailX, top + 2, GuiTheme.current().mutedText(), false);
                }
            }

            gui.disableScissor();
            suggestionScroll.render(
                    gui,
                    mouseX,
                    mouseY,
                    x + w + 2,
                    y,
                    4,
                    totalH,
                    10
            );
            gui.pose().popPose();
        }

        public boolean handleMouseClick(double mouseX, double mouseY) {
            if (!isFocused() || suggestions.isEmpty()) return false;
            int x = this.getX() - 4;
            int y = this.getY() + this.getHeight() + 4;
            int itemH = 12;
            int w = this.maxSuggestionWidth;
            int totalH = Math.min(suggestions.size(), MAX_VISIBLE) * itemH;

            suggestionScroll.update(
                    suggestions.size(),
                    MAX_VISIBLE
            );

            if (suggestionScroll.beginDrag(
                    mouseX,
                    mouseY,
                    x + w + 2,
                    y,
                    4,
                    totalH,
                    10,
                    2
            )) {
                return true;
            }
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY < y + totalH) {
                int localY = (int) Math.floor(mouseY - y + suggestionScroll.visualShift(itemH));
                int clickedIdx = suggestionScroll.smoothIndexOffset() + localY / itemH;
                if (clickedIdx >= 0 && clickedIdx < suggestions.size()) {
                    selectItem(clickedIdx);
                    return true;
                }
            }
            return false;
        }

        public boolean handleMouseDragged(double mouseY) {
            if (!Double.isFinite(mouseY) || !isFocused()) {
                return false;
            }

            return suggestionScroll.drag(
                    mouseY,
                    this.getY() + this.getHeight() + 4,
                    Math.min(
                            suggestions.size(),
                            MAX_VISIBLE
                    ) * 12,
                    10
            );
        }

        public boolean handleMouseDragged(
                double mouseX,
                double mouseY
        ) {
            if (!Double.isFinite(mouseX)
                    || !Double.isFinite(mouseY)) {
                return false;
            }

            return handleMouseDragged(mouseY);
        }

        public boolean handleMouseReleased(int button) {
            return suggestionScroll.release(button);
        }

        public String normalizedValue() {
            return normalizeValue(getValue());
        }

        public static String normalizeValue(String value) {
            if (value == null || value.isEmpty()) {
                return "";
            }

            int separator = value.indexOf(" - ");

            if (separator < 0) {
                return value;
            }

            return value.substring(0, separator).trim();
        }
    }

    public static class AutoCompleteBoxGroup {
        private final List<AutoCompleteBox> boxes =
                new ArrayList<>();

        public void set(AutoCompleteBox... inputs) {
            boxes.clear();

            if (inputs == null) {
                return;
            }

            for (AutoCompleteBox input : inputs) {
                if (input != null) {
                    boxes.add(input);
                }
            }
        }

        public boolean handleMouseScrolled(double delta) {
            for (AutoCompleteBox box : boxes) {
                if (box.handleMouseScrolled(delta)) {
                    return true;
                }
            }

            return false;
        }

        public boolean handleSuggestionClick(
                double mouseX,
                double mouseY
        ) {
            for (AutoCompleteBox box : boxes) {
                if (box.handleMouseClick(
                        mouseX,
                        mouseY
                )) {
                    return true;
                }
            }

            return false;
        }

        public boolean handleMouseDragged(
                double mouseX,
                double mouseY
        ) {
            for (AutoCompleteBox box : boxes) {
                if (box.handleMouseDragged(
                        mouseX,
                        mouseY
                )) {
                    return true;
                }
            }

            return false;
        }

        public boolean handleMouseReleased(int button) {
            for (AutoCompleteBox box : boxes) {
                if (box.handleMouseReleased(button)) {
                    return true;
                }
            }

            return false;
        }

        public boolean handleKeyPressed(int keyCode) {
            for (AutoCompleteBox box : boxes) {
                if (box.handleKeyPressed(keyCode)) {
                    return true;
                }
            }

            return false;
        }

        public void clearFocusOutside(
                double mouseX,
                double mouseY
        ) {
            for (AutoCompleteBox box : boxes) {
                if (!box.isMouseOver(
                        mouseX,
                        mouseY
                )) {
                    box.setFocused(false);
                }
            }
        }

        public void renderSuggestions(
                GuiGraphics graphics,
                int mouseX,
                int mouseY
        ) {
            for (AutoCompleteBox box : boxes) {
                box.renderSuggestions(
                        graphics,
                        mouseX,
                        mouseY
                );
            }
        }
    }

    public static class NumericAutoCompleteBox extends AutoCompleteBox {
        public enum Type {
            INTEGER,
            LONG,
            DECIMAL
        }


        private final NumericAutoCompleteBox.Type type;
        private final boolean allowNegative;
        private final Number minValue;
        private final Number maxValue;
        private final Predicate<Number> validator;

        public NumericAutoCompleteBox(
                Font font,
                int x,
                int y,
                int width,
                int height,
                Component message,
                Supplier<List<String>> dictionarySupplier,
                NumericAutoCompleteBox.Type type,
                boolean allowNegative,
                Number minValue,
                Number maxValue
        ) {
            this(font, x, y, width, height, message, dictionarySupplier, type, allowNegative, minValue, maxValue, null);
        }

        public NumericAutoCompleteBox(
                Font font,
                int x,
                int y,
                int width,
                int height,
                Component message,
                Supplier<List<String>> dictionarySupplier,
                NumericAutoCompleteBox.Type type,
                boolean allowNegative,
                Number minValue,
                Number maxValue,
                Predicate<Number> validator
        ) {
            super(font, x, y, width, height, message, dictionarySupplier);
            this.type = type;
            this.allowNegative = allowNegative;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.validator = validator == null ? ignored -> true : validator;
            setFilter(this::isAllowedText);
        }

        /** 返回通过范围与业务校验的数值；空值、编辑中间态、格式错误或校验失败返回 null。 */
        public Integer getIntValue() {
            String raw = getValue().trim();
            if (raw.isEmpty() || "-".equals(raw)) return null;

            try {
                int value = Integer.parseInt(raw);
                return isInLongRange(value) && validator.test(value) ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        /** 返回通过范围与业务校验的数值；空值、编辑中间态、格式错误或校验失败返回 null。 */
        public Long getLongValue() {
            String raw = getValue().trim();
            if (raw.isEmpty() || "-".equals(raw)) return null;

            try {
                long value = Long.parseLong(raw);
                return isInLongRange(value) && validator.test(value) ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        /** 返回通过范围与业务校验的数值；空值、编辑中间态、格式错误或校验失败返回 null。 */
        public Double getDoubleValue() {
            String raw = getValue().trim();
            if (raw.isEmpty() || "-".equals(raw) || ".".equals(raw) || "-.".equals(raw)) return null;

            try {
                double value = Double.parseDouble(raw);
                return Double.isFinite(value) && isInDoubleRange(value) && validator.test(value) ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        public boolean isValueValid() {
            return switch (type) {
                case INTEGER -> getIntValue() != null;
                case LONG -> getLongValue() != null;
                case DECIMAL -> getDoubleValue() != null;
            };
        }

        public void setIntValue(int value) {
            setValue(Integer.toString(value));
        }

        public void setLongValue(long value) {
            setValue(Long.toString(value));
        }

        public void setDoubleValue(double value) {
            setValue(format(value));
        }

        public static String format(double value) {
            if (!Double.isFinite(value)) return Double.toString(value);
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }

        private boolean isAllowedText(String value) {
            if (value == null || value.isEmpty()) return true;
            if ("-".equals(value)) return allowNegative;

            int start = value.charAt(0) == '-' ? 1 : 0;
            if (start == 1 && !allowNegative) return false;

            if (type != NumericAutoCompleteBox.Type.DECIMAL) {
                for (int i = start; i < value.length(); i++) {
                    if (!Character.isDigit(value.charAt(i))) return false;
                }
                return true;
            }

            boolean dotSeen = false;
            for (int i = start; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '.') {
                    if (dotSeen) return false;
                    dotSeen = true;
                } else if (!Character.isDigit(c)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isInLongRange(long value) {
            if (minValue != null && value < minValue.longValue()) return false;
            return maxValue == null || value <= maxValue.longValue();
        }

        private boolean isInDoubleRange(double value) {
            if (minValue != null && value < minValue.doubleValue()) return false;
            return maxValue == null || value <= maxValue.doubleValue();
        }
    }

    private static NumericAutoCompleteBox integer(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue
    ) {
        return integer(font, x, y, width, height, message, dictionarySupplier, allowNegative, minValue, maxValue, null);
    }

    private static NumericAutoCompleteBox integer(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator
    ) {
        return new NumericAutoCompleteBox(
                font, x, y, width, height, message, dictionarySupplier,
                NumericAutoCompleteBox.Type.INTEGER, allowNegative, minValue, maxValue, validator
        );
    }

    private static NumericAutoCompleteBox longInteger(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue
    ) {
        return longInteger(font, x, y, width, height, message, dictionarySupplier, allowNegative, minValue, maxValue, null);
    }

    private static NumericAutoCompleteBox longInteger(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator
    ) {
        return new NumericAutoCompleteBox(
                font, x, y, width, height, message, dictionarySupplier,
                NumericAutoCompleteBox.Type.LONG, allowNegative, minValue, maxValue, validator
        );
    }

    private static NumericAutoCompleteBox decimal(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue
    ) {
        return decimal(font, x, y, width, height, message, dictionarySupplier, allowNegative, minValue, maxValue, null);
    }

    private static NumericAutoCompleteBox decimal(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator
    ) {
        return new NumericAutoCompleteBox(
                font, x, y, width, height, message, dictionarySupplier,
                NumericAutoCompleteBox.Type.DECIMAL, allowNegative, minValue, maxValue, validator
        );
    }
}
