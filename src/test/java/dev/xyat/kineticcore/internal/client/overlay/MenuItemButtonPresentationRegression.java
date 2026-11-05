package dev.xyat.kineticcore.internal.client.overlay;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.MenuItem;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.MenuItemStyle;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Explicit button presentation must retain menu behavior and bypass the text-row renderer. */
public final class MenuItemButtonPresentationRegression {
    public static void main(String[] args) throws Exception {
        copiesRetainAllState();
        explicitButtonsBypassTextRows();
        System.out.println("PASS: menu button copies preserve state and override only their own renderer");
    }

    private static void copiesRetainAllState() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        MenuItem original = MenuItem.create(Component.literal("label"), Component.literal("detail"),
                Component.literal("tooltip"), true, calls::incrementAndGet, false, MenuItemStyle.DANGER);
        MenuItem button = asButton(original);
        check(button != original && !isButton(original) && isButton(button), "asButton must return an independent opt-in copy");
        sameState(original, button);
        button.action().run();
        check(calls.get() == 1, "button copy lost the original action");

        MenuItem choice = MenuItem.choice(Component.literal("choice"), Component.literal("translation"),
                Component.literal("help"), false, calls::incrementAndGet);
        sameState(choice, asButton(choice));
        MenuItem separator = MenuItem.separator();
        sameState(separator, asButton(separator));
    }

    private static void sameState(MenuItem original, MenuItem copy) {
        check(copy.label() == original.label() && copy.detail() == original.detail() && copy.tooltip() == original.tooltip(),
                "presentation copy lost label, detail or tooltip");
        check(copy.checked() == original.checked() && copy.isChoice() == original.isChoice(), "presentation copy lost selection state");
        check(copy.action() == original.action() && copy.enabled() == original.enabled() && copy.style() == original.style(),
                "presentation copy lost action, enabled state or style");
    }

    private static void explicitButtonsBypassTextRows() throws Exception {
        MenuItem first = MenuItem.toggle(Component.literal("first"), Component.empty(), true, () -> {});
        MenuItem second = MenuItem.toggle(Component.literal("second"), Component.empty(), false, () -> {});
        MenuItem third = MenuItem.toggle(Component.literal("third"), Component.empty(), true, () -> {});
        List<MenuItem> defaults = List.of(first, second, third);
        check(!textRow(first, defaults), "default multi-toggle menus must use real buttons");
        MenuItem explicit = asButton(first);
        List<MenuItem> mixed = List.of(explicit, second, third);
        check(!textRow(explicit, mixed), "explicit button still uses the selection-row renderer");
        check(!textRow(second, mixed), "all non-search menu entries must use real buttons");
        List<MenuItem> buttons = List.of(explicit, asButton(second), asButton(third));
        for (MenuItem button : buttons) check(!textRow(button, buttons), "explicit multi-toggle menu still uses text rows");
    }

    private static MenuItem asButton(MenuItem item) throws Exception {
        try {
            return (MenuItem) MenuItem.class.getMethod("asButton").invoke(item);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("MenuItem must expose immutable asButton() presentation", missing);
        }
    }

    private static boolean isButton(MenuItem item) throws Exception {
        return (Boolean) MenuItem.class.getMethod("isButton").invoke(item);
    }

    private static boolean textRow(MenuItem item, List<MenuItem> items) throws Exception {
        Class<?> controlType = Class.forName(GuiOverlayRuntime.class.getName() + "$MenuControl");
        Constructor<?> controlConstructor = controlType.getDeclaredConstructors()[0];
        controlConstructor.setAccessible(true);
        List<Object> controls = new ArrayList<>();
        for (MenuItem row : items) controls.add(controlConstructor.newInstance(row, null));
        Class<?> menuType = Class.forName(GuiOverlayRuntime.class.getName() + "$ContextMenu");
        Constructor<?> menuConstructor = menuType.getDeclaredConstructors()[0];
        menuConstructor.setAccessible(true);
        Object menu = menuConstructor.newInstance(0, 0, controls, 160, null);
        Method selector = GuiOverlayRuntime.class.getDeclaredMethod("textRow", MenuItem.class, menuType);
        selector.setAccessible(true);
        return (Boolean) selector.invoke(null, item, menu);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
