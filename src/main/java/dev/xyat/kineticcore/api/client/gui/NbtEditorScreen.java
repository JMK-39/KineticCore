package dev.xyat.kineticcore.api.client.gui;

import dev.xyat.kineticcore.api.client.NbtEditorWidget;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.feature.nbt.network.NbtNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class NbtEditorScreen extends ScaledScreen {
    private final String initialNbt;
    private final Consumer<String> onSave;
    private final Screen parentScreen;

    private NbtEditorWidget nbtEditor;
    private EditBox searchBox;

    public NbtEditorScreen(String initialNbt, byte ignoredTargetType, String ignoredTargetId) {
        super(Component.translatable("screen.kineticcore.nbt_editor"));
        this.initialNbt = initialNbt;
        this.parentScreen = null;
        this.onSave = value -> NbtNetwork.sendToServer(new NbtNetwork.SaveNbtPacket(value));
        configureResponsiveCanvas(
                640f,
                360f,
                6
        );
    }

    public NbtEditorScreen(String initialNbt, Consumer<String> onSave, Screen parentScreen) {
        super(Component.translatable("screen.kineticcore.nbt_editor"));
        this.initialNbt = initialNbt;
        this.onSave = onSave;
        this.parentScreen = parentScreen;
        configureResponsiveCanvas(
                640f,
                360f,
                6
        );
    }

    @Override
    protected void initScaled() {
        searchBox = new EditBox(this.font, 20, 10, 120, 20, Component.translatable("gui.kineticcore.search"));
        searchBox.setResponder(query -> {
            if (nbtEditor != null) nbtEditor.setSearchQuery(query);
        });
        this.addRenderableWidget(searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("↑"), b -> nbtEditor.navigateSearch(-1))
                .bounds(145, 10, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("↓"), b -> nbtEditor.navigateSearch(1))
                .bounds(170, 10, 20, 20).build());

        int btnW = 80;
        int gap = 10;
        int closeX = this.vWidth - 20 - btnW;
        int saveX = closeX - gap - btnW;
        int clearX = saveX - gap - btnW;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticcore.nbt.save"), b -> {
            String val = nbtEditor.getValue().trim();
            if (val.isEmpty() || val.equals("{}")) {
                onSave.accept("");
            } else {
                try {
                    TagParser.parseTag(val);
                    onSave.accept(val);
                } catch (Exception e) {
                    nbtEditor.setError(Component.translatable("gui.kineticcore.nbt.editor.invalid").getString());
                    return;
                }
            }
            if (this.minecraft != null) this.minecraft.setScreen(parentScreen);
        }).bounds(saveX, 10, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticcore.nbt.clear"), b -> nbtEditor.setValue("")).bounds(clearX, 10, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.kineticcore.nbt.close"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(parentScreen);
        }).bounds(closeX, 10, btnW, 20).build());

        int editorX = 20;
        int editorY = 40;
        int editorW = this.vWidth - 40;
        int editorH = this.vHeight - 60;

        nbtEditor = new NbtEditorWidget(this.font, editorX, editorY, editorW, editorH);
        nbtEditor.setValue(initialNbt);
        nbtEditor.setResponder(val -> {
            try {
                if (val.trim().isEmpty() || val.trim().equals("{}")) { nbtEditor.setError(""); return; }
                TagParser.parseTag(val);
                nbtEditor.setError("");
            } catch (Exception e) {
                nbtEditor.setError(e.getMessage());
            }
        });
    }

    @Override
    public void renderScaledBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.vWidth, this.vHeight, 0xCC000000);
        nbtEditor.render(g);

        if (nbtEditor != null) {
            String countText = nbtEditor.getSearchMatchCount();
            if (!countText.isEmpty()) {
                g.drawString(this.font, countText, 195, 16, 0xFFFFFFFF, false);
            }
        }
    }

    @Override
    protected void renderScaledForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (searchBox != null && !searchBox.isFocused() && searchBox.getValue().isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.kineticcore.search_hint"),
                    searchBox.getX() + 4, searchBox.getY() + 6, 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean universalMouseClicked(double mx, double my, int btn) {
        if (searchBox != null && !searchBox.isMouseOver(mx, my)) {
            if (this.getFocused() == searchBox) {
                this.setFocused(null);
            }
            searchBox.setFocused(false);
        }

        if (nbtEditor.mouseClicked(mx, my, btn)) return true;
        return super.universalMouseClicked(mx, my, btn);
    }

    @Override
    public boolean universalMouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (nbtEditor.mouseDragged(mx, my)) return true;
        return super.universalMouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean universalMouseReleased(double mx, double my, int btn) {
        if (nbtEditor.mouseReleased()) return true;
        return super.universalMouseReleased(mx, my, btn);
    }

    @Override
    public boolean universalMouseScrolled(double mx, double my, double d) {
        if (nbtEditor.mouseScrolled(d)) return true;
        return super.universalMouseScrolled(mx, my, d);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                nbtEditor.navigateSearch(1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.setFocused(null);
                searchBox.setFocused(false);
                return true;
            }
        }

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            this.setFocused(searchBox);
            searchBox.setFocused(true);
            return true;
        }

        if (nbtEditor.keyPressed(keyCode)) return true;

        assert this.minecraft != null;
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nbtEditor.charTyped(codePoint)) return true;
        return super.charTyped(codePoint, modifiers);
    }
}
