package dev.xyat.kineticcore.internal.client.gui.widget;

import dev.xyat.kineticcore.api.client.gui.widget.list.ActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridDensity;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemSelectionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticActionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemActionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemGrid;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemSelectionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticMultiActionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticMultiToggleList;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticSelectionList;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTabStrip;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticToggleActionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticToggleList;
import dev.xyat.kineticcore.api.client.gui.widget.list.MultiActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.MultiToggleItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.SelectionItem;
import dev.xyat.kineticcore.api.client.gui.widget.TabStripItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleHit;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleItem;

import dev.xyat.kineticcore.api.client.search.KineticSuggestion;

import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.ColorPreviewButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.ColorSwatchButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.CycleButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.HighZToggleButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.MenuButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.ItemButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticNumericFields;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticTextFields.KineticMultiLineEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.gui.widget.KineticDropdown.Option;
import dev.xyat.kineticcore.internal.client.gui.widget.slider.KineticSliders.Slider;
import dev.xyat.kineticcore.internal.client.gui.widget.render.KineticEntityPreview.EntityPreviewRenderer;
import dev.xyat.kineticcore.internal.client.gui.widget.tab.TabBar;
import dev.xyat.kineticcore.internal.client.gui.widget.tab.TabStripWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.SelectionListWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.ItemSelectionListWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.ItemGridWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.ActionListWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.ToggleActionListWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.MultiActionListWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.ItemActionListWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.MultiToggleListWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.list.ToggleListWidget;
import dev.xyat.kineticcore.internal.client.gui.widget.tab.TabBarButtons;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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
            Supplier<List<KineticSuggestion>> dictionarySupplier,
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
            Supplier<List<KineticSuggestion>> dictionarySupplier,
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
            Supplier<List<KineticSuggestion>> dictionarySupplier,
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
            Supplier<List<KineticSuggestion>> dictionarySupplier,
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
    public static TabBarButtons createTabBar(
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
    public static TabBarButtons createHighZTabBar(
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
    public static TabBarButtons createVerticalHighZTabBar(
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

    private static TabBarButtons createTabBarInternal(
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
        TabBar tabBar = new TabBar(FACTORY_ACCESS, x, y, totalWidth, labels, selectedIndex, responder, vertical, highZ, zLevel);
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
    public static KineticTabStrip createScrollableTabStrip(
            Font font,
            int x,
            int y,
            int width,
            List<? extends TabStripItem> tabs,
            int pinnedLeadingTabs,
            int selectedIndex,
            int initialScrollOffset,
            Component previousText,
            Component nextText,
            Consumer<Integer> responder
    ) {
        return new TabStripWidget(
                FACTORY_ACCESS, font, x, y, width, tabs, pinnedLeadingTabs, selectedIndex, initialScrollOffset,
                previousText, nextText, responder
        );
    }



    /** Creates a detached smooth vertical single-selection list using standard Kinetic row controls. */
    public static KineticSelectionList createScrollableSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends SelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder
    ) {
        return new SelectionListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset, responder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list rendered at the supplied Z depth. */
    public static KineticSelectionList createHighZScrollableSelectionList(
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
        return new SelectionListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates a detached smooth vertical item-backed single-selection list. */
    public static KineticItemSelectionList createScrollableItemSelectionList(
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
        return new ItemSelectionListWidget(
                FACTORY_ACCESS, font, x, y, width, height, items, selectedIndex, initialScrollOffset, responder, 0
        );
    }

    /** Creates a detached smooth vertical item-backed single-selection list rendered at the supplied Z depth. */
    public static KineticItemSelectionList createHighZScrollableItemSelectionList(
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
        return new ItemSelectionListWidget(
                FACTORY_ACCESS, font, x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates a detached smooth scrollable item-slot grid. */
    public static KineticItemGrid createScrollableItemGrid(
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
        return new ItemGridWidget(
                FACTORY_ACCESS, font, x, y, width, height, density, items, initialScrollOffset, responder, 0
        );
    }

    /** Creates a detached smooth scrollable item-slot grid rendered at the supplied Z depth. */
    public static KineticItemGrid createHighZScrollableItemGrid(
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
        return new ItemGridWidget(
                FACTORY_ACCESS, font, x, y, width, height, density, items, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates a detached smooth vertical single-selection list with one trailing row action. */
    public static KineticActionList createScrollableActionList(
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
        return new ActionListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list with one trailing row action at the supplied Z depth. */
    public static KineticActionList createHighZScrollableActionList(
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
        return new ActionListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical single-selection list with multiple trailing row actions. */
    public static KineticMultiActionList createScrollableMultiActionList(
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
        return new MultiActionListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, actionResponder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list with multiple trailing row actions at the supplied Z depth. */
    public static KineticMultiActionList createHighZScrollableMultiActionList(
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
        return new MultiActionListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, actionResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical single-selection list with one real toggle and one trailing row action. */
    public static KineticToggleActionList createScrollableToggleActionList(
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
        return new ToggleActionListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list with one real toggle and one trailing row action at the supplied Z depth. */
    public static KineticToggleActionList createHighZScrollableToggleActionList(
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
        return new ToggleActionListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical item-backed single-selection list with one trailing row action. */
    public static KineticItemActionList createScrollableItemActionList(
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
        return new ItemActionListWidget(
                FACTORY_ACCESS, font, x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, 0
        );
    }

    /** Creates a detached smooth vertical item-backed single-selection list with one trailing row action at the supplied Z depth. */
    public static KineticItemActionList createHighZScrollableItemActionList(
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
        return new ItemActionListWidget(
                FACTORY_ACCESS, font, x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical single-selection list with any number of real toggles per row. */
    public static KineticMultiToggleList createScrollableMultiToggleList(
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
        return new MultiToggleListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, toggleResponder, 0
        );
    }

    /** Creates a detached smooth vertical single-selection list with any number of real toggles per row at the supplied Z depth. */
    public static KineticMultiToggleList createHighZScrollableMultiToggleList(
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
        return new MultiToggleListWidget(
                FACTORY_ACCESS, x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, toggleResponder, zLevel
        );
    }

    /** Creates a detached smooth vertical multi-toggle list using standard Kinetic row controls. */
    public static KineticToggleList createScrollableToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder
    ) {
        return new ToggleListWidget(
                FACTORY_ACCESS, x, y, width, height, items, initialScrollOffset, responder, 0
        );
    }

    /** Creates a detached smooth vertical multi-toggle list rendered at the supplied Z depth. */
    public static KineticToggleList createHighZScrollableToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder,
            int zLevel
    ) {
        return new ToggleListWidget(
                FACTORY_ACCESS, x, y, width, height, items, initialScrollOffset, responder, zLevel
        );
    }

    /**
     * Applies the standard Minecraft tooltip to any detached widget and returns the same widget for fluent setup.
     * A {@code null} or blank component clears the tooltip.
     */
    private static <T extends AbstractWidget & InternalControl> T attachTooltip(T widget, Component tooltip) {
        if (widget == null) return null;
        KineticControlBridge.setTooltip(widget, tooltip);
        return widget;
    }
}
