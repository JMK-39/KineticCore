package dev.xyat.kineticcore.internal.client.gui.layout;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.internal.client.gui.page.PageHost;
import dev.xyat.kineticcore.internal.client.gui.text.KineticText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * In-game layout editor for every Kinetic page (one core command, no per-mod setup): {@code /ktlayout} in chat, or
 * {@code /} then {@code ktlayout} inside a Kinetic page, switches edit mode on; the same command again asks whether
 * to save. Two kinds of items are edited:
 * <ul>
 *   <li>controls (buttons, fields, lists, grids …) – moved and resized directly;</li>
 *   <li>named regions a page declares with {@code region(...)} (list areas, panels) – changing one rebuilds the page,
 *   which lays its controls, grids and background out in the new rectangle. Dragging a region edge also moves the
 *   facing edge of a neighbouring region (a divider), unless Ctrl is held.</li>
 * </ul>
 * Drag to move, drag an edge or corner to resize, the wheel scales the item under the pointer, arrow keys nudge by
 * 1 px (Shift: 4 px) and resize instead after an edge was dragged, right-click restores the default, Ctrl+Z undoes.
 * Moves and resizes snap to other items' edges, centres and 4 px gaps, the canvas margin and centre (hold Alt to place
 * freely), and stay inside the 640×360 canvas.
 */
public final class LayoutEditor {
    private static final int CANVAS_W = 640, CANVAS_H = 360, MARGIN = 4, SNAP = 4, EDGE = 3, MIN_SIZE = 8, LINK_GAP = 12;
    private static final int EDGE_LEFT = 1, EDGE_RIGHT = 2, EDGE_TOP = 4, EDGE_BOTTOM = 8;
    private static final String REGION = "region:";
    private static final Map<AbstractWidget, Entry> ENTRIES = Collections.synchronizedMap(new WeakHashMap<>());
    // Page instance -> region id -> default rect, in declaration order.
    private static final Map<Object, Map<String, int[]>> REGIONS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Deque<Map<String, int[]>> UNDO = new ArrayDeque<>();
    private static final List<int[]> GUIDES = new ArrayList<>();

    private record Entry(String page, String key, int[] defaults, boolean owned) {
    }

    /** One editable thing on the current page: a control or a region, addressed by key so rebuilds keep it. */
    private record Item(String key, int[] defaults, int[] rect, AbstractWidget widget) {
        boolean region() {
            return widget == null;
        }
    }

    /** An edge of another region that follows a dragged region edge. */
    private record Link(String key, int edges, int[] start) {
    }

    private static boolean active;
    // The primary item (handles, resizing, numbers) and every selected item (moves, alignment), in selection order.
    private static String selected;
    private static final java.util.LinkedHashSet<String> SELECTION = new java.util.LinkedHashSet<>();
    private static Map<String, int[]> groupStart = Map.of();
    // Rubber-band selection started on empty space: start corner and current corner, in canvas coordinates.
    private static int[] marquee;
    private static String lastClickKey;
    private static long lastClickAt;
    private static double lastClickX, lastClickY;
    // A /ktlayout sub-command from the chat, run at the end of the tick.
    private static String pendingCommand;
    private static int dragEdges = -1, arrowEdges;
    private static boolean arrowsResize;
    private static double startX, startY;
    private static int[] startRect;
    private static List<Link> links = List.of();
    private static Map<String, int[]> pendingUndo;
    private static long sizeLabelUntil;
    // The in-page command line: a vanilla text field (selection, copy and paste) with vanilla command suggestions.
    private static EditBox command;
    private static dev.xyat.kineticcore.internal.client.gui.command.KineticCommandSuggestions.Session suggestions;
    private static boolean swallowSlash;

    private LayoutEditor() {
    }

    // ---- registration ------------------------------------------------------------------------------------------

    /** The page a control belongs to: the page class for page screens, otherwise the screen class. */
    public static String pageId(Screen screen) {
        return screen instanceof PageHost host && host.page() != null ? host.page().getClass().getName() : screen.getClass().getName();
    }

