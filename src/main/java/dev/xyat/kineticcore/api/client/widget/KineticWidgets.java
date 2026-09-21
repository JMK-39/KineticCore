package dev.xyat.kineticcore.api.client.widget;

import javax.annotation.Nonnull;

import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorPreviewButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorSwatchButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.CycleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZToggleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.MenuButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ItemButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.TextureButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticMultiLineEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Option;
import dev.xyat.kineticcore.api.client.widget.slider.KineticSliders.Slider;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.TabBar;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableTab;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableTabStrip;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.SelectionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemSelectionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemGridDensity;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemGridItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemGridMarker;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemGrid;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemSelectionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.MultiActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.RowAction;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ActionHit;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableMultiActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableToggleActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.RowToggle;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.MultiToggleItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleHit;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableMultiToggleList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableSelectionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableToggleList;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.GridScrollController;
import dev.xyat.kineticcore.api.client.widget.render.KineticEntityPreview.EntityPreviewRenderer;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Single detached factory for standard Kinetic controls.
 * <p>
 * {@link KineticScreen} subclasses should prefer their {@code addXxx(...)} methods so registration, focus,
 * clipping, and overlay tooltips stay screen-managed. Helpers, tabs, panels, and vanilla-screen injections
 * use {@code KineticWidgets.createXxx(...)} and are responsible for registering the returned control.
 */
public final class KineticWidgets {
    private static final FactoryAccess FACTORY_ACCESS = new FactoryAccess();

    private KineticWidgets() {
    }

    /** Opaque construction token used by API controls whose instances must come from Kinetic widget factories. */
    public static final class FactoryAccess {
        private FactoryAccess() {
        }
    }

    /** Enables or disables one existing vanilla or business-specific external widget. */
    public static void setExternalWidgetEnabled(AbstractWidget widget, boolean enabled) {
        KineticControlBridge.setEnabled(widget, enabled);
    }

    /** Shows or hides one existing vanilla or business-specific external widget. */
    public static void setExternalWidgetVisible(AbstractWidget widget, boolean visible) {
        KineticControlBridge.setVisible(widget, visible);
    }


    /** Renders one detached Kinetic control through the API-managed widget lifecycle. */
    public static void renderControl(KineticControl control, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (control == null || graphics == null || !control.isVisible()) return;
        KineticControlBridge.render(control, graphics, mouseX, mouseY, partialTick);
    }

    /** Creates a detached standard-height state button and attaches the optional tooltip. */
    public static StateButton createButton(int x, int y, int width, Component text, Component tooltip, Runnable action) {
        StateButton button = new StateButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                ignored -> { if (action != null) action.run(); }
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached content-rich card button using the API-defined card height. */
    public static StateButton createCardButton(
            int x, int y, int width, Component narration, Component tooltip, Runnable action
    ) {
        StateButton button = new StateButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.CARD_BUTTON_HEIGHT,
                narration == null ? Component.empty() : narration,
                ignored -> { if (action != null) action.run(); }
        );
        button.setTextVisible(false);
        button.setContentCardSurface(true);
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached standard-height state button whose handler receives that state button instance. */
    public static StateButton createButtonWithHandler(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Consumer<StateButton> action
    ) {
        StateButton button = new StateButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                pressed -> {
                    if (action != null) action.accept(pressed);
                }
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached standard Kinetic slider. Range, step, validation, and responder are caller-defined. */
    public static Slider createSlider(
            int x,
            int y,
            int width,
            Component message,
            double minValue,
            double maxValue,
            double step,
            double value,
            Predicate<Double> validator,
            DoubleConsumer responder,
            Component tooltip
    ) {
        Slider slider = new Slider(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                minValue, maxValue, step, value, validator, responder
        );
        return attachTooltip(slider, tooltip);
    }

    /** Creates a detached item button using the API-defined item-button height and layout. */
    public static ItemButton createItemButton(
            int x, int y, int width, ItemStack icon, Component text, Component tooltip, Runnable action
    ) {
        ItemButton button = new ItemButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.ITEM_BUTTON_HEIGHT,
                icon, text == null ? Component.empty() : text,
                ignored -> { if (action != null) action.run(); }
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached compact-height state button using the API-defined compact height. */
    public static StateButton createCompactButton(int x, int y, int width, Component text, Component tooltip, Runnable action) {
        StateButton button = new StateButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                ignored -> { if (action != null) action.run(); }
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached menu entry whose final bounds are normally assigned by the API menu layout. */
    public static MenuButton createMenuButton(Component text, boolean enabled, boolean danger, Runnable action) {
        MenuButton button = new MenuButton(
                FACTORY_ACCESS, 0, 0, 1, KineticScreen.STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                ignored -> { if (action != null) action.run(); },
                danger
        );
        button.setEnabled(enabled);
        return button;
    }

    /** Creates a detached texture button using the supplied texture coordinates and hover V offset. */
    public static TextureButton createTextureButton(
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
            Component tooltip,
            Runnable action
    ) {
        TextureButton button = new TextureButton(
                FACTORY_ACCESS, x, y, width, height, texture, u, v, hoverVOffset, textureWidth, textureHeight,
                narration == null ? Component.empty() : narration,
                ignored -> { if (action != null) action.run(); }
        );
        return attachTooltip(button, tooltip);
    }

    /** Draws a texture-button icon using the same hover-offset rules as {@link #createTextureButton}. */
    public static void renderTextureButtonIcon(
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

    /**
     * Creates a detached single-line Kinetic text field.
     *
     * @param placeholder display-only placeholder; {@code null} shows no placeholder
     * @param validator business validator; {@code null} accepts every text value and never rewrites the value
     * @param tooltip optional tooltip attached to the returned widget
     */
    public static KineticEditBox createTextField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Predicate<String> validator,
            Component tooltip
    ) {
        KineticEditBox box = new KineticEditBox(
                FACTORY_ACCESS, font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message
        );
        box.setPlaceholder(placeholder);
        box.setValidator(validator);
        return attachTooltip(box, tooltip);
    }

    /** Creates a detached multiline field; height is clamped to at least 20 pixels and placeholder is display-only. */
    public static KineticMultiLineEditBox createMultiLineTextField(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Component placeholder,
            Component tooltip
    ) {
        KineticMultiLineEditBox box = new KineticMultiLineEditBox(
                FACTORY_ACCESS, font, x, y, width, Math.max(20, height),
                message == null ? Component.empty() : message,
                placeholder == null ? Component.empty() : placeholder
        );
        return attachTooltip(box, tooltip);
    }

    /**
     * Creates a detached text-autocomplete field.
     * <p>
     * Suggestions keep their raw value separate from display-only translations; selecting a row writes only the raw
     * value into the field. The dictionary supplier is read whenever suggestions are refreshed and may return an
     * empty list. A {@code null} placeholder disables placeholder rendering.
     */
    public static AutoCompleteBox createAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
            Component tooltip
    ) {
        AutoCompleteBox box = new AutoCompleteBox(
                FACTORY_ACCESS, font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier
        );
        box.setPlaceholder(placeholder);
        return attachTooltip(box, tooltip);
    }

    /** Creates a detached integer field. Sign, range, and validator rules are entirely caller-defined. */
    public static NumericEditBox createIntegerField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = new NumericEditBox(
                FACTORY_ACCESS, font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                KineticNumericFields.Type.INTEGER, allowNegative, minValue, maxValue, validator
        );
        return attachTooltip(box, tooltip);
    }

    /** Creates a detached long field. The API never adds implicit sign or range limits. */
    public static NumericEditBox createLongField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = new NumericEditBox(
                FACTORY_ACCESS, font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                KineticNumericFields.Type.LONG, allowNegative, minValue, maxValue, validator
        );
        return attachTooltip(box, tooltip);
    }

    /** Creates a detached decimal field. The API never adds implicit sign or range limits. */
    public static NumericEditBox createDecimalField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = new NumericEditBox(
                FACTORY_ACCESS, font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                KineticNumericFields.Type.DECIMAL, allowNegative, minValue, maxValue, validator
        );
        return attachTooltip(box, tooltip);
    }

