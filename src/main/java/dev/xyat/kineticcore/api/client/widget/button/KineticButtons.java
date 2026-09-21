package dev.xyat.kineticcore.api.client.widget.button;

import dev.xyat.kineticcore.api.client.widget.KineticControl;
import dev.xyat.kineticcore.internal.client.widget.KineticValidation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.FactoryAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Public button types returned by Kinetic screen and detached-widget factories.
 * <p>
 * Add-ons should create these controls through {@code KineticScreen.addXxx(...)} or
 * {@code KineticWidgets.createXxx(...)} so size, tooltip, focus, and theme behavior stay unified.
 */
public final class KineticButtons {
    private KineticButtons() {}

    private static void renderTextureButtonIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            ResourceLocation texture,
            int u,
            int v,
            int hoverVOffset,
            int textureWidth,
            int textureHeight,
            boolean hovered
    ) {
        if (graphics == null || texture == null) return;
        graphics.blit(
                texture,
                x,
                y,
                u,
                hovered ? v + hoverVOffset : v,
                width,
                height,
                textureWidth,
                textureHeight
        );
    }

    /** Standard Kinetic button rendered with an explicit texture. */
    public static final class TextureButton extends Button implements KineticControl {
        private final ResourceLocation texture;
        private final int u;
        private final int v;
        private final int hoverVOffset;
        private final int textureWidth;
        private final int textureHeight;

        /** Creates a new {@code TextureButton}. */
        public TextureButton(
                FactoryAccess access,
                int x,
                int y,
                int width,
                int height,
                ResourceLocation texture,
                int u,
                int v,
                int hoverVOffset,
                int textureWidth,
                int textureHeight,
                Component narration,
                Consumer<TextureButton> onPress
        ) {
            super(x, y, width, height, narration, pressed -> {
                if (onPress != null) onPress.accept((TextureButton) pressed);
            }, DEFAULT_NARRATION);
            Objects.requireNonNull(access, "factory access");
            this.texture = Objects.requireNonNull(texture, "texture");
            this.u = u;
            this.v = v;
            this.hoverVOffset = hoverVOffset;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }


        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderTextureButtonIcon(
                    graphics,
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    texture,
                    u,
                    v,
                    hoverVOffset,
                    textureWidth,
                    textureHeight,
                    isHoveredOrFocused()
            );
        }

        @Override
        public void updateWidgetNarration(@NotNull NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** Standard Kinetic button with selected and error visual states. */
    public static class StateButton extends Button implements KineticControl {
        private boolean selected;
        private boolean error;
        private boolean textVisible = true;
        private boolean contentCardSurface;
        private boolean clipEnabled;
        private int clipLeft;
        private int clipTop;
        private int clipRight;
        private int clipBottom;

        /** Creates a new {@code StateButton}. */
        public StateButton(
                FactoryAccess access,
                int x,
                int y,
                int width,
                int height,
                Component message,
                Consumer<StateButton> onPress
        ) {
            super(x, y, width, height, message, pressed -> {
                if (onPress != null) onPress.accept((StateButton) pressed);
            }, DEFAULT_NARRATION);
            Objects.requireNonNull(access, "factory access");
        }

        /** Replaces the button label without exposing the inherited Minecraft message API. */
        public void setText(Component text) {
            setMessage(text == null ? Component.empty() : text);
        }

        /** Returns the current button label. */
        public Component text() {
            return getMessage();
        }

        /** Sets whether this button is rendered as selected. */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        /** Sets whether this button is rendered with the Kinetic error state. */
        public void setError(boolean error) {
            this.error = error;
        }

        /** Sets whether this button accepts interaction and uses its enabled visual state. */
        public void setEnabled(boolean enabled) {
            this.active = enabled;
        }

        /** Sets whether this button participates in rendering and hit testing. */
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        /** Returns whether this button currently accepts interaction. */
        public boolean isEnabled() {
            return this.active;
        }

        /** Returns whether this button currently participates in rendering and hit testing. */
        public boolean isVisible() {
            return this.visible;
        }

        /** Returns whether this state button is currently selected. */
        public boolean isSelected() {
            return selected;
        }

        /** Returns whether this state button is currently showing an error state. */
        public boolean isError() {
            return error;
        }

        /** Controls whether the visual button label is rendered while keeping the text available for narration. */
        public void setTextVisible(boolean visible) {
            this.textVisible = visible;
        }

        /** Returns whether the visual button label is currently rendered. */
        public boolean isTextVisible() {
            return textVisible;
        }

        /**
         * Uses the single-layer Kinetic content-card surface instead of Minecraft's standard button chrome.
         * Content-card factories enable this automatically so add-ons can draw rich content without doubled frames.
         */
        public void setContentCardSurface(boolean contentCardSurface) {
            this.contentCardSurface = contentCardSurface;
        }

        /** Returns whether this control is using the Kinetic content-card surface. */
        public boolean isContentCardSurface() {
            return contentCardSurface;
        }

        /** Restricts rendering and hit testing to the supplied UI-coordinate rectangle. */
        public void setClipBounds(int left, int top, int right, int bottom) {
            this.clipLeft = Math.min(left, right);
            this.clipTop = Math.min(top, bottom);
            this.clipRight = Math.max(left, right);
            this.clipBottom = Math.max(top, bottom);
            this.clipEnabled = this.clipRight > this.clipLeft && this.clipBottom > this.clipTop;
        }

        /** Clears a clipping rectangle previously supplied through {@link #setClipBounds(int, int, int, int)}. */
        public void clearClipBounds() {
            this.clipEnabled = false;
        }

        /** Returns whether this button currently has an active render and hit-test clip rectangle. */
        public boolean hasClipBounds() {
            return clipEnabled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (clipEnabled && (mouseX < clipLeft || mouseX >= clipRight || mouseY < clipTop || mouseY >= clipBottom)) {
                return false;
            }
            return super.isMouseOver(mouseX, mouseY);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (clipEnabled) graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
            Component storedText = null;
            try {
                if (contentCardSurface) {
                    GuiTheme.stateSurface(
                            graphics,
                            getX(),
                            getY(),
                            getWidth(),
                            getHeight(),
                            GuiTheme.Surface.PANEL_ALT,
                            selected,
                            isHovered(),
                            error
                    );
                    return;
                }
                if (!textVisible) {
                    storedText = getMessage();
                    setMessage(Component.empty());
                }
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
                if (storedText != null) setMessage(storedText);
                if (selected || error) {
                    GuiTheme.stateOutline(
                            graphics,
                            getX(),
                            getY(),
                            getWidth(),
                            getHeight(),
                            selected,
                            isHovered(),
                            error
                    );
                }
            } finally {
                if (storedText != null) setMessage(storedText);
                if (clipEnabled) graphics.disableScissor();
            }
        }
    }

    /** Standard Kinetic button that renders an item icon alongside its label. */
    public static class ItemButton extends StateButton {
        private final ItemStack icon;

        /** Creates a new API-managed item button. */
        public ItemButton(
                FactoryAccess access,
                int x,
                int y,
                int width,
                int height,
                ItemStack icon,
                Component message,
                Consumer<StateButton> onPress
        ) {
            super(access, x, y, width, height, message, onPress);
            this.icon = icon == null ? ItemStack.EMPTY : icon.copy();
        }

        /** Returns a defensive copy of the icon rendered by this button. */
        public ItemStack icon() {
            return icon.copy();
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (!icon.isEmpty()) {
                graphics.renderFakeItem(icon, getX() + 8, getY() + (getHeight() - 16) / 2);
            }
        }
    }

    /** Standard Kinetic button that renders a compact color swatch. */
    public static class ColorSwatchButton extends Button implements KineticControl {
        private int rgb;

        /** Creates a new {@code ColorSwatchButton}. */
        public ColorSwatchButton(FactoryAccess access, int x, int y, int size, int rgb, Consumer<ColorSwatchButton> onPress) {
            super(x, y, size, size, Component.empty(), pressed -> {
                if (onPress != null) onPress.accept((ColorSwatchButton) pressed);
            }, DEFAULT_NARRATION);
            Objects.requireNonNull(access, "factory access");
            this.rgb = rgb & 0xFFFFFF;
        }

        /** Replaces the displayed RGB color; alpha is intentionally ignored. */
        public void setRgb(int rgb) {
            this.rgb = rgb & 0xFFFFFF;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            int inset = Math.max(4, Math.min(getWidth(), getHeight()) / 4);
            graphics.fill(
                    getX() + inset,
                    getY() + inset,
                    getX() + getWidth() - inset,
                    getY() + getHeight() - inset,
                    0xFF000000 | rgb
            );
        }
    }

    /** Standard Kinetic button that renders a color preview alongside its state. */
    public static class ColorPreviewButton extends StateButton {
        private int rgb;

        /** Creates a new {@code ColorPreviewButton}. */
        public ColorPreviewButton(FactoryAccess access, int x, int y, int width, int height, int rgb, Component message, Consumer<StateButton> onPress) {
            super(access, x, y, width, height, message, onPress);
            this.rgb = rgb & 0xFFFFFF;
        }

        /** Replaces the displayed RGB color; alpha is intentionally ignored. */
        public void setRgb(int rgb) {
            this.rgb = rgb & 0xFFFFFF;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            int size = Math.min(12, Math.max(8, getHeight() - 8));
            int x = getX() + 6;
            int y = getY() + (getHeight() - size) / 2;
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFFFFFFFF);
            graphics.fill(x, y, x + size, y + size, 0xFF000000 | rgb);
        }
    }

    /** Standard Kinetic button intended to open an API-managed menu. */
    public static class MenuButton extends StateButton {
        /** Creates a new {@code MenuButton}. */
        public MenuButton(FactoryAccess access, int x, int y, int width, int height, Component message, Consumer<StateButton> onPress, boolean danger) {
            super(access, x, y, width, height, message, onPress);
            setError(danger);
        }

        /** Repositions and resizes this menu entry; width and height are clamped to at least one pixel. */
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }
    }

    /** Standard Kinetic button rendered at an elevated overlay depth. */
    public static class HighZButton extends StateButton {
        private final int zLevel;

        /** Creates a new {@code HighZButton}. */
        public HighZButton(FactoryAccess access, int x, int y, int width, int height, Component message, Consumer<StateButton> onPress, int zLevel) {
            super(access, x, y, width, height, message, onPress);
            this.zLevel = zLevel;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    /** Standard Kinetic toggle control with a validated boolean value. */
    public static class ToggleButton extends StateButton {
        private boolean value;
        private final Component onText;
        private final Component offText;
        private final Predicate<Boolean> validator;
        private final Consumer<Boolean> responder;

        /** Creates a new {@code ToggleButton}. */
        public ToggleButton(
                FactoryAccess access,
                int x,
                int y,
                int width,
                int height,
                boolean value,
                Component onText,
                Component offText,
                Predicate<Boolean> validator,
                Consumer<Boolean> responder
        ) {
            super(access, x, y, width, height, value ? onText : offText, ignored -> { });
            this.value = value;
            this.onText = Objects.requireNonNullElse(onText, Component.empty());
            this.offText = Objects.requireNonNullElse(offText, Component.empty());
            this.validator = validator == null ? ignored -> true : validator;
            this.responder = responder == null ? ignored -> { } : responder;
            setError(!KineticValidation.accepts(this.validator, value));
        }

        /** Returns the current boolean value represented by this toggle. */
        public boolean value() {
            return value;
        }

        /** Updates the value, visible label, and validation state without invoking the responder. */
        public void setValue(boolean value) {
            applyValue(value, !KineticValidation.accepts(validator, value));
        }

        private void applyValue(boolean value, boolean invalid) {
            this.value = value;
            setMessage(value ? onText : offText);
            setError(invalid);
        }

        @Override
        public void onPress() {
            boolean next = !value;
            if (!KineticValidation.accepts(validator, next)) return;
            boolean previous = value;
            try {
                applyValue(next, false);
                responder.accept(value);
            } catch (RuntimeException | Error failure) {
                try {
                    setValue(previous);
                } catch (RuntimeException | Error restoreFailure) {
                    if (restoreFailure != failure) failure.addSuppressed(restoreFailure);
                }
                throw failure;
            }
        }

    }

    /** Standard Kinetic multi-state cycle control with a validated selected index. */
    public static class CycleButton extends StateButton {
        private final List<Component> options;
        private int index;
        private final Predicate<Integer> validator;
        private final Consumer<Integer> responder;

        /** Creates a new {@code CycleButton}. */
        public CycleButton(
                FactoryAccess access,
                int x,
                int y,
                int width,
                int height,
                int index,
                List<Component> options,
                Predicate<Integer> validator,
                Consumer<Integer> responder
        ) {
            super(access, x, y, width, height, initialCycleMessage(options, index), ignored -> { });
            this.options = List.copyOf(options);
            this.index = index;
            this.validator = validator == null ? ignored -> true : validator;
            this.responder = responder == null ? ignored -> { } : responder;
            setError(!KineticValidation.accepts(this.validator, index));
        }

        /** Returns the current option index. */
        public int index() {
            return index;
        }

        /** Returns the immutable option labels used by this control. */
        public List<Component> options() {
            return options;
        }

        /** Updates the selected index, visible label, and validation state without invoking the responder. */
        public void setIndex(int index) {
            requireCycleIndex(index, options.size());
            applyIndex(index, !KineticValidation.accepts(validator, index));
        }

        private void applyIndex(int index, boolean invalid) {
            this.index = index;
            setMessage(options.get(index));
            setError(invalid);
        }

        @Override
        public void onPress() {
            int next = (index + 1) % options.size();
            if (!KineticValidation.accepts(validator, next)) return;
            int previous = index;
            try {
                applyIndex(next, false);
                responder.accept(index);
            } catch (RuntimeException | Error failure) {
                try {
                    setIndex(previous);
                } catch (RuntimeException | Error restoreFailure) {
                    if (restoreFailure != failure) failure.addSuppressed(restoreFailure);
                }
                throw failure;
            }
        }

        private static Component initialCycleMessage(List<Component> options, int index) {
            Objects.requireNonNull(options, "options");
            if (options.isEmpty()) throw new IllegalArgumentException("options must not be empty");
            requireCycleIndex(index, options.size());
            return Objects.requireNonNull(options.get(index), "options[" + index + "]");
        }

        private static void requireCycleIndex(int index, int size) {
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException("index out of range: " + index + " for " + size + " options");
            }
        }
    }

    /** Standard Kinetic toggle rendered at an elevated overlay depth. */
    public static class HighZToggleButton extends ToggleButton {
        private final int zLevel;

        /** Creates a new {@code HighZToggleButton}. */
        public HighZToggleButton(
                FactoryAccess access,
                int x,
                int y,
                int width,
                int height,
                boolean value,
                Component onText,
                Component offText,
                Predicate<Boolean> validator,
                Consumer<Boolean> responder,
                int zLevel
        ) {
            super(access, x, y, width, height, value, onText, offText, validator, responder);
            this.zLevel = zLevel;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            } finally {
                graphics.pose().popPose();
            }
        }
    }
}
