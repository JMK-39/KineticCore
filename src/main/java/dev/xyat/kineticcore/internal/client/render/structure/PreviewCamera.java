package dev.xyat.kineticcore.internal.client.render.structure;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The structure preview's camera, as in a modelling program: it circles a target point (the snapshot's middle until
 * it is moved) at a distance, turned by yaw about the vertical and by pitch about the screen's horizontal axis, with
 * no limit either way, and looks through a perspective lens whose near plane is a few hundredths of a block away, so
 * it can come up close and go inside. Lengths are in snapshot cells.
 */
public final class PreviewCamera {
    /** Vertical field of view, in degrees. */
    public static final float FOV = 50F;
    /** Closest the camera comes to its target; zooming in further moves the target forward instead. */
    public static final float MIN_DISTANCE = 0.75F;

    private PreviewCamera() {
    }

    /**
     * Distance at which the snapshot, seen from the default angle, fills a view of this aspect (width over height) with
     * a small margin. Fitting at one fixed angle keeps the size steady while the view is turned.
     */
    public static float fitDistance(float sizeX, float sizeY, float sizeZ, float aspect) {
        float tanY = (float) Math.tan(Math.toRadians(FOV / 2F));
        float tanX = tanY * Math.max(0.05F, aspect);
        Matrix4f turn = new Matrix4f()
                .rotateX((float) Math.toRadians(dev.xyat.kineticcore.api.client.gui.widget.KineticStructureView.DEFAULT_PITCH))
                .rotateY((float) Math.toRadians(dev.xyat.kineticcore.api.client.gui.widget.KineticStructureView.DEFAULT_YAW));
        float distance = MIN_DISTANCE;
        Vector3f corner = new Vector3f();
        for (int i = 0; i < 8; i++) {
            corner.set(((i & 1) == 0 ? -sizeX : sizeX) / 2F, ((i & 2) == 0 ? -sizeY : sizeY) / 2F, ((i & 4) == 0 ? -sizeZ : sizeZ) / 2F);
            turn.transformPosition(corner);
            // A corner nearer the camera (larger z) needs it farther back to stay inside the lens.
            distance = Math.max(distance, corner.z + Math.max(Math.abs(corner.x) / tanX, Math.abs(corner.y) / tanY));
        }
        return distance * 1.06F;
    }

    /** Half the snapshot's diagonal. */
    public static float radius(float sizeX, float sizeY, float sizeZ) {
        return (float) Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ) / 2F;
    }

    /** Maps snapshot cells into the camera's space (x right, y up, looking down -z). */
    public static Matrix4f view(float yaw, float pitch, float distance, float targetX, float targetY, float targetZ) {
        return new Matrix4f()
                .translate(0F, 0F, -distance)
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw))
                .translate(-targetX, -targetY, -targetZ);
    }

    /**
     * The lens for a view of this aspect. The near plane shrinks as the camera comes closer, so nothing in front of it
     * is cut away; the far plane reaches past the far side of the snapshot from wherever the camera is.
     */
    public static Matrix4f projection(float aspect, float distance, float reach) {
        float near = Math.max(0.05F, Math.min(1F, distance * 0.02F));
        // Past the far side of the snapshot and the floor grid around it.
        float far = Math.max(near * 2F, distance + reach + 40F);
        return new Matrix4f().perspective((float) Math.toRadians(FOV), Math.max(0.05F, aspect), near, far);
    }

    /** How far past the target the far plane must reach: the target's distance from the middle plus the radius. */
    public static float reach(float sizeX, float sizeY, float sizeZ, float targetX, float targetY, float targetZ) {
        float dx = targetX - sizeX / 2F, dy = targetY - sizeY / 2F, dz = targetZ - sizeZ / 2F;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz) + radius(sizeX, sizeY, sizeZ);
    }

    /** A direction given in the camera's space (for example 1, 0, 0 for its right), in snapshot cells. */
    public static Vector3f axis(float yaw, float pitch, float x, float y, float z) {
        return new Vector3f(x, y, z)
                .rotateX((float) -Math.toRadians(pitch))
                .rotateY((float) -Math.toRadians(yaw));
    }

    /** Snapshot cells per view unit at the target's distance, for a view {@code viewHeight} units tall. */
    public static float unitsPerPixel(float distance, float viewHeight) {
        return 2F * distance * (float) Math.tan(Math.toRadians(FOV / 2F)) / Math.max(1F, viewHeight);
    }
}
