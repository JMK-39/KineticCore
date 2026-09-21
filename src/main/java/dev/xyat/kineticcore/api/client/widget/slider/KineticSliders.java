package dev.xyat.kineticcore.api.client.widget.slider;

import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.internal.client.widget.KineticValidation;
import dev.xyat.kineticcore.api.client.widget.KineticControl;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.FactoryAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;

/** Public Kinetic slider controls with API-owned rendering and interaction behavior. */
public final class KineticSliders {
    private KineticSliders() {
    }

    /** Standard horizontal slider whose business range, step, validation, and responder are caller-defined. */
    public static final class Slider extends AbstractSliderButton implements KineticControl {
        private static final int TRACK_HEIGHT = 6;
        private static final int KNOB_WIDTH = 6;

        private final double minValue;
        private final double maxValue;
        private final double step;
        private final Predicate<Double> validator;
        private final DoubleConsumer responder;
        private boolean error;
        private double lastCommittedValue;

        /** Creates a slider. Instances must come from Kinetic screen or detached-widget factories. */
        public Slider(
                FactoryAccess access,
                int x,
                int y,
                int width,
                int height,
                Component message,
                double minValue,
                double maxValue,
                double step,
                double value,
                Predicate<Double> validator,
                DoubleConsumer responder
        ) {
            super(x, y, width, height, message == null ? Component.empty() : message, 0.0D);
            Objects.requireNonNull(access, "factory access");
            if (!Double.isFinite(minValue) || !Double.isFinite(maxValue)
                    || maxValue <= minValue || !Double.isFinite(maxValue - minValue)) {
                throw new IllegalArgumentException("Slider requires a finite, positive value range");
            }
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.step = Double.isFinite(step) && step > 0.0D ? step : 0.0D;
            this.validator = validator == null ? ignored -> true : validator;
            this.responder = responder == null ? ignored -> { } : responder;
            setValue(value);
        }

        /** Replaces the slider label without exposing the inherited Minecraft message API. */
        public void setText(Component text) {
            setMessage(text == null ? Component.empty() : text);
        }

        /** Returns the current slider label. */
        public Component text() {
            return getMessage();
        }

        /** Returns the current business value after range clamping and step snapping. */
        public double value() {
            return actualValue(this.value);
        }

        /** Updates the slider value without invoking the responder. */
        public void setValue(double value) {
            if (!Double.isFinite(value)) {
                this.error = true;
                return;
            }
            double actual = normalizeActual(value);
            if (!Double.isFinite(actual)) {
                this.error = true;
                return;
            }
            this.value = normalizedValue(actual);
            this.error = !KineticValidation.accepts(validator, actual);
            // Programmatic updates establish the state to restore if a later drag is rejected.
            this.lastCommittedValue = this.value;
            updateMessage();
        }

        /** Returns whether the current value fails the caller-provided validator. */
        public boolean isError() {
            return error;
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

        @Override
        protected void updateMessage() {
        }

        @Override
        protected void applyValue() {
            double actual = normalizeActual(actualValue(this.value));
            if (!Double.isFinite(actual) || !KineticValidation.accepts(validator, actual)) {
                // Vanilla updates the thumb before invoking applyValue. Restore the
                // last accepted position rather than sending an invalid business value.
                this.value = lastCommittedValue;
                this.error = true;
                return;
            }
            double previousValue = lastCommittedValue;
            boolean previousError = error;
            this.value = normalizedValue(actual);
            this.error = false;
            try {
                responder.accept(actual);
                lastCommittedValue = this.value;
            } catch (RuntimeException | Error failure) {
                // A failed save must leave the slider at its previous stable value.
                this.value = previousValue;
                this.lastCommittedValue = previousValue;
                this.error = previousError;
                throw failure;
            }
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;

            GuiTheme.Palette theme = GuiTheme.current();
            int trackY = y + Math.max(0, (height - TRACK_HEIGHT) / 2);
            int trackBottom = Math.min(y + height, trackY + TRACK_HEIGHT);
            graphics.fill(x, trackY, x + width, trackBottom, theme.scrollTrack());

            int travel = Math.max(0, width - KNOB_WIDTH);
            int knobX = x + (int) Math.round(this.value * travel);
            int fillRight = Math.max(x + 1, Math.min(x + width - 1, knobX + KNOB_WIDTH / 2));
            if (trackBottom - trackY > 2 && fillRight > x + 1) {
                graphics.fill(x + 1, trackY + 1, fillRight, trackBottom - 1, theme.accentHover());
            }

            int knobTop = y + 1;
            int knobBottom = Math.max(knobTop + 1, y + height - 2);
            int knobColor = error
                    ? theme.danger()
                    : isHoveredOrFocused() ? theme.scrollThumbHover() : theme.scrollThumb();
            graphics.fill(knobX, knobTop, knobX + KNOB_WIDTH, knobBottom, knobColor);
            GuiTheme.stateOutline(graphics, x, y, width, height, false, isHoveredOrFocused(), error);
        }

        private double actualValue(double normalized) {
            return minValue + Math.max(0.0D, Math.min(1.0D, normalized)) * (maxValue - minValue);
        }

        private double normalizedValue(double actual) {
            return (actual - minValue) / (maxValue - minValue);
        }

        private double normalizeActual(double value) {
            double clamped = Math.max(minValue, Math.min(maxValue, value));
            if (step <= 0.0D) return clamped;
            double snapped = minValue + Math.round((clamped - minValue) / step) * step;
            return Math.max(minValue, Math.min(maxValue, snapped));
        }
    }
}
