package dev.xyat.kineticcore.api.client.gui.widget;

import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.render.structure.PreviewCamera;
import dev.xyat.kineticcore.internal.client.render.structure.PreviewOverlay;
import dev.xyat.kineticcore.internal.client.render.structure.StructurePreviewRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Interactive 3D view of a {@link KineticBlockSnapshot}, handled like a modelling program: drag with the left button
 * to turn it all the way round in any direction, with the right or middle button to move it, scroll to zoom toward
 * the pointer (zooming on goes into the structure, to look inside) and double-click empty space to reset. The camera
 * looks through a perspective lens. The snapshot's entities are drawn as their models. A large snapshot is meshed over
 * several frames; meanwhile, and while the snapshot itself is still arriving ({@link #setLoading(float)}), the view
 * shows a percentage. Call {@link #release()} when the screen closes so the GPU memory is freed at once.
 * <p>
 * {@link #setMarkers Markers} are boxes the pointer can pick, such as containers and entities; blocks in front of one
 * hide it. The one under the pointer is outlined and named in the view's bottom-left corner, the current one is
 * outlined yellow and other selected ones green, visible through blocks. A {@link PickListener} hears clicks (a press
 * and release without dragging) and double-clicks on them.
 */
public final class KineticStructureView extends KineticCustomControl {
    /** Turn around the vertical axis a new or reset view starts at, in degrees. */
    public static final float DEFAULT_YAW = 225F;
    /** Downward tilt a new or reset view starts at, in degrees. */
    public static final float DEFAULT_PITCH = 30F;
    private static final float MIN_ZOOM = 0.2F;
    /** Degrees the view turns per pixel dragged. */
    private static final float TURN_PER_PIXEL = 0.8F;
    private static final float WHEEL_STEP = 1.2F;
    /** How quickly a zoom eases to where the wheel put it: the share left after a second is e^-this. */
    private static final float EASE_RATE = 18F;
    private static final long DOUBLE_CLICK_NANOS = 300_000_000L;
    /** Size of the "?" badge in the top-right corner whose tooltip explains the controls: 2 px around the glyph. */
    private static final int HELP_W = 13, HELP_H = 14;
    /** Pointer travel, in view units, below which a press and release is a click rather than a drag. */
    private static final double CLICK_SLOP = 3D;
    /** How far from a marker the camera stops when sent to it, in cells. */
    private static final float LOOK_DISTANCE = 12F;
    /** Most markers outlined at once when all are shown. */
    private static final int MAX_SHOWN_MARKERS = 2048;
    /** Grows outlines a little past the faces they surround, so the faces do not cover them. */
    private static final float OUTLINE_PAD = 0.004F;
    private static final int HOVER_COLOR = 0xFFFFFFFF, SHOWN_COLOR = 0x80B8C2D0;
    private static final int GRID_COLOR = 0x12FFFFFF, FOOTPRINT_COLOR = 0x50FFFFFF;
    /** Side of the square the orientation gizmo is drawn in, in the bottom-right corner. */
    private static final int AXES_SIZE = 46;
    /** Half the side of the square around an axis letter that takes its click. */
    private static final int AXIS_HIT = 5;
    private static final int[] AXIS_COLORS = {0xFFFF6B6B, 0xFF7BD88F, 0xFF6FA8FF};
    private static final String[] AXIS_NAMES = {"X", "Y", "Z"};

    /**
     * Something in the view the pointer can pick, such as a container or an entity: a box in blocks, from the
     * snapshot's minimum corner, and the name shown while the pointer is on it. A {@code color} other than 0 (ARGB)
     * outlines it at all times, for example to show what has been set up.
     */
    public record Marker(String key, Component label, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                         int color) {
        public Marker {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
        }

        /** A marker outlined only while pointed at or selected. */
        public Marker(String key, Component label, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this(key, label, minX, minY, minZ, maxX, maxY, maxZ, 0);
        }
    }

    /** Hears clicks on the view. */
    public interface PickListener {
        /** A click without dragging, on a marker or on none ({@code null}); {@code input} carries the press's modifiers. */
        void clicked(@Nullable Marker marker, MouseInput input);

        /**
         * A click at the mouse press's camera position. An unobscured marker takes priority over its block, so at
         * most one of {@code marker} and {@code cell} is present. A cell is an index into the displayed snapshot
         * from its minimum corner. When {@code scale > 1}, it represents a region and does not identify an exact
         * block. A scale of zero means there is no snapshot. The default forwards to the original listener method.
         */
        default void clicked(@Nullable Marker marker, @Nullable BlockPos cell, int scale, MouseInput input) {
            clicked(marker, input);
        }

        /** A double-click on a marker. A double-click on no marker resets the view instead. */
        default void activated(Marker marker) {
        }
    }

    @Nullable private KineticBlockSnapshot snapshot;
    private float loading = -1F;
    @Nullable private Component message;
    private float yaw = DEFAULT_YAW, pitch = DEFAULT_PITCH;
    // Where the wheel and drags put the camera; the drawn camera eases toward it.
    private float zoom = 1F;
    @Nullable private Vector3f target;
    private float shownZoom = 1F;
    @Nullable private Vector3f shownTarget;
    private long lastFrame;
    private int cutY = Integer.MAX_VALUE;
    // A button went down on the view and has not come up yet.
    private boolean held;
    private boolean helpHovered;
    /** Where the gizmo's letters were last drawn, how far each axis points at the viewer, and the letter under the pointer. */
    private final int[][] axisLabels = new int[3][2];
    private final float[] axisDepth = new float[3];
    private boolean axesDrawn;
    private int axisHovered = -1, pressAxis = -1;
    // The left press that may still turn out to be a click: where, its angle, its marker and its modifiers.
    @Nullable private MouseInput press;
    private double pressX, pressY;
    private float pressYaw, pressPitch;
    private boolean pressMoved;
    @Nullable private Pick pressPick;
    @Nullable private KineticBlockSnapshot pressSnapshot;
    private long lastClick;
    @Nullable private String lastClickKey;

    private List<Marker> markers = List.of();
    @Nullable private String currentKey;
    private Set<String> selectedKeys = Set.of();
    private boolean markersShown;
    private boolean gridShown = true, axesShown = true;
    @Nullable private PickListener pickListener;
    @Nullable private Marker hovered;
    @Nullable private Component hoveredBlock;
    // The entities drawn: the snapshot's own unless set, made into models once per list.
    @Nullable private List<KineticBlockSnapshot.EntityEntry> entities;
    @Nullable private List<KineticBlockSnapshot.EntityEntry> figuresSource;
    private List<PreviewOverlay.Figure> figures = List.of();
    private PreviewOverlay overlay = PreviewOverlay.EMPTY;
    private boolean overlayChanged = true;

    /** An empty view; give it a snapshot with {@link #setSnapshot}. */
    public KineticStructureView(int x, int y, int width, int height) {
        super(x, y, width, height);
        // Only the corner badge explains the controls, so the hint never covers the structure being looked at.
        Component hint = KineticI18n.translatable("gui.kineticcore.structure_view.hint");
        Component pickHint = KineticI18n.translatable("gui.kineticcore.structure_view.hint_pick");
        setTooltip(() -> !helpHovered ? null : pickListener != null ? pickHint : hint);
    }

    /** Shows a snapshot, keeping the current angle and zoom; {@code null} clears the view. */
    public void setSnapshot(@Nullable KineticBlockSnapshot snapshot) {
        if (this.snapshot != null && this.snapshot != snapshot) StructurePreviewRuntime.release(this.snapshot);
        boolean resized = snapshot == null || this.snapshot == null || snapshot.sizeX() != this.snapshot.sizeX()
                || snapshot.sizeY() != this.snapshot.sizeY() || snapshot.sizeZ() != this.snapshot.sizeZ();
        this.snapshot = snapshot;
        // A point picked inside one structure means nothing in another.
        if (resized) {
            target = null;
            shownTarget = null;
        }
        if (snapshot != null) {
            loading = -1F;
            message = null;
        }
        overlayChanged = true;
    }

    /** The snapshot shown, or {@code null}. */
    @Nullable
    public KineticBlockSnapshot snapshot() {
        return snapshot;
    }

    /** Shows how much of a snapshot has arrived, from 0 to 1, until {@link #setSnapshot} is called. */
    public void setLoading(float share) {
        loading = Math.max(0F, Math.min(1F, share));
    }

    /** A line shown when there is nothing to draw, for example why a preview failed; {@code null} hides it. */
    public void setMessage(@Nullable Component message) {
        this.message = message;
        if (message != null) loading = -1F;
    }

    /** Hides cells at or above this height, to look inside; {@link Integer#MAX_VALUE} shows everything. */
    public void setCutY(int cutY) {
        this.cutY = Math.max(1, cutY);
    }

    /** Height from which cells are hidden; {@link Integer#MAX_VALUE} when none are. */
    public int cutY() {
        return cutY;
    }

    /**
     * The entities drawn, positioned in blocks from the snapshot's minimum corner, for example with data a screen has
     * changed; {@code null} draws the snapshot's own. Entities are drawn only while the snapshot is at full scale.
     */
    public void setEntities(@Nullable List<KineticBlockSnapshot.EntityEntry> entities) {
        this.entities = entities == null ? null : List.copyOf(entities);
        overlayChanged = true;
    }

    /** The boxes the pointer can pick; see {@link Marker}. */
    public void setMarkers(List<Marker> markers) {
        this.markers = List.copyOf(markers);
        if (hovered != null && !this.markers.contains(hovered)) hovered = null;
        overlayChanged = true;
    }

    /** The markers that can be picked. */
    public List<Marker> markers() {
        return markers;
    }

    /** Outlines {@code current} yellow and the other {@code selected} markers green; both may be empty. */
    public void setSelection(@Nullable String current, Collection<String> selected) {
        currentKey = current;
        selectedKeys = Set.copyOf(selected);
        overlayChanged = true;
    }

    /**
     * Whether every marker is outlined faintly, not only the one under the pointer and the selected ones; for a coarse
     * snapshot whose cells hide what the markers stand for.
     */
    public void setMarkersShown(boolean shown) {
        markersShown = shown;
        overlayChanged = true;
    }

    /** Whether the floor grid under the structure is drawn; it is by default. */
    public void setGridShown(boolean shown) {
        gridShown = shown;
        overlayChanged = true;
    }

    /** Whether the orientation gizmo (X, Y, Z) in the bottom-right corner is drawn; it is by default. */
    public void setAxesShown(boolean shown) {
        axesShown = shown;
    }

    /** Hears clicks and double-clicks on markers; {@code null} for none. */
    public void setPickListener(@Nullable PickListener listener) {
        pickListener = listener;
    }

    /** The marker under the pointer, or {@code null}. */
    @Nullable
    public Marker hoveredMarker() {
        return hovered;
    }

    /** The marker at a point of the view, given in the same units as the view's position, or {@code null}. */
    @Nullable
    public Marker markerAt(double mouseX, double mouseY) {
        Pick pick = pick(mouseX, mouseY);
        return pick == null ? null : pick.marker();
    }

    /**
     * The first visible block at a point in control units, relative to a full-resolution snapshot's minimum corner.
     * Returns {@code null} for air, an unobscured marker, or a downsampled snapshot. Use {@link #cellAt} to find
     * the corresponding region in a downsampled snapshot.
     */
    @Nullable
    public BlockPos blockAt(double mouseX, double mouseY) {
        return snapshot == null || snapshot.scale() != 1 ? null : cellAt(mouseX, mouseY);
    }

    /**
     * The first visible occupied cell at a point in control units, as an index in the displayed snapshot.
     * One cell spans {@link KineticBlockSnapshot#scale()} blocks along each axis. An unobscured marker has priority.
     */
    @Nullable
    public BlockPos cellAt(double mouseX, double mouseY) {
        if (mouseX < controlX() + 1 || mouseY < controlY() + 1
                || mouseX >= controlX() + controlWidth() - 1 || mouseY >= controlY() + controlHeight() - 1) return null;
        return cell(pick(mouseX, mouseY));
    }

    @Nullable
    private static BlockPos cell(@Nullable Pick pick) {
        if (pick == null || pick.cell() == null) return null;
        int[] hit = pick.cell();
        return new BlockPos(hit[0], hit[1], hit[2]);
    }

    /** Eases the camera to look at a point, in blocks from the snapshot's minimum corner, coming closer if it is far. */
    public void lookAt(double x, double y, double z) {
        if (snapshot == null) return;
        float scale = snapshot.scale();
        target = new Vector3f((float) (x / scale), (float) (y / scale), (float) (z / scale));
        if (shownTarget == null) shownTarget = middle();
        zoom = Math.max(zoom, Math.min(fit() / LOOK_DISTANCE, fit() / PreviewCamera.MIN_DISTANCE));
    }

    /**
     * Sets the angle and zoom, looking at the snapshot's middle, for example to restore a view a screen remembered.
     * Angles are in degrees and any value is taken; at zoom 1 the whole snapshot fits, larger values come closer.
     */
    public void setView(float yaw, float pitch, float zoom) {
        this.yaw = wrap(yaw);
        this.pitch = wrap(pitch);
        this.zoom = Math.max(MIN_ZOOM, zoom);
        shownZoom = this.zoom;
        target = null;
        shownTarget = null;
    }

    /** Turn around the vertical axis, in degrees from -180 to 180. */
    public float yaw() {
        return yaw;
    }

    /** Tilt, in degrees from -180 to 180; beyond ±90 the view is upside down. */
    public float pitch() {
        return pitch;
    }

    /** How close the camera is: 1 fits the whole snapshot, larger values are closer. */
    public float zoom() {
        return zoom;
    }

    /** Back to the default angle, zoom and position. */
    public void resetView() {
        setView(DEFAULT_YAW, DEFAULT_PITCH, 1F);
    }

    /** Frees the GPU memory held for the current snapshot. */
    public void release() {
        if (snapshot != null) StructurePreviewRuntime.release(snapshot);
    }

    @Override
    protected void render(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = controlX(), y = controlY(), width = controlWidth(), height = controlHeight();
        KineticTheme.Palette palette = KineticTheme.current();
        // A backdrop that is lighter at the top, as modelling programs have, so the structure reads against it.
        graphics.fillGradient(x, y, x + width, y + height, mix(palette.panelAlt(), 0xFFFFFFFF, 0.08F), mix(palette.panelAlt(), 0xFF000000, 0.30F));
        graphics.outline(x, y, width, height, controlHovered() ? palette.accentHover() : palette.border());
        int helpX = helpX(), helpY = helpY();
        helpHovered = !held && KineticTheme.hovering(mouseX, mouseY, helpX, helpY, HELP_W, HELP_H);
        Component status = message;
        boolean drawn = false;
        if (snapshot != null && status == null) {
            if (!StructurePreviewRuntime.supported()) {
                status = KineticI18n.translatable("gui.kineticcore.structure_view.unsupported");
            } else {
                ease();
                updateHover(mouseX, mouseY);
                Vector3f at = shownTarget != null ? shownTarget : middle();
                float built = StructurePreviewRuntime.draw(GuiGraphicsAdapter.unwrap(graphics), snapshot, cutY,
                        x + 1, y + 1, width - 2, height - 2, yaw, pitch, fit() / shownZoom, at.x, at.y, at.z, overlay());
                if (built < 1F) status = KineticI18n.translatable("gui.kineticcore.structure_view.building", percent(built));
                drawn = built >= 1F;
            }
        } else if (snapshot == null && status == null && loading >= 0F) {
            status = KineticI18n.translatable("gui.kineticcore.structure_view.loading", percent(loading));
        }
        if (status != null) {
            graphics.scrollingTextCentered(status, x + width / 2, y + (height - 8) / 2, width - 8, palette.mutedText(), false);
        }
        graphics.push();
        // Above the 3D picture, which 26.1 draws after everything else of its layer.
        graphics.raise(1);
        KineticTheme.stateSurface(graphics, helpX, helpY, HELP_W, HELP_H, KineticTheme.Surface.PANEL, false, helpHovered, false);
        graphics.text("?", helpX + 4, helpY + 3, helpHovered ? palette.text() : palette.mutedText(), false);
        Component name = !drawn ? null : hovered != null ? hovered.label() : hoveredBlock;
        if (name != null) {
            // What the pointer is on, in the bottom-left corner.
            int nameW = Math.min(graphics.textWidth(name) + 6, width - 6), nameY = y + height - 3 - 14;
            KineticTheme.surface(graphics, x + 3, nameY, nameW, 14, KineticTheme.Surface.PANEL);
            graphics.scrollingText(name, x + 6, nameY + 3, nameW - 6, hovered != null ? palette.text() : palette.mutedText(), false);
        }
        axesDrawn = drawn && axesShown;
        if (axesDrawn) axes(graphics, x + width - 3 - AXES_SIZE / 2, y + height - 3 - AXES_SIZE / 2, mouseX, mouseY);
        else axisHovered = -1;
        graphics.pop();
    }

    /**
     * The orientation gizmo: where the three axes point in this view, X red (east), Y green (up), Z blue (south), the
     * ones pointing away from the viewer drawn first. A click on a letter looks straight along its axis.
     */
    private void axes(KineticGraphics graphics, int centerX, int centerY, int mouseX, int mouseY) {
        float[][] tips = new float[3][];
        Integer[] order = {0, 1, 2};
        for (int axis = 0; axis < 3; axis++) {
            Vector3f tip = new Vector3f(axis == 0 ? 1F : 0F, axis == 1 ? 1F : 0F, axis == 2 ? 1F : 0F)
                    .rotateY((float) Math.toRadians(yaw)).rotateX((float) Math.toRadians(pitch));
            tips[axis] = new float[] {tip.x, tip.y, tip.z};
        }
        java.util.Arrays.sort(order, (a, b) -> Float.compare(tips[a][2], tips[b][2]));
        float reach = AXES_SIZE / 2F - 9F;
        int over = -1;
        for (int axis : order) {
            int tipX = centerX + Math.round(tips[axis][0] * reach), tipY = centerY - Math.round(tips[axis][1] * reach);
            // The letter sits past the tip, along the axis. An axis pointing straight at or away from the viewer has
            // no direction on the screen: its letter goes diagonally beside the middle, where no other axis runs.
            float length = (float) Math.hypot(tips[axis][0], tips[axis][1]);
            boolean flat = length >= 0.05F;
            axisLabels[axis][0] = flat ? tipX + Math.round(tips[axis][0] / length * 6F) : centerX - 5;
            axisLabels[axis][1] = flat ? tipY - Math.round(tips[axis][1] / length * 6F) : centerY + 5;
            axisDepth[axis] = tips[axis][2];
            // Of two letters under the pointer, the one drawn last, nearer the viewer, takes it.
            if (!held && Math.abs(mouseX - axisLabels[axis][0]) <= AXIS_HIT && Math.abs(mouseY - axisLabels[axis][1]) <= AXIS_HIT) over = axis;
        }
        axisHovered = over;
        for (int axis : order) {
            int tipX = centerX + Math.round(tips[axis][0] * reach), tipY = centerY - Math.round(tips[axis][1] * reach);
            line(graphics, centerX, centerY, tipX, tipY, AXIS_COLORS[axis]);
            graphics.text(AXIS_NAMES[axis], axisLabels[axis][0] - graphics.textWidth(AXIS_NAMES[axis]) / 2, axisLabels[axis][1] - 4,
                    axis == axisHovered ? 0xFFFFFFFF : AXIS_COLORS[axis], false);
        }
    }

    /** The gizmo letter at a point, or -1. */
    private int axisAt(double x, double y) {
        if (!axesDrawn) return -1;
        int found = -1;
        float depth = Float.NEGATIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(x - axisLabels[axis][0]) <= AXIS_HIT && Math.abs(y - axisLabels[axis][1]) <= AXIS_HIT && axisDepth[axis] > depth) {
                found = axis;
                depth = axisDepth[axis];
            }
        }
        return found;
    }

    /**
     * Looks straight along an axis: from east for X, from above for Y, from south for Z, or from the opposite side when
     * the view already looks that way. Zoom and position stay.
     */
    private void faceAxis(int axis) {
        boolean facing = axisDepth[axis] > 0.999F;
        if (axis == 1) {
            yaw = wrap(Math.round(yaw / 90F) * 90F);
            pitch = facing ? -90F : 90F;
        } else {
            yaw = axis == 0 ? (facing ? 90F : -90F) : (facing ? 180F : 0F);
            pitch = 0F;
        }
    }

    /** A one-pixel line between two points, a pixel at a time. */
    private static void line(KineticGraphics graphics, int fromX, int fromY, int toX, int toY, int argb) {
        int steps = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
        for (int i = 0; i <= steps; i++) {
            int px = steps == 0 ? fromX : fromX + Math.round((toX - fromX) * i / (float) steps);
            int py = steps == 0 ? fromY : fromY + Math.round((toY - fromY) * i / (float) steps);
            graphics.fill(px, py, px + 1, py + 1, argb);
        }
    }

    /** {@code argb} moved {@code share} of the way to {@code toward}, opaque. */
    private static int mix(int argb, int toward, float share) {
        int r = Math.round(((argb >> 16) & 0xFF) + (((toward >> 16) & 0xFF) - ((argb >> 16) & 0xFF)) * share);
        int g = Math.round(((argb >> 8) & 0xFF) + (((toward >> 8) & 0xFF) - ((argb >> 8) & 0xFF)) * share);
        int b = Math.round((argb & 0xFF) + ((toward & 0xFF) - (argb & 0xFF)) * share);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private int helpX() {
        return controlX() + controlWidth() - HELP_W - 3;
    }

    private int helpY() {
        return controlY() + 3;
    }

    @Override
    protected int[] tooltipBounds(double mouseX, double mouseY) {
        return new int[] {helpX(), helpY(), HELP_W, HELP_H};
    }

    /** Moves the drawn zoom and target a frame's share of the way to where the wheel put them. */
    private void ease() {
        long now = System.nanoTime();
        float seconds = lastFrame == 0L ? 1F : Math.min(1F, (now - lastFrame) / 1.0E9F);
        lastFrame = now;
        float share = 1F - (float) Math.exp(-EASE_RATE * seconds);
        shownZoom += (zoom - shownZoom) * share;
        if (Math.abs(zoom - shownZoom) < zoom * 1.0E-4F) shownZoom = zoom;
        if (target == null) {
            shownTarget = null;
        } else {
            if (shownTarget == null) shownTarget = middle();
            shownTarget.lerp(target, share);
        }
    }

    /** What the pointer is on, while it rests on the picture and no button is down. */
    private void updateHover(int mouseX, int mouseY) {
        Marker marker = null;
        Component block = null;
        if (!held && !helpHovered && axisHovered < 0 && controlHovered()) {
            Pick pick = pick(mouseX, mouseY);
            if (pick != null) {
                marker = pick.marker();
                if (marker == null && pick.cell() != null) block = blockName(pick.cell());
            }
        }
        if (!Objects.equals(marker, hovered)) overlayChanged = true;
        hovered = marker;
        hoveredBlock = block;
    }

    private Component blockName(int[] cell) {
        int scale = snapshot.scale();
        int index = snapshot.index(cell[0], cell[1], cell[2]);
        BlockState state = snapshot.state(cell[0], cell[1], cell[2]);
        Component name = snapshot.isMissing(index) ? Component.literal(snapshot.paletteName(index)) : state.getBlock().getName();
        return KineticI18n.translatable("gui.kineticcore.structure_view.at", name, cell[0] * scale, cell[1] * scale, cell[2] * scale);
    }

    /** The overlay for this frame: the same object until something in it changes. */
    private PreviewOverlay overlay() {
        List<KineticBlockSnapshot.EntityEntry> source = snapshot == null || snapshot.scale() != 1 ? List.of()
                : entities != null ? entities : snapshot.entities();
        if (source != figuresSource) {
            figuresSource = source;
            figures = PreviewOverlay.figures(source, figures);
            overlayChanged = true;
        }
        if (!overlayChanged) return overlay;
        overlayChanged = false;
        List<PreviewOverlay.Box> boxes = new ArrayList<>();
        if (gridShown && snapshot != null) floorGrid(boxes);
        Marker current = null;
        int shown = 0;
        for (Marker marker : markers) {
            if (marker.key().equals(currentKey)) {
                current = marker;
            } else if (selectedKeys.contains(marker.key())) {
                boxes.add(box(marker, KineticTheme.indicatorColor(KineticTheme.Indicator.SUCCESS)));
            } else if (marker.color() != 0) {
                boxes.add(box(marker, marker.color()));
            } else if (markersShown && shown < MAX_SHOWN_MARKERS) {
                boxes.add(box(marker, SHOWN_COLOR));
                shown++;
            }
        }
        if (current != null) boxes.add(box(current, KineticTheme.indicatorColor(KineticTheme.Indicator.WARNING)));
        if (hovered != null && hovered != current) boxes.add(box(hovered, HOVER_COLOR));
        overlay = boxes.isEmpty() && figures.isEmpty() ? PreviewOverlay.EMPTY : new PreviewOverlay(boxes, figures);
        return overlay;
    }

    /**
     * A grid on the floor the structure stands on, reaching a little past it on every side, with the structure's own
     * outline a shade brighter: it shows where the ground is and how large the structure is while the view is turned.
     */
    private void floorGrid(List<PreviewOverlay.Box> boxes) {
        int sizeX = snapshot.sizeX(), sizeZ = snapshot.sizeZ(), longest = Math.max(sizeX, sizeZ);
        int step = longest <= 32 ? 1 : longest <= 64 ? 2 : longest <= 128 ? 4 : longest <= 256 ? 8 : 16;
        // Never far past the structure: the camera's far plane ends a little beyond it.
        int margin = Math.min(16, Math.max(2 * step, Math.round(longest * 0.25F / step) * step));
        // A line is a box a fiftieth of a cell thick.
        float thin = 0.01F, y = -0.02F;
        for (int x = -margin; x <= sizeX + margin; x += step) {
            boxes.add(new PreviewOverlay.Box(x - thin, y - thin, -margin, x + thin, y + thin, sizeZ + margin, GRID_COLOR, false, true));
        }
        for (int z = -margin; z <= sizeZ + margin; z += step) {
            boxes.add(new PreviewOverlay.Box(-margin, y - thin, z - thin, sizeX + margin, y + thin, z + thin, GRID_COLOR, false, true));
        }
        boxes.add(new PreviewOverlay.Box(0F, y - thin, 0F, sizeX, y + thin, sizeZ, FOOTPRINT_COLOR, false, true));
    }

    private PreviewOverlay.Box box(Marker marker, int argb) {
        float scale = snapshot == null ? 1F : snapshot.scale();
        return new PreviewOverlay.Box((float) marker.minX() / scale - OUTLINE_PAD, (float) marker.minY() / scale - OUTLINE_PAD,
                (float) marker.minZ() / scale - OUTLINE_PAD, (float) marker.maxX() / scale + OUTLINE_PAD,
                (float) marker.maxY() / scale + OUTLINE_PAD, (float) marker.maxZ() / scale + OUTLINE_PAD, argb, true, false);
    }

    private static int percent(float share) {
        return Math.round(share * 100F);
    }

    /** The snapshot's middle, in cells. */
    private Vector3f middle() {
        return snapshot == null ? new Vector3f() : new Vector3f(snapshot.sizeX() / 2F, snapshot.sizeY() / 2F, snapshot.sizeZ() / 2F);
    }

    /** Camera distance at zoom 1 for this view's shape. */
    private float fit() {
        if (snapshot == null) return 1F;
        return PreviewCamera.fitDistance(snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeZ(),
                (controlWidth() - 2) / (float) Math.max(1, controlHeight() - 2));
    }

    // ---- picking ----

    /** What a ray through the pointer meets first: a marker, or else the cell of a block ({@code null} for neither). */
    private record Pick(@Nullable Marker marker, @Nullable int[] cell) {
    }

    @Nullable
    private Pick pick(double mouseX, double mouseY) {
        if (snapshot == null) return null;
        int width = controlWidth() - 2, height = controlHeight() - 2;
        if (width <= 0 || height <= 0) return null;
        // The drawn camera: the eased zoom and target.
        float tan = (float) Math.tan(Math.toRadians(PreviewCamera.FOV / 2F));
        float half = height / 2F;
        float dx = (float) (mouseX - (controlX() + 1 + width / 2D)) / half * tan;
        float dy = (float) ((controlY() + 1 + height / 2D) - mouseY) / half * tan;
        Vector3f direction = PreviewCamera.axis(yaw, pitch, dx, dy, -1F).normalize();
        Vector3f at = shownTarget != null ? shownTarget : middle();
        Vector3f eye = PreviewCamera.axis(yaw, pitch, 0F, 0F, fit() / shownZoom).add(at);
        int[] cell = new int[3];
        float blockAt = castBlocks(eye, direction, cell);
        Marker best = null;
        float bestAt = Float.POSITIVE_INFINITY;
        float scale = snapshot.scale();
        for (Marker marker : markers) {
            float[] range = slab(eye, direction, (float) marker.minX() / scale, (float) marker.minY() / scale, (float) marker.minZ() / scale,
                    (float) marker.maxX() / scale, (float) marker.maxY() / scale, (float) marker.maxZ() / scale);
            if (range == null) continue;
            float enter = Math.max(0F, range[0]);
            // A marker on a block (a chest) is entered where its own cell is.
            if (enter <= blockAt + 0.01F && enter < bestAt) {
                best = marker;
                bestAt = enter;
            }
        }
        if (best != null) return new Pick(best, null);
        return blockAt < Float.POSITIVE_INFINITY ? new Pick(null, cell) : null;
    }

    /**
     * How far along the ray the first cell with a block is, cells at or above the cut left out, writing the cell into
     * {@code hit}; infinity if there is none. Steps cell by cell through the snapshot's grid.
     */
    private float castBlocks(Vector3f eye, Vector3f direction, int[] hit) {
        int sizeX = snapshot.sizeX(), sizeY = Math.min(snapshot.sizeY(), cutY), sizeZ = snapshot.sizeZ();
        float[] range = slab(eye, direction, 0F, 0F, 0F, sizeX, sizeY, sizeZ);
        if (range == null) return Float.POSITIVE_INFINITY;
        float t = Math.max(0F, range[0]);
        float px = eye.x + direction.x * (t + 1.0E-4F), py = eye.y + direction.y * (t + 1.0E-4F), pz = eye.z + direction.z * (t + 1.0E-4F);
        int x = clamp((int) Math.floor(px), sizeX), y = clamp((int) Math.floor(py), sizeY), z = clamp((int) Math.floor(pz), sizeZ);
        int stepX = direction.x > 0 ? 1 : -1, stepY = direction.y > 0 ? 1 : -1, stepZ = direction.z > 0 ? 1 : -1;
        float deltaX = direction.x == 0 ? Float.POSITIVE_INFINITY : Math.abs(1F / direction.x);
        float deltaY = direction.y == 0 ? Float.POSITIVE_INFINITY : Math.abs(1F / direction.y);
        float deltaZ = direction.z == 0 ? Float.POSITIVE_INFINITY : Math.abs(1F / direction.z);
        float nextX = direction.x == 0 ? Float.POSITIVE_INFINITY : ((stepX > 0 ? x + 1 : x) - eye.x) / direction.x;
        float nextY = direction.y == 0 ? Float.POSITIVE_INFINITY : ((stepY > 0 ? y + 1 : y) - eye.y) / direction.y;
        float nextZ = direction.z == 0 ? Float.POSITIVE_INFINITY : ((stepZ > 0 ? z + 1 : z) - eye.z) / direction.z;
        for (int steps = sizeX + sizeY + sizeZ + 3; steps > 0; steps--) {
            if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) break;
            if (snapshot.index(x, y, z) != 0) {
                hit[0] = x;
                hit[1] = y;
                hit[2] = z;
                return t;
            }
            if (nextX <= nextY && nextX <= nextZ) {
                x += stepX;
                t = nextX;
                nextX += deltaX;
            } else if (nextY <= nextZ) {
                y += stepY;
                t = nextY;
                nextY += deltaY;
            } else {
                z += stepZ;
                t = nextZ;
                nextZ += deltaZ;
            }
        }
        return Float.POSITIVE_INFINITY;
    }

    private static int clamp(int value, int size) {
        return Math.max(0, Math.min(size - 1, value));
    }

    /** Where the ray enters and leaves a box, or {@code null} if it misses it or the box is behind. */
    @Nullable
    private static float[] slab(Vector3f eye, Vector3f direction, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float near = Float.NEGATIVE_INFINITY, far = Float.POSITIVE_INFINITY;
        float[] origin = {eye.x, eye.y, eye.z}, dir = {direction.x, direction.y, direction.z};
        float[] min = {minX, minY, minZ}, max = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(dir[axis]) < 1.0E-7F) {
                if (origin[axis] < min[axis] || origin[axis] > max[axis]) return null;
                continue;
            }
            float a = (min[axis] - origin[axis]) / dir[axis], b = (max[axis] - origin[axis]) / dir[axis];
            near = Math.max(near, Math.min(a, b));
            far = Math.min(far, Math.max(a, b));
        }
        return near <= far && far >= 0F ? new float[] {near, far} : null;
    }

    // ---- input ----

    @Override
    protected boolean onMouseClick(MouseInput input) {
        if (!input.inside(controlX(), controlY(), controlWidth(), controlHeight())) return false;
        if (!input.isLeft() && !input.isRight() && !input.isMiddle()) return false;
        held = true;
        if (input.isLeft()) {
            press = input;
            pressX = input.x();
            pressY = input.y();
            pressYaw = yaw;
            pressPitch = pitch;
            pressMoved = false;
            pressAxis = axisAt(input.x(), input.y());
            pressSnapshot = snapshot;
            pressPick = pressAxis >= 0 ? null : pick(input.x(), input.y());
        }
        return true;
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        held = false;
        MouseInput pressed = press;
        if (!input.isLeft() || pressed == null) return false;
        press = null;
        Pick clicked = pressSnapshot == snapshot ? pressPick : null;
        pressPick = null;
        pressSnapshot = null;
        if (pressMoved || Math.abs(input.x() - pressX) >= CLICK_SLOP || Math.abs(input.y() - pressY) >= CLICK_SLOP) {
            lastClick = 0L;
            return false;
        }
        // A click, not a drag: the little the pointer moved does not turn the view.
        yaw = pressYaw;
        pitch = pressPitch;
        if (pressAxis >= 0) {
            // A click on a letter of the gizmo turns the view; it picks nothing.
            faceAxis(pressAxis);
            pressAxis = -1;
            lastClick = 0L;
            return true;
        }
        long now = System.nanoTime();
        Marker pressMarker = clicked == null ? null : clicked.marker();
        String key = pressMarker == null ? null : pressMarker.key();
        boolean doubleClick = lastClick != 0L && now - lastClick < DOUBLE_CLICK_NANOS && Objects.equals(key, lastClickKey);
        lastClick = doubleClick ? 0L : now;
        lastClickKey = key;
        if (pickListener != null) pickListener.clicked(pressMarker, cell(clicked), snapshot == null ? 0 : snapshot.scale(), pressed);
        if (doubleClick) {
            if (pressMarker == null) resetView();
            else if (pickListener != null) pickListener.activated(pressMarker);
        }
        return true;
    }

    @Override
    protected boolean onMouseDrag(MouseDragInput input) {
        if (input.isLeft()) {
            if (press != null && (Math.abs(input.x() - pressX) >= CLICK_SLOP || Math.abs(input.y() - pressY) >= CLICK_SLOP)) pressMoved = true;
            pitch = wrap(pitch + (float) input.deltaY() * TURN_PER_PIXEL);
            // Upside down, the vertical axis points the other way; turning with the drag keeps following the mouse.
            float sense = Math.cos(Math.toRadians(pitch)) < 0 ? -1F : 1F;
            yaw = wrap(yaw + sense * (float) input.deltaX() * TURN_PER_PIXEL);
        } else if (snapshot != null) {
            // The structure follows the mouse at the target's depth.
            float units = PreviewCamera.unitsPerPixel(fit() / zoom, controlHeight() - 2);
            Vector3f move = PreviewCamera.axis(yaw, pitch, 1F, 0F, 0F).mul((float) -input.deltaX() * units)
                    .add(PreviewCamera.axis(yaw, pitch, 0F, 1F, 0F).mul((float) input.deltaY() * units));
            if (target == null) target = middle();
            target.add(move);
            if (shownTarget == null) shownTarget = middle();
            shownTarget.add(move);
        }
        return true;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        if (input.deltaY() == 0 || snapshot == null) return false;
        float fit = fit();
        float distance = fit / zoom;
        float units = PreviewCamera.unitsPerPixel(distance, controlHeight() - 2);
        // The point under the pointer at the target's depth, as an offset from the target.
        float offsetX = (float) (input.x() - (controlX() + controlWidth() / 2D)) * units;
        float offsetY = (float) (input.y() - (controlY() + controlHeight() / 2D)) * units;
        Vector3f pointer = PreviewCamera.axis(yaw, pitch, 1F, 0F, 0F).mul(offsetX)
                .add(PreviewCamera.axis(yaw, pitch, 0F, 1F, 0F).mul(-offsetY));
        if (target == null) target = middle();
        if (input.deltaY() > 0) {
            float closer = distance / WHEEL_STEP;
            if (closer >= PreviewCamera.MIN_DISTANCE) {
                // Zooming toward the pointer keeps the point under it in place.
                target.add(new Vector3f(pointer).mul(1F - closer / distance));
                zoom = fit / closer;
            } else {
                // As close as it comes: fly on along the ray through the pointer, into the structure.
                Vector3f ray = PreviewCamera.axis(yaw, pitch, 0F, 0F, -1F).mul(distance).add(pointer).normalize();
                target.add(ray.mul(Math.max(0.5F, distance * (WHEEL_STEP - 1F))));
                zoom = fit / PreviewCamera.MIN_DISTANCE;
            }
        } else {
            float farther = Math.min(distance * WHEEL_STEP, fit / MIN_ZOOM);
            target.add(new Vector3f(pointer).mul(1F - farther / distance));
            zoom = fit / farther;
        }
        return true;
    }

    /** The angle in degrees, from -180 to 180. */
    private static float wrap(float degrees) {
        float wrapped = degrees % 360F;
        if (wrapped > 180F) wrapped -= 360F;
        if (wrapped <= -180F) wrapped += 360F;
        return wrapped;
    }
}
