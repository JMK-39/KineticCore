package dev.xyat.kineticcore.internal.client.render;

//? if >=26.1 {
/*import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
*///?} else {
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

/** Internal low-level renderer used by the public entity-preview API. */
public final class KineticEntityPreviewRuntime {
    private KineticEntityPreviewRuntime() {
    }

    //? if >=26.1 {
    /*/^*
     * Draws one prepared entity centred in the box {@code x0,y0 - x1,y1}, given in the graphics' current coordinates.
     * 26.1 draws GUI entities into their own texture at screen coordinates, so the box and scale go through the pose
     * here. {@code scale} is GUI units per block and {@code fitHeight} the height, in blocks, that is centred.
     ^/
    public static void render(Entity entity, GuiGraphics graphics, int x0, int y0, int x1, int y1, float scale,
                              float fitHeight, float angle) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(graphics, "graphics");
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.getRenderer(entity).createRenderState(entity, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;

        Vector2f topLeft = graphics.pose().transformPosition(x0, y0, new Vector2f());
        Vector2f bottomRight = graphics.pose().transformPosition(x1, y1, new Vector2f());
        float poseScale = x1 > x0 ? (bottomRight.x - topLeft.x) / (x1 - x0) : 1.0F;
        Quaternionf rotation = Axis.ZP.rotationDegrees(180f)
                .mul(Axis.XP.rotationDegrees(-10f))
                .mul(Axis.YP.rotationDegrees(180f - angle));
        graphics.entity(state, scale * poseScale, new Vector3f(0.0F, fitHeight / 2.0F, 0.0F), rotation, null,
                Math.round(topLeft.x), Math.round(topLeft.y), Math.round(bottomRight.x), Math.round(bottomRight.y));
    }

    /^* Nothing to restore on 26.1; GUI entities are drawn later from their render state. ^/
    public static void restore() {
    }
    *///?} else {
    /** Renders one prepared entity using the active client entity dispatcher. */
    public static void render(Entity entity, GuiGraphics graphics) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(graphics, "graphics");
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Lighting.setupFor3DItems();
        RenderSystem.runAsFancy(() -> dispatcher.render(
                entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, graphics.pose(), graphics.bufferSource(), 15728880
        ));
        graphics.bufferSource().endBatch();
    }

    /** Restores the standard dispatcher shadow and 3D-item lighting state after preview rendering. */
    public static void restore() {
        Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(true);
        Lighting.setupFor3DItems();
    }
    //?}
}
