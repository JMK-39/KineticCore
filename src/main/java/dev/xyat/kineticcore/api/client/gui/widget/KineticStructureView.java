package dev.xyat.kineticcore.api.client.gui.widget;

import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.render.structure.StructurePreviewRuntime;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Interactive 3D view of a {@link KineticBlockSnapshot}: drag with the left button to turn it, with the right or
 * middle button to move it, scroll to zoom and double-click to reset. A large snapshot is meshed over several
 * frames; meanwhile, and while the snapshot itself is still arriving ({@link #setLoading(float)}), the view shows a
 * percentage. Call {@link #release()} when the screen closes so the GPU memory is freed at once.
 */
public final class KineticStructureView extends KineticCustomControl {
    /** Turn around the vertical axis a new or reset view starts at, in degrees. */
    public static final float DEFAULT_YAW = 225F;
    /** Downward tilt a new or reset view starts at, in degrees. */
    public static final float DEFAULT_PITCH = 30F;
    private static final float MIN_ZOOM = 0.2F, MAX_ZOOM = 16F;
    private static final long DOUBLE_CLICK_NANOS = 300_000_000L;

    @Nullable private KineticBlockSnapshot snapshot;
    private float loading = -1F;
    @Nullable private Component message;
    private float yaw = DEFAULT_YAW, pitch = DEFAULT_PITCH, zoom = 1F, panX, panY;
    private int cutY = Integer.MAX_VALUE;
    private long lastLeftClick;

    /** An empty view; give it a snapshot with {@link #setSnapshot}. */
    public KineticStructureView(int x, int y, int width, int height) {
        super(x, y, width, height);
        setTooltip(KineticI18n.translatable("gui.kineticcore.structure_view.hint"));
    }

    /** Shows a snapshot, keeping the current angle and zoom; {@code null} clears the view. */
    public void setSnapshot(@Nullable KineticBlockSnapshot snapshot) {
        if (this.snapshot != null && this.snapshot != snapshot) StructurePreviewRuntime.release(this.snapshot);
        this.snapshot = snapshot;
        if (snapshot != null) {
            loading = -1F;
            message = null;
        }
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
     * Sets the angle and zoom, for example to restore a view a screen remembered. {@code pitch} is kept between
     * -89 and 89 degrees and {@code zoom} between 0.2 and 16.
     */
    public void setView(float yaw, float pitch, float zoom) {
        this.yaw = yaw;
        this.pitch = Math.max(-89F, Math.min(89F, pitch));
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    /** Back to the default angle, zoom and position. */
    public void resetView() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        zoom = 1F;
        panX = 0F;
        panY = 0F;
    }

    /** Frees the GPU memory held for the current snapshot. */
    public void release() {
        if (snapshot != null) StructurePreviewRuntime.release(snapshot);
    }

    @Override
    protected void render(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = controlX(), y = controlY(), width = controlWidth(), height = controlHeight();
        KineticTheme.Palette palette = KineticTheme.current();
        graphics.fill(x, y, x + width, y + height, palette.panelAlt());
        graphics.outline(x, y, width, height, controlHovered() ? palette.accentHover() : palette.border());
        Component status = message;
        if (snapshot != null && status == null) {
            if (!StructurePreviewRuntime.supported()) {
                status = KineticI18n.translatable("gui.kineticcore.structure_view.unsupported");
            } else {
                float built = StructurePreviewRuntime.draw(GuiGraphicsAdapter.unwrap(graphics), snapshot, cutY,
                        x + 1, y + 1, width - 2, height - 2, yaw, pitch, zoom, panX, panY);
                if (built < 1F) status = KineticI18n.translatable("gui.kineticcore.structure_view.building", percent(built));
            }
        } else if (snapshot == null && status == null && loading >= 0F) {
            status = KineticI18n.translatable("gui.kineticcore.structure_view.loading", percent(loading));
        }
        if (status != null) {
            graphics.scrollingTextCentered(status, x + width / 2, y + (height - 8) / 2, width - 8, palette.mutedText(), false);
        }
    }

    private static int percent(float share) {
        return Math.round(share * 100F);
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        if (!input.inside(controlX(), controlY(), controlWidth(), controlHeight())) return false;
        if (input.isLeft()) {
            long now = System.nanoTime();
            if (now - lastLeftClick < DOUBLE_CLICK_NANOS) {
                resetView();
                lastLeftClick = 0L;
            } else {
                lastLeftClick = now;
            }
        }
        return input.isLeft() || input.isRight() || input.isMiddle();
    }

    @Override
    protected boolean onMouseDrag(MouseDragInput input) {
        if (input.isLeft()) {
            yaw += (float) input.deltaX() * 0.8F;
            pitch = Math.max(-89F, Math.min(89F, pitch + (float) input.deltaY() * 0.8F));
        } else {
            panX += (float) input.deltaX();
            panY += (float) input.deltaY();
        }
        return true;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        if (input.deltaY() == 0) return false;
        float factor = input.deltaY() > 0 ? 1.15F : 1F / 1.15F;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        return true;
    }
}
