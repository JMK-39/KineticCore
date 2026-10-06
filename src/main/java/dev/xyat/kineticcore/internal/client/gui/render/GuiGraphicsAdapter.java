package dev.xyat.kineticcore.internal.client.gui.render;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.render.KineticTexture;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.text.KineticText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.20.1 implementation of {@link KineticGraphics} over a vanilla {@link GuiGraphics}. This is the single place
 * that must change when the vanilla GUI pipeline changes (e.g. GuiGraphicsExtractor in 26.1).
 */
public final class GuiGraphicsAdapter implements KineticGraphics {
    /** Depth added per {@link #raise(int)} step; one step clears an item icon and its decorations. */
    public static final int RAISE_STEP_DEPTH = 250;

    private static final Map<KineticTexture, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();

    private final GuiGraphics graphics;

    private GuiGraphicsAdapter(GuiGraphics graphics) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
    }

    /** Wraps a vanilla graphics context. */
    public static KineticGraphics wrap(GuiGraphics graphics) {
        return new GuiGraphicsAdapter(graphics);
    }

    /** Returns the vanilla graphics context behind a Kinetic graphics surface. */
    public static GuiGraphics unwrap(KineticGraphics graphics) {
        if (graphics instanceof GuiGraphicsAdapter adapter) return adapter.graphics;
        throw new IllegalArgumentException("Unsupported KineticGraphics implementation: " + graphics);
    }

    /** Resolves a Kinetic texture to its vanilla resource location. */
    public static ResourceLocation location(KineticTexture texture) {
        return TEXTURES.computeIfAbsent(texture, key -> ResourceLocation.fromNamespaceAndPath(key.namespace(), key.path()));
    }

    private static Font font() {
        return KineticClientRuntimeImpl.font();
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        graphics.fill(x1, y1, x2, y2, argb);
    }

    @Override
    public void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb) {
        graphics.fillGradient(x1, y1, x2, y2, topArgb, bottomArgb);
    }

    @Override
    public void outline(int x, int y, int width, int height, int argb) {
        VanillaGuiDraw.outline(graphics, x, y, width, height, argb);
    }

    @Override
    public int text(String text, int x, int y, int argb, boolean shadow) {
        return VanillaGuiDraw.text(graphics, font(), text == null ? "" : text, x, y, argb, shadow);
    }

    @Override
    public int text(Component text, int x, int y, int argb, boolean shadow) {
        return VanillaGuiDraw.text(graphics, font(), text == null ? Component.empty() : text, x, y, argb, shadow);
    }

    @Override
    public int text(FormattedCharSequence text, int x, int y, int argb, boolean shadow) {
        return VanillaGuiDraw.text(graphics, font(), text == null ? FormattedCharSequence.EMPTY : text, x, y, argb, shadow);
    }

    @Override
    public void centeredText(Component text, int centerX, int y, int argb, boolean shadow) {
        Component safe = text == null ? Component.empty() : text;
        VanillaGuiDraw.text(graphics, font(), safe, centerX - font().width(safe) / 2, y, argb, shadow);
    }

    @Override
    public void centeredText(String text, int centerX, int y, int argb, boolean shadow) {
        String safe = text == null ? "" : text;
        VanillaGuiDraw.text(graphics, font(), safe, centerX - font().width(safe) / 2, y, argb, shadow);
    }

    @Override
    public void wrappedText(Component text, int x, int y, int maxWidth, int argb) {
        VanillaGuiDraw.wordWrap(graphics, font(), text == null ? Component.empty() : text, x, y, Math.max(1, maxWidth), argb);
    }

    @Override
    public int scrollingText(Component text, int x, int y, int maxWidth, int argb, boolean shadow) {
        return KineticText.drawScrollingLeft(graphics, font(), text, x, y, maxWidth, argb, shadow);
    }

    @Override
    public int scrollingTextCentered(Component text, int centerX, int y, int maxWidth, int argb, boolean shadow) {
        return KineticText.drawScrollingCentered(graphics, font(), text, centerX, y, maxWidth, argb, shadow);
    }

    @Override
    public int scrollingTextRight(Component text, int rightX, int y, int maxWidth, int argb, boolean shadow) {
        return KineticText.drawScrollingRight(graphics, font(), text, rightX, y, maxWidth, argb, shadow);
    }

    @Override
    public void item(ItemStack stack, int x, int y) {
        if (stack != null && !stack.isEmpty()) VanillaGuiDraw.item(graphics, stack, x, y);
    }

    @Override
    public void item(ItemStack stack, int x, int y, float alpha) {
        if (stack == null || stack.isEmpty()) return;
        float safeAlpha = Float.isFinite(alpha) ? Math.max(0.0F, Math.min(1.0F, alpha)) : 1.0F;
        if (safeAlpha <= 0.0F) return;
        VanillaGuiDraw.item(graphics, stack, x, y, safeAlpha);
    }

    @Override
    public void fakeItem(ItemStack stack, int x, int y) {
        if (stack != null && !stack.isEmpty()) VanillaGuiDraw.fakeItem(graphics, stack, x, y);
    }

    @Override
    public void itemDecorations(ItemStack stack, int x, int y) {
        if (stack != null && !stack.isEmpty()) VanillaGuiDraw.itemDecorations(graphics, font(), stack, x, y);
    }

    @Override
    public void itemDecorations(ItemStack stack, int x, int y, String countText) {
        if (stack != null && !stack.isEmpty()) VanillaGuiDraw.itemDecorations(graphics, font(), stack, x, y, countText);
    }

    @Override
    public void texture(KineticTexture texture, int x, int y, int u, int v, int width, int height) {
        VanillaGuiDraw.texture(graphics, location(texture), x, y, u, v, width, height, texture.textureWidth(), texture.textureHeight());
    }

    @Override
    public void texture(KineticTexture texture, int x, int y, int width, int height, float u, float v,
                        int regionWidth, int regionHeight) {
        VanillaGuiDraw.texture(graphics, location(texture), x, y, width, height, u, v, regionWidth, regionHeight,
                texture.textureWidth(), texture.textureHeight());
    }

    @Override
    public void texture(KineticTexture texture, int x, int y, int u, int v, int width, int height, int argb) {
        VanillaGuiDraw.tintedTexture(graphics, location(texture), x, y, u, v, width, height,
                texture.textureWidth(), texture.textureHeight(), argb);
    }

    @Override
    public void effectIcon(MobEffect effect, int x, int y, int size) {
        if (effect == null || size <= 0) return;
        VanillaGuiDraw.effectIcon(graphics, effect, x, y, size);
    }

    @Override
    public void push() {
        VanillaGuiDraw.push(graphics);
    }

    @Override
    public void pop() {
        VanillaGuiDraw.pop(graphics);
    }

    @Override
    public void translate(float x, float y) {
        VanillaGuiDraw.translate(graphics, x, y, 0F);
    }

    @Override
    public void scale(float x, float y) {
        VanillaGuiDraw.scale(graphics, x, y, 1F);
    }

    @Override
    public void rotate(float degrees) {
        VanillaGuiDraw.rotate(graphics, degrees);
    }

    @Override
    public void raise(int steps) {
        if (steps > 0) VanillaGuiDraw.raise(graphics, (float) steps * RAISE_STEP_DEPTH);
    }

    @Override
    public void scissor(int left, int top, int right, int bottom) {
        dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime.enableScissor(graphics, left, top, right, bottom);
    }

    @Override
    public void endScissor() {
        graphics.disableScissor();
    }

    @Override
    public int textWidth(String text) {
        return font().width(text == null ? "" : text);
    }

    @Override
    public int textWidth(Component text) {
        return font().width(text == null ? Component.empty() : text);
    }

    @Override
    public int textWidth(FormattedCharSequence text) {
        return font().width(text == null ? FormattedCharSequence.EMPTY : text);
    }

    @Override
    public int lineHeight() {
        return font().lineHeight;
    }
}
