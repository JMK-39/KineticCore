package dev.xyat.kineticcore.internal.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

/** Internal low-level renderer used by the public entity-preview API. */
public final class KineticEntityPreviewRuntime {
    private KineticEntityPreviewRuntime() {
    }

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
}
