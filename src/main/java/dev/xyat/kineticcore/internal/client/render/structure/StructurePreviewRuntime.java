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
     * Draws a snapshot into a box, in the graphics' current coordinates, as {@link PreviewCamera} sees it: turned by
     * {@code yaw} and {@code pitch} (degrees), {@code distance} cells from the target point (in cells). Cells at or
     * above {@code cutY} are left out; {@code overlay} is drawn over the snapshot. Returns how far the mesh is built,
     * from 0 to 1; at 1 the snapshot was drawn.
     */
    public static float draw(GuiGraphics graphics, KineticBlockSnapshot snapshot, int cutY, int x, int y, int width,
                             int height, float yaw, float pitch, float distance, float targetX, float targetY, float targetZ,
                             PreviewOverlay overlay) {
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
        // The box in window pixels: the camera draws into it through a viewport of its own.
        var window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        Matrix4f pose = graphics.pose().last().pose();
        org.joml.Vector3f topLeft = pose.transformPosition(new org.joml.Vector3f(x, y, 0F));
        org.joml.Vector3f bottomRight = pose.transformPosition(new org.joml.Vector3f(x + width, y + height, 0F));
        int left = (int) Math.round(Math.min(topLeft.x, bottomRight.x) * guiScale);
        int right = (int) Math.round(Math.max(topLeft.x, bottomRight.x) * guiScale);
        int top = (int) Math.round(Math.min(topLeft.y, bottomRight.y) * guiScale);
        int bottom = (int) Math.round(Math.max(topLeft.y, bottomRight.y) * guiScale);
        if (right <= left || bottom <= top) return 1F;
        float sx = snapshot.sizeX(), sy = snapshot.sizeY(), sz = snapshot.sizeZ();
        Matrix4f view = PreviewCamera.view(yaw, pitch, distance, targetX, targetY, targetZ);
        Matrix4f projection = PreviewCamera.projection((right - left) / (float) (bottom - top), distance,
                PreviewCamera.reach(sx, sy, sz, targetX, targetY, targetZ));
        graphics.flush();
        KineticRenderRuntime.enableScissor(graphics, x, y, x + width, y + height);
        try {
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.viewport(left, window.getHeight() - bottom, right - left, bottom - top);
            RenderSystem.enableDepthTest();
            mesh.draw(graphics, view, projection);
            PreviewOverlayRenderer.draw(graphics, overlay, view, projection);
            // Later GUI drawing in this box must not be hidden behind the structure's depth.
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        } finally {
            RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
            KineticRenderRuntime.disableScissor(graphics);
        }
        return 1F;
        //?} else {
        /*if (snapshot == null || width <= 0 || height <= 0) return 0F;
        // 26.1 draws the preview into its own texture in screen units, so the box goes through the GUI pose first.
        var pose = graphics.pose();
        org.joml.Vector2f topLeft = pose.transformPosition(x, y, new org.joml.Vector2f());
        org.joml.Vector2f bottomRight = pose.transformPosition(x + width, y + height, new org.joml.Vector2f());
        PipAccess.submit(graphics, new StructurePreviewPip.State(snapshot, cutY, yaw, pitch, distance, targetX, targetY,
                targetZ, Math.round(topLeft.x), Math.round(topLeft.y), Math.round(bottomRight.x), Math.round(bottomRight.y),
                PipAccess.scissor(graphics), overlay));
        return StructurePreviewPip.progress(snapshot, cutY);
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

    /** How far the mesh of a snapshot and cross-section is built, from 0 to 1; 0 before it was first drawn. */
    public static float progress(KineticBlockSnapshot snapshot, int cutY) {
        //? if >=26.1 {
        /*return StructurePreviewPip.progress(snapshot, cutY);
        *///?} else {
        StructureMesh mesh = MESHES.get(new Key(snapshot, Math.max(0, cutY)));
        return mesh == null ? 0F : mesh.progress();
        //?}
    }

    /** Frees the meshes of a snapshot, for a screen that closes and will not show it again. */
    public static void release(KineticBlockSnapshot snapshot) {
        //? if >=26.1 {
        /*StructurePreviewPip.release(snapshot);
        *///?}
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
