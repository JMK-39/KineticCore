package dev.xyat.kineticcore.api.client.widget.selection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import static dev.xyat.kineticcore.api.client.widget.KineticWidgets.attachTooltip;

/** 控件实现分组；附属统一从 KineticWidgets 工厂进入。 */
public final class KineticDropdowns {
    private KineticDropdowns() {}

    public static Dropdown createDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder,
            Consumer<Dropdown> opener
    ) {
        return createDropdown(
                x, y, width, options, selectedIndex, tooltip,
                ignored -> true, responder, opener
        );
    }

    public static Dropdown createDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder,
            Consumer<Dropdown> opener
    ) {
        List<Component> normalizedOptions = options == null ? List.of() : new ArrayList<>(options);
        Dropdown dropdown = new Dropdown(
                x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                normalizedOptions, selectedIndex, validator, responder, opener
        );
        attachTooltip(dropdown, tooltip);
        return dropdown;
    }

    public static class Dropdown extends StateButton {
        private final List<Component> options;
        private final Predicate<Integer> validator;
        private final Consumer<Integer> responder;
        private final Consumer<Dropdown> opener;
        private int selectedIndex;

        public Dropdown(
                int x,
                int y,
                int width,
                int height,
                List<Component> options,
                int selectedIndex,
                Consumer<Integer> responder
        ) {
            this(x, y, width, height, options, selectedIndex, ignored -> true, responder, null);
        }

        public Dropdown(
                int x,
                int y,
                int width,
                int height,
                List<Component> options,
                int selectedIndex,
                Predicate<Integer> validator,
                Consumer<Integer> responder,
                Consumer<Dropdown> opener
        ) {
            super(x, y, width, height, messageAt(options, selectedIndex), ignored -> { });
            this.options = options == null ? List.of() : List.copyOf(options);
            this.validator = validator == null ? ignored -> true : validator;
            this.responder = responder == null ? ignored -> { } : responder;
            this.opener = opener;
            this.selectedIndex = normalizeIndex(selectedIndex, this.options.size());
            setMessage(messageAt(this.options, this.selectedIndex));
            setError(!this.options.isEmpty() && !this.validator.test(this.selectedIndex));
        }

        public List<Component> options() {
            return options;
        }

        public int selectedIndex() {
            return selectedIndex;
        }

        public Component selected() {
            return messageAt(options, selectedIndex);
        }

        public void setSelectedIndex(int index) {
            selectedIndex = normalizeIndex(index, options.size());
            setMessage(messageAt(options, selectedIndex));
            setError(!options.isEmpty() && !validator.test(selectedIndex));
        }

        public void choose(int index) {
            if (options.isEmpty()) return;
            setSelectedIndex(index);
            responder.accept(selectedIndex);
        }

        @Override
        public void onPress() {
            if (options.isEmpty()) return;
            if (opener != null) {
                opener.accept(this);
            } else {
                choose(selectedIndex + 1);
            }
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }

        private static int normalizeIndex(int index, int size) {
            if (size <= 0) return -1;
            int normalized = index % size;
            return normalized < 0 ? normalized + size : normalized;
        }

        private static Component messageAt(List<Component> options, int index) {
            if (options == null || options.isEmpty()) return Component.empty();
            int normalized = normalizeIndex(index, options.size());
            return options.get(normalized);
        }
    }
}
