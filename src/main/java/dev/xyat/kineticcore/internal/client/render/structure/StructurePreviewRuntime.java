package dev.xyat.kineticcore.internal.client.render.structure;

import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import net.minecraft.client.gui.GuiGraphics;
//? if <26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
//?}

/**
 * Draws block snapshots in the GUI for {@code KineticStructureView}. Meshes are cached per snapshot and cross-section
 * height (the six most recently drawn are kept; older ones free their GPU memory) and built a slice per frame, one
 * mesh at a time, so building never stalls the game.
 */
public final class StructurePreviewRuntime {
    /** Render-thread time one frame may spend building a mesh. */
    private static final long FRAME_BUDGET_NANOS = 6_000_000L;
    private static final int CACHED_MESHES = 6;
    //? if <26.1 {
    private static final Map<Key, StructureMesh> MESHES = new LinkedHashMap<>(16, 0.75F, true);
    private static final Map<StructureMesh, Long> LAST_DRAWN = new java.util.IdentityHashMap<>();
    private static StructureMesh building;
    //?}

    private StructurePreviewRuntime() {
    }

    /** Whether this game version can draw previews. */
    public static boolean supported() {
        return true;
    }

    /**
     * Draws a snapshot centred in a box, in the graphics' current coordinates. {@code yaw} and {@code pitch} are in
     * degrees, {@code zoom} multiplies the size that fits the box and {@code panX}/{@code panY} move it. Cells at or
     * above {@code cutY} are left out. Returns how far the mesh is built, from 0 to 1; at 1 the snapshot was drawn.
     */
    public static float draw(GuiGraphics graphics, KineticBlockSnapshot snapshot, int cutY, int x, int y, int width,
                             int height, float yaw, float pitch, float zoom, float panX, float panY) {
        //? if <26.1 {
        if (snapshot == null || width <= 0 || height <= 0) return 0F;
        StructureMesh mesh = mesh(snapshot, cutY);
        LAST_DRAWN.put(mesh, System.nanoTime());
        if (!mesh.ready()) {
            if (building != null && building != mesh && !building.ready() && stale(building)) {
                // The screen that wanted the other mesh is gone; let this one have the builders.
                drop(building);
            }
            if (building == null || building == mesh || building.ready()) {
                building = mesh;
                if (mesh.build(System.nanoTime() + FRAME_BUDGET_NANOS)) building = null;
            }
            if (!mesh.ready()) return mesh.progress();
        }
        float sx = snapshot.sizeX(), sy = snapshot.sizeY(), sz = snapshot.sizeZ();
        float half = (float) Math.sqrt(sx * sx + sy * sy + sz * sz) / 2F;
        float scale = fitScale(sx, sy, sz, width, height) * zoom;
        // Depth only orders faces in this orthographic view, so it is squeezed to stay inside the GUI depth range.
        float depth = Math.min(1F, 300F / Math.max(1F, scale * half));
        Matrix4f local = new Matrix4f()
                .translate(x + width / 2F + panX, y + height / 2F + panY, 400F)
                .scale(scale, -scale, scale * depth)
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw))
                .translate(-sx / 2F, -sy / 2F, -sz / 2F);
        Matrix4f model = new Matrix4f(graphics.pose().last().pose()).mul(local);
        graphics.flush();
        KineticRenderRuntime.enableScissor(graphics, x, y, x + width, y + height);
        try {
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableDepthTest();
            mesh.draw(graphics, model);
            // Later GUI drawing in this box must not be hidden behind the structure's depth.
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        } finally {
            KineticRenderRuntime.disableScissor(graphics);
        }
        return 1F;
        //?} else {
        /*if (snapshot == null || width <= 0 || height <= 0) return 0F;
        float sx = snapshot.sizeX(), sy = snapshot.sizeY(), sz = snapshot.sizeZ();
        float half = (float) Math.sqrt(sx * sx + sy * sy + sz * sz) / 2F;
        // 26.1 draws the preview into its own texture in screen units, so the box goes through the GUI pose first.
        var pose = graphics.pose();
        org.joml.Vector2f topLeft = pose.transformPosition(x, y, new org.joml.Vector2f());
        org.joml.Vector2f bottomRight = pose.transformPosition(x + width, y + height, new org.joml.Vector2f());
        float poseScale = (bottomRight.x - topLeft.x) / width;
        float blockScale = fitScale(sx, sy, sz, width, height) * zoom * poseScale;
        float guiScale = (float) net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float depth = Math.min(1F, 900F / Math.max(1F, blockScale * guiScale * half));
        PipAccess.submit(graphics, new StructurePreviewPip.State(snapshot, cutY, yaw, pitch, blockScale, depth,
                panX * poseScale, panY * poseScale, Math.round(topLeft.x), Math.round(topLeft.y),
                Math.round(bottomRight.x), Math.round(bottomRight.y), PipAccess.scissor(graphics)));
        return 1F;
        *///?}
    }

    //? if >=26.1 {
    /*// Hands picture-in-picture states to the GUI; the GUI keeps them in private fields with no public way in.
    private static final class PipAccess {
        private static java.lang.reflect.Field renderState, scissorStack;
        private static java.lang.reflect.Method add, peek;

        static void submit(GuiGraphics graphics, StructurePreviewPip.State state) {
            try {
                if (renderState == null) {
                    renderState = GuiGraphics.class.getDeclaredField("guiRenderState");
                    renderState.setAccessible(true);
                }
                Object target = renderState.get(graphics);
                if (add == null) {
                    add = target.getClass().getMethod("addPicturesInPictureState",
                            net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState.class);
                }
                add.invoke(target, state);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot submit a structure preview", e);
            }
        }

        @javax.annotation.Nullable
        static net.minecraft.client.gui.navigation.ScreenRectangle scissor(GuiGraphics graphics) {
            try {
                if (scissorStack == null) {
                    scissorStack = GuiGraphics.class.getDeclaredField("scissorStack");
                    scissorStack.setAccessible(true);
                }
                Object stack = scissorStack.get(graphics);
                if (peek == null) {
                    peek = stack.getClass().getDeclaredMethod("peek");
                    peek.setAccessible(true);
                }
                return (net.minecraft.client.gui.navigation.ScreenRectangle) peek.invoke(stack);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
    }
    *///?}

    /** Frees the meshes of a snapshot, for a screen that closes and will not show it again. */
    public static void release(KineticBlockSnapshot snapshot) {
        //? if <26.1 {
        for (Iterator<Map.Entry<Key, StructureMesh>> it = MESHES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Key, StructureMesh> entry = it.next();
            if (entry.getKey().snapshot == snapshot) {
                forget(entry.getValue());
                it.remove();
            }
        }
        //?}
    }

    /**
     * Scale at which the box, seen from the default angle, fills the view with a small margin. Fitting at one fixed
     * angle keeps the size steady while the view is turned.
     */
    static float fitScale(float sx, float sy, float sz, int width, int height) {
        org.joml.Matrix4f rotation = new org.joml.Matrix4f()
                .rotateX((float) Math.toRadians(dev.xyat.kineticcore.api.client.gui.widget.KineticStructureView.DEFAULT_PITCH))
                .rotateY((float) Math.toRadians(dev.xyat.kineticcore.api.client.gui.widget.KineticStructureView.DEFAULT_YAW));
        float maxX = 0.5F, maxY = 0.5F;
        org.joml.Vector3f corner = new org.joml.Vector3f();
        for (int i = 0; i < 8; i++) {
            corner.set(((i & 1) == 0 ? -sx : sx) / 2F, ((i & 2) == 0 ? -sy : sy) / 2F, ((i & 4) == 0 ? -sz : sz) / 2F);
            rotation.transformPosition(corner);
            maxX = Math.max(maxX, Math.abs(corner.x));
            maxY = Math.max(maxY, Math.abs(corner.y));
        }
        return Math.min(width / (2F * maxX), height / (2F * maxY)) * 0.92F;
    }

    //? if <26.1 {
    private static StructureMesh mesh(KineticBlockSnapshot snapshot, int cutY) {
        Key key = new Key(snapshot, cutY);
        StructureMesh mesh = MESHES.get(key);
        if (mesh != null) return mesh;
        mesh = new StructureMesh(snapshot, cutY);
        MESHES.put(key, mesh);
        while (MESHES.size() > CACHED_MESHES) {
            Iterator<Map.Entry<Key, StructureMesh>> eldest = MESHES.entrySet().iterator();
            forget(eldest.next().getValue());
            eldest.remove();
        }
        return mesh;
    }

    private static boolean stale(StructureMesh mesh) {
        Long drawn = LAST_DRAWN.get(mesh);
        return drawn == null || System.nanoTime() - drawn > 1_000_000_000L;
    }

    private static void drop(StructureMesh mesh) {
        MESHES.values().removeIf(cached -> cached == mesh);
        forget(mesh);
    }

    private static void forget(StructureMesh mesh) {
        mesh.release();
        LAST_DRAWN.remove(mesh);
        if (building == mesh) building = null;
    }

    /** Cache key: the snapshot by identity and the cross-section height. */
    private record Key(KineticBlockSnapshot snapshot, int cutY) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.snapshot == snapshot && key.cutY == cutY;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(snapshot) * 31 + cutY;
        }
    }
    //?}
}