    /** A fallback key for a control without a layout id: its type and the translation key (or literal) of its label. */
    public static String key(AbstractWidget widget) {
        String type = widget.getClass().getSimpleName();
        if (type.isEmpty()) type = widget.getClass().getSuperclass().getSimpleName();
        Component message = widget.getMessage();
        String label = "";
        if (message != null) {
            if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents) label = contents.getKey();
            else label = message.getString().trim();
        }
        if (label.length() > 48) label = label.substring(0, 48);
        return label.isEmpty() ? type : type + ":" + label;
    }

    /**
     * Records a freshly built control and applies its stored layout. A control laid exactly over one of the page's
     * regions belongs to that region: the region is edited instead, so the two cannot drift apart.
     */
    public static void register(Screen screen, AbstractWidget widget, String key) {
        String page = pageId(screen);
        int[] defaults = rect(widget);
        // Controls built off the canvas (per-row buttons parked at -1000) are positioned by the page every frame.
        if (defaults[0] < 0 || defaults[1] < 0) return;
        boolean owned = false;
        Map<String, int[]> regions = screen instanceof PageHost host && host.page() != null ? REGIONS.get(host.page()) : null;
        if (regions != null) {
            for (String id : regions.keySet()) {
                if (Arrays.equals(regionRect(page, id, regions.get(id)), defaults)) owned = true;
            }
        }
        ENTRIES.put(widget, new Entry(page, key, defaults, owned));
        if (owned) return;
        int[] stored = LayoutOverrides.get(page, key, defaults);
        if (stored != null) setRect(widget, stored);
    }

    /** A page's named region: the stored rect, or the default. Called by {@code KineticPage.region}. */
    public static int[] region(Object page, String id, int[] defaults) {
        REGIONS.computeIfAbsent(page, ignored -> Collections.synchronizedMap(new LinkedHashMap<>())).put(id, defaults.clone());
        return regionRect(page.getClass().getName(), id, defaults);
    }

    private static int[] regionRect(String page, String id, int[] defaults) {
        int[] stored = LayoutOverrides.get(page, REGION + id, defaults);
        return stored != null ? stored : defaults.clone();
    }

    // ---- mode ----------------------------------------------------------------------------------------------------

    public static boolean active() {
        return active;
    }

    private static boolean toggleRequested;

    /** Runs a requested toggle at the end of a client tick, after the chat screen that sent the command closed. */
    public static void tick() {
        if (pendingCommand != null) {
            String command = pendingCommand;
            pendingCommand = null;
            runLayoutCommand(command);
        }
        if (!toggleRequested) return;
        toggleRequested = false;
        toggle();
    }

    /**
     * Registers the single core command {@code /ktlayout} with its sub-commands (so the vanilla suggestions list them);
     * {@link #tick()} carries them out.
     */
    public static void install() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RegisterClientCommandsEvent event) -> {
            var root = net.minecraft.commands.Commands.literal("ktlayout").executes(context -> {
                toggleRequested = true;
                return 1;
            });
            root.then(net.minecraft.commands.Commands.literal("reset").executes(context -> queue("reset")));
            var position = com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, CANVAS_W);
            var size = com.mojang.brigadier.arguments.IntegerArgumentType.integer(MIN_SIZE, CANVAS_W);
            root.then(net.minecraft.commands.Commands.literal("set")
                    .then(net.minecraft.commands.Commands.argument("x", position)
                            .then(net.minecraft.commands.Commands.argument("y", position)
                                    .executes(context -> queue("set " + context.getArgument("x", Integer.class) + " " + context.getArgument("y", Integer.class)))
                                    .then(net.minecraft.commands.Commands.argument("width", size)
                                            .then(net.minecraft.commands.Commands.argument("height", size)
                                                    .executes(context -> queue("set " + context.getArgument("x", Integer.class) + " "
                                                            + context.getArgument("y", Integer.class) + " " + context.getArgument("width", Integer.class)
                                                            + " " + context.getArgument("height", Integer.class))))))));
            var align = net.minecraft.commands.Commands.literal("align");
            for (String edge : new String[]{"left", "right", "top", "bottom", "centerx", "centery"}) {
                align.then(net.minecraft.commands.Commands.literal(edge).executes(context -> queue("align " + edge)));
            }
            root.then(align);
            var same = net.minecraft.commands.Commands.literal("same");
            for (String what : new String[]{"width", "height", "size"}) {
                same.then(net.minecraft.commands.Commands.literal(what).executes(context -> queue("same " + what)));
            }
            root.then(same);
            var distribute = net.minecraft.commands.Commands.literal("distribute");
            for (String axis : new String[]{"horizontal", "vertical"}) {
                distribute.then(net.minecraft.commands.Commands.literal(axis).executes(context -> queue("distribute " + axis)));
            }
            root.then(distribute);
            event.getDispatcher().register(root);
        });
    }

    private static int queue(String command) {
        pendingCommand = command;
        return 1;
    }

    /** Switches edit mode on, or asks whether to save and switches it off. */
    public static void toggle() {
        if (!active) {
            active = true;
            clearSelection();
            UNDO.clear();
            int stale = LayoutOverrides.takeStaleCount();
            KineticOverlays.toast("kineticcore_layout_editor", stale > 0 ? Component.empty().append(tr("enabled")).append(" ").append(tr("stale", stale)) : tr("enabled"));
            return;
        }
        if (!LayoutOverrides.dirty()) {
            finish(tr("closed"));
            return;
        }
        Runnable save = () -> {
            try {
                LayoutOverrides.save();
                finish(tr("saved"));
            } catch (java.io.IOException failed) {
                KineticOverlays.toast("kineticcore_layout_editor", tr("save_failed", failed.getMessage()));
            }
        };
        Runnable discard = () -> {
            LayoutOverrides.discard();
            finish(tr("discarded"));
        };
        Screen current = Minecraft.getInstance().screen;
        int problems = current == null ? 0 : problems(current).size();
        Component message = problems > 0
                ? Component.empty().append(tr("save_message")).append(" ").append(tr("save_problems", problems))
                : tr("save_message");
        if (!KineticOverlays.openCurrentDialog(tr("save_title"), message, tr("yes"), tr("no"), save, discard)) {
            Minecraft minecraft = Minecraft.getInstance();
            Screen previous = minecraft.screen;
            minecraft.setScreen(new ConfirmScreen(accepted -> {
                minecraft.setScreen(previous);
                (accepted ? save : discard).run();
            }, tr("save_title"), message, tr("yes"), tr("no")));
        }
    }

    private static void finish(Component message) {
        active = false;
        clearSelection();
        dragEdges = -1;
        GUIDES.clear();
        UNDO.clear();
        KineticOverlays.toast("kineticcore_layout_editor", message);
        rebuild(Minecraft.getInstance().screen);
    }

    private static void rebuild(Screen screen) {
        if (screen instanceof dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen kinetic) kinetic.rebuildUi();
    }

    private static Component tr(String key, Object... args) {
        return KineticText.translatable("gui.kineticcore.layout_editor." + key, args);
    }

    // ---- items -------------------------------------------------------------------------------------------------

    private static int[] rect(AbstractWidget widget) {
        return new int[]{widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()};
    }

    private static void setRect(AbstractWidget widget, int[] rect) {
        widget.setX(rect[0]);
        widget.setY(rect[1]);
        widget.setWidth(rect[2]);
        widget.setHeight(rect[3]);
    }

    /** Regions first (they lie underneath), then the visible controls of the current page. */
    private static List<Item> items(Screen screen) {
        String page = pageId(screen);
        List<Item> items = new ArrayList<>();
        Map<String, int[]> regions = screen instanceof PageHost host && host.page() != null ? REGIONS.get(host.page()) : null;
        if (regions != null) {
            synchronized (regions) {
                for (Map.Entry<String, int[]> region : regions.entrySet()) {
                    items.add(new Item(REGION + region.getKey(), region.getValue(), regionRect(page, region.getKey(), region.getValue()), null));
                }
            }
        }
        for (var child : screen.children()) {
            if (!(child instanceof AbstractWidget widget) || !widget.visible) continue;
            Entry entry = ENTRIES.get(widget);
            if (entry != null && !entry.owned() && entry.page().equals(page)) items.add(new Item(entry.key(), entry.defaults(), rect(widget), widget));
        }
        return items;
    }

    private static Item item(Screen screen, String key) {
        if (key == null) return null;
        for (Item item : items(screen)) if (item.key().equals(key)) return item;
        return null;
    }

    private static boolean inside(int[] r, double x, double y) {
        return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
    }

    /** Region edges first (a control often fills its region), then controls, then the smallest region under the point. */
    private static Item itemAt(Screen screen, double x, double y) {
        List<Item> items = items(screen);
        for (Item item : items) if (item.region() && inside(item.rect(), x, y) && edgesAt(item.rect(), x, y) != 0) return item;
        for (int index = items.size() - 1; index >= 0; index--) {
            Item item = items.get(index);
            if (!item.region() && inside(item.rect(), x, y)) return item;
        }
        Item smallest = null;
        for (Item item : items) {
            if (item.region() && inside(item.rect(), x, y)
                    && (smallest == null || item.rect()[2] * item.rect()[3] < smallest.rect()[2] * smallest.rect()[3])) smallest = item;
        }
        return smallest;
    }

    private static int edgesAt(int[] r, double x, double y) {
        int edges = 0;
        if (x < r[0] + EDGE) edges |= EDGE_LEFT;
        if (x >= r[0] + r[2] - EDGE) edges |= EDGE_RIGHT;
        if (y < r[1] + EDGE) edges |= EDGE_TOP;
        if (y >= r[1] + r[3] - EDGE) edges |= EDGE_BOTTOM;
        return edges;
    }

    /** Stores an item's new rect; controls move at once, region changes rebuild the page once at the end. */
    private static boolean apply(Screen screen, Item item, int[] rect) {
        int[] clamped = clamp(rect);
        LayoutOverrides.put(pageId(screen), item.key(), clamped, item.defaults());
        if (item.region()) return true;
        setRect(item.widget(), clamped);
        return false;
    }

    private static void commit(Screen screen, Item item, int[] rect) {
        if (apply(screen, item, rect)) rebuild(screen);
    }

    private static int[] clamp(int[] rect) {
        int w = Math.max(MIN_SIZE, Math.min(CANVAS_W, rect[2]));
        int h = Math.max(MIN_SIZE, Math.min(CANVAS_H, rect[3]));
        int x = Math.max(0, Math.min(CANVAS_W - w, rect[0]));
        int y = Math.max(0, Math.min(CANVAS_H - h, rect[1]));
        return new int[]{x, y, w, h};
    }

    /** Starts one undo step; every item it touches is recorded with its rect before the step. */
    private static void beginUndo() {
        pendingUndo = new LinkedHashMap<>();
        UNDO.push(pendingUndo);
        while (UNDO.size() > 200) UNDO.removeLast();
    }

    private static void remember(Item item) {
        if (pendingUndo == null) beginUndo();
        pendingUndo.putIfAbsent(item.key(), item.rect().clone());
    }

    /** Snap lines: canvas margins and centre, plus the edges, centres and 4 px gaps of the page's other items. */
    private static List<Integer> lines(Screen screen, boolean vertical, List<String> skip) {
        List<Integer> lines = new ArrayList<>(List.of(MARGIN, vertical ? CANVAS_W - MARGIN : CANVAS_H - MARGIN, (vertical ? CANVAS_W : CANVAS_H) / 2));
        for (Item other : items(screen)) {
            if (skip.contains(other.key())) continue;
            int start = vertical ? other.rect()[0] : other.rect()[1];
            int size = vertical ? other.rect()[2] : other.rect()[3];
            lines.add(start);
            lines.add(start + size);
            lines.add(start + size / 2);
            // A 4 px gap to a neighbour is the house spacing, so its edges attract as well.
            lines.add(start - MARGIN);
            lines.add(start + size + MARGIN);
        }
        return lines;
    }

    /** Snaps the first matching position (within SNAP px) of any of the given offsets; returns the shift. */
    private static int snap(List<Integer> lines, int[] positions, boolean vertical) {
        int best = SNAP + 1, shift = 0, line = 0;
        for (int position : positions) {
            for (int candidate : lines) {
                int distance = Math.abs(candidate - position);
                if (distance < best) {
                    best = distance;
                    shift = candidate - position;
                    line = candidate;
                }
            }
        }
        if (best > SNAP) return 0;
        GUIDES.add(new int[]{vertical ? 1 : 0, line});
        return shift;
    }

    /**
     * The facing edges of other regions next to a dragged region edge (within {@value #LINK_GAP} px and overlapping
     * along the edge): they move with it, so dragging the line between two areas resizes both.
     */
    private static List<Link> linksFor(Screen screen, Item dragged, int edges) {
        if (!dragged.region() || edges == 0 || dev.xyat.kineticcore.api.runtime.KineticClientRuntime.controlModifierDown()) return List.of();
        int[] r = dragged.rect();
        List<Link> found = new ArrayList<>();
        for (Item other : items(screen)) {
            if (!other.region() || other.key().equals(dragged.key())) continue;
            int[] o = other.rect();
            boolean overlapY = o[1] < r[1] + r[3] && r[1] < o[1] + o[3];
            boolean overlapX = o[0] < r[0] + r[2] && r[0] < o[0] + o[2];
            int link = 0;
            if ((edges & EDGE_RIGHT) != 0 && overlapY && o[0] >= r[0] + r[2] && o[0] - (r[0] + r[2]) <= LINK_GAP) link |= EDGE_LEFT;
            if ((edges & EDGE_LEFT) != 0 && overlapY && o[0] + o[2] <= r[0] && r[0] - (o[0] + o[2]) <= LINK_GAP) link |= EDGE_RIGHT;
            if ((edges & EDGE_BOTTOM) != 0 && overlapX && o[1] >= r[1] + r[3] && o[1] - (r[1] + r[3]) <= LINK_GAP) link |= EDGE_TOP;
            if ((edges & EDGE_TOP) != 0 && overlapX && o[1] + o[3] <= r[1] && r[1] - (o[1] + o[3]) <= LINK_GAP) link |= EDGE_BOTTOM;
            if (link != 0) found.add(new Link(other.key(), link, o.clone()));
        }
        return found;
    }

    /** Moves the given edges of a rect by (dx, dy). */
    private static int[] moveEdges(int[] start, int edges, int dx, int dy) {
        int[] r = start.clone();
        if ((edges & EDGE_LEFT) != 0) { r[0] += dx; r[2] -= dx; }
        if ((edges & EDGE_RIGHT) != 0) r[2] += dx;
        if ((edges & EDGE_TOP) != 0) { r[1] += dy; r[3] -= dy; }
        if ((edges & EDGE_BOTTOM) != 0) r[3] += dy;
        return r;
    }

    /** Applies an edge move to the item and its linked neighbours; returns whether a rebuild is needed. */
    private static boolean resize(Screen screen, Item item, int[] start, int edges, int dx, int dy, List<Link> linked) {
        boolean rebuild = apply(screen, item, moveEdges(start, edges, dx, dy));
        for (Link link : linked) {
            Item other = item(screen, link.key());
            if (other == null) continue;
            remember(other);
            rebuild |= apply(screen, other, moveEdges(link.start(), link.edges(), dx, dy));
        }
        return rebuild;
    }

    // ---- input ---------------------------------------------------------------------------------------------------

    public static boolean mouseClicked(Screen screen, double x, double y, int button) {
        if (command != null && suggestions.mouseClicked(x, y, button)) return true;
        if (!active) return false;
        Item hit = itemAt(screen, x, y);
        if (button == 1) {
            if (hit != null) {
                // Right-click on one of several selected items restores all of them.
                List<Item> targets = SELECTION.contains(hit.key()) && SELECTION.size() > 1 ? selectedItems(screen) : List.of(hit);
                beginUndo();
                boolean rebuild = false;
                for (Item target : targets) {
                    remember(target);
                    rebuild |= apply(screen, target, target.defaults());
                }
                if (rebuild) rebuild(screen);
            }
            return true;
        }
        if (button != 0) return true;
        dragEdges = -1;
        boolean shift = dev.xyat.kineticcore.api.runtime.KineticClientRuntime.shiftModifierDown();
        if (hit == null) {
            // Empty space: a selection box (Shift keeps what is selected).
            lastClickKey = null;
            if (!shift) clearSelection();
            marquee = new int[]{(int) x, (int) y, (int) x, (int) y};
            return true;
        }
        if (shift) {
            if (!SELECTION.remove(hit.key())) {
                SELECTION.add(hit.key());
                selected = hit.key();
            } else if (hit.key().equals(selected)) {
                selected = SELECTION.isEmpty() ? null : SELECTION.iterator().next();
            }
            return true;
        }
        long now = System.currentTimeMillis();
        if (hit.key().equals(lastClickKey) && now - lastClickAt < 350
                && Math.abs(x - lastClickX) <= 3 && Math.abs(y - lastClickY) <= 3) {
            // Double-click: type the numbers instead of dragging.
            lastClickKey = null;
            int[] r = hit.rect();
            openCommand();
            command.setValue("/ktlayout set " + r[0] + " " + r[1] + " " + r[2] + " " + r[3]);
            return true;
        }
        lastClickKey = hit.key();
        lastClickAt = now;
        lastClickX = x;
        lastClickY = y;
        if (!SELECTION.contains(hit.key())) {
            SELECTION.clear();
            SELECTION.add(hit.key());
        }
        selected = hit.key();
        beginUndo();
        remember(hit);
        dragEdges = edgesAt(hit.rect(), x, y);
        arrowsResize = dragEdges > 0;
        if (dragEdges > 0) arrowEdges = dragEdges;
        links = linksFor(screen, hit, dragEdges);
        groupStart = new LinkedHashMap<>();
        if (dragEdges == 0) {
            for (Item item : selectedItems(screen)) {
                remember(item);
                groupStart.put(item.key(), item.rect().clone());
            }
        }
        startX = x;
        startY = y;
        startRect = hit.rect().clone();
        return true;
    }

    public static boolean mouseDragged(Screen screen, double x, double y, int button) {
        if (!active) return false;
        if (marquee != null) {
            marquee[2] = (int) x;
            marquee[3] = (int) y;
            return true;
        }
        Item item = item(screen, selected);
        if (item == null || dragEdges < 0 || button != 0) return true;
        int dx = (int) Math.round(x - startX), dy = (int) Math.round(y - startY);
        // A click that became a drag does not start a double-click.
        if (Math.abs(dx) > 2 || Math.abs(dy) > 2) lastClickKey = null;
        GUIDES.clear();
        boolean free = dev.xyat.kineticcore.api.runtime.KineticClientRuntime.altModifierDown();
        List<String> skip = new ArrayList<>(List.of(item.key()));
        for (Link link : links) skip.add(link.key());
        skip.addAll(groupStart.keySet());
        boolean rebuild;
        if (dragEdges == 0) {
            // The item under the pointer snaps; every other selected item moves by the same amount.
            int[] r = startRect.clone();
            r[0] += dx;
            r[1] += dy;
            if (!free) {
                r[0] += snap(lines(screen, true, skip), new int[]{r[0], r[0] + r[2], r[0] + r[2] / 2}, true);
                r[1] += snap(lines(screen, false, skip), new int[]{r[1], r[1] + r[3], r[1] + r[3] / 2}, false);
            }
            int moveX = r[0] - startRect[0], moveY = r[1] - startRect[1];
            rebuild = false;
            for (Map.Entry<String, int[]> entry : groupStart.entrySet()) {
                Item member = item(screen, entry.getKey());
                if (member == null) continue;
                int[] moved = entry.getValue().clone();
                moved[0] += moveX;
                moved[1] += moveY;
                rebuild |= apply(screen, member, moved);
            }
            if (!groupStart.containsKey(item.key())) rebuild |= apply(screen, item, r);
        } else {
            if (!free) {
                int[] moved = moveEdges(startRect, dragEdges, dx, dy);
                if ((dragEdges & (EDGE_LEFT | EDGE_RIGHT)) != 0) {
                    int edge = (dragEdges & EDGE_LEFT) != 0 ? moved[0] : moved[0] + moved[2];
                    dx += snap(lines(screen, true, skip), new int[]{edge}, true);
                }
                if ((dragEdges & (EDGE_TOP | EDGE_BOTTOM)) != 0) {
                    int edge = (dragEdges & EDGE_TOP) != 0 ? moved[1] : moved[1] + moved[3];
                    dy += snap(lines(screen, false, skip), new int[]{edge}, false);
                }
            }
            rebuild = resize(screen, item, startRect, dragEdges, dx, dy, links);
            sizeLabelUntil = System.currentTimeMillis() + 1500;
        }
        if (rebuild) rebuild(screen);
        return true;
    }

    public static boolean mouseReleased(double x, double y, int button) {
        if (!active) return false;
        if (marquee != null) {
            Screen screen = Minecraft.getInstance().screen;
            int left = Math.min(marquee[0], marquee[2]), right = Math.max(marquee[0], marquee[2]);
            int top = Math.min(marquee[1], marquee[3]), bottom = Math.max(marquee[1], marquee[3]);
            marquee = null;
            if (screen != null && right - left > 2 && bottom - top > 2) {
                // Items wholly inside the box join the selection.
                for (Item item : items(screen)) {
                    int[] r = item.rect();
                    if (r[0] >= left && r[1] >= top && r[0] + r[2] <= right && r[1] + r[3] <= bottom) {
                        SELECTION.add(item.key());
                        if (selected == null) selected = item.key();
                    }
                }
            }
        }
        dragEdges = -1;
        links = List.of();
        groupStart = Map.of();
        GUIDES.clear();
        return true;
    }

    public static boolean mouseScrolled(Screen screen, double x, double y, double delta) {
        if (command != null && suggestions.mouseScrolled(delta)) return true;
        if (!active) return false;
        Item target = itemAt(screen, x, y);
        if (target == null) return true;
        selectOnly(target.key());
        beginUndo();
        remember(target);
        int[] r = target.rect();
        double factor = delta > 0 ? 1.05 : 1 / 1.05;
        int w = Math.max(MIN_SIZE, (int) Math.round(r[2] * factor));
        int h = Math.max(MIN_SIZE, (int) Math.round(r[3] * factor));
        if (w == r[2]) w += delta > 0 ? 1 : -1;
        commit(screen, target, new int[]{r[0] - (w - r[2]) / 2, r[1] - (h - r[3]) / 2, w, h});
        sizeLabelUntil = System.currentTimeMillis() + 1500;
        return true;
    }

    private static boolean textInputFocused(Screen screen) {
        var focused = screen.getFocused();
        if (focused instanceof EditBox box) return box.isFocused();
        // Multi-line text areas and other text widgets: never steal their '/'.
        return focused instanceof AbstractWidget widget && widget.isFocused()
                && (widget.getClass().getName().contains("Edit") || widget.getClass().getName().contains("Text"));
    }

    public static boolean keyPressed(Screen screen, int key, int modifiers) {
        if (command != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) closeCommand();
            else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) runCommand();
            else if (!suggestions.keyPressed(key, 0, modifiers)) {
                dev.xyat.kineticcore.internal.client.gui.GuiInputCompat.keyPressed(command, key, 0, modifiers);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_SLASH && !textInputFocused(screen)) {
            openCommand();
            swallowSlash = true;
            return true;
        }
        if (!active) return false;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (control && key == GLFW.GLFW_KEY_Z) {
            // Each press undoes one step that actually changes something; steps that would change nothing are skipped.
            Map<String, int[]> undo;
            while ((undo = UNDO.poll()) != null) {
                boolean changes = false, rebuild = false;
                for (Map.Entry<String, int[]> entry : undo.entrySet()) {
                    Item item = item(screen, entry.getKey());
                    if (item == null || Arrays.equals(item.rect(), entry.getValue())) continue;
                    changes = true;
                    rebuild |= apply(screen, item, entry.getValue());
                }
                if (rebuild) rebuild(screen);
                if (changes) break;
            }
            pendingUndo = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && selected != null) {
            clearSelection();
            return true;
        }
        if (control && key == GLFW.GLFW_KEY_A) {
            // Every control of the page; regions are picked one by one.
            clearSelection();
            for (Item each : items(screen)) {
                if (each.region()) continue;
                SELECTION.add(each.key());
                if (selected == null) selected = each.key();
            }
            return true;
        }
        Item item = item(screen, selected);
        if (item == null) return key != GLFW.GLFW_KEY_ESCAPE;
        int step = shift ? 4 : 1;
        int dx = 0, dy = 0;
        if (key == GLFW.GLFW_KEY_LEFT) dx = -step;
        else if (key == GLFW.GLFW_KEY_RIGHT) dx = step;
        else if (key == GLFW.GLFW_KEY_UP) dy = -step;
        else if (key == GLFW.GLFW_KEY_DOWN) dy = step;
        else return true;
        beginUndo();
        remember(item);
        if (arrowsResize) {
            // The edges dragged last move; right and bottom by default.
            int edges = (arrowEdges & (EDGE_LEFT | EDGE_RIGHT)) == 0 ? EDGE_RIGHT : arrowEdges & (EDGE_LEFT | EDGE_RIGHT);
            edges |= (arrowEdges & (EDGE_TOP | EDGE_BOTTOM)) == 0 ? EDGE_BOTTOM : arrowEdges & (EDGE_TOP | EDGE_BOTTOM);
            int moving = (dx != 0 ? edges & (EDGE_LEFT | EDGE_RIGHT) : 0) | (dy != 0 ? edges & (EDGE_TOP | EDGE_BOTTOM) : 0);
            if (resize(screen, item, item.rect(), moving, dx, dy, linksFor(screen, item, moving))) rebuild(screen);
            sizeLabelUntil = System.currentTimeMillis() + 1500;
        } else {
            boolean rebuild = false;
            for (Item member : selectedItems(screen)) {
                remember(member);
                int[] r = member.rect().clone();
                r[0] += dx;
                r[1] += dy;
                rebuild |= apply(screen, member, r);
            }
            if (rebuild) rebuild(screen);
        }
        return true;
    }

    public static boolean charTyped(char character) {
        if (swallowSlash && character == '/') {
            swallowSlash = false;
            return true;
        }
        swallowSlash = false;
        if (command == null) return active;
        dev.xyat.kineticcore.internal.client.gui.GuiInputCompat.charTyped(command, character, 0);
        return true;
    }

    /** Whether the command line is open: it then receives every key and character before the page. */
    public static boolean commandLineOpen() {
        return command != null;
    }

    /** Opens the command line with "/" typed, like the vanilla chat after pressing '/'. */
    private static void openCommand() {
        command = new EditBox(Minecraft.getInstance().font, 7, CANVAS_H - 16, CANVAS_W - 14, 12, Component.empty());
        command.setMaxLength(256);
        command.setBordered(false);
        command.setValue("/");
        command.setFocused(true);
        suggestions = dev.xyat.kineticcore.internal.client.gui.command.KineticCommandSuggestions.create(command, CANVAS_W, CANVAS_H,
                dev.xyat.kineticcore.internal.client.gui.command.KineticCommandSuggestions.Options.fieldAligned(false, false, 10, 0xD0000000));
        suggestions.setAllowSuggestions(true);
        command.setResponder(value -> suggestions.update());
        suggestions.update();
    }

    private static void closeCommand() {
        command = null;
        suggestions = null;
    }

    /**
     * Runs the command line: {@code /ktlayout} toggles edit mode here; any other command is sent like from the chat,
     * so client and server commands work too.
     */
    private static void runCommand() {
        String text = command.getValue().trim();
        closeCommand();
        if (text.startsWith("/")) text = text.substring(1);
        if (text.isEmpty()) return;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.equals("ktlayout") || lower.equals("kt layout") || lower.equals("layout")) {
            toggle();
            return;
        }
        if (lower.startsWith("ktlayout ")) {
            runLayoutCommand(text.substring("ktlayout ".length()).trim());
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null) player.connection.sendCommand(text);
        else KineticOverlays.toast("kineticcore_layout_editor", tr("unknown_command", text));
    }

    // ---- selection and layout commands ----------------------------------------------------------------------------

    private static void clearSelection() {
        selected = null;
        SELECTION.clear();
        marquee = null;
    }

    private static void selectOnly(String key) {
        SELECTION.clear();
        SELECTION.add(key);
        selected = key;
    }

    /** The selected items still on the page, primary first. */
    private static List<Item> selectedItems(Screen screen) {
        List<Item> result = new ArrayList<>();
        Item primary = item(screen, selected);
        if (primary != null) result.add(primary);
        for (String key : SELECTION) {
            if (key.equals(selected)) continue;
            Item item = item(screen, key);
            if (item != null) result.add(item);
        }
        return result;
    }

    /**
     * Runs a {@code /ktlayout} sub-command on the current page: {@code reset}, {@code set x y [w h]},
     * {@code align left|right|top|bottom|centerx|centery}, {@code same width|height|size},
     * {@code distribute horizontal|vertical}.
     */
    private static void runLayoutCommand(String arguments) {
        String[] parts = arguments.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            toggle();
            return;
        }
        Screen screen = Minecraft.getInstance().screen;
        if (!active || !(screen instanceof dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen)) {
            KineticOverlays.toast("kineticcore_layout_editor", tr("needs_edit_mode"));
            return;
        }
        List<Item> targets = selectedItems(screen);
        Map<String, int[]> changes = new LinkedHashMap<>();
        switch (parts[0]) {
            case "reset" -> {
                for (Item item : items(screen)) changes.put(item.key(), item.defaults().clone());
            }
            case "set" -> {
                if (targets.isEmpty()) {
                    KineticOverlays.toast("kineticcore_layout_editor", tr("needs_selection"));
                    return;
                }
                int[] r = targets.get(0).rect().clone();
                try {
                    for (int i = 1; i < parts.length && i <= 4; i++) r[i - 1] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException invalid) {
                    KineticOverlays.toast("kineticcore_layout_editor", tr("unknown_command", "ktlayout " + arguments));
                    return;
                }
                changes.put(targets.get(0).key(), r);
            }
            case "align" -> {
                if (targets.isEmpty() || parts.length < 2) {
                    KineticOverlays.toast("kineticcore_layout_editor", tr("needs_selection"));
                    return;
                }
                // Several items line up with their common bounds; a single item with the canvas margins.
                int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
                if (targets.size() == 1) {
                    left = MARGIN;
                    top = MARGIN;
                    right = CANVAS_W - MARGIN;
                    bottom = CANVAS_H - MARGIN;
                } else {
                    for (Item item : targets) {
                        int[] r = item.rect();
                        left = Math.min(left, r[0]);
                        top = Math.min(top, r[1]);
                        right = Math.max(right, r[0] + r[2]);
                        bottom = Math.max(bottom, r[1] + r[3]);
                    }
                }
                for (Item item : targets) {
                    int[] r = item.rect().clone();
                    switch (parts[1]) {
                        case "left" -> r[0] = left;
                        case "right" -> r[0] = right - r[2];
                        case "top" -> r[1] = top;
                        case "bottom" -> r[1] = bottom - r[3];
                        case "centerx" -> r[0] = (left + right) / 2 - r[2] / 2;
                        case "centery" -> r[1] = (top + bottom) / 2 - r[3] / 2;
                        default -> { }
                    }
                    changes.put(item.key(), r);
                }
            }
            case "same" -> {
                if (targets.size() < 2 || parts.length < 2) {
                    KineticOverlays.toast("kineticcore_layout_editor", tr("needs_two"));
                    return;
                }
                // Everything takes the size of the primary item (the one clicked last).
                int[] model = targets.get(0).rect();
                for (Item item : targets.subList(1, targets.size())) {
                    int[] r = item.rect().clone();
                    if (!parts[1].equals("height")) r[2] = model[2];
                    if (!parts[1].equals("width")) r[3] = model[3];
                    changes.put(item.key(), r);
                }
            }
            case "distribute" -> {
                if (targets.size() < 3 || parts.length < 2) {
                    KineticOverlays.toast("kineticcore_layout_editor", tr("needs_three"));
                    return;
                }
                // Equal gaps between the outermost items, which stay where they are.
                int axis = parts[1].startsWith("v") ? 1 : 0;
                List<Item> sorted = new ArrayList<>(targets);
                sorted.sort(java.util.Comparator.comparingInt(item -> item.rect()[axis]));
                int start = sorted.get(0).rect()[axis];
                int[] last = sorted.get(sorted.size() - 1).rect();
                int end = last[axis] + last[axis + 2];
                int used = 0;
                for (Item item : sorted) used += item.rect()[axis + 2];
                double gap = (end - start - used) / (double) (sorted.size() - 1);
                double position = start;
                for (Item item : sorted) {
                    int[] r = item.rect().clone();
                    r[axis] = (int) Math.round(position);
                    position += r[axis + 2] + gap;
                    changes.put(item.key(), r);
                }
            }
            default -> {
                KineticOverlays.toast("kineticcore_layout_editor", tr("unknown_command", "ktlayout " + arguments));
                return;
            }
        }
        beginUndo();
        boolean rebuild = false;
        for (Map.Entry<String, int[]> change : changes.entrySet()) {
            Item item = item(screen, change.getKey());
            if (item == null) continue;
            remember(item);
            rebuild |= apply(screen, item, change.getValue());
        }
        if (rebuild) rebuild(screen);
    }

    /** A layout problem on the page: the item, the area to mark and what is wrong. */
    private record Problem(String key, int[] area, Component reason) {
    }

    /**
     * Problems shown while editing: controls overlapping each other, items outside the 4 px canvas margin and buttons
     * too narrow for their label. Only items moved or resized in the editor count, so the page's own layout is never
     * shown as a mistake of the player.
     */
    private static List<Problem> problems(Screen screen) {
        List<Problem> problems = new ArrayList<>();
        String page = pageId(screen);
        List<Item> items = items(screen);
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            boolean changed = LayoutOverrides.has(page, item.key());
            int[] r = item.rect();
            if (changed && (r[0] < MARGIN || r[1] < MARGIN || r[0] + r[2] > CANVAS_W - MARGIN || r[1] + r[3] > CANVAS_H - MARGIN)) {
                problems.add(new Problem(item.key(), r, tr("problem_margin")));
            }
            if (item.region()) continue;
            if (changed && item.widget() instanceof net.minecraft.client.gui.components.AbstractButton button
                    && button.getMessage() != null && !button.getMessage().getString().isBlank()
                    && Minecraft.getInstance().font.width(button.getMessage()) > r[2] - 8) {
                problems.add(new Problem(item.key(), r, tr("problem_squeezed")));
            }
            for (int j = i + 1; j < items.size(); j++) {
                Item other = items.get(j);
                if (other.region() || !changed && !LayoutOverrides.has(page, other.key())) continue;
                int[] o = other.rect();
                int left = Math.max(r[0], o[0]), top = Math.max(r[1], o[1]);
                int right = Math.min(r[0] + r[2], o[0] + o[2]), bottom = Math.min(r[1] + r[3], o[1] + o[3]);
                if (right > left && bottom > top) {
                    problems.add(new Problem(changed ? item.key() : other.key(), new int[]{left, top, right - left, bottom - top}, tr("problem_overlap")));
                }
            }
        }
        return problems;
    }

    // ---- drawing -------------------------------------------------------------------------------------------------

    /** Draws the edit overlay and the in-page command line over the page, in canvas coordinates. */
    public static void render(KineticGraphics g, Screen screen, int mouseX, int mouseY) {
        if (active) {
            String page = pageId(screen);
            Item hovered = itemAt(screen, mouseX, mouseY);
            Item chosen = null;
            for (Item item : items(screen)) {
                int[] r = item.rect();
                boolean changed = LayoutOverrides.has(page, item.key());
                if (item.key().equals(selected)) chosen = item;
                int color = SELECTION.contains(item.key()) ? 0xFFFFDD55 : changed ? 0xFF55FF88 : item.region() ? 0xFFFF9944 : 0xFF55DDFF;
                dashed(g, r[0], r[1], r[2], r[3], color);
                if (item.region()) {
                    // Regions carry their name inside the top-left corner, controls only when hovered.
                    String name = item.key().substring(REGION.length());
                    g.push();
                    g.raise(5);
                    g.fill(r[0] + 1, r[1] + 1, r[0] + 5 + g.textWidth(name), r[1] + 11, 0xC0402000);
                    g.text(name, r[0] + 3, r[1] + 2, 0xFFFFCC88, false);
                    g.pop();
                }
            }
            for (int[] guide : GUIDES) {
                if (guide[0] == 1) g.fill(guide[1], 0, guide[1] + 1, CANVAS_H, 0xC0FF55FF);
                else g.fill(0, guide[1], CANVAS_W, guide[1] + 1, 0xC0FF55FF);
            }
            // Problems: overlaps filled red, items outside the margin or with a squeezed label outlined red.
            List<Problem> problems = problems(screen);
            for (Problem problem : problems) {
                int[] a = problem.area();
                g.fill(a[0], a[1], a[0] + a[2], a[1] + a[3], 0x50FF3030);
                g.outline(a[0], a[1], a[2], a[3], 0xFFFF4040);
            }
            if (marquee != null) {
                int left = Math.min(marquee[0], marquee[2]), top = Math.min(marquee[1], marquee[3]);
                int width = Math.abs(marquee[2] - marquee[0]), height = Math.abs(marquee[3] - marquee[1]);
                g.fill(left, top, left + width, top + height, 0x2855AAFF);
                g.outline(left, top, width, height, 0xFF55AAFF);
            }
            if (chosen != null) {
                int[] r = chosen.rect();
                int x = r[0], y = r[1], w = r[2], h = r[3];
                for (int[] handle : new int[][]{{x, y}, {x + w, y}, {x, y + h}, {x + w, y + h}, {x + w / 2, y}, {x + w / 2, y + h}, {x, y + h / 2}, {x + w, y + h / 2}}) {
                    g.fill(handle[0] - 1, handle[1] - 1, handle[0] + 2, handle[1] + 2, 0xFFFFDD55);
                }
            }
            Item labelled = hovered != null ? hovered : chosen;
            if (labelled != null) {
                int[] r = labelled.rect();
                String size = r[2] + " × " + r[3];
                if (labelled.defaults()[2] > 0) size += "  " + Math.round(r[2] * 100.0 / labelled.defaults()[2]) + "%";
                boolean showSize = labelled.key().equals(selected) && System.currentTimeMillis() < sizeLabelUntil;
                String text = labelled.key() + "  " + r[0] + "," + r[1] + "  " + size;
                if (SELECTION.size() > 1 && SELECTION.contains(labelled.key())) text += "  " + tr("selected_count", SELECTION.size()).getString();
                for (Problem problem : problems) {
                    if (problem.key().equals(labelled.key())) text += "  ⚠ " + problem.reason().getString();
                }
                int width = g.textWidth(text) + 6;
                int x = Math.max(0, Math.min(CANVAS_W - width, r[0]));
                int y = r[1] >= 12 ? r[1] - 11 : Math.min(CANVAS_H - 11, r[1] + r[3] + 1);
                g.push();
                g.raise(10);
                g.fill(x, y, x + width, y + 11, showSize ? 0xE0303000 : 0xE0000000);
                g.text(text, x + 3, y + 2, showSize ? 0xFFFFDD55 : 0xFFFFFFFF, false);
                g.pop();
            }
        }
        if (command != null) {
            // The vanilla chat input look: a dark bar along the bottom, suggestions above it.
            int y = CANVAS_H - 18;
            g.push();
            g.raise(10);
            g.fill(4, y, CANVAS_W - 4, CANVAS_H - 4, 0xE0000000);
            var gui = dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter.unwrap(g);
            dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw.render(command, gui, mouseX, mouseY, 0F);
            suggestions.render(gui, mouseX, mouseY);
            g.pop();
        }
    }

    private static void dashed(KineticGraphics g, int x, int y, int w, int h, int color) {
        for (int i = 0; i < w; i += 4) {
            g.fill(x + i, y, x + Math.min(w, i + 2), y + 1, color);
            g.fill(x + i, y + h - 1, x + Math.min(w, i + 2), y + h, color);
        }
        for (int i = 0; i < h; i += 4) {
            g.fill(x, y + i, x + 1, y + Math.min(h, i + 2), color);
            g.fill(x + w - 1, y + i, x + w, y + Math.min(h, i + 2), color);
        }
    }
}
