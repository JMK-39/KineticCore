package dev.xyat.kineticcore.internal.client.gui.widget.tab;

import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.MenuButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import java.util.Objects;

/** Fixed-width tab bar built from standard Kinetic buttons; created only through {@code KineticWidgets}. */
public final class TabBar implements TabBarButtons {
    private final List<StateButton> buttons = new ArrayList<>();
    private final Consumer<Integer> responder;
    private int selectedIndex;

    public TabBar(
            FactoryAccess access,
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            int selected,
            Consumer<Integer> responder,
            boolean vertical,
            boolean highZ,
            int zLevel
    ) {
        Objects.requireNonNull(access, "access");
        this.responder = responder;
        if (labels == null || labels.isEmpty()) {
            selectedIndex = -1;
            return;
        }
        selectedIndex = Math.max(0, Math.min(labels.size() - 1, selected));
        int baseWidth = vertical ? totalWidth : Math.max(1, totalWidth / labels.size());
        int used = 0;
        for (int index = 0; index < labels.size(); index++) {
            int width = vertical || index != labels.size() - 1 ? baseWidth : totalWidth - used;
            int buttonX = vertical ? x : x + used;
            int buttonY = vertical ? y + index * (KineticScreen.STANDARD_CONTROL_HEIGHT + 2) : y;
            int buttonIndex = index;
            StateButton button = highZ
                    ? new HighZButton(
                            access, buttonX, buttonY, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                            labels.get(index), ignored -> select(buttonIndex, true), zLevel
                    )
                    : new MenuButton(
                            access, buttonX, buttonY, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                            labels.get(index), ignored -> select(buttonIndex, true), false
                    );
            button.setSelected(index == selectedIndex);
            buttons.add(button);
            if (!vertical) used += width;
        }
    }

    @Override
    public List<StateButton> buttons() {
        return List.copyOf(buttons);
    }

    @Override
    public int tabAt(double mouseX, double mouseY) {
        for (int index = 0; index < buttons.size(); index++) {
            StateButton button = buttons.get(index);
            if (button.isVisible() && button.isMouseOver(mouseX, mouseY)) return index;
        }
        return -1;
    }

    @Override
    public int selectedIndex() {
        return selectedIndex;
    }

    @Override
    public void setSelectedIndex(int index) {
        if (buttons.isEmpty()) {
            selectedIndex = -1;
            return;
        }
        select(Math.max(0, Math.min(buttons.size() - 1, index)), false);
    }

    @Override
    public void setTabActive(int index, boolean active) {
        if (index < 0 || index >= buttons.size()) return;
        buttons.get(index).active = active;
    }

    private void select(int index, boolean notify) {
        if (index < 0 || index >= buttons.size()) return;
        selectedIndex = index;
        for (int buttonIndex = 0; buttonIndex < buttons.size(); buttonIndex++) {
            buttons.get(buttonIndex).setSelected(buttonIndex == selectedIndex);
        }
        if (notify && responder != null) responder.accept(index);
    }
}