    /** Creates a detached integer autocomplete field with caller-defined numeric limits. */
    public static NumericAutoCompleteBox createIntegerAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = new NumericAutoCompleteBox(
                FACTORY_ACCESS, font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier,
                KineticNumericFields.Type.INTEGER,
                allowNegative, minValue, maxValue, validator
        );
        return attachTooltip(box, tooltip);
    }

    /** Creates a detached long autocomplete field with caller-defined numeric limits. */
    public static NumericAutoCompleteBox createLongAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = new NumericAutoCompleteBox(
                FACTORY_ACCESS, font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier,
                KineticNumericFields.Type.LONG,
                allowNegative, minValue, maxValue, validator
        );
        return attachTooltip(box, tooltip);
    }

    /** Creates a detached decimal autocomplete field with caller-defined numeric limits. */
    public static NumericAutoCompleteBox createDecimalAutoCompleteField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericAutoCompleteBox box = new NumericAutoCompleteBox(
                FACTORY_ACCESS, font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier,
                KineticNumericFields.Type.DECIMAL,
                allowNegative, minValue, maxValue, validator
        );
        return attachTooltip(box, tooltip);
    }


    /** Creates the standard entity-preview renderer using Kinetic's default cache and scaling values. */
    public static EntityPreviewRenderer createEntityPreviewRenderer() {
        return createEntityPreviewRenderer(
                EntityPreviewRenderer.DEFAULT_CACHE_SIZE,
                EntityPreviewRenderer.DEFAULT_FILL_RATIO,
                EntityPreviewRenderer.DEFAULT_MAX_AUTO_SCALE_FACTOR
        );
    }

    /** Creates the standard entity-preview renderer with caller-defined cache and automatic scaling limits. */
    public static EntityPreviewRenderer createEntityPreviewRenderer(
            int maxCacheSize,
            float fillRatio,
            float maxAutoScaleFactor
    ) {
        return new EntityPreviewRenderer(FACTORY_ACCESS, maxCacheSize, fillRatio, maxAutoScaleFactor);
    }

    /**
     * Creates a detached toggle; a {@code null} validator accepts both boolean states.
     * Creates a detached toggle without a custom validator.
     */
    public static ToggleButton createToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return createToggleButton(x, y, width, value, onText, offText, tooltip, null, responder);
    }

    /** Creates a detached standard Kinetic toggle button for helpers, panels, or injected screens. */
    public static ToggleButton createToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder
    ) {
        ToggleButton button = new ToggleButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                value,
                onText == null ? Component.empty() : onText,
                offText == null ? Component.empty() : offText,
                validator,
                responder
        );
        return attachTooltip(button, tooltip);
    }

    /**
     * Creates a detached compact-height toggle using the API-defined compact control height.
     * Creates a detached compact toggle without a custom validator.
     */
    public static ToggleButton createCompactToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return createCompactToggleButton(x, y, width, value, onText, offText, tooltip, null, responder);
    }

    /** Creates a detached compact Kinetic toggle button for helpers, panels, or injected screens. */
    public static ToggleButton createCompactToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder
    ) {
        ToggleButton button = new ToggleButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT,
                value,
                onText == null ? Component.empty() : onText,
                offText == null ? Component.empty() : offText,
                validator,
                responder
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached multi-state cycle control; options must be non-empty. */
    public static CycleButton createCycleButton(
            int x,
            int y,
            int width,
            int index,
            List<Component> options,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        CycleButton button = new CycleButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                index, options, validator, responder
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached high-z toggle; a {@code null} validator accepts both boolean states. */
    public static HighZToggleButton createHighZToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder,
            int zLevel
    ) {
        HighZToggleButton button = new HighZToggleButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                value,
                onText == null ? Component.empty() : onText,
                offText == null ? Component.empty() : offText,
                validator,
                responder,
                zLevel
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached square color-swatch button; only the RGB portion is displayed. */
    public static ColorSwatchButton createColorSwatchButton(
            int x,
            int y,
            int rgb,
            Component tooltip,
            Runnable action
    ) {
        ColorSwatchButton button = new ColorSwatchButton(
                FACTORY_ACCESS, x, y, KineticScreen.COMPACT_CONTROL_HEIGHT, rgb,
                ignored -> { if (action != null) action.run(); }
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached standard button with an inline RGB preview swatch. */
    public static ColorPreviewButton createColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        ColorPreviewButton button = new ColorPreviewButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT, color,
                text == null ? Component.empty() : text,
                ignored -> { if (action != null) action.run(); }
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached standard button rendered at the supplied Z depth. */
    public static HighZButton createHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
    ) {
        HighZButton button = new HighZButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                ignored -> { if (action != null) action.run(); },
                zLevel
        );
        return attachTooltip(button, tooltip);
    }

    /** Creates a detached compact button rendered at the supplied Z depth. */
    public static HighZButton createCompactHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
    ) {
        HighZButton button = new HighZButton(
                FACTORY_ACCESS, x, y, width, KineticScreen.COMPACT_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                ignored -> { if (action != null) action.run(); },
                zLevel
        );
        return attachTooltip(button, tooltip);
    }

    /**
     * Creates one detached dropdown from fully described options.
     * Validators and responders receive only the raw option value; display translations never become stored values.
     */
    public static Dropdown createDropdown(
            int x,
            int y,
            int width,
            List<? extends Option> options,
            String selectedValue,
            Component tooltip,
            Predicate<String> validator,
            Consumer<String> responder,
            Consumer<Dropdown> opener
    ) {
        List<Option> normalizedOptions = options == null ? List.of() : List.copyOf(options);
        Dropdown dropdown = new Dropdown(
                FACTORY_ACCESS, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                normalizedOptions, selectedValue, validator, responder, opener
        );
        return attachTooltip(dropdown, tooltip);
    }

    /**
     * Creates a detached tab bar using the standard control height and equal-width tab buttons.
     * <p>
     * {@code tooltips} may be {@code null} or shorter than {@code labels}; missing entries simply receive no tooltip.
     * The responder receives the selected tab index.
     */
    public static TabBar createTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return createTabBarInternal(x, y, totalWidth, labels, tooltips, selectedIndex, responder, false, false, 0);
    }

    /** Creates a detached tab bar rendered at the supplied Z depth. */
    public static TabBar createHighZTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return createTabBarInternal(x, y, totalWidth, labels, tooltips, selectedIndex, responder, false, true, zLevel);
    }

    /** Creates a detached vertical tab bar rendered at the supplied Z depth. */
    public static TabBar createVerticalHighZTabBar(
            int x,
            int y,
            int width,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return createTabBarInternal(x, y, width, labels, tooltips, selectedIndex, responder, true, true, zLevel);
    }

    private static TabBar createTabBarInternal(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder,
            boolean vertical,
            boolean highZ,
            int zLevel
    ) {
        TabBarImpl tabBar = new TabBarImpl(x, y, totalWidth, labels, selectedIndex, responder, vertical, highZ, zLevel);
        List<? extends Component> safeTooltips = tooltips == null ? List.of() : tooltips;
        List<StateButton> buttons = tabBar.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            Component tooltip = index < safeTooltips.size() ? safeTooltips.get(index) : null;
            attachTooltip(buttons.get(index), tooltip);
        }
        return tabBar;
    }

    /**
     * Creates a compact variable-width tab strip with optional pinned leading tabs and API-managed horizontal scrolling.
     * The returned strip is one detached widget and can be registered on any Kinetic screen surface.
     */
    public static ScrollableTabStrip createScrollableTabStrip(
            Font font,
            int x,
            int y,
            int width,
            List<? extends ScrollableTab> tabs,
            int pinnedLeadingTabs,
            int selectedIndex,
            int initialScrollOffset,
            Component previousText,
            Component nextText,
            Consumer<Integer> responder
    ) {
        return new ScrollableTabStripWidget(
                font, x, y, width, tabs, pinnedLeadingTabs, selectedIndex, initialScrollOffset,
                previousText, nextText, responder
        );
    }



    /** Creates a detached smooth vertical single-selection list using standard Kinetic row controls. */
    public static ScrollableSelectionList createScrollableSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends SelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder
    ) {
        return new ScrollableSelectionListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list rendered at the supplied Z depth. */
    public static ScrollableSelectionList createHighZScrollableSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends SelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return new ScrollableSelectionListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates a detached smooth vertical item-backed single-selection list. */
    public static ScrollableItemSelectionList createScrollableItemSelectionList(
            Font font,
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemSelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder
    ) {
        return new ScrollableItemSelectionListWidget(
                font, x, y, width, height, items, selectedIndex, initialScrollOffset, responder, 0
        );
    }

    /** Creates a detached smooth vertical item-backed single-selection list rendered at the supplied Z depth. */
    public static ScrollableItemSelectionList createHighZScrollableItemSelectionList(
            Font font,
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemSelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return new ScrollableItemSelectionListWidget(
                font, x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates a detached smooth scrollable item-slot grid. */
    public static ScrollableItemGrid createScrollableItemGrid(
            Font font,
            int x,
            int y,
            int width,
            int height,
            ItemGridDensity density,
            List<? extends ItemGridItem> items,
            int initialScrollOffset,
            Consumer<Integer> responder
    ) {
        return new ScrollableItemGridWidget(
                font, x, y, width, height, density, items, initialScrollOffset, responder, 0
        );
    }

    /** Creates a detached smooth scrollable item-slot grid rendered at the supplied Z depth. */
    public static ScrollableItemGrid createHighZScrollableItemGrid(
            Font font,
            int x,
            int y,
            int width,
            int height,
            ItemGridDensity density,
            List<? extends ItemGridItem> items,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return new ScrollableItemGridWidget(
                font, x, y, width, height, density, items, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates a detached smooth vertical single-selection list with one trailing row action. */
    public static ScrollableActionList createScrollableActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder
    ) {
        return new ScrollableActionListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list with one trailing row action at the supplied Z depth. */
    public static ScrollableActionList createHighZScrollableActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        return new ScrollableActionListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical single-selection list with multiple trailing row actions. */
    public static ScrollableMultiActionList createScrollableMultiActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<Integer, Integer> actionResponder
    ) {
        return new ScrollableMultiActionListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, actionResponder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list with multiple trailing row actions at the supplied Z depth. */
    public static ScrollableMultiActionList createHighZScrollableMultiActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<Integer, Integer> actionResponder,
            int zLevel
    ) {
        return new ScrollableMultiActionListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, actionResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical single-selection list with one real toggle and one trailing row action. */
    public static ScrollableToggleActionList createScrollableToggleActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int toggleWidth,
            int actionWidth,
            Consumer<Integer> responder,
            BiConsumer<Integer, Boolean> toggleResponder,
            Consumer<Integer> actionResponder
    ) {
        return new ScrollableToggleActionListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list with one real toggle and one trailing row action at the supplied Z depth. */
    public static ScrollableToggleActionList createHighZScrollableToggleActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int toggleWidth,
            int actionWidth,
            Consumer<Integer> responder,
            BiConsumer<Integer, Boolean> toggleResponder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        return new ScrollableToggleActionListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical item-backed single-selection list with one trailing row action. */
    public static ScrollableItemActionList createScrollableItemActionList(
            Font font,
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder
    ) {
        return new ScrollableItemActionListWidget(
                font, x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, 0
        );
    }

    /** Creates a detached smooth vertical item-backed single-selection list with one trailing row action at the supplied Z depth. */
    public static ScrollableItemActionList createHighZScrollableItemActionList(
            Font font,
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        return new ScrollableItemActionListWidget(
                font, x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical single-selection list with any number of real toggles per row. */
    public static ScrollableMultiToggleList createScrollableMultiToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiToggleItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<ToggleHit, Boolean> toggleResponder
    ) {
        return new ScrollableMultiToggleListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, toggleResponder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list with any number of real toggles per row at the supplied Z depth. */
    public static ScrollableMultiToggleList createHighZScrollableMultiToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiToggleItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<ToggleHit, Boolean> toggleResponder,
            int zLevel
    ) {
        return new ScrollableMultiToggleListWidget(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, toggleResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical multi-toggle list using standard Kinetic row controls. */
    public static ScrollableToggleList createScrollableToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder
    ) {
        return new ScrollableToggleListWidget(
                x, y, width, height, items, initialScrollOffset, responder, 0
        );
    }

    /** Creates a detached smooth vertical multi-toggle list rendered at the supplied Z depth. */
    public static ScrollableToggleList createHighZScrollableToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder,
            int zLevel
    ) {
        return new ScrollableToggleListWidget(
                x, y, width, height, items, initialScrollOffset, responder, zLevel
        );
    }

    /**
     * Applies the standard Minecraft tooltip to any detached widget and returns the same widget for fluent setup.
     * A {@code null} or blank component clears the tooltip.
     */
    private static <T extends AbstractWidget & KineticControl> T attachTooltip(T widget, Component tooltip) {
        if (widget == null) return null;
        KineticControlBridge.setTooltip(widget, tooltip);
        return widget;
    }

    private static final class TabBarImpl implements TabBar {
        private final List<StateButton> buttons = new ArrayList<>();
        private final Consumer<Integer> responder;
        private int selectedIndex;

        private TabBarImpl(
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
                                FACTORY_ACCESS, buttonX, buttonY, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                                labels.get(index), ignored -> select(buttonIndex, true), zLevel
                        )
                        : new MenuButton(
                                FACTORY_ACCESS, buttonX, buttonY, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
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

    private static final class ScrollableTabStripWidget extends AbstractWidget implements ScrollableTabStrip {
        private static final int TAB_HEIGHT = KineticScreen.COMPACT_CONTROL_HEIGHT;
        private static final int TAB_GAP = 4;
        private static final int ARROW_WIDTH = 18;
        private static final int EDGE_PADDING = 2;
        private static final int SCROLLBAR_GAP = 5;
        private static final int SCROLLBAR_HEIGHT = 4;
        private static final int MIN_TAB_WIDTH = 36;
        private static final int MAX_TAB_WIDTH = 126;
        private static final int MIN_THUMB_WIDTH = 24;

        private final Font font;
        private final int pinnedLeadingTabs;
        private final Consumer<Integer> responder;
        private final GridScrollController scroll = new GridScrollController();
        private final StateButton previousButton;
        private final StateButton nextButton;
        private final List<StateButton> tabButtons = new ArrayList<>();
        private final List<Integer> tabWidths = new ArrayList<>();
        private final List<Integer> scrollStarts = new ArrayList<>();
        private List<ScrollableTab> tabs = List.of();
        private int selectedIndex = -1;
        private int hoveredTabIndex = -1;
        private int pendingInitialScrollOffset;

        private ScrollableTabStripWidget(
                Font font,
                int x,
                int y,
                int width,
                List<? extends ScrollableTab> tabs,
                int pinnedLeadingTabs,
                int selectedIndex,
                int initialScrollOffset,
                Component previousText,
                Component nextText,
                Consumer<Integer> responder
        ) {
            super(x, y, Math.max(1, width), TAB_HEIGHT + SCROLLBAR_GAP + SCROLLBAR_HEIGHT, Component.empty());
            this.font = font;
            this.pinnedLeadingTabs = Math.max(0, pinnedLeadingTabs);
            Component previousLabel = previousText == null ? Component.empty() : previousText;
            Component nextLabel = nextText == null ? Component.empty() : nextText;
            this.responder = responder;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            this.previousButton = new StateButton(
                    FACTORY_ACCESS, x, y, ARROW_WIDTH, TAB_HEIGHT, previousLabel,
                    ignored -> scrollBy(-scrollStep())
            );
            this.nextButton = new StateButton(
                    FACTORY_ACCESS, x, y, ARROW_WIDTH, TAB_HEIGHT, nextLabel,
                    ignored -> scrollBy(scrollStep())
            );
            setTabs(tabs);
            setSelectedIndex(selectedIndex);
        }

        @Override
        public void setTabs(List<? extends ScrollableTab> nextTabs) {
            tabs = nextTabs == null ? List.of() : List.copyOf(nextTabs);
            tabButtons.clear();
            tabWidths.clear();
            scrollStarts.clear();

            for (int index = 0; index < tabs.size(); index++) {
                ScrollableTab tab = tabs.get(index);
                int buttonIndex = index;
                StateButton button = new StateButton(
                        FACTORY_ACCESS, getX(), getY(), tabWidth(tab), TAB_HEIGHT,
                        displayLabel(tab, index == selectedIndex),
                        ignored -> select(buttonIndex, true)
                );
                tabButtons.add(button);
                tabWidths.add(button.getWidth());
            }

            if (tabs.isEmpty()) selectedIndex = -1;
            else if (selectedIndex < 0 || selectedIndex >= tabs.size()) selectedIndex = 0;

            refreshRange();
            refreshLayout();
            refreshSelection();
        }

        @Override
        public List<ScrollableTab> tabs() {
            return List.copyOf(tabs);
        }

        @Override
        public int selectedIndex() {
            return selectedIndex;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (tabs.isEmpty()) {
                selectedIndex = -1;
                refreshSelection();
                return;
            }
            select(Math.max(0, Math.min(tabs.size() - 1, index)), false);
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public void scrollBy(int pixels) {
            int next = Math.max(0, Math.min(scroll.maxOffset(), scroll.offset() + pixels));
            scroll.setOffset(next);
            refreshLayout();
        }

        @Override
        public void ensureSelectedVisible() {
            int pinned = pinnedCount();
            if (selectedIndex < pinned || selectedIndex < 0 || selectedIndex >= tabs.size()) return;
            int local = selectedIndex - pinned;
            if (local < 0 || local >= scrollStarts.size()) return;
            int start = scrollStarts.get(local);
            int end = start + tabWidths.get(selectedIndex);
            int viewportWidth = scrollViewportWidth();
            int offset = scroll.offset();
            if (start < offset) setScrollOffset(start);
            else if (end > offset + viewportWidth) setScrollOffset(end - viewportWidth);
        }

        @Override
        public int tabAt(double mouseX, double mouseY) {
            if (mouseY < getY() || mouseY >= getY() + TAB_HEIGHT) return -1;
            int pinned = pinnedCount();
            for (int index = 0; index < pinned; index++) {
                if (tabButtons.get(index).isMouseOver(mouseX, mouseY)) return index;
            }
            if (mouseX < scrollViewportLeft() || mouseX >= scrollViewportRight()) return -1;
            for (int index = pinned; index < tabButtons.size(); index++) {
                if (tabButtons.get(index).isMouseOver(mouseX, mouseY)) return index;
            }
            return -1;
        }

        @Override
        public Component hoveredTooltip() {
            return hoveredTabIndex >= 0 && hoveredTabIndex < tabs.size()
                    ? tabs.get(hoveredTabIndex).tooltip()
                    : null;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshLayout();
            int pinned = pinnedCount();
            for (int index = 0; index < pinned; index++) {
                tabButtons.get(index).render(graphics, mouseX, mouseY, partialTick);
            }

            previousButton.render(graphics, mouseX, mouseY, partialTick);
            nextButton.render(graphics, mouseX, mouseY, partialTick);

            if (scrollViewportWidth() > 0) {
                KineticRenderRuntime.enableScissor(graphics, scrollViewportLeft(), getY(), scrollViewportRight(), getY() + TAB_HEIGHT);
                try {
                    for (int index = pinned; index < tabButtons.size(); index++) {
                        StateButton button = tabButtons.get(index);
                        if (button.getX() + button.getWidth() <= scrollViewportLeft() || button.getX() >= scrollViewportRight()) continue;
                        int clippedMouseX = mouseX >= scrollViewportLeft() && mouseX < scrollViewportRight()
                                ? mouseX
                                : Integer.MIN_VALUE;
                        button.render(graphics, clippedMouseX, mouseY, partialTick);
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
            }

            if (scroll.canScroll()) {
                scroll.renderHorizontal(
                        graphics, mouseX, mouseY,
                        getX(), scrollbarY(), getWidth(), SCROLLBAR_HEIGHT, MIN_THUMB_WIDTH
                );
            }

            hoveredTabIndex = tabAt(mouseX, mouseY);
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginHorizontalDrag(
                    mouseX, mouseY, getX(), scrollbarY(), getWidth(), SCROLLBAR_HEIGHT, MIN_THUMB_WIDTH, 2
            )) return true;
            if (previousButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (nextButton.mouseClicked(mouseX, mouseY, button)) return true;
            int tab = tabAt(mouseX, mouseY);
            return tab >= 0 && tabButtons.get(tab).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.dragHorizontal(mouseX, getX(), getWidth(), MIN_THUMB_WIDTH)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            scroll.scroll(delta, 28D);
            refreshLayout();
            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible
                    && mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= getY()
                    && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (selectedIndex >= 0 && selectedIndex < tabs.size()) {
                output.add(NarratedElementType.TITLE, displayLabel(tabs.get(selectedIndex), true));
            }
        }

        private void select(int index, boolean notify) {
            if (index < 0 || index >= tabs.size()) return;
            selectedIndex = index;
            refreshSelection();
            ensureSelectedVisible();
            if (notify && responder != null) responder.accept(index);
        }

        private void refreshSelection() {
            for (int index = 0; index < tabButtons.size(); index++) {
                StateButton button = tabButtons.get(index);
                boolean selected = index == selectedIndex;
                button.setSelected(selected);
                button.setMessage(displayLabel(tabs.get(index), selected));
            }
            setMessage(selectedIndex >= 0 && selectedIndex < tabs.size()
                    ? displayLabel(tabs.get(selectedIndex), true)
                    : Component.empty());
        }

        private void refreshRange() {
            int pinned = pinnedCount();
            scrollStarts.clear();
            int contentWidth = 0;
            for (int index = pinned; index < tabs.size(); index++) {
                scrollStarts.add(contentWidth);
                contentWidth += tabWidths.get(index) + TAB_GAP;
            }
            if (contentWidth > 0) contentWidth -= TAB_GAP;
            int viewportWidth = scrollViewportWidth();
            scroll.updateRange(Math.max(0, contentWidth - viewportWidth), contentWidth, Math.max(1, viewportWidth));
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int pinned = pinnedCount();
            int cursor = getX();
            for (int index = 0; index < pinned; index++) {
                StateButton button = tabButtons.get(index);
                button.setX(cursor);
                button.setY(getY());
                cursor += button.getWidth() + TAB_GAP;
            }

            int previousX = cursor;
            previousButton.setX(previousX);
            previousButton.setY(getY());
            previousButton.active = scroll.offset() > 0;
            previousButton.visible = !tabs.isEmpty();

            nextButton.setX(getX() + getWidth() - ARROW_WIDTH);
            nextButton.setY(getY());
            nextButton.active = scroll.offset() < scroll.maxOffset();
            nextButton.visible = !tabs.isEmpty();

            double offset = scroll.smoothOffset();
            int viewportLeft = scrollViewportLeft();
            int local = 0;
            for (int index = pinned; index < tabButtons.size(); index++) {
                StateButton button = tabButtons.get(index);
                int start = local < scrollStarts.size() ? scrollStarts.get(local) : 0;
                button.setX(viewportLeft + start - (int) Math.round(offset));
                button.setY(getY());
                local++;
            }
        }

        private int pinnedCount() {
            return Math.min(pinnedLeadingTabs, tabs.size());
        }

        private int pinnedWidth() {
            int width = 0;
            int pinned = pinnedCount();
            for (int index = 0; index < pinned; index++) width += tabWidths.get(index) + TAB_GAP;
            return width;
        }

        private int scrollViewportLeft() {
            return getX() + pinnedWidth() + ARROW_WIDTH + TAB_GAP + EDGE_PADDING;
        }

        private int scrollViewportRight() {
            return Math.max(scrollViewportLeft(), getX() + getWidth() - ARROW_WIDTH - TAB_GAP - EDGE_PADDING);
        }

        private int scrollViewportWidth() {
            return Math.max(1, scrollViewportRight() - scrollViewportLeft());
        }

        private int scrollbarY() {
            return getY() + TAB_HEIGHT + SCROLLBAR_GAP;
        }

        private int scrollStep() {
            return Math.max(24, scrollViewportWidth() / 3);
        }

        private int tabWidth(ScrollableTab tab) {
            Component normal = tab == null || tab.label() == null ? Component.empty() : tab.label();
            Component selected = tab == null || tab.selectedLabel() == null ? normal : tab.selectedLabel();
            int textWidth = Math.max(font.width(normal), font.width(selected));
            return Math.max(MIN_TAB_WIDTH, Math.min(MAX_TAB_WIDTH, textWidth + 18));
        }

        private Component displayLabel(ScrollableTab tab, boolean selected) {
            if (tab == null) return Component.empty();
            Component normal = tab.label() == null ? Component.empty() : tab.label();
            Component selectedLabel = tab.selectedLabel() == null ? normal : tab.selectedLabel();
            return selected ? selectedLabel : normal;
        }
    }


    private static final class ScrollableSelectionListWidget extends AbstractWidget implements ScrollableSelectionList {
        private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
        private static final int ROW_PITCH = ROW_HEIGHT + 5;
        private static final int ROW_TOP_PADDING = 2;
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final Consumer<Integer> responder;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private final List<StateButton> rowButtons = new ArrayList<>();
        private List<SelectionItem> items = List.of();
        private int selectedIndex = -1;
        private int hoveredIndex = -1;
        private int pendingInitialScrollOffset;

        private ScrollableSelectionListWidget(
                int x,
                int y,
                int width,
                int height,
                List<? extends SelectionItem> items,
                int selectedIndex,
                int initialScrollOffset,
                Consumer<Integer> responder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.responder = responder;
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
            setSelectedIndex(selectedIndex);
        }

        @Override
        public void setItems(List<? extends SelectionItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            rowButtons.clear();
            for (int index = 0; index < items.size(); index++) {
                SelectionItem item = items.get(index);
                int rowIndex = index;
                StateButton button = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        contentWidth(),
                        ROW_HEIGHT,
                        itemLabel(item),
                        ignored -> select(rowIndex, true)
                );
                button.active = item != null && item.active();
                button.setError(item != null && item.error());
                rowButtons.add(button);
            }
            if (items.isEmpty()) selectedIndex = -1;
            else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            refreshRange();
            refreshSelection();
            refreshLayout();
        }

        @Override
        public List<SelectionItem> items() {
            return List.copyOf(items);
        }

        @Override
        public int selectedIndex() {
            return selectedIndex;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (items.isEmpty()) {
                selectedIndex = -1;
                refreshSelection();
                return;
            }
            if (index < 0) {
                selectedIndex = -1;
                refreshSelection();
                return;
            }
            select(Math.min(items.size() - 1, index), false);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
            refreshLayout();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public void ensureSelectedVisible() {
            if (selectedIndex < 0 || selectedIndex >= items.size()) return;
            int visible = visibleRows();
            int offset = scroll.offset();
            if (selectedIndex < offset) {
                setScrollOffset(selectedIndex);
            } else if (selectedIndex >= offset + visible) {
                setScrollOffset(selectedIndex - visible + 1);
            }
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            if (!visible || mouseX < getX() || mouseX >= getX() + contentWidth()
                    || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            for (int index = start; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                if (rowY >= getY() + getHeight()) break;
                if (rowY + ROW_HEIGHT <= getY()) continue;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
            }
            return -1;
        }

        @Override
        public Component hoveredTooltip() {
            return hoveredIndex >= 0 && hoveredIndex < items.size() && items.get(hoveredIndex) != null
                    ? items.get(hoveredIndex).tooltip()
                    : null;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            refreshLayout();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int start = scroll.smoothIndexOffset();
                    int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                    for (int index = start; index < end; index++) {
                        StateButton button = rowButtons.get(index);
                        if (!button.visible) continue;
                        int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                                ? mouseX
                                : Integer.MIN_VALUE;
                        button.render(graphics, clippedMouseX, mouseY, partialTick);
                        SelectionItem item = items.get(index);
                        if (item != null && item.marked()) {
                            int markerX = button.getX() + button.getWidth() - 8;
                            int markerY = button.getY() + Math.max(2, (button.getHeight() - 4) / 2);
                            graphics.fill(markerX, markerY, markerX + 4, markerY + 4, 0xFF00C853);
                        }
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(
                            graphics,
                            mouseX,
                            mouseY,
                            scrollbarX(),
                            getY(),
                            SCROLLBAR_WIDTH,
                            getHeight(),
                            MIN_THUMB_HEIGHT
                    );
                }
            } finally {
                graphics.pose().popPose();
            }
            hoveredIndex = itemAt(mouseX, mouseY);
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(
                    mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT, 2
            )) return true;
            int index = itemAt(mouseX, mouseY);
            return index >= 0 && rowButtons.get(index).active && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            boolean handled = scroll.scroll(delta, 1.0D);
            refreshLayout();
            return handled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible
                    && mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= getY()
                    && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
            }
        }

        private void select(int index, boolean notify) {
            if (index < 0 || index >= items.size()) return;
            SelectionItem item = items.get(index);
            if (item == null || !item.active()) return;
            selectedIndex = index;
            refreshSelection();
            ensureSelectedVisible();
            if (notify && responder != null) responder.accept(index);
        }

        private void refreshSelection() {
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                SelectionItem item = items.get(index);
                button.setSelected(index == selectedIndex);
                button.setError(item != null && item.error());
                button.setMessage(itemLabel(item));
                button.active = active && item != null && item.active();
            }
            setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                    ? itemLabel(items.get(selectedIndex))
                    : Component.empty());
        }

        private void refreshRange() {
            scroll.update(items.size(), visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                boolean rowVisible = visible && index >= start && index < end;
                button.visible = rowVisible;
                button.setX(getX());
                button.setY(getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING);
                button.setWidth(contentWidth());
                SelectionItem item = items.get(index);
                button.active = active && rowVisible && item != null && item.active();
            }
        }

        private int visibleRows() {
            return Math.max(1, getHeight() / ROW_PITCH);
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private static Component itemLabel(SelectionItem item) {
            if (item == null) return Component.empty();
            Component primary = item.label() == null ? Component.empty() : item.label();
            Component secondary = item.secondaryLabel();
            if (secondary == null || secondary.getString().isBlank()) return primary;
            return primary.copy().append("   ").append(secondary);
        }
    }

    private static final class ScrollableItemSelectionListWidget extends AbstractWidget implements ScrollableItemSelectionList {
        private static final int ROW_HEIGHT = 20;
        private static final int ROW_PITCH = ROW_HEIGHT + 2;
        private static final int ROW_TOP_PADDING = 1;
        private static final int ITEM_SLOT_SIZE = 18;
        private static final int ITEM_LEFT_PADDING = 2;
        private static final int TEXT_LEFT_PADDING = 24;
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final Font font;
        private final Consumer<Integer> responder;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private final List<StateButton> rowButtons = new ArrayList<>();
        private List<ItemSelectionItem> items = List.of();
        private int selectedIndex = -1;
        private int hoveredIndex = -1;
        private ItemStack hoveredStack = ItemStack.EMPTY;
        private int pendingInitialScrollOffset;

        private ScrollableItemSelectionListWidget(
                Font font,
                int x,
                int y,
                int width,
                int height,
                List<? extends ItemSelectionItem> items,
                int selectedIndex,
                int initialScrollOffset,
                Consumer<Integer> responder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.font = font;
            this.responder = responder;
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
            setSelectedIndex(selectedIndex);
        }

        @Override
        public void setItems(List<? extends ItemSelectionItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            rowButtons.clear();
            for (int index = 0; index < items.size(); index++) {
                ItemSelectionItem item = items.get(index);
                int rowIndex = index;
                StateButton button = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        contentWidth(),
                        ROW_HEIGHT,
                        Component.empty(),
                        ignored -> select(rowIndex, true)
                );
                button.active = item != null && item.active();
                button.setError(item != null && item.error());
                rowButtons.add(button);
            }
            if (items.isEmpty()) selectedIndex = -1;
            else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            refreshRange();
            refreshSelection();
            refreshLayout();
        }

        @Override
        public List<ItemSelectionItem> items() {
            return List.copyOf(items);
        }

        @Override
        public int selectedIndex() {
            return selectedIndex;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (items.isEmpty() || index < 0) {
                selectedIndex = -1;
                refreshSelection();
                return;
            }
            select(Math.min(items.size() - 1, index), false);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
            refreshLayout();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public void ensureSelectedVisible() {
            if (selectedIndex < 0 || selectedIndex >= items.size()) return;
            int visible = visibleRows();
            int offset = scroll.offset();
            if (selectedIndex < offset) {
                setScrollOffset(selectedIndex);
            } else if (selectedIndex >= offset + visible) {
                setScrollOffset(selectedIndex - visible + 1);
            }
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            if (!visible || mouseX < getX() || mouseX >= getX() + contentWidth()
                    || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            for (int index = start; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                if (rowY >= getY() + getHeight()) break;
                if (rowY + ROW_HEIGHT <= getY()) continue;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
            }
            return -1;
        }

        @Override
        public ItemStack stackAt(double mouseX, double mouseY) {
            int index = itemAt(mouseX, mouseY);
            if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
            StateButton button = rowButtons.get(index);
            int slotX = button.getX() + ITEM_LEFT_PADDING;
            int slotY = button.getY() + Math.max(0, (ROW_HEIGHT - ITEM_SLOT_SIZE) / 2);
            if (!GuiTheme.hovering(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) return ItemStack.EMPTY;
            ItemSelectionItem item = items.get(index);
            return safeStack(item);
        }

        @Override
        public ItemStack hoveredStack() {
            return hoveredStack;
        }

        @Override
        public Component hoveredTooltip() {
            return hoveredIndex >= 0 && hoveredIndex < items.size() && items.get(hoveredIndex) != null
                    ? items.get(hoveredIndex).tooltip()
                    : null;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            refreshLayout();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int start = scroll.smoothIndexOffset();
                    int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                    for (int index = start; index < end; index++) {
                        StateButton button = rowButtons.get(index);
                        if (!button.visible) continue;
                        int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                                ? mouseX
                                : Integer.MIN_VALUE;
                        button.render(graphics, clippedMouseX, mouseY, partialTick);
                        renderRowContent(graphics, button, items.get(index), mouseX, mouseY);
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(
                            graphics,
                            mouseX,
                            mouseY,
                            scrollbarX(),
                            getY(),
                            SCROLLBAR_WIDTH,
                            getHeight(),
                            MIN_THUMB_HEIGHT
                    );
                }
            } finally {
                graphics.pose().popPose();
            }
            hoveredIndex = itemAt(mouseX, mouseY);
            hoveredStack = stackAt(mouseX, mouseY);
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        private void renderRowContent(
                GuiGraphics graphics,
                StateButton button,
                ItemSelectionItem item,
                int mouseX,
                int mouseY
        ) {
            if (item == null) return;
            int slotX = button.getX() + ITEM_LEFT_PADDING;
            int slotY = button.getY() + Math.max(0, (ROW_HEIGHT - ITEM_SLOT_SIZE) / 2);
            boolean slotHovered = GuiTheme.hovering(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
            ItemStack stack = safeStack(item);
            GuiTheme.itemSlot(graphics, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, 4, false, slotHovered, item.error());
            GuiTheme.item(graphics, font, stack, slotX, slotY, ITEM_SLOT_SIZE, 1.0F, false);

            int textX = button.getX() + TEXT_LEFT_PADDING;
            int textWidth = Math.max(1, button.getWidth() - TEXT_LEFT_PADDING - 10);
            KineticText.drawScrollingLeft(
                    graphics, font, itemLabel(item), textX,
                    button.getY() + Math.max(1, (ROW_HEIGHT - font.lineHeight) / 2),
                    textWidth, GuiTheme.current().text(), true
            );
            if (item.marked()) {
                int markerX = button.getX() + button.getWidth() - 8;
                int markerY = button.getY() + Math.max(2, (button.getHeight() - 4) / 2);
                graphics.fill(markerX, markerY, markerX + 4, markerY + 4, 0xFF00C853);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(
                    mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT, 2
            )) return true;
            int index = itemAt(mouseX, mouseY);
            return index >= 0 && rowButtons.get(index).active && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            boolean handled = scroll.scroll(delta, 1.0D);
            refreshLayout();
            return handled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible
                    && mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= getY()
                    && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
            }
        }

        private void select(int index, boolean notify) {
            if (index < 0 || index >= items.size()) return;
            ItemSelectionItem item = items.get(index);
            if (item == null || !item.active()) return;
            selectedIndex = index;
            refreshSelection();
            ensureSelectedVisible();
            if (notify && responder != null) responder.accept(index);
        }

        private void refreshSelection() {
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                ItemSelectionItem item = items.get(index);
                button.setSelected(index == selectedIndex);
                button.setError(item != null && item.error());
                button.active = active && item != null && item.active();
            }
            setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                    ? itemLabel(items.get(selectedIndex))
                    : Component.empty());
        }

        private void refreshRange() {
            scroll.update(items.size(), visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                boolean rowVisible = visible && index >= start && index < end;
                button.visible = rowVisible;
                button.setX(getX());
                button.setY(getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING);
                button.setWidth(contentWidth());
                ItemSelectionItem item = items.get(index);
                button.active = active && rowVisible && item != null && item.active();
            }
        }

        private int visibleRows() {
            return Math.max(1, getHeight() / ROW_PITCH);
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private static ItemStack safeStack(ItemSelectionItem item) {
            return item == null || item.stack() == null ? ItemStack.EMPTY : item.stack();
        }

        private static Component itemLabel(ItemSelectionItem item) {
            if (item == null) return Component.empty();
            Component primary = item.label() == null ? Component.empty() : item.label();
            Component secondary = item.secondaryLabel();
            if (secondary == null || secondary.getString().isBlank()) return primary;
            return primary.copy().append("   ").append(secondary);
        }
    }

    private static final class ScrollableItemGridWidget extends AbstractWidget implements ScrollableItemGrid {
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final Font font;
        private final ItemGridDensity density;
        private final Consumer<Integer> responder;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private List<ItemGridItem> items = List.of();
        private int hoveredIndex = -1;
        private ItemStack hoveredStack = ItemStack.EMPTY;
        private int pendingInitialScrollOffset;

        private ScrollableItemGridWidget(
                Font font,
                int x,
                int y,
                int width,
                int height,
                ItemGridDensity density,
                List<? extends ItemGridItem> items,
                int initialScrollOffset,
                Consumer<Integer> responder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.font = font;
            this.density = density == null ? ItemGridDensity.STANDARD : density;
            this.responder = responder;
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
        }

        @Override
        public void setItems(List<? extends ItemGridItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            refreshRange();
        }

        @Override
        public List<ItemGridItem> items() {
            return List.copyOf(items);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public int columns() {
            int available = Math.max(1, contentWidth() - density.padding() * 2);
            return Math.max(1, (available + density.gap()) / density.cellPitch());
        }

        @Override
        public int visibleRows() {
            int available = Math.max(density.slotSize(), getHeight() - density.padding() * 2);
            return Math.max(1, (available + density.gap()) / density.cellPitch());
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            if (!visible || mouseX < gridX() || mouseX >= gridRight()
                    || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int shift = scroll.visualShift(density.cellPitch());
            double localX = mouseX - gridX();
            double localY = mouseY - gridY() + shift;
            if (localX < 0 || localY < 0) return -1;
            int col = (int) (localX / density.cellPitch());
            int row = (int) (localY / density.cellPitch());
            if (col < 0 || col >= columns()) return -1;
            if (localX - col * density.cellPitch() >= density.slotSize()
                    || localY - row * density.cellPitch() >= density.slotSize()) return -1;
            int index = (scroll.smoothIndexOffset() + row) * columns() + col;
            return index >= 0 && index < items.size() ? index : -1;
        }

        @Override
        public ItemStack stackAt(double mouseX, double mouseY) {
            int index = itemAt(mouseX, mouseY);
            return index >= 0 ? safeStack(items.get(index)) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack hoveredStack() {
            return hoveredStack;
        }

        @Override
        public Component hoveredTooltip() {
            return hoveredIndex >= 0 && hoveredIndex < items.size() && items.get(hoveredIndex) != null
                    ? items.get(hoveredIndex).tooltip()
                    : null;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int cols = columns();
                    int startRow = scroll.smoothIndexOffset();
                    int shift = scroll.visualShift(density.cellPitch());
                    int start = startRow * cols;
                    int end = Math.min(items.size(), start + cols * (visibleRows() + 1));
                    for (int index = start; index < end; index++) {
                        int local = index - start;
                        int x = gridX() + (local % cols) * density.cellPitch();
                        int y = gridY() + (local / cols) * density.cellPitch() - shift;
                        ItemGridItem item = items.get(index);
                        if (item == null) continue;
                        boolean hover = GuiTheme.hovering(mouseX, mouseY, x, y, density.slotSize(), density.slotSize());
                        GuiTheme.itemSlot(
                                graphics, x, y, density.slotSize(), density.slotSize(), 4,
                                item.selected(), hover, item.error()
                        );
                        ItemStack stack = safeStack(item);
                        if (!stack.isEmpty()) {
                            GuiTheme.item(graphics, font, stack, x, y, density.slotSize(), density.renderScale(), density.decorations());
                        }
                        ItemGridMarker marker = item.marker() == null
                                ? (item.marked() ? ItemGridMarker.SUCCESS : ItemGridMarker.NONE)
                                : item.marker();
                        if (marker != ItemGridMarker.NONE) {
                            int markerSize = Math.max(4, Math.min(8, density.slotSize() / 8));
                            int markerX = x + density.slotSize() - markerSize - 3;
                            int markerY = y + 3;
                            int markerColor = marker == ItemGridMarker.SUCCESS ? 0xFF00C853 : 0xFFFFAA00;
                            graphics.fill(
                                    markerX,
                                    markerY,
                                    markerX + markerSize,
                                    markerY + markerSize,
                                    markerColor
                            );
                        }
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(
                            graphics, mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH,
                            getHeight(), MIN_THUMB_HEIGHT
                    );
                }
            } finally {
                graphics.pose().popPose();
            }
            hoveredIndex = itemAt(mouseX, mouseY);
            hoveredStack = stackAt(mouseX, mouseY);
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(
                    mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT, 2
            )) return true;
            int index = itemAt(mouseX, mouseY);
            if (index < 0 || index >= items.size()) return false;
            ItemGridItem item = items.get(index);
            if (item == null || !item.active()) return false;
            if (responder != null) responder.accept(index);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            return scroll.scroll(delta, 1.0D);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible && mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= getY() && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (hoveredIndex < 0 || hoveredIndex >= items.size()) return;
            ItemStack stack = safeStack(items.get(hoveredIndex));
            if (!stack.isEmpty()) output.add(NarratedElementType.TITLE, stack.getHoverName());
        }

        private void refreshRange() {
            int rows = (items.size() + columns() - 1) / columns();
            scroll.update(rows, visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int gridX() {
            return getX() + density.padding();
        }

        private int gridY() {
            return getY() + density.padding();
        }

        private int gridRight() {
            return gridX() + columns() * density.cellPitch() - density.gap();
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private static ItemStack safeStack(ItemGridItem item) {
            return item == null || item.stack() == null ? ItemStack.EMPTY : item.stack();
        }
    }

    private static final class ScrollableActionListWidget extends AbstractWidget implements ScrollableActionList {
        private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
        private static final int ROW_PITCH = ROW_HEIGHT + 5;
        private static final int ROW_TOP_PADDING = 2;
        private static final int ACTION_GAP = 4;
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final Consumer<Integer> responder;
        private final Consumer<Integer> actionResponder;
        private final int actionWidth;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private final List<StateButton> rowButtons = new ArrayList<>();
        private final List<StateButton> actionButtons = new ArrayList<>();
        private List<ActionItem> items = List.of();
        private int selectedIndex = -1;
        private int hoveredIndex = -1;
        private boolean hoveredAction;
        private int pendingInitialScrollOffset;

        private ScrollableActionListWidget(
                int x,
                int y,
                int width,
                int height,
                List<? extends ActionItem> items,
                int selectedIndex,
                int initialScrollOffset,
                int actionWidth,
                Consumer<Integer> responder,
                Consumer<Integer> actionResponder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.responder = responder;
            this.actionResponder = actionResponder;
            this.actionWidth = Math.max(1, actionWidth);
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
            setSelectedIndex(selectedIndex);
        }

        @Override
        public void setItems(List<? extends ActionItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            rowButtons.clear();
            actionButtons.clear();
            for (int index = 0; index < items.size(); index++) {
                ActionItem item = items.get(index);
                int rowIndex = index;
                StateButton rowButton = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        rowWidth(),
                        ROW_HEIGHT,
                        itemLabel(item),
                        ignored -> select(rowIndex, true)
                );
                rowButton.active = item != null && item.active();
                rowButton.setError(item != null && item.error());
                rowButtons.add(rowButton);

                StateButton actionButton = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        this.actionWidth,
                        ROW_HEIGHT,
                        item == null || item.actionLabel() == null ? Component.empty() : item.actionLabel(),
                        ignored -> {
                            ActionItem current = rowIndex < items.size() ? items.get(rowIndex) : null;
                            if (current == null || !current.actionActive()) return;
                            if (actionResponder != null) actionResponder.accept(rowIndex);
                        }
                );
                actionButton.active = item != null && item.actionActive();
                actionButtons.add(actionButton);
            }
            if (items.isEmpty()) selectedIndex = -1;
            else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            refreshRange();
            refreshSelection();
            refreshLayout();
        }

        @Override
        public List<ActionItem> items() {
            return List.copyOf(items);
        }

        @Override
        public int selectedIndex() {
            return selectedIndex;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (items.isEmpty()) {
                selectedIndex = -1;
                refreshSelection();
                return;
            }
            if (index < 0) {
                selectedIndex = -1;
                refreshSelection();
                return;
            }
            select(Math.min(items.size() - 1, index), false);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
            refreshLayout();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public void ensureSelectedVisible() {
            if (selectedIndex < 0 || selectedIndex >= items.size()) return;
            int visible = visibleRows();
            int offset = scroll.offset();
            if (selectedIndex < offset) {
                setScrollOffset(selectedIndex);
            } else if (selectedIndex >= offset + visible) {
                setScrollOffset(selectedIndex - visible + 1);
            }
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            return rowIndexAt(mouseX, mouseY, false);
        }

        @Override
        public int actionAt(double mouseX, double mouseY) {
            return rowIndexAt(mouseX, mouseY, true);
        }

        @Override
        public Component hoveredTooltip() {
            if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
            ActionItem item = items.get(hoveredIndex);
            if (item == null) return null;
            return hoveredAction ? item.actionTooltip() : item.tooltip();
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            refreshLayout();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int start = scroll.smoothIndexOffset();
                    int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                    for (int index = start; index < end; index++) {
                        StateButton rowButton = rowButtons.get(index);
                        StateButton actionButton = actionButtons.get(index);
                        if (!rowButton.visible) continue;
                        int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                                ? mouseX
                                : Integer.MIN_VALUE;
                        rowButton.render(graphics, clippedMouseX, mouseY, partialTick);
                        actionButton.render(graphics, clippedMouseX, mouseY, partialTick);
                        ActionItem item = items.get(index);
                        if (item != null && item.marked()) {
                            int markerX = rowButton.getX() + rowButton.getWidth() - 8;
                            int markerY = rowButton.getY() + Math.max(2, (rowButton.getHeight() - 4) / 2);
                            graphics.fill(markerX, markerY, markerX + 4, markerY + 4, 0xFF00C853);
                        }
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(
                            graphics,
                            mouseX,
                            mouseY,
                            scrollbarX(),
                            getY(),
                            SCROLLBAR_WIDTH,
                            getHeight(),
                            MIN_THUMB_HEIGHT
                    );
                }
            } finally {
                graphics.pose().popPose();
            }
            int actionIndex = actionAt(mouseX, mouseY);
            if (actionIndex >= 0) {
                hoveredIndex = actionIndex;
                hoveredAction = true;
            } else {
                hoveredIndex = itemAt(mouseX, mouseY);
                hoveredAction = false;
            }
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(
                    mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT, 2
            )) return true;
            int actionIndex = actionAt(mouseX, mouseY);
            if (actionIndex >= 0) {
                StateButton actionButton = actionButtons.get(actionIndex);
                return actionButton.active && actionButton.mouseClicked(mouseX, mouseY, button);
            }
            int index = itemAt(mouseX, mouseY);
            return index >= 0 && rowButtons.get(index).active && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            boolean handled = scroll.scroll(delta, 1.0D);
            refreshLayout();
            return handled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible
                    && mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= getY()
                    && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
            }
        }

        private void select(int index, boolean notify) {
            if (index < 0 || index >= items.size()) return;
            ActionItem item = items.get(index);
            if (item == null || !item.active()) return;
            selectedIndex = index;
            refreshSelection();
            ensureSelectedVisible();
            if (notify && responder != null) responder.accept(index);
        }

        private void refreshSelection() {
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton rowButton = rowButtons.get(index);
                StateButton actionButton = actionButtons.get(index);
                ActionItem item = items.get(index);
                rowButton.setSelected(index == selectedIndex);
                rowButton.setError(item != null && item.error());
                rowButton.setMessage(itemLabel(item));
                rowButton.active = active && item != null && item.active();
                actionButton.setSelected(false);
                actionButton.setError(false);
                actionButton.setMessage(item == null || item.actionLabel() == null ? Component.empty() : item.actionLabel());
                actionButton.active = active && item != null && item.actionActive();
            }
            setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                    ? itemLabel(items.get(selectedIndex))
                    : Component.empty());
        }

        private void refreshRange() {
            scroll.update(items.size(), visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton rowButton = rowButtons.get(index);
                StateButton actionButton = actionButtons.get(index);
                boolean rowVisible = visible && index >= start && index < end;
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                rowButton.visible = rowVisible;
                rowButton.setX(getX());
                rowButton.setY(rowY);
                rowButton.setWidth(rowWidth());
                ActionItem item = items.get(index);
                actionButton.visible = rowVisible && item != null && item.actionLabel() != null
                        && !item.actionLabel().getString().isBlank();
                actionButton.setX(getX() + rowWidth() + ACTION_GAP);
                actionButton.setY(rowY);
                actionButton.setWidth(actionWidth);
                rowButton.active = active && rowVisible && item != null && item.active();
                actionButton.active = active && rowVisible && item != null && item.actionActive();
            }
        }

        private int rowIndexAt(double mouseX, double mouseY, boolean action) {
            if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(items.size(), start + visibleRows() + 1);
            for (int index = start; index < end; index++) {
                StateButton button = action ? actionButtons.get(index) : rowButtons.get(index);
                if (!button.visible) continue;
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
            }
            return -1;
        }

        private int visibleRows() {
            return Math.max(1, getHeight() / ROW_PITCH);
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int rowWidth() {
            return Math.max(1, contentWidth() - actionWidth - ACTION_GAP);
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private static Component itemLabel(ActionItem item) {
            if (item == null) return Component.empty();
            Component primary = item.label() == null ? Component.empty() : item.label();
            Component secondary = item.secondaryLabel();
            if (secondary == null || secondary.getString().isBlank()) return primary;
            return primary.copy().append("   ").append(secondary);
        }
    }

    private static final class ScrollableToggleActionListWidget extends AbstractWidget implements ScrollableToggleActionList {
        private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
        private static final int ROW_PITCH = ROW_HEIGHT + 5;
        private static final int ROW_TOP_PADDING = 2;
        private static final int CONTROL_GAP = 4;
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final Consumer<Integer> responder;
        private final BiConsumer<Integer, Boolean> toggleResponder;
        private final Consumer<Integer> actionResponder;
        private final int toggleWidth;
        private final int actionWidth;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private final List<StateButton> rowButtons = new ArrayList<>();
        private final List<ToggleButton> toggleButtons = new ArrayList<>();
        private final List<StateButton> actionButtons = new ArrayList<>();
        private List<ToggleActionItem> items = List.of();
        private int selectedIndex = -1;
        private int hoveredIndex = -1;
        private int hoveredControl;
        private int pendingInitialScrollOffset;

        private ScrollableToggleActionListWidget(
                int x,
                int y,
                int width,
                int height,
                List<? extends ToggleActionItem> items,
                int selectedIndex,
                int initialScrollOffset,
                int toggleWidth,
                int actionWidth,
                Consumer<Integer> responder,
                BiConsumer<Integer, Boolean> toggleResponder,
                Consumer<Integer> actionResponder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.responder = responder;
            this.toggleResponder = toggleResponder;
            this.actionResponder = actionResponder;
            this.toggleWidth = Math.max(1, toggleWidth);
            this.actionWidth = Math.max(1, actionWidth);
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
            setSelectedIndex(selectedIndex);
        }

        @Override
        public void setItems(List<? extends ToggleActionItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            rowButtons.clear();
            toggleButtons.clear();
            actionButtons.clear();
            for (int index = 0; index < items.size(); index++) {
                ToggleActionItem item = items.get(index);
                int rowIndex = index;
                StateButton rowButton = new StateButton(
                        FACTORY_ACCESS, getX(), getY(), rowWidth(), ROW_HEIGHT, itemLabel(item),
                        ignored -> select(rowIndex, true)
                );
                rowButtons.add(rowButton);

                ToggleButton toggleButton = new ToggleButton(
                        FACTORY_ACCESS, getX(), getY(), this.toggleWidth, ROW_HEIGHT,
                        item != null && item.toggleValue(),
                        item == null ? Component.empty() : safe(item.toggleOnLabel()),
                        item == null ? Component.empty() : safe(item.toggleOffLabel()),
                        null,
                        value -> setToggleValueInternal(rowIndex, value, true)
                );
                toggleButtons.add(toggleButton);

                StateButton actionButton = new StateButton(
                        FACTORY_ACCESS, getX(), getY(), this.actionWidth, ROW_HEIGHT,
                        item == null ? Component.empty() : safe(item.actionLabel()),
                        ignored -> {
                            ToggleActionItem current = rowIndex < items.size() ? items.get(rowIndex) : null;
                            if (current == null || !current.actionActive()) return;
                            if (actionResponder != null) actionResponder.accept(rowIndex);
                        }
                );
                actionButtons.add(actionButton);
            }
            if (items.isEmpty()) selectedIndex = -1;
            else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            refreshRange();
            refreshState();
            refreshLayout();
        }

        @Override
        public List<ToggleActionItem> items() {
            return List.copyOf(items);
        }

        @Override
        public int selectedIndex() {
            return selectedIndex;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (items.isEmpty() || index < 0) {
                selectedIndex = -1;
                refreshState();
                return;
            }
            select(Math.min(items.size() - 1, index), false);
        }

        @Override
        public boolean toggleValue(int index) {
            requireIndex(index);
            ToggleActionItem item = items.get(index);
            return item != null && item.toggleValue();
        }

        @Override
        public void setToggleValue(int index, boolean value) {
            setToggleValueInternal(index, value, false);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
            refreshLayout();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public void ensureSelectedVisible() {
            if (selectedIndex < 0 || selectedIndex >= items.size()) return;
            int visible = visibleRows();
            int offset = scroll.offset();
            if (selectedIndex < offset) setScrollOffset(selectedIndex);
            else if (selectedIndex >= offset + visible) setScrollOffset(selectedIndex - visible + 1);
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            return rowIndexAt(mouseX, mouseY, 0);
        }

        @Override
        public int toggleAt(double mouseX, double mouseY) {
            return rowIndexAt(mouseX, mouseY, 1);
        }

        @Override
        public int actionAt(double mouseX, double mouseY) {
            return rowIndexAt(mouseX, mouseY, 2);
        }

        @Override
        public Component hoveredTooltip() {
            if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
            ToggleActionItem item = items.get(hoveredIndex);
            if (item == null) return null;
            if (hoveredControl == 1) return item.toggleTooltip();
            if (hoveredControl == 2) return item.actionTooltip();
            return item.tooltip();
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            refreshLayout();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int start = scroll.smoothIndexOffset();
                    int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                    for (int index = start; index < end; index++) {
                        StateButton rowButton = rowButtons.get(index);
                        if (!rowButton.visible) continue;
                        int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                                ? mouseX : Integer.MIN_VALUE;
                        rowButton.render(graphics, clippedMouseX, mouseY, partialTick);
                        toggleButtons.get(index).render(graphics, clippedMouseX, mouseY, partialTick);
                        actionButtons.get(index).render(graphics, clippedMouseX, mouseY, partialTick);
                        ToggleActionItem item = items.get(index);
                        if (item != null && item.marked()) {
                            int markerX = rowButton.getX() + rowButton.getWidth() - 8;
                            int markerY = rowButton.getY() + Math.max(2, (rowButton.getHeight() - 4) / 2);
                            graphics.fill(markerX, markerY, markerX + 4, markerY + 4, 0xFF00C853);
                        }
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(graphics, mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH,
                            getHeight(), MIN_THUMB_HEIGHT);
                }
            } finally {
                graphics.pose().popPose();
            }
            int hit = toggleAt(mouseX, mouseY);
            if (hit >= 0) {
                hoveredIndex = hit;
                hoveredControl = 1;
            } else if ((hit = actionAt(mouseX, mouseY)) >= 0) {
                hoveredIndex = hit;
                hoveredControl = 2;
            } else {
                hoveredIndex = itemAt(mouseX, mouseY);
                hoveredControl = 0;
            }
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH,
                    getHeight(), MIN_THUMB_HEIGHT, 2)) return true;
            int index = toggleAt(mouseX, mouseY);
            if (index >= 0) {
                ToggleButton toggleButton = toggleButtons.get(index);
                return toggleButton.active && toggleButton.mouseClicked(mouseX, mouseY, button);
            }
            index = actionAt(mouseX, mouseY);
            if (index >= 0) {
                StateButton actionButton = actionButtons.get(index);
                return actionButton.active && actionButton.mouseClicked(mouseX, mouseY, button);
            }
            index = itemAt(mouseX, mouseY);
            return index >= 0 && rowButtons.get(index).active && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            boolean handled = scroll.scroll(delta, 1.0D);
            refreshLayout();
            return handled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible && mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= getY() && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
            }
        }

        private void select(int index, boolean notify) {
            if (index < 0 || index >= items.size()) return;
            ToggleActionItem item = items.get(index);
            if (item == null || !item.active()) return;
            selectedIndex = index;
            refreshState();
            ensureSelectedVisible();
            if (notify && responder != null) responder.accept(index);
        }

        private void setToggleValueInternal(int index, boolean value, boolean notify) {
            requireIndex(index);
            ToggleActionItem item = items.get(index);
            if (item == null) return;
            List<ToggleActionItem> next = new ArrayList<>(items);
            next.set(index, withToggleValue(item, value));
            items = List.copyOf(next);
            refreshState();
            if (notify && toggleResponder != null) toggleResponder.accept(index, value);
        }

        private void refreshState() {
            for (int index = 0; index < rowButtons.size(); index++) {
                ToggleActionItem item = items.get(index);
                StateButton rowButton = rowButtons.get(index);
                ToggleButton toggleButton = toggleButtons.get(index);
                StateButton actionButton = actionButtons.get(index);
                rowButton.setSelected(index == selectedIndex);
                rowButton.setError(item != null && item.error());
                rowButton.setMessage(itemLabel(item));
                rowButton.active = active && item != null && item.active();
                toggleButton.setValue(item != null && item.toggleValue());
                toggleButton.active = active && item != null && item.toggleActive();
                actionButton.setSelected(false);
                actionButton.setError(item != null && item.actionError());
                actionButton.setMessage(item == null ? Component.empty() : safe(item.actionLabel()));
                actionButton.active = active && item != null && item.actionActive();
            }
            setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                    ? itemLabel(items.get(selectedIndex)) : Component.empty());
        }

        private void refreshRange() {
            scroll.update(items.size(), visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
            for (int index = 0; index < rowButtons.size(); index++) {
                ToggleActionItem item = items.get(index);
                boolean rowVisible = visible && index >= start && index < end;
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                StateButton rowButton = rowButtons.get(index);
                ToggleButton toggleButton = toggleButtons.get(index);
                StateButton actionButton = actionButtons.get(index);
                rowButton.visible = rowVisible;
                rowButton.setX(getX());
                rowButton.setY(rowY);
                rowButton.setWidth(rowWidth());
                rowButton.active = active && rowVisible && item != null && item.active();

                toggleButton.visible = rowVisible;
                toggleButton.setX(getX() + rowWidth() + CONTROL_GAP);
                toggleButton.setY(rowY);
                toggleButton.setWidth(toggleWidth);
                toggleButton.active = active && rowVisible && item != null && item.toggleActive();

                actionButton.visible = rowVisible && item != null && item.actionLabel() != null
                        && !item.actionLabel().getString().isBlank();
                actionButton.setX(getX() + rowWidth() + CONTROL_GAP + toggleWidth + CONTROL_GAP);
                actionButton.setY(rowY);
                actionButton.setWidth(actionWidth);
                actionButton.active = active && rowVisible && item != null && item.actionActive();
            }
        }

        private int rowIndexAt(double mouseX, double mouseY, int control) {
            if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(items.size(), start + visibleRows() + 1);
            for (int index = start; index < end; index++) {
                AbstractWidget widget = control == 1 ? toggleButtons.get(index)
                        : control == 2 ? actionButtons.get(index) : rowButtons.get(index);
                if (!widget.visible) continue;
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
            }
            return -1;
        }

        private int visibleRows() {
            return Math.max(1, getHeight() / ROW_PITCH);
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int rowWidth() {
            return Math.max(1, contentWidth() - toggleWidth - actionWidth - CONTROL_GAP * 2);
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private void requireIndex(int index) {
            if (index < 0 || index >= items.size()) throw new IndexOutOfBoundsException(index);
        }

        private static ToggleActionItem withToggleValue(ToggleActionItem item, boolean value) {
            return new ToggleActionItem(
                    item.label(), item.secondaryLabel(), item.tooltip(), item.active(), item.marked(), item.error(), value,
                    item.toggleOnLabel(), item.toggleOffLabel(), item.toggleTooltip(), item.toggleActive(),
                    item.actionLabel(), item.actionTooltip(), item.actionActive(), item.actionError()
            );
        }

        private static Component itemLabel(ToggleActionItem item) {
            if (item == null) return Component.empty();
            Component primary = safe(item.label());
            Component secondary = item.secondaryLabel();
            if (secondary == null || secondary.getString().isBlank()) return primary;
            return primary.copy().append("   ").append(secondary);
        }

        private static Component safe(Component component) {
            return component == null ? Component.empty() : component;
        }
    }

    private static final class ScrollableMultiActionListWidget extends AbstractWidget implements ScrollableMultiActionList {
        private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
        private static final int ROW_PITCH = ROW_HEIGHT + 5;
        private static final int ROW_TOP_PADDING = 2;
        private static final int ACTION_GAP = 2;
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final Consumer<Integer> responder;
        private final BiConsumer<Integer, Integer> actionResponder;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private final List<StateButton> rowButtons = new ArrayList<>();
        private final List<List<StateButton>> actionButtons = new ArrayList<>();
        private List<MultiActionItem> items = List.of();
        private int selectedIndex = -1;
        private int hoveredIndex = -1;
        private int hoveredActionIndex = -1;
        private int pendingInitialScrollOffset;

        private ScrollableMultiActionListWidget(
                int x,
                int y,
                int width,
                int height,
                List<? extends MultiActionItem> items,
                int selectedIndex,
                int initialScrollOffset,
                Consumer<Integer> responder,
                BiConsumer<Integer, Integer> actionResponder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.responder = responder;
            this.actionResponder = actionResponder;
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
            setSelectedIndex(selectedIndex);
        }

        @Override
        public void setItems(List<? extends MultiActionItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            rowButtons.clear();
            actionButtons.clear();
            for (int rowIndex = 0; rowIndex < items.size(); rowIndex++) {
                MultiActionItem item = items.get(rowIndex);
                int currentRow = rowIndex;
                StateButton rowButton = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        rowWidth(item),
                        ROW_HEIGHT,
                        itemLabel(item),
                        ignored -> select(currentRow, true)
                );
                rowButton.active = item != null && item.active();
                rowButton.setError(item != null && item.error());
                rowButtons.add(rowButton);

                List<StateButton> rowActions = new ArrayList<>();
                List<RowAction> actions = actions(item);
                for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
                    RowAction action = actions.get(actionIndex);
                    int currentAction = actionIndex;
                    StateButton actionButton = new StateButton(
                            FACTORY_ACCESS,
                            getX(),
                            getY(),
                            Math.max(1, action.width()),
                            ROW_HEIGHT,
                            action.label() == null ? Component.empty() : action.label(),
                            ignored -> {
                                if (!action.active()) return;
                                if (actionResponder != null) actionResponder.accept(currentRow, currentAction);
                            }
                    );
                    actionButton.active = action.active();
                    actionButton.setError(action.error());
                    rowActions.add(actionButton);
                }
                actionButtons.add(rowActions);
            }
            if (items.isEmpty()) selectedIndex = -1;
            else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            refreshRange();
            refreshSelection();
            refreshLayout();
        }

        @Override
        public List<MultiActionItem> items() {
            return List.copyOf(items);
        }

        @Override
        public int selectedIndex() {
            return selectedIndex;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (items.isEmpty() || index < 0) {
                selectedIndex = -1;
                refreshSelection();
                return;
            }
            select(Math.min(items.size() - 1, index), false);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
            refreshLayout();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public void ensureSelectedVisible() {
            if (selectedIndex < 0 || selectedIndex >= items.size()) return;
            int visible = visibleRows();
            int offset = scroll.offset();
            if (selectedIndex < offset) setScrollOffset(selectedIndex);
            else if (selectedIndex >= offset + visible) setScrollOffset(selectedIndex - visible + 1);
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(items.size(), start + visibleRows() + 1);
            for (int index = start; index < end; index++) {
                StateButton button = rowButtons.get(index);
                if (!button.visible) continue;
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
            }
            return -1;
        }

        @Override
        public ActionHit actionAt(double mouseX, double mouseY) {
            if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return null;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(items.size(), start + visibleRows() + 1);
            for (int rowIndex = start; rowIndex < end; rowIndex++) {
                int rowY = getY() + (rowIndex - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                List<StateButton> buttons = actionButtons.get(rowIndex);
                for (int actionIndex = 0; actionIndex < buttons.size(); actionIndex++) {
                    StateButton button = buttons.get(actionIndex);
                    if (!button.visible) continue;
                    if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                            && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                        return new ActionHit(rowIndex, actionIndex);
                    }
                }
            }
            return null;
        }

        @Override
        public Component hoveredTooltip() {
            if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
            MultiActionItem item = items.get(hoveredIndex);
            if (item == null) return null;
            if (hoveredActionIndex >= 0) {
                List<RowAction> actions = actions(item);
                if (hoveredActionIndex >= actions.size()) return null;
                return actions.get(hoveredActionIndex).tooltip();
            }
            return item.tooltip();
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            refreshLayout();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int start = scroll.smoothIndexOffset();
                    int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                    for (int index = start; index < end; index++) {
                        StateButton rowButton = rowButtons.get(index);
                        if (!rowButton.visible) continue;
                        int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                                ? mouseX
                                : Integer.MIN_VALUE;
                        rowButton.render(graphics, clippedMouseX, mouseY, partialTick);
                        for (StateButton actionButton : actionButtons.get(index)) {
                            if (actionButton.visible) actionButton.render(graphics, clippedMouseX, mouseY, partialTick);
                        }
                        MultiActionItem item = items.get(index);
                        if (item != null && item.marked()) {
                            int markerX = rowButton.getX() + rowButton.getWidth() - 8;
                            int markerY = rowButton.getY() + Math.max(2, (rowButton.getHeight() - 4) / 2);
                            graphics.fill(markerX, markerY, markerX + 4, markerY + 4, 0xFF00C853);
                        }
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(
                            graphics,
                            mouseX,
                            mouseY,
                            scrollbarX(),
                            getY(),
                            SCROLLBAR_WIDTH,
                            getHeight(),
                            MIN_THUMB_HEIGHT
                    );
                }
            } finally {
                graphics.pose().popPose();
            }
            ActionHit hit = actionAt(mouseX, mouseY);
            if (hit != null) {
                hoveredIndex = hit.rowIndex();
                hoveredActionIndex = hit.actionIndex();
            } else {
                hoveredIndex = itemAt(mouseX, mouseY);
                hoveredActionIndex = -1;
            }
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(
                    mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT, 2
            )) return true;
            ActionHit hit = actionAt(mouseX, mouseY);
            if (hit != null) {
                StateButton actionButton = actionButtons.get(hit.rowIndex()).get(hit.actionIndex());
                return actionButton.active && actionButton.mouseClicked(mouseX, mouseY, button);
            }
            int index = itemAt(mouseX, mouseY);
            return index >= 0 && rowButtons.get(index).active && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            boolean handled = scroll.scroll(delta, 1.0D);
            refreshLayout();
            return handled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible
                    && mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= getY()
                    && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
            }
        }

        private void select(int index, boolean notify) {
            if (index < 0 || index >= items.size()) return;
            MultiActionItem item = items.get(index);
            if (item == null || !item.active()) return;
            selectedIndex = index;
            refreshSelection();
            ensureSelectedVisible();
            if (notify && responder != null) responder.accept(index);
        }

        private void refreshSelection() {
            for (int index = 0; index < rowButtons.size(); index++) {
                MultiActionItem item = items.get(index);
                StateButton rowButton = rowButtons.get(index);
                rowButton.setSelected(index == selectedIndex);
                rowButton.setError(item != null && item.error());
                rowButton.setMessage(itemLabel(item));
                rowButton.active = active && item != null && item.active();

                List<RowAction> actions = actions(item);
                List<StateButton> buttons = actionButtons.get(index);
                for (int actionIndex = 0; actionIndex < buttons.size(); actionIndex++) {
                    RowAction action = actions.get(actionIndex);
                    StateButton button = buttons.get(actionIndex);
                    button.setSelected(false);
                    button.setError(action.error());
                    button.setMessage(action.label() == null ? Component.empty() : action.label());
                    button.active = active && action.active();
                }
            }
            setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                    ? itemLabel(items.get(selectedIndex))
                    : Component.empty());
        }

        private void refreshRange() {
            scroll.update(items.size(), visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
            for (int index = 0; index < rowButtons.size(); index++) {
                MultiActionItem item = items.get(index);
                StateButton rowButton = rowButtons.get(index);
                boolean rowVisible = visible && index >= start && index < end;
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                rowButton.visible = rowVisible;
                rowButton.setX(getX());
                rowButton.setY(rowY);
                rowButton.setWidth(rowWidth(item));
                rowButton.active = active && rowVisible && item != null && item.active();

                int actionX = getX() + rowButton.getWidth() + ACTION_GAP;
                List<RowAction> actions = actions(item);
                List<StateButton> buttons = actionButtons.get(index);
                for (int actionIndex = 0; actionIndex < buttons.size(); actionIndex++) {
                    RowAction action = actions.get(actionIndex);
                    StateButton button = buttons.get(actionIndex);
                    boolean hasLabel = action.label() != null && !action.label().getString().isBlank();
                    button.visible = rowVisible && hasLabel;
                    button.setX(actionX);
                    button.setY(rowY);
                    button.setWidth(Math.max(1, action.width()));
                    button.active = active && rowVisible && action.active();
                    if (hasLabel) actionX += Math.max(1, action.width()) + ACTION_GAP;
                }
            }
        }

        private int visibleRows() {
            return Math.max(1, getHeight() / ROW_PITCH);
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int rowWidth(MultiActionItem item) {
            int actionSpace = 0;
            List<RowAction> actions = actions(item);
            for (RowAction action : actions) {
                if (action.label() == null || action.label().getString().isBlank()) continue;
                actionSpace += Math.max(1, action.width());
                actionSpace += ACTION_GAP;
            }
            return Math.max(1, contentWidth() - actionSpace);
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private static List<RowAction> actions(MultiActionItem item) {
            return item == null || item.actions() == null ? List.of() : item.actions();
        }

        private static Component itemLabel(MultiActionItem item) {
            if (item == null) return Component.empty();
            Component primary = item.label() == null ? Component.empty() : item.label();
            Component secondary = item.secondaryLabel();
            if (secondary == null || secondary.getString().isBlank()) return primary;
            return primary.copy().append("   ").append(secondary);
        }
    }

    private static final class ScrollableItemActionListWidget extends AbstractWidget implements ScrollableItemActionList {
        private static final int ROW_HEIGHT = 26;
        private static final int ROW_PITCH = ROW_HEIGHT + 2;
        private static final int ROW_TOP_PADDING = 1;
        private static final int ITEM_SLOT_SIZE = 18;
        private static final int ITEM_LEFT_PADDING = 4;
        private static final int TEXT_LEFT_PADDING = 26;
        private static final int ACTION_GAP = 4;
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final Font font;
        private final Consumer<Integer> responder;
        private final Consumer<Integer> actionResponder;
        private final int actionWidth;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private final List<StateButton> rowButtons = new ArrayList<>();
        private final List<StateButton> actionButtons = new ArrayList<>();
        private List<ItemActionItem> items = List.of();
        private int selectedIndex = -1;
        private int hoveredIndex = -1;
        private boolean hoveredAction;
        private ItemStack hoveredStack = ItemStack.EMPTY;
        private int pendingInitialScrollOffset;

        private ScrollableItemActionListWidget(
                Font font,
                int x,
                int y,
                int width,
                int height,
                List<? extends ItemActionItem> items,
                int selectedIndex,
                int initialScrollOffset,
                int actionWidth,
                Consumer<Integer> responder,
                Consumer<Integer> actionResponder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.font = font;
            this.responder = responder;
            this.actionResponder = actionResponder;
            this.actionWidth = Math.max(1, actionWidth);
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
            setSelectedIndex(selectedIndex);
        }

        @Override
        public void setItems(List<? extends ItemActionItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            rowButtons.clear();
            actionButtons.clear();
            for (int index = 0; index < items.size(); index++) {
                ItemActionItem item = items.get(index);
                int rowIndex = index;
                StateButton rowButton = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        rowWidth(),
                        ROW_HEIGHT,
                        Component.empty(),
                        ignored -> select(rowIndex, true)
                );
                rowButton.active = item != null && item.active();
                rowButton.setError(item != null && item.error());
                rowButtons.add(rowButton);

                StateButton actionButton = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        this.actionWidth,
                        KineticScreen.STANDARD_CONTROL_HEIGHT,
                        item == null || item.actionLabel() == null ? Component.empty() : item.actionLabel(),
                        ignored -> {
                            ItemActionItem current = rowIndex < items.size() ? items.get(rowIndex) : null;
                            if (current == null || !current.actionActive()) return;
                            if (actionResponder != null) actionResponder.accept(rowIndex);
                        }
                );
                actionButton.active = item != null && item.actionActive();
                actionButtons.add(actionButton);
            }
            if (items.isEmpty()) selectedIndex = -1;
            else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            refreshRange();
            refreshSelection();
            refreshLayout();
        }

        @Override
        public List<ItemActionItem> items() {
            return List.copyOf(items);
        }

        @Override
        public int selectedIndex() {
            return selectedIndex;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (items.isEmpty() || index < 0) {
                selectedIndex = -1;
                refreshSelection();
                return;
            }
            select(Math.min(items.size() - 1, index), false);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
            refreshLayout();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public void ensureSelectedVisible() {
            if (selectedIndex < 0 || selectedIndex >= items.size()) return;
            int visible = visibleRows();
            int offset = scroll.offset();
            if (selectedIndex < offset) {
                setScrollOffset(selectedIndex);
            } else if (selectedIndex >= offset + visible) {
                setScrollOffset(selectedIndex - visible + 1);
            }
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            return rowIndexAt(mouseX, mouseY, false);
        }

        @Override
        public int actionAt(double mouseX, double mouseY) {
            return rowIndexAt(mouseX, mouseY, true);
        }

        @Override
        public ItemStack stackAt(double mouseX, double mouseY) {
            int index = itemAt(mouseX, mouseY);
            if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
            StateButton button = rowButtons.get(index);
            int slotX = button.getX() + ITEM_LEFT_PADDING;
            int slotY = button.getY() + Math.max(0, (ROW_HEIGHT - ITEM_SLOT_SIZE) / 2);
            if (!GuiTheme.hovering(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) return ItemStack.EMPTY;
            return safeStack(items.get(index));
        }

        @Override
        public ItemStack hoveredStack() {
            return hoveredStack;
        }

        @Override
        public Component hoveredTooltip() {
            if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
            ItemActionItem item = items.get(hoveredIndex);
            if (item == null) return null;
            return hoveredAction ? item.actionTooltip() : item.tooltip();
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            refreshLayout();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int start = scroll.smoothIndexOffset();
                    int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                    for (int index = start; index < end; index++) {
                        StateButton rowButton = rowButtons.get(index);
                        StateButton actionButton = actionButtons.get(index);
                        if (!rowButton.visible) continue;
                        int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                                ? mouseX
                                : Integer.MIN_VALUE;
                        rowButton.render(graphics, clippedMouseX, mouseY, partialTick);
                        renderRowContent(graphics, rowButton, items.get(index), mouseX, mouseY);
                        if (actionButton.visible) actionButton.render(graphics, clippedMouseX, mouseY, partialTick);
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(
                            graphics,
                            mouseX,
                            mouseY,
                            scrollbarX(),
                            getY(),
                            SCROLLBAR_WIDTH,
                            getHeight(),
                            MIN_THUMB_HEIGHT
                    );
                }
            } finally {
                graphics.pose().popPose();
            }
            int actionIndex = actionAt(mouseX, mouseY);
            if (actionIndex >= 0) {
                hoveredIndex = actionIndex;
                hoveredAction = true;
                hoveredStack = ItemStack.EMPTY;
            } else {
                hoveredIndex = itemAt(mouseX, mouseY);
                hoveredAction = false;
                hoveredStack = stackAt(mouseX, mouseY);
            }
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        private void renderRowContent(
                GuiGraphics graphics,
                StateButton button,
                ItemActionItem item,
                int mouseX,
                int mouseY
        ) {
            if (item == null) return;
            int slotX = button.getX() + ITEM_LEFT_PADDING;
            int slotY = button.getY() + Math.max(0, (ROW_HEIGHT - ITEM_SLOT_SIZE) / 2);
            boolean slotHovered = GuiTheme.hovering(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
            ItemStack stack = safeStack(item);
            GuiTheme.itemSlot(graphics, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, 4, false, slotHovered, item.error());
            GuiTheme.item(graphics, font, stack, slotX, slotY, ITEM_SLOT_SIZE, 1.0F, false);

            int textX = button.getX() + TEXT_LEFT_PADDING;
            int textWidth = Math.max(1, button.getWidth() - TEXT_LEFT_PADDING - 10);
            KineticText.drawScrollingLeft(
                    graphics,
                    font,
                    itemLabel(item),
                    textX,
                    button.getY() + Math.max(1, (ROW_HEIGHT - font.lineHeight) / 2),
                    textWidth,
                    GuiTheme.current().text(),
                    true
            );
            if (item.marked()) {
                int markerX = button.getX() + button.getWidth() - 8;
                int markerY = button.getY() + Math.max(2, (button.getHeight() - 4) / 2);
                graphics.fill(markerX, markerY, markerX + 4, markerY + 4, 0xFF00C853);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(
                    mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT, 2
            )) return true;
            int actionIndex = actionAt(mouseX, mouseY);
            if (actionIndex >= 0) {
                StateButton actionButton = actionButtons.get(actionIndex);
                return actionButton.active && actionButton.mouseClicked(mouseX, mouseY, button);
            }
            int index = itemAt(mouseX, mouseY);
            return index >= 0 && rowButtons.get(index).active && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            boolean handled = scroll.scroll(delta, 1.0D);
            refreshLayout();
            return handled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible
                    && mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= getY()
                    && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
            }
        }

        private void select(int index, boolean notify) {
            if (index < 0 || index >= items.size()) return;
            ItemActionItem item = items.get(index);
            if (item == null || !item.active()) return;
            selectedIndex = index;
            refreshSelection();
            ensureSelectedVisible();
            if (notify && responder != null) responder.accept(index);
        }

        private void refreshSelection() {
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton rowButton = rowButtons.get(index);
                StateButton actionButton = actionButtons.get(index);
                ItemActionItem item = items.get(index);
                rowButton.setSelected(index == selectedIndex);
                rowButton.setError(item != null && item.error());
                rowButton.active = active && item != null && item.active();
                actionButton.setSelected(false);
                actionButton.setError(false);
                actionButton.setMessage(item == null || item.actionLabel() == null ? Component.empty() : item.actionLabel());
                actionButton.active = active && item != null && item.actionActive();
            }
            setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                    ? itemLabel(items.get(selectedIndex))
                    : Component.empty());
        }

        private void refreshRange() {
            scroll.update(items.size(), visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton rowButton = rowButtons.get(index);
                StateButton actionButton = actionButtons.get(index);
                ItemActionItem item = items.get(index);
                boolean rowVisible = visible && index >= start && index < end;
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                rowButton.visible = rowVisible;
                rowButton.setX(getX());
                rowButton.setY(rowY);
                rowButton.setWidth(rowWidth());
                actionButton.visible = rowVisible && item != null && item.actionLabel() != null
                        && !item.actionLabel().getString().isBlank();
                actionButton.setX(getX() + rowWidth() + ACTION_GAP);
                actionButton.setY(rowY + Math.max(0, (ROW_HEIGHT - KineticScreen.STANDARD_CONTROL_HEIGHT) / 2));
                actionButton.setWidth(actionWidth);
                rowButton.active = active && rowVisible && item != null && item.active();
                actionButton.active = active && rowVisible && item != null && item.actionActive();
            }
        }

        private int rowIndexAt(double mouseX, double mouseY, boolean action) {
            if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int start = scroll.smoothIndexOffset();
            int end = Math.min(items.size(), start + visibleRows() + 1);
            for (int index = start; index < end; index++) {
                StateButton button = action ? actionButtons.get(index) : rowButtons.get(index);
                if (!button.visible || !button.isMouseOver(mouseX, mouseY)) continue;
                return index;
            }
            return -1;
        }

        private int visibleRows() {
            return Math.max(1, getHeight() / ROW_PITCH);
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int rowWidth() {
            return Math.max(1, contentWidth() - actionWidth - ACTION_GAP);
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private static ItemStack safeStack(ItemActionItem item) {
            return item == null || item.stack() == null ? ItemStack.EMPTY : item.stack();
        }

        private static Component itemLabel(ItemActionItem item) {
            if (item == null) return Component.empty();
            Component primary = item.label() == null ? Component.empty() : item.label();
            Component secondary = item.secondaryLabel();
            if (secondary == null || secondary.getString().isBlank()) return primary;
            return primary.copy().append("   ").append(secondary);
        }
    }

    private static final class ScrollableMultiToggleListWidget extends AbstractWidget implements ScrollableMultiToggleList {
        private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
        private static final int ROW_PITCH = ROW_HEIGHT + 5;
        private static final int ROW_TOP_PADDING = 2;
        private static final int TOGGLE_GAP = 2;
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final Consumer<Integer> responder;
        private final BiConsumer<ToggleHit, Boolean> toggleResponder;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private final List<StateButton> rowButtons = new ArrayList<>();
        private final List<List<ToggleButton>> toggleButtons = new ArrayList<>();
        private List<MultiToggleItem> items = List.of();
        private int selectedIndex = -1;
        private int hoveredIndex = -1;
        private int hoveredToggleIndex = -1;
        private int pendingInitialScrollOffset;

        private ScrollableMultiToggleListWidget(
                int x,
                int y,
                int width,
                int height,
                List<? extends MultiToggleItem> items,
                int selectedIndex,
                int initialScrollOffset,
                Consumer<Integer> responder,
                BiConsumer<ToggleHit, Boolean> toggleResponder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.responder = responder;
            this.toggleResponder = toggleResponder;
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
            setSelectedIndex(selectedIndex);
        }

        @Override
        public void setItems(List<? extends MultiToggleItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            rowButtons.clear();
            toggleButtons.clear();
            for (int index = 0; index < items.size(); index++) {
                MultiToggleItem item = items.get(index);
                int rowIndex = index;
                StateButton rowButton = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        rowWidth(item),
                        ROW_HEIGHT,
                        itemLabel(item),
                        ignored -> select(rowIndex, true)
                );
                rowButtons.add(rowButton);

                List<RowToggle> toggles = toggles(item);
                List<ToggleButton> buttons = new ArrayList<>(toggles.size());
                for (int toggleIndex = 0; toggleIndex < toggles.size(); toggleIndex++) {
                    RowToggle toggle = toggles.get(toggleIndex);
                    int currentToggleIndex = toggleIndex;
                    ToggleButton button = new ToggleButton(
                            FACTORY_ACCESS,
                            getX(),
                            getY(),
                            Math.max(1, toggle.width()),
                            ROW_HEIGHT,
                            toggle.value(),
                            safe(toggle.onLabel()),
                            safe(toggle.offLabel()),
                            null,
                            value -> setToggleValueInternal(rowIndex, currentToggleIndex, value, true)
                    );
                    button.setError(toggle.error());
                    buttons.add(button);
                }
                toggleButtons.add(buttons);
            }
            if (items.isEmpty()) selectedIndex = -1;
            else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
            refreshRange();
            refreshState();
            refreshLayout();
        }

        @Override
        public List<MultiToggleItem> items() {
            return List.copyOf(items);
        }

        @Override
        public int selectedIndex() {
            return selectedIndex;
        }

        @Override
        public void setSelectedIndex(int index) {
            if (items.isEmpty() || index < 0) {
                selectedIndex = -1;
                refreshState();
                return;
            }
            select(Math.min(items.size() - 1, index), false);
        }

        @Override
        public boolean toggleValue(int rowIndex, int toggleIndex) {
            RowToggle toggle = requireToggle(rowIndex, toggleIndex);
            return toggle.value();
        }

        @Override
        public void setToggleValue(int rowIndex, int toggleIndex, boolean value) {
            setToggleValueInternal(rowIndex, toggleIndex, value, false);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
            refreshLayout();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public void ensureSelectedVisible() {
            if (selectedIndex < 0 || selectedIndex >= items.size()) return;
            int visible = visibleRows();
            int offset = scroll.offset();
            if (selectedIndex < offset) setScrollOffset(selectedIndex);
            else if (selectedIndex >= offset + visible) setScrollOffset(selectedIndex - visible + 1);
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(items.size(), start + visibleRows() + 1);
            for (int index = start; index < end; index++) {
                StateButton button = rowButtons.get(index);
                if (!button.visible) continue;
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
            }
            return -1;
        }

        @Override
        public ToggleHit toggleAt(double mouseX, double mouseY) {
            if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return null;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(items.size(), start + visibleRows() + 1);
            for (int rowIndex = start; rowIndex < end; rowIndex++) {
                int rowY = getY() + (rowIndex - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                List<ToggleButton> buttons = toggleButtons.get(rowIndex);
                for (int toggleIndex = 0; toggleIndex < buttons.size(); toggleIndex++) {
                    ToggleButton button = buttons.get(toggleIndex);
                    if (!button.visible) continue;
                    if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                            && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                        return new ToggleHit(rowIndex, toggleIndex);
                    }
                }
            }
            return null;
        }

        @Override
        public Component hoveredTooltip() {
            if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
            MultiToggleItem item = items.get(hoveredIndex);
            if (item == null) return null;
            if (hoveredToggleIndex >= 0) {
                List<RowToggle> toggles = toggles(item);
                if (hoveredToggleIndex < toggles.size()) return toggles.get(hoveredToggleIndex).tooltip();
            }
            return item.tooltip();
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            refreshLayout();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int start = scroll.smoothIndexOffset();
                    int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                    for (int index = start; index < end; index++) {
                        StateButton rowButton = rowButtons.get(index);
                        if (!rowButton.visible) continue;
                        int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                                ? mouseX : Integer.MIN_VALUE;
                        rowButton.render(graphics, clippedMouseX, mouseY, partialTick);
                        for (ToggleButton toggleButton : toggleButtons.get(index)) {
                            if (toggleButton.visible) toggleButton.render(graphics, clippedMouseX, mouseY, partialTick);
                        }
                        MultiToggleItem item = items.get(index);
                        if (item != null && item.marked()) {
                            int markerX = rowButton.getX() + rowButton.getWidth() - 8;
                            int markerY = rowButton.getY() + Math.max(2, (rowButton.getHeight() - 4) / 2);
                            graphics.fill(markerX, markerY, markerX + 4, markerY + 4, 0xFF00C853);
                        }
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(graphics, mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH,
                            getHeight(), MIN_THUMB_HEIGHT);
                }
            } finally {
                graphics.pose().popPose();
            }
            ToggleHit hit = toggleAt(mouseX, mouseY);
            if (hit != null) {
                hoveredIndex = hit.rowIndex();
                hoveredToggleIndex = hit.toggleIndex();
            } else {
                hoveredIndex = itemAt(mouseX, mouseY);
                hoveredToggleIndex = -1;
            }
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH,
                    getHeight(), MIN_THUMB_HEIGHT, 2)) return true;
            ToggleHit hit = toggleAt(mouseX, mouseY);
            if (hit != null) {
                ToggleButton toggleButton = toggleButtons.get(hit.rowIndex()).get(hit.toggleIndex());
                return toggleButton.active && toggleButton.mouseClicked(mouseX, mouseY, button);
            }
            int index = itemAt(mouseX, mouseY);
            return index >= 0 && rowButtons.get(index).active
                    && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            boolean handled = scroll.scroll(delta, 1.0D);
            refreshLayout();
            return handled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible && mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= getY() && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
            }
        }

        private void select(int index, boolean notify) {
            if (index < 0 || index >= items.size()) return;
            MultiToggleItem item = items.get(index);
            if (item == null || !item.active()) return;
            selectedIndex = index;
            refreshState();
            ensureSelectedVisible();
            if (notify && responder != null) responder.accept(index);
        }

        private void setToggleValueInternal(int rowIndex, int toggleIndex, boolean value, boolean notify) {
            RowToggle toggle = requireToggle(rowIndex, toggleIndex);
            MultiToggleItem item = items.get(rowIndex);
            List<RowToggle> nextToggles = new ArrayList<>(toggles(item));
            nextToggles.set(toggleIndex, withValue(toggle, value));
            List<MultiToggleItem> nextItems = new ArrayList<>(items);
            nextItems.set(rowIndex, new MultiToggleItem(
                    item.label(), item.secondaryLabel(), item.tooltip(), item.active(), item.marked(), item.error(),
                    List.copyOf(nextToggles)
            ));
            items = List.copyOf(nextItems);
            refreshState();
            if (notify && toggleResponder != null) toggleResponder.accept(new ToggleHit(rowIndex, toggleIndex), value);
        }

        private void refreshState() {
            for (int rowIndex = 0; rowIndex < rowButtons.size(); rowIndex++) {
                MultiToggleItem item = items.get(rowIndex);
                StateButton rowButton = rowButtons.get(rowIndex);
                rowButton.setSelected(rowIndex == selectedIndex);
                rowButton.setError(item != null && item.error());
                rowButton.setMessage(itemLabel(item));
                rowButton.active = active && item != null && item.active();

                List<RowToggle> toggles = toggles(item);
                List<ToggleButton> buttons = toggleButtons.get(rowIndex);
                for (int toggleIndex = 0; toggleIndex < buttons.size(); toggleIndex++) {
                    RowToggle toggle = toggles.get(toggleIndex);
                    ToggleButton button = buttons.get(toggleIndex);
                    button.setValue(toggle.value());
                    button.setError(toggle.error());
                    button.active = active && toggle.active();
                }
            }
            setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                    ? itemLabel(items.get(selectedIndex)) : Component.empty());
        }

        private void refreshRange() {
            scroll.update(items.size(), visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
            for (int rowIndex = 0; rowIndex < rowButtons.size(); rowIndex++) {
                MultiToggleItem item = items.get(rowIndex);
                boolean rowVisible = visible && rowIndex >= start && rowIndex < end;
                int rowY = getY() + (rowIndex - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                StateButton rowButton = rowButtons.get(rowIndex);
                rowButton.visible = rowVisible;
                rowButton.setX(getX());
                rowButton.setY(rowY);
                rowButton.setWidth(rowWidth(item));
                rowButton.active = active && rowVisible && item != null && item.active();

                int toggleX = getX() + rowButton.getWidth() + TOGGLE_GAP;
                List<RowToggle> toggles = toggles(item);
                List<ToggleButton> buttons = toggleButtons.get(rowIndex);
                for (int toggleIndex = 0; toggleIndex < buttons.size(); toggleIndex++) {
                    RowToggle toggle = toggles.get(toggleIndex);
                    ToggleButton button = buttons.get(toggleIndex);
                    button.visible = rowVisible;
                    button.setX(toggleX);
                    button.setY(rowY);
                    button.setWidth(Math.max(1, toggle.width()));
                    button.active = active && rowVisible && toggle.active();
                    toggleX += Math.max(1, toggle.width()) + TOGGLE_GAP;
                }
            }
        }

        private int visibleRows() {
            return Math.max(1, getHeight() / ROW_PITCH);
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int rowWidth(MultiToggleItem item) {
            int toggleSpace = 0;
            for (RowToggle toggle : toggles(item)) {
                toggleSpace += Math.max(1, toggle.width()) + TOGGLE_GAP;
            }
            return Math.max(1, contentWidth() - toggleSpace);
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private RowToggle requireToggle(int rowIndex, int toggleIndex) {
            if (rowIndex < 0 || rowIndex >= items.size()) throw new IndexOutOfBoundsException(rowIndex);
            List<RowToggle> toggles = toggles(items.get(rowIndex));
            if (toggleIndex < 0 || toggleIndex >= toggles.size()) throw new IndexOutOfBoundsException(toggleIndex);
            return toggles.get(toggleIndex);
        }

        private static List<RowToggle> toggles(MultiToggleItem item) {
            return item == null || item.toggles() == null ? List.of() : item.toggles();
        }

        private static RowToggle withValue(RowToggle toggle, boolean value) {
            return new RowToggle(
                    toggle.onLabel(), toggle.offLabel(), toggle.tooltip(), toggle.width(), value,
                    toggle.active(), toggle.error()
            );
        }

        private static Component itemLabel(MultiToggleItem item) {
            if (item == null) return Component.empty();
            Component primary = safe(item.label());
            Component secondary = item.secondaryLabel();
            if (secondary == null || secondary.getString().isBlank()) return primary;
            return primary.copy().append("   ").append(secondary);
        }

        private static Component safe(Component component) {
            return component == null ? Component.empty() : component;
        }
    }

    private static final class ScrollableToggleListWidget extends AbstractWidget implements ScrollableToggleList {
        private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
        private static final int ROW_PITCH = ROW_HEIGHT + 5;
        private static final int ROW_TOP_PADDING = 2;
        private static final int SCROLLBAR_GAP = 4;
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int MIN_THUMB_HEIGHT = 15;

        private final BiConsumer<Integer, Boolean> responder;
        private final int zLevel;
        private final GridScrollController scroll = new GridScrollController();
        private final List<StateButton> rowButtons = new ArrayList<>();
        private List<ToggleItem> items = List.of();
        private int hoveredIndex = -1;
        private int pendingInitialScrollOffset;

        private ScrollableToggleListWidget(
                int x,
                int y,
                int width,
                int height,
                List<? extends ToggleItem> items,
                int initialScrollOffset,
                BiConsumer<Integer, Boolean> responder,
                int zLevel
        ) {
            super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
            this.responder = responder == null ? (index, value) -> { } : responder;
            this.zLevel = zLevel;
            this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
            setItems(items);
        }

        @Override
        public void setItems(List<? extends ToggleItem> nextItems) {
            items = nextItems == null ? List.of() : List.copyOf(nextItems);
            rowButtons.clear();
            for (int index = 0; index < items.size(); index++) {
                ToggleItem item = items.get(index);
                int rowIndex = index;
                StateButton button = new StateButton(
                        FACTORY_ACCESS,
                        getX(),
                        getY(),
                        contentWidth(),
                        ROW_HEIGHT,
                        itemLabel(item),
                        ignored -> toggle(rowIndex)
                );
                rowButtons.add(button);
            }
            refreshRange();
            refreshValues();
            refreshLayout();
        }

        @Override
        public List<ToggleItem> items() {
            return List.copyOf(items);
        }

        @Override
        public boolean value(int index) {
            requireIndex(index);
            ToggleItem item = items.get(index);
            return item != null && item.value();
        }

        @Override
        public void setValue(int index, boolean value) {
            setValueInternal(index, value, false);
        }

        @Override
        public void setValues(List<Boolean> values) {
            if (values == null || values.size() != items.size()) {
                throw new IllegalArgumentException("values size must match items size");
            }
            List<ToggleItem> next = new ArrayList<>(items.size());
            for (int index = 0; index < items.size(); index++) {
                ToggleItem item = items.get(index);
                next.add(withValue(item, Boolean.TRUE.equals(values.get(index))));
            }
            items = List.copyOf(next);
            refreshValues();
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            setX(x);
            setY(y);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            refreshRange();
            refreshLayout();
        }

        @Override
        public int scrollOffset() {
            return scroll.offset();
        }

        @Override
        public void setScrollOffset(int offset) {
            scroll.setOffset(offset);
            refreshLayout();
        }

        @Override
        public int maxScrollOffset() {
            return scroll.maxOffset();
        }

        @Override
        public int itemAt(double mouseX, double mouseY) {
            if (!visible || mouseX < getX() || mouseX >= getX() + contentWidth()
                    || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            for (int index = start; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
                if (rowY >= getY() + getHeight()) break;
                if (rowY + ROW_HEIGHT <= getY()) continue;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
            }
            return -1;
        }

        @Override
        public Component hoveredTooltip() {
            return hoveredIndex >= 0 && hoveredIndex < items.size() && items.get(hoveredIndex) != null
                    ? items.get(hoveredIndex).tooltip()
                    : null;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshRange();
            refreshLayout();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zLevel);
            try {
                KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
                try {
                    int start = scroll.smoothIndexOffset();
                    int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                    for (int index = start; index < end; index++) {
                        StateButton button = rowButtons.get(index);
                        if (!button.visible) continue;
                        int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                                ? mouseX
                                : Integer.MIN_VALUE;
                        button.render(graphics, clippedMouseX, mouseY, partialTick);
                    }
                } finally {
                    KineticRenderRuntime.disableScissor(graphics);
                }
                if (scroll.canScroll()) {
                    scroll.render(
                            graphics,
                            mouseX,
                            mouseY,
                            scrollbarX(),
                            getY(),
                            SCROLLBAR_WIDTH,
                            getHeight(),
                            MIN_THUMB_HEIGHT
                    );
                }
            } finally {
                graphics.pose().popPose();
            }
            hoveredIndex = itemAt(mouseX, mouseY);
            Component tooltip = hoveredTooltip();
            KineticControlBridge.setTooltip(this, tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0) return false;
            if (scroll.beginDrag(
                    mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT, 2
            )) return true;
            int index = itemAt(mouseX, mouseY);
            return index >= 0 && rowButtons.get(index).active && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
                refreshLayout();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scroll.release(button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
            boolean handled = scroll.scroll(delta, 1.0D);
            refreshLayout();
            return handled;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return visible
                    && mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= getY()
                    && mouseY < getY() + getHeight();
        }

        @Override
        protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
            if (hoveredIndex >= 0 && hoveredIndex < items.size()) {
                output.add(NarratedElementType.TITLE, itemLabel(items.get(hoveredIndex)));
            }
        }

        private void toggle(int index) {
            requireIndex(index);
            ToggleItem item = items.get(index);
            if (item == null || !item.active()) return;
            setValueInternal(index, !item.value(), true);
        }

        private void setValueInternal(int index, boolean value, boolean notify) {
            requireIndex(index);
            ToggleItem item = items.get(index);
            if (item == null) return;
            List<ToggleItem> next = new ArrayList<>(items);
            next.set(index, withValue(item, value));
            items = List.copyOf(next);
            refreshValues();
            if (notify) responder.accept(index, value);
        }

        private void refreshValues() {
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                ToggleItem item = items.get(index);
                button.setSelected(item != null && item.value());
                button.setMessage(itemLabel(item));
                button.active = active && item != null && item.active();
            }
        }

        private void refreshRange() {
            scroll.update(items.size(), visibleRows());
            if (pendingInitialScrollOffset > 0) {
                scroll.setOffset(pendingInitialScrollOffset);
                pendingInitialScrollOffset = 0;
            }
        }

        private void refreshLayout() {
            int start = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(ROW_PITCH);
            int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
            for (int index = 0; index < rowButtons.size(); index++) {
                StateButton button = rowButtons.get(index);
                boolean rowVisible = visible && index >= start && index < end;
                button.visible = rowVisible;
                button.setX(getX());
                button.setY(getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING);
                button.setWidth(contentWidth());
                ToggleItem item = items.get(index);
                button.active = active && rowVisible && item != null && item.active();
            }
        }

        private int visibleRows() {
            return Math.max(1, getHeight() / ROW_PITCH);
        }

        private int contentWidth() {
            return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
        }

        private int scrollbarX() {
            return getX() + getWidth() - SCROLLBAR_WIDTH;
        }

        private void requireIndex(int index) {
            if (index < 0 || index >= items.size()) {
                throw new IllegalArgumentException("index out of range: " + index + " for " + items.size() + " items");
            }
        }

        private static ToggleItem withValue(ToggleItem item, boolean value) {
            return item == null ? null : new ToggleItem(item.label(), item.tooltip(), value, item.active());
        }

        private static Component itemLabel(ToggleItem item) {
            return item == null || item.label() == null ? Component.empty() : item.label();
        }
    }

}
