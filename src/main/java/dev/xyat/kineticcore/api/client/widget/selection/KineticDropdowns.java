package dev.xyat.kineticcore.api.client.widget.selection;

import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.internal.client.widget.KineticValidation;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.FactoryAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Defines Kinetic dropdown option data and the standard dropdown widget implementation.
 * Addons create dropdowns through a Kinetic screen or {@code KineticWidgets}; option translations are display-only.
 */
public final class KineticDropdowns {
    private KineticDropdowns() {
    }

    /**
     * One dropdown choice. {@code value} is the stored business value; {@code translation} is optional display-only text.
     * When present, the translation is always used for display while {@code value} remains the stored business value.
     */
    public record Option(String value, Component translation, Component tooltip) {
        /** Normalizes one dropdown option while preserving raw value separately from display metadata. */
        public Option {
            value = Objects.requireNonNull(value, "value").trim();
            if (value.isEmpty()) throw new IllegalArgumentException("dropdown value cannot be blank");
            translation = Objects.requireNonNullElse(translation, Component.empty());
            tooltip = Objects.requireNonNullElse(tooltip, Component.empty());
        }
    }

    /** Standard Kinetic dropdown widget. Creation is routed through Kinetic screen helpers or {@code KineticWidgets}. */
    public static class Dropdown extends StateButton {
        private final List<Option> options;
        private final Predicate<String> validator;
        private final Consumer<String> responder;
        private final Consumer<Dropdown> opener;
        private int selectedIndex;

        /** Creates the fully configured dropdown implementation used by Kinetic factories. */
        public Dropdown(
                FactoryAccess access,
                int x,
                int y,
                int width,
                int height,
                List<Option> options,
                String selectedValue,
                Predicate<String> validator,
                Consumer<String> responder,
                Consumer<Dropdown> opener
        ) {
            super(access, x, y, width, height, Component.empty(), ignored -> { });
            this.options = options == null ? List.of() : List.copyOf(options);
            this.validator = validator == null ? ignored -> true : validator;
            this.responder = responder == null ? ignored -> { } : responder;
            this.opener = opener;
            this.selectedIndex = indexOfValue(selectedValue, this.options);
            refreshState();
        }

        /** Returns the immutable dropdown choices, including display-only translations and tooltips. */
        public List<Option> options() {
            return options;
        }

        /** Returns the selected option index, or {@code -1} when the dropdown has no choices. */
        public int selectedIndex() {
            return selectedIndex;
        }

        /** Returns the raw selected value, or an empty string when the dropdown has no choices. */
        public String selectedValue() {
            Option selected = selectedOption();
            return selected == null ? "" : selected.value();
        }

        /** Returns the selected option, or {@code null} when the dropdown has no choices. */
        public Option selectedOption() {
            return selectedIndex >= 0 && selectedIndex < options.size() ? options.get(selectedIndex) : null;
        }

        /** Selects one option by its raw value without invoking the responder. */
        public void setSelectedValue(String value) {
            selectedIndex = indexOfValue(value, options);
            refreshState();
        }

        /** Selects one option by index and sends only its raw value to the responder. */
        public void choose(int index) {
            if (options.isEmpty()) return;
            int nextIndex = normalizeIndex(index, options.size());
            // Reject invalid choices before changing the selected value or
            // forwarding it to an add-on's configuration writer.
            if (!KineticValidation.accepts(validator, options.get(nextIndex).value())) return;
            int previousIndex = selectedIndex;
            selectedIndex = nextIndex;
            try {
                // The selected value has just passed validation. Rendering must not
                // run the add-on's potentially stateful validator a second time.
                refreshState(true);
                responder.accept(selectedValue());
            } catch (RuntimeException | Error failure) {
                // The responder can fail while persisting a config value. Never
                // leave the control showing a value that was not accepted.
                selectedIndex = previousIndex;
                try {
                    refreshState();
                } catch (RuntimeException | Error restoreFailure) {
                    if (restoreFailure != failure) failure.addSuppressed(restoreFailure);
                }
                throw failure;
            }
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

        private void refreshState() {
            Option selected = selectedOption();
            refreshState(selected == null || KineticValidation.accepts(validator, selected.value()));
        }

        private void refreshState(boolean validSelection) {
            Option selected = selectedOption();
            String value = selected == null ? "" : selected.value();
            Component display = selected == null || selected.translation().getString().isBlank()
                    ? Component.literal(value)
                    : selected.translation();
            setMessage(display);
            setError(selected != null && !validSelection);
        }

        private static int indexOfValue(String value, List<Option> options) {
            if (options == null || options.isEmpty()) return -1;
            if (value != null) {
                for (int index = 0; index < options.size(); index++) {
                    if (options.get(index).value().equals(value)) return index;
                }
            }
            return 0;
        }

        private static int normalizeIndex(int index, int size) {
            if (size <= 0) return -1;
            int normalized = index % size;
            return normalized < 0 ? normalized + size : normalized;
        }
    }
}
