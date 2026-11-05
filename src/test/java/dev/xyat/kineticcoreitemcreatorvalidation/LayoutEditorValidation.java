package dev.xyat.kineticcoreitemcreatorvalidation;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;
import dev.xyat.kineticcore.api.client.gui.widget.list.SelectionItem;
import dev.xyat.kineticcore.api.runtime.KineticPaths;
import dev.xyat.kineticcore.internal.client.gui.layout.LayoutEditor;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Real-client check of the Core layout editor (-Dkineticcore.layoutEditorValidation=true): in a world, opens a test
 * page, switches edit mode on through the in-page command line, moves, resizes, scales and nudges controls, switches
 * it off through the save dialog, checks config/kineticcore/gui-layouts/kineticcoreitemcreatorvalidation.json and
 * that a reopened page uses the saved layout, then removes the file and stops the client.
 */
public final class LayoutEditorValidation {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Path FILE = KineticPaths.configFile("kineticcore/gui-layouts/kineticcoreitemcreatorvalidation.json");
    private static int phase, ticks, captures, worldTicks;
    private static boolean captureReady = true;
    private static TestPage page;

    private LayoutEditorValidation() {}

    public static void install() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, LayoutEditorValidation::tick);
    }

    /** Two buttons and a list; the button keys are their literal labels. */
    public static final class TestPage extends KineticPage {
        KineticButton first, second;
        public TestPage() {
            super(Component.literal("Layout editor validation"));
            useCanvas(640, 360, 4);
        }
        @Override protected void build(KineticUi ui) {
            first = ui.button(100, 100, 40).text(Component.literal("Layout A")).build();
            second = ui.button(300, 100, 60).text(Component.literal("Layout B")).build();
            var rows = new ArrayList<SelectionItem>();
            for (int i = 0; i < 30; i++) rows.add(new SelectionItem(Component.literal("row " + i), null, null, true, false));
            ui.selectionList(300, 150, 200, 100, rows).textRows().build();
        }
        @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
            KineticTheme.panel(g, 0, 0, width(), height());
            g.centeredText(title(), 320, 8, 0xFFFFFFFF, false);
        }
    }

    private static void tick() {
        if (phase == 99) return;
        var client = Minecraft.getInstance();
        try {
            require(++ticks < (phase == 0 ? 12000 : 400), "layout editor validation timed out in phase " + phase);
            if (client.getOverlay() != null || client.player == null || client.level == null) return;
            var screen = client.screen;
            switch (phase) {
                case 0 -> {
                    // Five seconds in the world first: a large pack is still busy with its first frames after joining.
                    if (screen != null || ++worldTicks < 100) return;
                    Files.deleteIfExists(FILE);
                    dev.xyat.kineticcore.internal.client.gui.layout.LayoutOverrides.discard();
                    page = new TestPage();
                    KineticGui.open(page);
                    next();
                }
                case 1 -> {
                    if (!(screen instanceof KineticScreenHost) || page.first == null) return;
                    type(screen, "ktlayout");
                    require(LayoutEditor.active(), "the in-page command line switches edit mode on");
                    // Move Layout A by (+50, +21): it snaps to the canvas grid lines nearby within 4 px.
                    LayoutEditor.mouseClicked(screen, 110, 108, 0);
                    LayoutEditor.mouseDragged(screen, 160, 129, 0);
                    LayoutEditor.mouseReleased(160, 129, 0);
                    require(Math.abs(page.first.controlX() - 150) <= 4 && Math.abs(page.first.controlY() - 121) <= 4,
                            "dragging moves the control: " + page.first.controlX() + "," + page.first.controlY());
                    int x = page.first.controlX();
                    LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_RIGHT, 0);
                    require(page.first.controlX() == x + 1, "an arrow key nudges a clicked control by 1 px");
                    LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_MOD_SHIFT);
                    int y = page.first.controlY();
                    // Resize from the right edge, then arrows resize instead of moving.
                    int right = page.first.controlX() + page.first.controlWidth() - 1;
                    LayoutEditor.mouseClicked(screen, right, y + 8, 0);
                    LayoutEditor.mouseDragged(screen, right + 31, y + 8, 0);
                    LayoutEditor.mouseReleased(right + 31, y + 8, 0);
                    require(Math.abs(page.first.controlWidth() - 71) <= 4, "dragging the right edge resizes: " + page.first.controlWidth());
                    int width = page.first.controlWidth();
                    LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_RIGHT, 0);
                    require(page.first.controlWidth() == width + 1 && page.first.controlY() == y, "after an edge drag arrows resize");
                    // The wheel scales the list under the pointer around its centre.
                    LayoutEditor.mouseScrolled(screen, 400, 200, 1.0);
                    checkSelectionCommands(screen);
                    // Select Layout A again so the capture shows its handles and size label.
                    LayoutEditor.mouseClicked(screen, page.first.controlX() + 10, page.first.controlY() + 8, 0);
                    LayoutEditor.mouseReleased(page.first.controlX() + 10, page.first.controlY() + 8, 0);
                    next();
                }
                case 2 -> {
                    // A screenshot reads the last drawn frame: capture once the edit overlay has been drawn.
                    if (ticks == 10) capture("edit");
                    if (ticks <= 10 || !captureReady) return;
                    type(screen, "ktlayout");
                    require(value(((KineticScreenHost) screen).kineticRuntime().overlays(), "dialog") != null,
                            "switching off with changes asks whether to save");
                    next();
                }
                case 3 -> {
                    // The dialog lays out its buttons when drawn; a busy hidden client may draw few frames, so wait about a
                    // second and retry the click every ten ticks.
                    if (ticks < 20 || ticks % 10 != 0) return;
                    Object dialog = value(((KineticScreenHost) screen).kineticRuntime().overlays(), "dialog");
                    require(dialog != null || !LayoutEditor.active(), "the save dialog closed without switching edit mode off");
                    if (dialog != null) {
                        var confirm = (KineticButton) value(dialog, "confirmButton");
                        double cx = confirm.controlX() + confirm.controlWidth() / 2.0, cy = confirm.controlY() + confirm.controlHeight() / 2.0;
                        LOG.info("LAYOUT_EDITOR_CONFIRM_CLICK tick={} at={},{} size={}x{}", ticks, cx, cy, confirm.controlWidth(), confirm.controlHeight());
                        clickScreen(cx, cy);
                    }
                    if (LayoutEditor.active() && ticks < 120) return;
                    require(!LayoutEditor.active(), "confirming switches edit mode off");
                    require(Files.isRegularFile(FILE), "the layout file is written: " + FILE);
                    String json = Files.readString(FILE);
                    require(json.contains("LayoutEditorValidation$TestPage") && json.contains("Layout A") && json.contains("SelectionList"),
                            "the file holds the page's moved controls: " + json);
                    next();
                }
                case 4 -> {
                    int[] saved = {page.first.controlX(), page.first.controlY(), page.first.controlWidth(), page.first.controlHeight()};
                    // Forget the in-memory copy so the reopened page reads the written file.
                    dev.xyat.kineticcore.internal.client.gui.layout.LayoutOverrides.discard();
                    page = new TestPage();
                    KineticGui.open(page);
                    savedRect = saved;
                    next();
                }
                case 5 -> {
                    if (page.first == null) return;
                    require(page.first.controlX() == savedRect[0] && page.first.controlY() == savedRect[1]
                            && page.first.controlWidth() == savedRect[2], "a reopened page uses the saved layout");
                    require(page.second.controlX() == 300 && page.second.controlY() == 100, "untouched controls keep their defaults");
                    capture("reopened");
                    next();
                }
                case 6 -> {
                    if (!captureReady) return;
                    Files.deleteIfExists(FILE);
                    dev.xyat.kineticcore.internal.client.gui.layout.LayoutOverrides.discard();
                    LOG.info("LAYOUT_EDITOR_PASS captures={}", captures);
                    phase = 99;
                    dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
                }
                default -> { }
            }
        } catch (Throwable failure) {
            LOG.error("LAYOUT_EDITOR_FAIL phase={}", phase, failure);
            try { Files.deleteIfExists(FILE); } catch (Exception ignored) { }
            phase = 99;
            dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
        }
    }

    private static int[] savedRect;

    /**
     * Box selection, Ctrl+A, the align / same / set / distribute / reset commands, the problem check, undo and the
     * double-click number entry. Ends with Layout A where it was, so the save phases see the same layout.
     */
    private static void checkSelectionCommands(net.minecraft.client.gui.screens.Screen screen) throws Exception {
        int[] before = rect(page.first);
        // A box around both buttons (not the list) selects them.
        LayoutEditor.mouseClicked(screen, 20, 40, 0);
        LayoutEditor.mouseDragged(screen, 400, 145, 0);
        LayoutEditor.mouseReleased(400, 145, 0);
        java.util.Set<?> selection = (java.util.Set<?>) staticValue("SELECTION");
        require(selection.size() == 2, "the box selects the two buttons: " + selection);
        type(screen, "ktlayout align top");
        int top = Math.min(before[1], 100);
        require(page.first.controlY() == top && page.second.controlY() == top,
                "align top lines both up: " + page.first.controlY() + "," + page.second.controlY());
        type(screen, "ktlayout same width");
        require(page.first.controlWidth() == page.second.controlWidth(),
                "same width: " + page.first.controlWidth() + "," + page.second.controlWidth());
        // Layout A onto Layout B is an overlap the editor reports.
        LayoutEditor.mouseClicked(screen, page.first.controlX() + 5, page.first.controlY() + 5, 0);
        LayoutEditor.mouseReleased(page.first.controlX() + 5, page.first.controlY() + 5, 0);
        type(screen, "ktlayout set " + page.second.controlX() + " " + page.second.controlY() + " 40 16");
        require(page.first.controlX() == page.second.controlX(), "set moves the selected control by the numbers");
        java.lang.reflect.Method problems = LayoutEditor.class.getDeclaredMethod("problems", net.minecraft.client.gui.screens.Screen.class);
        problems.setAccessible(true);
        require(!((java.util.List<?>) problems.invoke(null, screen)).isEmpty(), "an overlap is reported as a problem");
        // Ctrl+A selects every control; distribute spaces them evenly.
        LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_A, GLFW.GLFW_MOD_CONTROL);
        require(((java.util.Set<?>) staticValue("SELECTION")).size() == 3, "Ctrl+A selects all three controls");
        type(screen, "ktlayout distribute vertical");
        // Reset restores every default, and undo brings the edits back.
        type(screen, "ktlayout reset");
        require(page.first.controlX() == 100 && page.first.controlY() == 100 && page.first.controlWidth() == 40,
                "reset restores the defaults: " + java.util.Arrays.toString(rect(page.first)));
        LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL);
        require(page.first.controlX() != 100 || page.first.controlY() != 100, "Ctrl+Z undoes the reset");
        // Undo back to where Layout A was before these checks.
        for (int i = 0; i < 10 && !java.util.Arrays.equals(rect(page.first), before); i++) {
            LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL);
        }
        require(java.util.Arrays.equals(rect(page.first), before), "undo returns to the earlier layout: " + java.util.Arrays.toString(rect(page.first)));
        // A double-click opens the command line with the control's numbers.
        int cx = page.first.controlX() + 5, cy = page.first.controlY() + 5;
        LayoutEditor.mouseClicked(screen, cx, cy, 0);
        LayoutEditor.mouseReleased(cx, cy, 0);
        LayoutEditor.mouseClicked(screen, cx, cy, 0);
        LayoutEditor.mouseReleased(cx, cy, 0);
        require(LayoutEditor.commandLineOpen(), "a double-click opens the command line");
        String typed = ((net.minecraft.client.gui.components.EditBox) staticValue("command")).getValue();
        require(typed.equals("/ktlayout set " + before[0] + " " + before[1] + " " + before[2] + " " + before[3]),
                "the command line holds the control's numbers: " + typed);
        LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_ESCAPE, 0);
        LOG.info("LAYOUT_EDITOR_SELECTION_PASS box=true align=true same=true set=true problems=true selectAll=true reset=true undo=true doubleClick=true");
    }

    private static int[] rect(KineticButton button) {
        return new int[]{button.controlX(), button.controlY(), button.controlWidth(), button.controlHeight()};
    }

    private static Object staticValue(String name) throws ReflectiveOperationException {
        Field field = LayoutEditor.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void next() {
        phase++;
        ticks = 0;
    }

    /** Opens the in-page command line with '/', types the command and presses Enter. */
    private static void type(net.minecraft.client.gui.screens.Screen screen, String text) {
        LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_SLASH, 0);
        LayoutEditor.charTyped('/');
        for (char c : text.toCharArray()) LayoutEditor.charTyped(c);
        LayoutEditor.keyPressed(screen, GLFW.GLFW_KEY_ENTER, 0);
    }

    private static void capture(String name) {
        var client = Minecraft.getInstance();
        captureReady = false;
        String file = "core-layout-editor-" + name + ".png";
        //? if >=26.1 {
        /*net.minecraft.client.Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), 1, message -> {
        *///?} else {
        net.minecraft.client.Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), message -> {
        //?}
            captures++; captureReady = true; LOG.info("LAYOUT_EDITOR_CAPTURE {}", file);
        });
    }

    private static void clickScreen(double sx, double sy) {
        var screen = Minecraft.getInstance().screen;
        //? if >=26.1 {
        /*var input = new net.minecraft.client.input.MouseButtonEvent(sx, sy, new net.minecraft.client.input.MouseButtonInfo(0, 0));
        screen.mouseClicked(input, false); screen.mouseReleased(input);
        *///?} else {
        screen.mouseClicked(sx, sy, 0); screen.mouseReleased(sx, sy, 0);
        //?}
    }

    private static Object value(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) try {
            Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(object);
        } catch (NoSuchFieldException ignored) { }
        throw new NoSuchFieldException(name);
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
