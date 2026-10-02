package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.ui.*;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.internal.client.gui.widget.tab.TabBarButtons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/** Internal {@link KineticUi} implementation registering controls with a Kinetic screen host. */
public final class PageUi implements KineticUi {
    private final KineticScreenHost host;
    private final Viewport viewport;
    private final List<CustomControlSupport.Widget> customs;

    private record Viewport(int left, int top, int right, int bottom, DoubleSupplier pixelOffset) {
    }

    public PageUi(KineticScreenHost host) {
        this(host, null, new ArrayList<>());
    }

    private PageUi(KineticScreenHost host, Viewport viewport, List<CustomControlSupport.Widget> customs) {
        this.host = Objects.requireNonNull(host, "host");
        this.viewport = viewport;
        this.customs = customs;
    }

    /** Forgets custom controls before a rebuild. */
    public void reset() {
        customs.clear();
    }

    /**
     * 把松开事件补发给按下后被拖出范围的自绘控件 / Delivers a release to custom controls that were pressed and then
     * dragged outside their bounds (vanilla only notifies the child under the pointer).
     */
    public void releasePressedCustomControls(double mouseX, double mouseY, int button) {
        for (CustomControlSupport.Widget widget : List.copyOf(customs)) {
            // 鼠标下的控件由原版分发 / The control under the pointer gets the release from vanilla.
            if (widget.isPressed(button) && !widget.isMouseOver(mouseX, mouseY)) widget.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void tickCustomControls() {
        for (CustomControlSupport.Widget widget : List.copyOf(customs)) {
            CustomControlSupport.tick(widget);
        }
    }

    private static int z(int layer) {
        return layer * GuiGraphicsAdapter.RAISE_STEP_DEPTH;
    }

    private static <T> Consumer<T> orNoop(Consumer<T> consumer) {
        return consumer == null ? ignored -> { } : consumer;
    }

    private static <A, B> BiConsumer<A, B> orNoop(BiConsumer<A, B> consumer) {
        return consumer == null ? (a, b) -> { } : consumer;
    }

    private static Integer asInt(Number number) {
        return number == null ? null : number.intValue();
    }

    private static Long asLong(Number number) {
        return number == null ? null : number.longValue();
    }

    private static Double asDouble(Number number) {
        return number == null ? null : number.doubleValue();
    }

    private void bindViewport(InternalControl control) {
        if (viewport == null) return;
        if (!(host.screen() instanceof KineticScreen canvas)) {
            throw new UnsupportedOperationException("Scroll viewports are only supported on canvas pages");
        }
        canvas.addScrollableWidget(control, viewport.left(), viewport.top(), viewport.right(), viewport.bottom(),
                viewport.pixelOffset());
    }

    private <C extends InternalControl> C finish(C control, Component tooltip, Supplier<Component> dynamicTooltip,
                                                 boolean enabled, boolean visible) {
        if (dynamicTooltip != null) {
            host.registerDynamicWidgetTooltip(control, dynamicTooltip);
        } else if (tooltip != null) {
            host.registerWidgetTooltip(control, tooltip);
        }
        if (!enabled) control.setEnabled(false);
        if (!visible) control.setVisible(false);
        bindViewport(control);
        return control;
    }

    @Override
    public ButtonBuilder button(int x, int y, int width) {
        return new ButtonBuilder(x, y, width) {
            @Override
            public KineticButton build() {
                StateButton[] self = new StateButton[1];
                Consumer<KineticButton> handler = onClick;
                Runnable action = () -> {
                    if (handler != null) handler.accept(self[0]);
                };
                StateButton button;
                if (card) {
                    if (layer > 0) throw new IllegalStateException("card buttons cannot be layered");
                    button = host.addCardButton(x, y, width, text, null, action);
                } else if (layer > 0) {
                    button = compact
                            ? host.addCompactHighZButton(x, y, width, text, null, z(layer), action)
                            : host.addHighZButton(x, y, width, text, null, z(layer), action);
                } else {
                    button = compact
                            ? host.addCompactButton(x, y, width, text, null, action)
                            : host.addButton(x, y, width, text, null, action);
                }
                self[0] = button;
                return finish(button, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ItemButtonBuilder itemButton(int x, int y, int width, ItemStack icon) {
        return new ItemButtonBuilder(x, y, width, icon) {
            @Override
            public KineticItemButton build() {
                Runnable action = onClick == null ? () -> { } : onClick;
                return finish(host.addItemButton(x, y, width, icon, text, null, action),
                        tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ToggleBuilder toggle(int x, int y, int width) {
        return new ToggleBuilder(x, y, width) {
            @Override
            public KineticToggle build() {
                Consumer<Boolean> change = orNoop(onChange);
                if (layer > 0) {
                    if (compact) throw new IllegalStateException("compact toggles cannot be layered");
                    return finish(host.addHighZToggleButton(x, y, width, value, onText, offText, null, validator, change, z(layer)),
                            tooltip, dynamicTooltip, enabled, visible);
                }
                return finish(compact
                                ? host.addCompactToggleButton(x, y, width, value, onText, offText, null, validator, change)
                                : host.addToggleButton(x, y, width, value, onText, offText, null, validator, change),
                        tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public CycleButtonBuilder cycleButton(int x, int y, int width, List<Component> options) {
        return new CycleButtonBuilder(x, y, width, options) {
            @Override
            public KineticCycleButton build() {
                return finish(host.addCycleButton(x, y, width, index, options, null, validator, orNoop(onChange)),
                        tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ColorButtonBuilder colorButton(int x, int y, int width, int rgb) {
        return new ColorButtonBuilder(x, y, width, rgb) {
            @Override
            public KineticColorButton build() {
                Runnable action = onClick == null ? () -> { } : onClick;
                return finish(host.addColorPreviewButton(x, y, width, rgb, text, null, action),
                        tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ColorSwatchBuilder colorSwatch(int x, int y, int rgb) {
        return new ColorSwatchBuilder(x, y, 0, rgb) {
            @Override
            public KineticColorSwatch build() {
                Runnable action = onClick == null ? () -> { } : onClick;
                return finish(host.addColorSwatchButton(x, y, rgb, null, action), tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public SliderBuilder slider(int x, int y, int width) {
        return new SliderBuilder(x, y, width) {
            @Override
            public KineticSlider build() {
                return finish(host.addSlider(x, y, width, label, min, max, step, value, validator,
                                onChange == null ? ignored -> { } : onChange, null),
                        tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public TextFieldBuilder textField(int x, int y, int width) {
        return new TextFieldBuilder(x, y, width) {
            @Override
            public KineticTextField build() {
                var field = host.addTextField(x, y, width, label, placeholder, validator, null);
                if (maxLength > 0) field.setMaxLength(maxLength);
                if (value != null) field.setValue(value);
                if (onChange != null) field.setResponder(onChange);
                if (firstShownTextAsDefault) field.useFirstShownTextAsDefault();
                else field.setDefaultText(defaultText);
                field.setValueColor(valueColor);
                return finish(field, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public TextAreaBuilder textArea(int x, int y, int width, int height) {
        return new TextAreaBuilder(x, y, width, height) {
            @Override
            public KineticTextArea build() {
                var area = host.addMultiLineTextField(x, y, width, height, label, placeholder, null);
                if (maxLength > 0) area.setCharacterLimit(maxLength);
                if (value != null) area.setValue(value);
                if (onChange != null) area.setValueListener(onChange);
                return finish(area, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public NumberFieldBuilder numberField(int x, int y, int width, NumberType type) {
        return new NumberFieldBuilder(x, y, width, Objects.requireNonNull(type, "type")) {
            @Override
            public KineticNumberField build() {
                NumericEditBox field = switch (type) {
                    case INT -> host.addIntegerField(x, y, width, label, allowNegative, asInt(min), asInt(max), validator, null);
                    case LONG -> host.addLongField(x, y, width, label, allowNegative, asLong(min), asLong(max), validator, null);
                    case DECIMAL -> host.addDecimalField(x, y, width, label, allowNegative, asDouble(min), asDouble(max), validator, null);
                };
                if (value != null) field.setValue(value);
                if (onChange != null) field.setResponder(onChange);
                if (firstShownTextAsDefault) field.useFirstShownTextAsDefault();
                else field.setDefaultText(defaultText);
                field.setValueColor(valueColor);
                return finish(field, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public AutoCompleteBuilder autoComplete(int x, int y, int width, Supplier<List<KineticSuggestion>> dictionary) {
        return new AutoCompleteBuilder(x, y, width, dictionary) {
            @Override
            public KineticAutoCompleteField build() {
                var field = host.addAutoCompleteField(x, y, width, label, placeholder, dictionary, null);
                if (maxLength > 0) field.setMaxLength(maxLength);
                if (value != null) field.setValue(value);
                if (onChange != null) field.setResponder(onChange);
                if (firstShownTextAsDefault) field.useFirstShownTextAsDefault();
                else field.setDefaultText(defaultText);
                field.setValueColor(valueColor);
                if (onSelect != null) field.setSelectionResponder(onSelect);
                return finish(field, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public NumberAutoCompleteBuilder numberAutoComplete(int x, int y, int width, NumberType type,
                                                        Supplier<List<KineticSuggestion>> dictionary) {
        return new NumberAutoCompleteBuilder(x, y, width, Objects.requireNonNull(type, "type"), dictionary) {
            @Override
            public KineticNumberAutoCompleteField build() {
                NumericAutoCompleteBox field = switch (type) {
                    case INT -> host.addIntegerAutoCompleteField(x, y, width, label, dictionary, allowNegative,
                            asInt(min), asInt(max), validator, null);
                    case LONG -> host.addLongAutoCompleteField(x, y, width, label, dictionary, allowNegative,
                            asLong(min), asLong(max), validator, null);
                    case DECIMAL -> host.addDecimalAutoCompleteField(x, y, width, label, dictionary, allowNegative,
                            asDouble(min), asDouble(max), validator, null);
                };
                if (value != null) field.setValue(value);
                if (onChange != null) field.setResponder(onChange);
                if (firstShownTextAsDefault) field.useFirstShownTextAsDefault();
                else field.setDefaultText(defaultText);
                field.setValueColor(valueColor);
                if (onSelect != null) field.setSelectionResponder(onSelect);
                return finish(field, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public DropdownBuilder dropdown(int x, int y, int width, List<KineticDropdown.Option> options) {
        return new DropdownBuilder(x, y, width, options) {
            @Override
            public KineticDropdown build() {
                return finish(host.addDropdown(x, y, width, options, selected, null, validator, orNoop(onChange)),
                        tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public TabBarBuilder tabBar(int x, int y, int width, List<Component> labels) {
        return new TabBarBuilder(x, y, width, labels) {
            @Override
            public KineticTabBar build() {
                Consumer<Integer> select = orNoop(onSelect);
                TabBarButtons bar;
                if (vertical) {
                    bar = host.addVerticalHighZTabBar(x, y, width, labels, tooltips, selected, select, z(layer));
                } else if (layer > 0) {
                    bar = host.addHighZTabBar(x, y, width, labels, tooltips, selected, select, z(layer));
                } else {
                    bar = host.addTabBar(x, y, width, labels, tooltips, selected, select);
                }
                for (StateButton button : bar.buttons()) {
                    if (!enabled) button.setEnabled(false);
                    if (!visible) button.setVisible(false);
                    bindViewport(button);
                }
                return bar;
            }
        };
    }

    @Override
    public TabStripBuilder tabStrip(int x, int y, int width, List<TabStripItem> tabs) {
        return new TabStripBuilder(x, y, width, tabs) {
            @Override
            public KineticTabStrip build() {
                KineticTabStrip strip = host.addScrollableTabStrip(x, y, width, tabs, pinnedLeading, selected, scrollOffset,
                        previousText, nextText, orNoop(onSelect));
                return finishPublic(strip, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    private <C extends KineticControl> C finishPublic(C control, Component tooltip, Supplier<Component> dynamicTooltip,
                                                      boolean enabled, boolean visible) {
        finish(CustomControlSupport.widget(control), tooltip, dynamicTooltip, enabled, visible);
        return control;
    }

    @Override
    public SelectionListBuilder selectionList(int x, int y, int width, int height, List<SelectionItem> items) {
        return new SelectionListBuilder(x, y, width, height, items) {
            @Override
            public KineticSelectionList build() {
                KineticSelectionList list = layer > 0
                        ? host.addHighZScrollableSelectionList(x, y, width, height, items, selected, scrollOffset, orNoop(onSelect), z(layer))
                        : host.addScrollableSelectionList(x, y, width, height, items, selected, scrollOffset, orNoop(onSelect));
                return finishPublic(list, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ItemSelectionListBuilder itemSelectionList(int x, int y, int width, int height, List<ItemSelectionItem> items) {
        return new ItemSelectionListBuilder(x, y, width, height, items) {
            @Override
            public KineticItemSelectionList build() {
                KineticItemSelectionList list = layer > 0
                        ? host.addHighZScrollableItemSelectionList(x, y, width, height, items, selected, scrollOffset, orNoop(onSelect), z(layer))
                        : host.addScrollableItemSelectionList(x, y, width, height, items, selected, scrollOffset, orNoop(onSelect));
                return finishPublic(list, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ItemGridBuilder itemGrid(int x, int y, int width, int height, ItemGridDensity density, List<ItemGridItem> items) {
        return new ItemGridBuilder(x, y, width, height, density, items) {
            @Override
            public KineticItemGrid build() {
                KineticItemGrid grid = layer > 0
                        ? host.addHighZScrollableItemGrid(x, y, width, height, density, items, scrollOffset, orNoop(onClick), z(layer))
                        : host.addScrollableItemGrid(x, y, width, height, density, items, scrollOffset, orNoop(onClick));
                return finishPublic(grid, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ActionListBuilder actionList(int x, int y, int width, int height, List<ActionItem> items) {
        return new ActionListBuilder(x, y, width, height, items) {
            @Override
            public KineticActionList build() {
                KineticActionList list = layer > 0
                        ? host.addHighZScrollableActionList(x, y, width, height, items, selected, scrollOffset, actionWidth,
                        orNoop(onSelect), orNoop(onAction), z(layer))
                        : host.addScrollableActionList(x, y, width, height, items, selected, scrollOffset, actionWidth,
                        orNoop(onSelect), orNoop(onAction));
                return finishPublic(list, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ItemActionListBuilder itemActionList(int x, int y, int width, int height, List<ItemActionItem> items) {
        return new ItemActionListBuilder(x, y, width, height, items) {
            @Override
            public KineticItemActionList build() {
                KineticItemActionList list = layer > 0
                        ? host.addHighZScrollableItemActionList(x, y, width, height, items, selected, scrollOffset, actionWidth,
                        orNoop(onSelect), orNoop(onAction), z(layer))
                        : host.addScrollableItemActionList(x, y, width, height, items, selected, scrollOffset, actionWidth,
                        orNoop(onSelect), orNoop(onAction));
                return finishPublic(list, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public MultiActionListBuilder multiActionList(int x, int y, int width, int height, List<MultiActionItem> items) {
        return new MultiActionListBuilder(x, y, width, height, items) {
            @Override
            public KineticMultiActionList build() {
                KineticMultiActionList list = layer > 0
                        ? host.addHighZScrollableMultiActionList(x, y, width, height, items, selected, scrollOffset,
                        orNoop(onSelect), orNoop(onAction), z(layer))
                        : host.addScrollableMultiActionList(x, y, width, height, items, selected, scrollOffset,
                        orNoop(onSelect), orNoop(onAction));
                return finishPublic(list, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ToggleActionListBuilder toggleActionList(int x, int y, int width, int height, List<ToggleActionItem> items) {
        return new ToggleActionListBuilder(x, y, width, height, items) {
            @Override
            public KineticToggleActionList build() {
                KineticToggleActionList list = layer > 0
                        ? host.addHighZScrollableToggleActionList(x, y, width, height, items, selected, scrollOffset,
                        toggleWidth, actionWidth, orNoop(onSelect), orNoop(onToggle), orNoop(onAction), z(layer))
                        : host.addScrollableToggleActionList(x, y, width, height, items, selected, scrollOffset,
                        toggleWidth, actionWidth, orNoop(onSelect), orNoop(onToggle), orNoop(onAction));
                return finishPublic(list, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public MultiToggleListBuilder multiToggleList(int x, int y, int width, int height, List<MultiToggleItem> items) {
        return new MultiToggleListBuilder(x, y, width, height, items) {
            @Override
            public KineticMultiToggleList build() {
                KineticMultiToggleList list = layer > 0
                        ? host.addHighZScrollableMultiToggleList(x, y, width, height, items, selected, scrollOffset,
                        orNoop(onSelect), orNoop(onToggle), z(layer))
                        : host.addScrollableMultiToggleList(x, y, width, height, items, selected, scrollOffset,
                        orNoop(onSelect), orNoop(onToggle));
                return finishPublic(list, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public ToggleListBuilder toggleList(int x, int y, int width, int height, List<ToggleItem> items) {
        return new ToggleListBuilder(x, y, width, height, items) {
            @Override
            public KineticToggleList build() {
                KineticToggleList list = layer > 0
                        ? host.addHighZScrollableToggleList(x, y, width, height, items, scrollOffset, orNoop(onToggle), z(layer))
                        : host.addScrollableToggleList(x, y, width, height, items, scrollOffset, orNoop(onToggle));
                return finishPublic(list, tooltip, dynamicTooltip, enabled, visible);
            }
        };
    }

    @Override
    public <C extends KineticCustomControl> C add(C control) {
        CustomControlSupport.Widget widget = CustomControlSupport.adapt(Objects.requireNonNull(control, "control"));
        host.addControl(widget, null);
        host.registerDynamicWidgetTooltip(widget, widget::tooltip);
        customs.add(widget);
        bindViewport(widget);
        return control;
    }

    @Override
    public void remove(KineticControl control) {
        if (control == null) return;
        InternalControl internal = CustomControlSupport.widget(control);
        host.removeKineticControl(internal);
        if (internal instanceof CustomControlSupport.Widget widget) customs.remove(widget);
    }

    @Override
    public KineticUi scrollViewport(int left, int top, int right, int bottom, DoubleSupplier pixelOffset) {
        if (!(host.screen() instanceof KineticScreen)) {
            throw new UnsupportedOperationException("Scroll viewports are only supported on canvas pages");
        }
        return new PageUi(host, new Viewport(left, top, right, bottom, Objects.requireNonNull(pixelOffset, "pixelOffset")), customs);
    }
}
