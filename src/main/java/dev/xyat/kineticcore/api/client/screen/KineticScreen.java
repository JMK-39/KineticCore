package dev.xyat.kineticcore.api.client.screen;

import dev.xyat.kineticcore.api.client.layout.GuiLayout;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class KineticScreen extends Screen {
    public static final int STANDARD_CANVAS_WIDTH = 640;
    public static final int STANDARD_CANVAS_HEIGHT = 360;
    public static final int STANDARD_SAFE_MARGIN = 6;
    public static final int STANDARD_CONTROL_HEIGHT = 16;
    public static final int COMPACT_CONTROL_HEIGHT = 16;

    private int canvasWidth = STANDARD_CANVAS_WIDTH;
    private int canvasHeight = STANDARD_CANVAS_HEIGHT;
    private float canvasScale;
    private int canvasX;
    private int canvasY;
    protected boolean renderRenderablesOnly;
    private GuiLayout.SafeArea safeArea = GuiLayout.SafeArea.of(1, 1, 0);
    private GuiLayout.Metrics metrics = GuiLayout.measure(1, 1, 640, 360);
    private final GuiOverlay overlays = new GuiOverlay();
    private final Map<AbstractWidget, Component> widgetTooltips = new IdentityHashMap<>();
    private final List<ScrollViewportWidget> scrollViewportWidgets = new ArrayList<>();
    private GuiSession.DraftSession draftSession = new GuiSession.DraftSession();

    private record ScrollViewportKey(int left, int top, int right, int bottom) {
    }

    private record ScrollViewportWidget(
            AbstractWidget widget,
            int baseY,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        ScrollViewportKey key() {
            return new ScrollViewportKey(left, top, right, bottom);
        }
    }

    protected KineticScreen(Component title) {
        super(title);
        dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl.initialize();
    }

    public final int canvasWidth() {
        return canvasWidth;
    }

    public final int canvasHeight() {
        return canvasHeight;
    }

    public final float canvasScale() {
        return canvasScale;
    }

    public final int canvasX() {
        return canvasX;
    }

    public final int canvasY() {
        return canvasY;
    }

    public final GuiLayout.SafeArea safeArea() {
        return safeArea;
    }

    public final GuiLayout.Metrics layout() {
        return metrics;
    }

    public final GuiLayout.Level layoutLevel() {
        return metrics.level();
    }

    public final boolean isPortraitLayout() {
        return metrics.isPortrait();
    }

    public final boolean isUltrawideLayout() {
        return metrics.isUltrawide();
    }

    public final boolean isCompactLayout() {
        return metrics.isCompact();
    }

    protected final GuiOverlay overlays() {
        return overlays;
    }

    public final KineticWidgets.KineticEditBox addTextField(
            int x,
            int y,
            int width,
            Component message
    ) {
        return addTextField(x, y, width, message, null);
    }

    public final KineticWidgets.KineticEditBox addTextField(
            int x,
            int y,
            int width,
            Component message,
            Component tooltip
    ) {
        return addTextField(x, y, width, message, null, tooltip);
    }

    public final KineticWidgets.KineticEditBox addTextField(
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Component tooltip
    ) {
        KineticWidgets.KineticEditBox box = KineticWidgets.createTextField(
                font, x, y, width, message, placeholder, null
        );
        registerWidgetTooltip(box, tooltip);
        addRenderableWidget(box);
        return box;
    }

    public final KineticWidgets.KineticEditBox addTextField(
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Predicate<String> validator,
            Component tooltip
    ) {
        KineticWidgets.KineticEditBox box = KineticWidgets.createTextField(
                font, x, y, width, message, placeholder, validator, null
        );
        registerWidgetTooltip(box, tooltip);
        addRenderableWidget(box);
        return box;
    }

    public final MultiLineEditBox addMultiLineTextField(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Component placeholder,
            Component tooltip
    ) {
        MultiLineEditBox box = createMultiLineTextField(
                x, y, width, height, message, placeholder, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    private MultiLineEditBox createMultiLineTextField(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Component placeholder,
            Component tooltip
    ) {
        MultiLineEditBox box = KineticWidgets.createMultiLineTextField(
                font, x, y, width, height, message, placeholder, null
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.AutoCompleteBox addAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        return addAutoCompleteField(
                x, y, width, message, null, dictionarySupplier, tooltip
        );
    }

    public final KineticWidgets.AutoCompleteBox addAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        KineticWidgets.AutoCompleteBox box = KineticWidgets.createAutoCompleteField(
                font, x, y, width, message, placeholder, dictionarySupplier, null
        );
        registerWidgetTooltip(box, tooltip);
        addRenderableWidget(box);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return addIntegerAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    public final KineticWidgets.NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = createIntegerAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    private KineticWidgets.NumericAutoCompleteBox createIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return createIntegerAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    private KineticWidgets.NumericAutoCompleteBox createIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = KineticWidgets.createIntegerAutoCompleteField(
                font, x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return addLongAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    public final KineticWidgets.NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = createLongAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    private KineticWidgets.NumericAutoCompleteBox createLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return createLongAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    private KineticWidgets.NumericAutoCompleteBox createLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = KineticWidgets.createLongAutoCompleteField(
                font, x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return addDecimalAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    public final KineticWidgets.NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = createDecimalAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    private KineticWidgets.NumericAutoCompleteBox createDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return createDecimalAutoCompleteField(
                x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, null, tooltip
        );
    }

    private KineticWidgets.NumericAutoCompleteBox createDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = KineticWidgets.createDecimalAutoCompleteField(
                font, x, y, width, message, dictionarySupplier,
                allowNegative, minValue, maxValue, validator, null
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return addIntegerField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    public final KineticWidgets.NumericEditBox addIntegerField(
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
        KineticWidgets.NumericEditBox box = createIntegerField(
                x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    private KineticWidgets.NumericEditBox createIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return createIntegerField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    private KineticWidgets.NumericEditBox createIntegerField(
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
        KineticWidgets.NumericEditBox box = KineticWidgets.createIntegerField(
                font, x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return addLongField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    public final KineticWidgets.NumericEditBox addLongField(
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
        KineticWidgets.NumericEditBox box = createLongField(
                x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    private KineticWidgets.NumericEditBox createLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return createLongField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    private KineticWidgets.NumericEditBox createLongField(
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
        KineticWidgets.NumericEditBox box = KineticWidgets.createLongField(
                font, x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return addDecimalField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    public final KineticWidgets.NumericEditBox addDecimalField(
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
        KineticWidgets.NumericEditBox box = createDecimalField(
                x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    private KineticWidgets.NumericEditBox createDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return createDecimalField(x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    private KineticWidgets.NumericEditBox createDecimalField(
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
        KineticWidgets.NumericEditBox box = KineticWidgets.createDecimalField(
                font, x, y, width, message,
                allowNegative, minValue, maxValue, validator, null
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return addTabBar(x, y, totalWidth, labels, List.of(), selectedIndex, responder);
    }

    public final KineticWidgets.TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        KineticWidgets.TabBar tabBar = KineticWidgets.createTabBar(
                x, y, totalWidth, labels, selectedIndex, responder
        );
        List<? extends Component> safeTooltips = tooltips == null ? List.of() : tooltips;
        List<Button> buttons = tabBar.buttons();
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            addRenderableWidget(button);
            Component tooltip = index < safeTooltips.size() ? safeTooltips.get(index) : null;
            registerWidgetTooltip(button, tooltip);
        }
        return tabBar;
    }

    public final Button addButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        return addButton(x, y, width, STANDARD_CONTROL_HEIGHT, text, tooltip, action);
    }

    public final Button addButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Button.OnPress action
    ) {
        return addButton(x, y, width, STANDARD_CONTROL_HEIGHT, text, tooltip, action);
    }

    private Button createButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        return createButton(
                x, y, width, text, tooltip,
                action == null ? null : ignored -> action.run()
        );
    }

    private Button createButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Button.OnPress action
    ) {
        Button button = KineticWidgets.createButton(x, y, width, text, null, action);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    private Button addButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        Button.OnPress onPress = ignored -> { if (action != null) action.run(); };
        Button button = height == COMPACT_CONTROL_HEIGHT
                ? KineticWidgets.createCompactButton(x, y, width, text, null, onPress)
                : KineticWidgets.createButton(x, y, width, text, null, onPress);
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    private Button addButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            Button.OnPress action
    ) {
        Button button = height == COMPACT_CONTROL_HEIGHT
                ? KineticWidgets.createCompactButton(x, y, width, text, null, action)
                : KineticWidgets.createButton(x, y, width, text, null, action);
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final Button addCompactButton(
            int x, int y, int width, Component text, Component tooltip, Runnable action
    ) {
        return addButton(x, y, width, COMPACT_CONTROL_HEIGHT, text, tooltip, action);
    }

    public final Button addCompactButton(
            int x, int y, int width, Component text, Component tooltip, Button.OnPress action
    ) {
        return addButton(x, y, width, COMPACT_CONTROL_HEIGHT, text, tooltip, action);
    }

    public final KineticWidgets.HighZButton addHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
    ) {
        return addHighZButton(x, y, width, STANDARD_CONTROL_HEIGHT, text, tooltip, zLevel, action);
    }

    public final KineticWidgets.HighZButton addHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = createHighZButton(x, y, width, text, tooltip, zLevel, action);
        addRenderableWidget(button);
        return button;
    }

    public final KineticWidgets.HighZButton addCompactHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
    ) {
        return addHighZButton(x, y, width, COMPACT_CONTROL_HEIGHT, text, tooltip, zLevel, action);
    }

    public final KineticWidgets.HighZButton addCompactHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = KineticWidgets.createCompactHighZButton(
                x, y, width, text, null, zLevel, action
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    private KineticWidgets.HighZButton createHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = KineticWidgets.createHighZButton(
                x, y, width, text, null, zLevel, action
        );
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    private KineticWidgets.HighZButton addHighZButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
    ) {
        Button.OnPress onPress = ignored -> { if (action != null) action.run(); };
        KineticWidgets.HighZButton button = height == COMPACT_CONTROL_HEIGHT
                ? KineticWidgets.createCompactHighZButton(x, y, width, text, null, zLevel, onPress)
                : KineticWidgets.createHighZButton(x, y, width, text, null, zLevel, onPress);
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return addToggleButton(x, y, width, value, onText, offText, tooltip, ignored -> true, responder);
    }

    public final KineticWidgets.ToggleButton addToggleButton(
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
        KineticWidgets.ToggleButton button = KineticWidgets.createToggleButton(
                x, y, width, value, onText, offText, null, validator, responder
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final Button addCompactScrollableButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Runnable action,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        return addScrollableButton(
                x, y, width, COMPACT_CONTROL_HEIGHT,
                text, tooltip, action,
                viewportLeft, viewportTop, viewportRight, viewportBottom, pixelOffset
        );
    }

    public final Button addScrollableButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Runnable action,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        return addScrollableButton(
                x, y, width, STANDARD_CONTROL_HEIGHT,
                text, tooltip, action,
                viewportLeft, viewportTop, viewportRight, viewportBottom, pixelOffset
        );
    }

    private Button addScrollableButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            Runnable action,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        Button.OnPress onPress = ignored -> { if (action != null) action.run(); };
        KineticWidgets.HighZButton button = height == COMPACT_CONTROL_HEIGHT
                ? KineticWidgets.createCompactHighZButton(x, y, width, text, null, 0, onPress)
                : KineticWidgets.createHighZButton(x, y, width, text, null, 0, onPress);
        addScrollableWidget(
                button,
                viewportLeft,
                viewportTop,
                viewportRight,
                viewportBottom,
                pixelOffset
        );
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.ColorSwatchButton addColorSwatchButton(
            int x, int y, int rgb, Component tooltip, Runnable action
    ) {
        KineticWidgets.ColorSwatchButton button = KineticWidgets.createColorSwatchButton(
                x, y, rgb, null, action
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.ColorPreviewButton addColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        KineticWidgets.ColorPreviewButton button = KineticWidgets.createColorPreviewButton(
                x, y, width, color, text, null, action
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final void closeContextMenu() {
        overlays.closeMenu();
    }

    public final <T extends AbstractWidget> T registerWidgetTooltip(T widget, Component tooltip) {
        if (widget == null) return null;
        if (tooltip == null || tooltip.getString().isBlank()) widgetTooltips.remove(widget);
        else widgetTooltips.put(widget, tooltip);
        return widget;
    }

    public final <T extends AbstractWidget> T addControl(T widget, Component tooltip) {
        if (widget == null) return null;
        addRenderableWidget(widget);
        if (tooltip != null && !tooltip.getString().isBlank()) {
            registerWidgetTooltip(widget, tooltip);
        }
        return widget;
    }

    public final void removeControl(AbstractWidget widget) {
        if (widget == null) return;
        widgetTooltips.remove(widget);
        scrollViewportWidgets.removeIf(entry -> entry.widget() == widget);
        removeWidget(widget);
    }

    private void requestWidgetTooltip(double mouseX, double mouseY) {
        for (Map.Entry<AbstractWidget, Component> entry : widgetTooltips.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget == null || !widget.visible) continue;
            if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                    && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) {
                overlays.tooltip(entry.getValue(), 320);
                return;
            }
        }
    }

    public final <T extends ObjectSelectionList<?>> T addEventListWidget(T list) {
        addWidget(list);
        return list;
    }

    public final void resetScrollableWidgets() {
        scrollViewportWidgets.clear();
    }

    public final <T extends AbstractWidget> T addScrollableWidget(
            T widget,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        addRenderableWidget(widget);
        return bindScrollableWidget(widget, left, top, right, bottom, pixelOffset);
    }

    /**
     * Registers a widget that was already created through a KineticScreen API factory as belonging
     * to a scrolling viewport. Child screens must use this instead of re-adding the widget.
     */
    public final <T extends AbstractWidget> T bindScrollableWidget(
            T widget,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        if (widget == null) return null;
        scrollViewportWidgets.add(new ScrollViewportWidget(
                widget,
                widget.getY(),
                left,
                top,
                right,
                bottom,
                pixelOffset
        ));
        return widget;
    }

    /**
     * Makes an arbitrary widget participate in a Kinetic scrolling viewport. Widgets created by an
     * API factory are only bound; custom widgets that are not yet attached to the screen are added
     * once before being bound.
     */
    public final <T extends AbstractWidget> T attachScrollableWidget(
            T widget,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        if (widget == null) return null;
        if (!children().contains(widget)) addRenderableWidget(widget);
        return bindScrollableWidget(widget, left, top, right, bottom, pixelOffset);
    }

    private void updateScrollableWidgetPositions() {
        Map<ScrollViewportKey, Double> offsets = new HashMap<>();
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            double offset = offsets.computeIfAbsent(
                    viewportWidget.key(),
                    ignored -> Math.max(0D, viewportWidget.pixelOffset().getAsDouble())
            );
            viewportWidget.widget().setY(viewportWidget.baseY() - (int) Math.round(offset));
        }
    }

    private void renderScrollableWidgets(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            AbstractWidget widget = viewportWidget.widget();
            if (!widget.visible) continue;
            int widgetTop = widget.getY();
            int widgetBottom = widgetTop + widget.getHeight();
            if (widgetBottom <= viewportWidget.top() || widgetTop >= viewportWidget.bottom()) continue;

            enableUiScissor(
                    graphics,
                    viewportWidget.left(),
                    viewportWidget.top(),
                    viewportWidget.right(),
                    viewportWidget.bottom()
            );
            try {
                widget.render(graphics, mouseX, mouseY, partialTick);
            } finally {
                disableUiScissor(graphics);
            }
        }
    }

    private List<AbstractWidget> hideScrollableWidgetsOutsideViewport(double mouseX, double mouseY) {
        List<AbstractWidget> hidden = new ArrayList<>();
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            AbstractWidget widget = viewportWidget.widget();
            if (!widget.visible) continue;
            boolean insideViewport = mouseX >= viewportWidget.left()
                    && mouseX < viewportWidget.right()
                    && mouseY >= viewportWidget.top()
                    && mouseY < viewportWidget.bottom();
            if (!insideViewport) {
                widget.visible = false;
                hidden.add(widget);
            }
        }
        return hidden;
    }

    private void restoreScrollableWidgetVisibility(List<AbstractWidget> widgets) {
        for (AbstractWidget widget : widgets) {
            widget.visible = true;
        }
    }


    protected final <T> void configureDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, false);
    }

    protected final <T> void configureStandaloneDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, true);
    }







    protected final void commitDraft() {
        draftSession.commitBaseline(this);
    }

    protected final void discardDraft() {
        draftSession.discardToBaseline();
    }

    protected final boolean hasUnsavedEdits() {
        return draftSession.isDirty();
    }


    public final void showTooltip(Component component) {
        overlays.tooltip(component);
    }

    public final void showTooltip(List<? extends Component> lines) {
        overlays.tooltip(lines);
    }

    public final void showTooltip(Component component, int maxWidth) {
        overlays.tooltip(component, maxWidth);
    }

    public final void showTooltip(List<? extends Component> lines, int maxWidth) {
        overlays.tooltip(lines, maxWidth);
    }

    public final void showFormattedTooltip(List<FormattedCharSequence> lines) {
        overlays.formattedTooltip(lines);
    }

    public final void showItemTooltip(ItemStack stack) {
        overlays.itemTooltip(stack);
    }

    public final void focusControl(GuiEventListener control) {
        if (control == null) {
            clearControlFocus();
            return;
        }
        GuiEventListener current = getFocused();
        if (current != null && current != control) current.setFocused(false);
        setFocused(control);
        control.setFocused(true);
    }

    public final void blurControl(GuiEventListener control) {
        if (control == null) return;
        control.setFocused(false);
        if (getFocused() == control) setFocused(null);
    }

    public final void clearControlFocus() {
        GuiEventListener current = getFocused();
        if (current != null) current.setFocused(false);
        setFocused(null);
    }

    public final boolean isControlFocused(GuiEventListener control) {
        return control != null && getFocused() == control && control.isFocused();
    }

    public final void openContextMenu(double virtualX, double virtualY, List<GuiOverlay.MenuItem> items) {
        overlays.openMenu(toScreenX(virtualX), toScreenY(virtualY), items);
    }

    public final void openDialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        overlays.openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
    }

    public final KineticWidgets.Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return addDropdown(x, y, width, options, List.of(), selectedIndex, tooltip, ignored -> true, responder);
    }

    public final KineticWidgets.Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        return addDropdown(x, y, width, options, List.of(), selectedIndex, tooltip, validator, responder);
    }

    public final KineticWidgets.Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return addDropdown(x, y, width, options, optionTooltips, selectedIndex, tooltip, ignored -> true, responder);
    }

    public final KineticWidgets.Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        List<Component> normalizedOptions = options == null ? new ArrayList<>() : new ArrayList<>(options);
        List<Component> normalizedTooltips = optionTooltips == null ? new ArrayList<>() : new ArrayList<>(optionTooltips);
        KineticWidgets.Dropdown control = dropdown(
                x, y, width, STANDARD_CONTROL_HEIGHT, normalizedOptions, normalizedTooltips, selectedIndex, validator, responder
        );
        addRenderableWidget(control);
        registerWidgetTooltip(control, tooltip);
        return control;
    }

    private KineticWidgets.Dropdown dropdown(
            int x,
            int y,
            int width,
            int height,
            List<Component> options,
            List<Component> optionTooltips,
            int selectedIndex,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        return KineticWidgets.createDropdown(
                x, y, width, options, selectedIndex, null, validator, responder, control -> {
                    List<GuiOverlay.MenuItem> entries = new ArrayList<>();
                    List<Component> values = control.options();
                    for (int index = 0; index < values.size(); index++) {
                        int optionIndex = index;
                        Component optionTooltip = index < optionTooltips.size() && optionTooltips.get(index) != null
                                && !optionTooltips.get(index).getString().isBlank()
                                ? optionTooltips.get(index)
                                : values.get(index);
                        entries.add(GuiOverlay.MenuItem.toggle(
                                values.get(index),
                                optionTooltip,
                                optionIndex == control.selectedIndex(),
                                () -> control.choose(optionIndex)
                        ));
                    }
                    openContextMenu(x, y + height, entries);
                }
        );
    }

    @Override
    protected final void init() {
        super.init();
        updateMetrics();
        rebuildUi();
    }

    public final void rebuildUi() {
        clearWidgets();
        widgetTooltips.clear();
        scrollViewportWidgets.clear();
        buildUi();
    }

    protected abstract void buildUi();

    private void updateMetrics() {
        safeArea = GuiLayout.SafeArea.of(width, height, STANDARD_SAFE_MARGIN);
        metrics = GuiLayout.measure(
                safeArea.width(),
                safeArea.height(),
                STANDARD_CANVAS_WIDTH,
                STANDARD_CANVAS_HEIGHT
        );
        canvasScale = Math.max(0.0001f, metrics.fitScale());
        canvasWidth = STANDARD_CANVAS_WIDTH;
        canvasHeight = STANDARD_CANVAS_HEIGHT;
        canvasX = safeArea.left() + Math.round(
                (safeArea.width() - STANDARD_CANVAS_WIDTH * canvasScale) / 2f
        );
        canvasY = safeArea.top() + Math.round(
                (safeArea.height() - STANDARD_CANVAS_HEIGHT * canvasScale) / 2f
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        overlays.beginFrame();
        int virtualMouseX = (int) Math.floor(toVirtualX(mouseX));
        int virtualMouseY = (int) Math.floor(toVirtualY(mouseY));

        GuiGraphics canvasGraphics = new CanvasGuiGraphics(graphics);
        canvasGraphics.pose().pushPose();
        canvasGraphics.pose().translate(canvasX, canvasY, 0);
        canvasGraphics.pose().scale(canvasScale, canvasScale, 1f);
        try {
            updateScrollableWidgetPositions();
            updateCanvasWidgets(virtualMouseX, virtualMouseY, partialTick);
            renderCanvasBackground(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);

            List<AbstractWidget> temporarilyHidden = new ArrayList<>();
            for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
                AbstractWidget widget = viewportWidget.widget();
                if (widget.visible) {
                    widget.visible = false;
                    temporarilyHidden.add(widget);
                }
            }
            try {
                if (renderRenderablesOnly) {
                    for (Renderable renderable : renderables) {
                        renderable.render(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
                    }
                } else {
                    super.render(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
                }
            } finally {
                for (AbstractWidget widget : temporarilyHidden) {
                    widget.visible = true;
                }
            }

            renderScrollableWidgets(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
            renderCanvasForeground(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            canvasGraphics.pose().popPose();
        }

        if (!overlays.blocksInput()) {
            requestWidgetTooltip(virtualMouseX, virtualMouseY);
            renderTooltips(graphics, virtualMouseX, virtualMouseY, mouseX, mouseY);
        }
        renderScreenOverlay(graphics, virtualMouseX, virtualMouseY, mouseX, mouseY, partialTick);
        overlays.render(graphics, font, width, height, mouseX, mouseY);
    }

    private final class CanvasGuiGraphics extends GuiGraphics {
        private CanvasGuiGraphics(GuiGraphics source) {
            super(KineticScreen.this.minecraft, source.bufferSource());
        }

        @Override
        public void enableScissor(int left, int top, int right, int bottom) {
            super.enableScissor(
                    toScreenX(left),
                    toScreenY(top),
                    toScreenRight(right),
                    toScreenBottom(bottom)
            );
        }
    }

    protected void updateCanvasWidgets(int mouseX, int mouseY, float partialTick) {
    }

    protected void renderCanvasBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderCanvasForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderTooltips(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY
    ) {
    }

    protected void renderScreenOverlay(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY,
            float partialTick
    ) {
    }

    public final double toVirtualX(double screenX) {
        return (screenX - canvasX) / canvasScale;
    }

    public final double toVirtualY(double screenY) {
        return (screenY - canvasY) / canvasScale;
    }

    public final int toScreenX(double virtualX) {
        return canvasX + (int) Math.floor(virtualX * canvasScale);
    }

    public final int toScreenY(double virtualY) {
        return canvasY + (int) Math.floor(virtualY * canvasScale);
    }

    public final int toScreenRight(double virtualX) {
        return canvasX + (int) Math.ceil(virtualX * canvasScale);
    }

    public final int toScreenBottom(double virtualY) {
        return canvasY + (int) Math.ceil(virtualY * canvasScale);
    }

    public final boolean isInsideCanvas(double screenX, double screenY) {
        double virtualX = toVirtualX(screenX);
        double virtualY = toVirtualY(screenY);
        return virtualX >= 0 && virtualX < canvasWidth && virtualY >= 0 && virtualY < canvasHeight;
    }

    public final void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics instanceof CanvasGuiGraphics) {
            graphics.enableScissor(left, top, right, bottom);
            return;
        }
        graphics.enableScissor(toScreenX(left), toScreenY(top), toScreenRight(right), toScreenBottom(bottom));
    }

    public final void disableUiScissor(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    public final void renderScaledList(
            ObjectSelectionList<?> list,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (list == null || minecraft == null) return;
        if (graphics instanceof CanvasGuiGraphics) {
            list.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        GuiGraphics proxy = new CanvasGuiGraphics(graphics);
        proxy.pose().pushPose();
        proxy.pose().translate(canvasX, canvasY, 0);
        proxy.pose().scale(canvasScale, canvasScale, 1f);
        try {
            list.render(proxy, mouseX, mouseY, partialTick);
        } finally {
            proxy.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (overlays.mouseClicked(mouseX, mouseY, button, width, height, font)) {
            return true;
        }
        boolean handled = canvasMouseClicked(toVirtualX(mouseX), toVirtualY(mouseY), button);
        if (!handled) {
            setFocused(null);
        }
        return handled;
    }

    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        updateScrollableWidgetPositions();
        List<AbstractWidget> hidden = hideScrollableWidgetsOutsideViewport(mouseX, mouseY);
        try {
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            restoreScrollableWidgetVisibility(hidden);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (overlays.blocksInput()) {
            return true;
        }
        return canvasMouseReleased(toVirtualX(mouseX), toVirtualY(mouseY), button);
    }

    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overlays.blocksInput()) return true;
        return canvasMouseDragged(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button,
                dragX / canvasScale,
                dragY / canvasScale
        );
    }

    protected boolean canvasMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overlays.blocksInput()) return true;
        return canvasMouseScrolled(toVirtualX(mouseX), toVirtualY(mouseY), delta);
    }

    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (GuiSession.routeSelectionListWheel(children(), mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!overlays.blocksInput()) canvasMouseMoved(toVirtualX(mouseX), toVirtualY(mouseY));
    }

    protected void canvasMouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (overlays.keyPressed(keyCode)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    final GuiSession.DraftSession draftSession() {
        return draftSession;
    }

    final void adoptDraftSession(GuiSession.DraftSession sharedDraft) {
        if (sharedDraft != null && sharedDraft.enabled()) {
            this.draftSession = sharedDraft;
        }
    }

    final void discardPendingEditsForNavigation() {
        draftSession.discardToBaseline();
    }

    public final void navigateBack() {
        GuiSession.back(this);
    }

    @Override
    public void onClose() {
        navigateBack();
    }
}
