package dev.xyat.kineticcore.api.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AutoCompleteBox extends EditBox {
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

        int x = this.getX() - 4; int y = this.getY() + this.getHeight() + 4;
        int itemH = 12; int w = this.maxSuggestionWidth;
        int visibleCount = Math.min(suggestions.size(), MAX_VISIBLE);
        int totalH = visibleCount * itemH;

        gui.pose().pushPose(); gui.pose().translate(0, 0, 600);
        gui.fill(x, y, x + w, y + totalH, 0xFF0A0A0A);
        gui.renderOutline(x, y, w, totalH, 0xFF555555);

        for (int i = 0; i < visibleCount; i++) {
            int index = i + suggestionScroll.offset();
            if (index >= suggestions.size()) break;
            int top = y + (i * itemH);

            gui.fill(x + 1, top, x + w - 1, top + itemH, (index % 2 == 0) ? 0xFF1C1C1C : 0xFF0A0A0A);

            if ((mouseX >= x && mouseX <= x + w && mouseY >= top && mouseY < top + itemH) || index == selectedIndex) {
                gui.fill(x + 1, top, x + w - 1, top + itemH, 0xFF0055AA);
            }

            String[] parts = suggestions.get(index).split(" - ", 2);
            MutableComponent line = Component.literal(parts[0]).withStyle(ChatFormatting.GOLD);
            if (parts.length > 1) line.append(Component.literal(" - " + parts[1]).withStyle(ChatFormatting.WHITE));
            gui.drawString(Minecraft.getInstance().font, line, x + 4, top + 2, 0xFFFFFF);
        }

        suggestionScroll.update(
                suggestions.size(),
                MAX_VISIBLE
        );

        suggestionScroll.render(
                gui,
                mouseX,
                mouseY,
                x + w + 2,
                y,
                6,
                totalH,
                10
        );
        gui.pose().popPose();
    }

    public boolean handleMouseClick(double mouseX, double mouseY) {
        if (!isFocused() || suggestions.isEmpty()) return false;
        int x = this.getX() - 4; int y = this.getY() + this.getHeight() + 4;
        int w = this.maxSuggestionWidth; int totalH = Math.min(suggestions.size(), MAX_VISIBLE) * 12;

        suggestionScroll.update(
                suggestions.size(),
                MAX_VISIBLE
        );

        if (suggestionScroll.beginDrag(
                mouseX,
                mouseY,
                x + w + 2,
                y,
                6,
                totalH,
                10,
                2
        )) {
            return true;
        }
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY < y + totalH) {
            int clickedIdx = (int) ((mouseY - y) / 12) + suggestionScroll.offset();
            if (clickedIdx >= 0 && clickedIdx < suggestions.size()) { selectItem(clickedIdx); return true; }
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
