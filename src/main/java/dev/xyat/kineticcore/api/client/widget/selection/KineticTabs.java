package dev.xyat.kineticcore.api.client.widget.selection;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.MenuButton;
import static dev.xyat.kineticcore.api.client.widget.KineticWidgets.attachTooltip;

/** 控件实现分组；附属统一从 KineticWidgets 工厂进入。 */
public final class KineticTabs {
    private KineticTabs() {}

    public static TabBar createTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        TabBar tabBar = new TabBar();
        tabBar.build(x, y, totalWidth, labels, selectedIndex, responder);
        return tabBar;
    }

    public static TabBar createTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        TabBar tabBar = createTabBar(x, y, totalWidth, labels, selectedIndex, responder);
        List<? extends Component> safeTooltips = tooltips == null ? List.of() : tooltips;
        List<Button> buttons = tabBar.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            Component tooltip = index < safeTooltips.size() ? safeTooltips.get(index) : null;
            attachTooltip(buttons.get(index), tooltip);
        }
        return tabBar;
    }

    public static class TabBar {
        private final List<Button> buttons = new ArrayList<>();
        private int selectedIndex;

        public TabBar() {
        }

        public List<Button> buttons() {
            return List.copyOf(buttons);
        }

        public int selectedIndex() {
            return selectedIndex;
        }

        public final void build(
                int x,
                int y,
                int totalWidth,
                List<? extends Component> labels,
                int selected,
                Consumer<Integer> responder
        ) {
            buttons.clear();
            if (labels == null || labels.isEmpty()) {
                selectedIndex = -1;
                return;
            }
            selectedIndex = Math.max(0, Math.min(labels.size() - 1, selected));
            int baseWidth = Math.max(1, totalWidth / labels.size());
            int used = 0;
            for (int index = 0; index < labels.size(); index++) {
                int width = index == labels.size() - 1 ? totalWidth - used : baseWidth;
                int buttonIndex = index;
                MenuButton button = new MenuButton(
                        x + used,
                        y,
                        width,
                        KineticScreen.STANDARD_CONTROL_HEIGHT,
                        labels.get(index),
                        ignored -> {
                            selectedIndex = buttonIndex;
                            for (int i = 0; i < buttons.size(); i++) {
                                if (buttons.get(i) instanceof MenuButton menuButton) {
                                    menuButton.setSelected(i == selectedIndex);
                                }
                            }
                            if (responder != null) responder.accept(buttonIndex);
                        },
                        false
                );
                button.setSelected(index == selectedIndex);
                buttons.add(button);
                used += width;
            }
        }
    }
}
