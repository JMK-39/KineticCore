package dev.xyat.kineticcore.api.client.gui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class AutoCompleteBoxGroup {
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
