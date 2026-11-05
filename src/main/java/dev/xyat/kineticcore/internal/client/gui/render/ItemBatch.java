package dev.xyat.kineticcore.internal.client.gui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws many item icons together. Up to 1.21.5 {@code GuiGraphics.renderItem} sends every icon to the GPU on its own,
 * so a grid of a few hundred items costs a few hundred draw calls a frame; inside {@link #run} the icons are collected
 * and drawn in two passes (flat and 3D lighting), each sent at once, then their decorations. Since 1.21.6 the game
 * collects GUI items itself, and this simply draws them.
 *
 * <p>The icons draw when the batch ends, in front of what was drawn in the batch (they sit at the vanilla item depth),
 * so a batch must end inside the scissor that clips them.</p>
 */
public final class ItemBatch {
    private record Entry(ItemStack stack, org.joml.Matrix4f pose, boolean decorations) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static int depth;

    private ItemBatch() {
    }

    /** Runs {@code draw}; items it draws through the Kinetic theme are drawn together when it returns. */
    public static void run(GuiGraphics graphics, Font font, Runnable draw) {
        //? if >=26.1 {
        /*draw.run();
        *///?} else {
        if (depth > 0) {
            draw.run();
            return;
        }
        depth++;
        try {
            draw.run();
        } finally {
            depth--;
            flush(graphics, font);
        }
        //?}
    }

    /** Queues an icon whose 16 x 16 square starts at the current pose's origin; false when no batch runs. */
    public static boolean queue(GuiGraphics graphics, ItemStack stack, boolean decorations) {
        //? if >=26.1 {
        /*return false;
        *///?} else {
        if (depth == 0) return false;
        ENTRIES.add(new Entry(stack, new org.joml.Matrix4f(graphics.pose().last().pose()), decorations));
        return true;
        //?}
    }

    //? if <26.1 {
    private static void flush(GuiGraphics graphics, Font font) {
        if (ENTRIES.isEmpty()) return;
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var renderer = minecraft.getItemRenderer();
        List<Entry> entries = new ArrayList<>(ENTRIES);
        ENTRIES.clear();
        var models = new net.minecraft.client.resources.model.BakedModel[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            try {
                models[i] = renderer.getModel(entries.get(i).stack(), minecraft.level, minecraft.player, 0);
            } catch (RuntimeException broken) {
                models[i] = null;
            }
        }
        graphics.flush();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        try {
            // Flat (item-sprite) lighting first, then block lighting: one GPU submission each instead of one per icon.
            com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
            for (int i = 0; i < entries.size(); i++) {
                if (models[i] != null && !models[i].usesBlockLight()) draw(graphics, renderer, entries.get(i), models[i]);
            }
            graphics.flush();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            for (int i = 0; i < entries.size(); i++) {
                if (models[i] != null && models[i].usesBlockLight()) draw(graphics, renderer, entries.get(i), models[i]);
            }
            graphics.flush();
            if (font != null) {
                for (Entry entry : entries) {
                    if (!entry.decorations()) continue;
                    VanillaGuiDraw.push(graphics);
                    try {
                        graphics.pose().last().pose().set(entry.pose());
                        VanillaGuiDraw.itemDecorations(graphics, font, entry.stack(), 0, 0);
                    } finally {
                        VanillaGuiDraw.pop(graphics);
                    }
                }
            }
        } finally {
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        }
    }

    /** The vanilla GUI item transform, without the per-icon flush. */
    private static void draw(GuiGraphics graphics, net.minecraft.client.renderer.entity.ItemRenderer renderer, Entry entry,
                             net.minecraft.client.resources.model.BakedModel model) {
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.last().pose().set(entry.pose());
        pose.translate(8F, 8F, 150F);
        pose.last().pose().mul(new org.joml.Matrix4f().scaling(1F, -1F, 1F));
        pose.scale(16F, 16F, 16F);
        try {
            renderer.render(entry.stack(), net.minecraft.world.item.ItemDisplayContext.GUI, false, pose, graphics.bufferSource(),
                    15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, model);
        } catch (RuntimeException broken) {
            // One broken item model must not take the rest of the grid with it.
        }
    }
    //?}
}
