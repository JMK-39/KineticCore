package dev.xyat.kineticcore.api.client.gui.command;

import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTextField;
import dev.xyat.kineticcore.internal.client.gui.command.KineticCommandSuggestions;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import net.minecraft.client.gui.components.EditBox;

import java.util.Objects;

/**
 * 为文本框提供原版风格的命令补全弹窗（取代旧的 {@code KineticCommandSuggestions}）。
 * 页面需要转发：{@code renderForeground → render}、{@code onKeyPress → keyPress}、
 * {@code onMouseClickCapture → mouseClick}、{@code onMouseScroll → mouseScroll}。
 * Vanilla-style command suggestions for a text field (replaces the old {@code KineticCommandSuggestions}).
 * The page forwards {@code renderForeground → render}, {@code onKeyPress → keyPress},
 * {@code onMouseClickCapture → mouseClick} and {@code onMouseScroll → mouseScroll}.
 */
public final class KineticCommandAssist {
    private final KineticCommandSuggestions.Session session;

    private KineticCommandAssist(KineticCommandSuggestions.Session session) {
        this.session = session;
    }

    /**
     * 绑定到文本框。会接管该文本框的内容变化回调（值变化时刷新补全，再调用 {@code onChange}）。
     * Attaches to a text field. Takes over the field's responder: suggestions refresh on every change and then
     * {@code onChange} (nullable) runs.
     *
     * @param pageWidth     页面宽度 / page width ({@code KineticPage.width()})
     * @param pageHeight    页面高度 / page height ({@code KineticPage.height()})
     * @param commandsOnly  仅补全命令 / only complete commands (no leading-slash-less chat)
     * @param maxLines      最多显示行数 / maximum visible suggestion lines
     */
    public static KineticCommandAssist attach(KineticTextField field, int pageWidth, int pageHeight, boolean commandsOnly,
                                              int maxLines, java.util.function.Consumer<String> onChange) {
        Objects.requireNonNull(field, "field");
        if (!(field instanceof EditBox editBox)) {
            throw new IllegalArgumentException("Text field is not a Kinetic built-in text field: " + field);
        }
        KineticCommandSuggestions.Session session = KineticCommandSuggestions.create(editBox, pageWidth, pageHeight,
                KineticCommandSuggestions.Options.fieldAligned(commandsOnly, false, Math.max(1, maxLines), 0xD0000000));
        session.setAllowSuggestions(true);
        field.setResponder(value -> {
            session.update();
            if (onChange != null) onChange.accept(value);
        });
        session.update();
        return new KineticCommandAssist(session);
    }

    /** 启用或禁用补全 / Enables or disables suggestions. */
    public void setEnabled(boolean enabled) {
        session.setAllowSuggestions(enabled);
    }

    /** 手动刷新补全 / Refreshes suggestions manually. */
    public void update() {
        session.update();
    }

    /** 在 renderForeground 中调用 / Call from renderForeground. */
    public void render(KineticGraphics graphics, int mouseX, int mouseY) {
        session.render(GuiGraphicsAdapter.unwrap(graphics), mouseX, mouseY);
    }

    /** 在 onKeyPress 中调用；返回 true 表示已消费 / Call from onKeyPress; true when consumed. */
    public boolean keyPress(KeyInput input) {
        return session.keyPressed(input.keyCode(), input.scanCode(), input.modifiers());
    }

    /** 在 onMouseClickCapture 中调用 / Call from onMouseClickCapture. */
    public boolean mouseClick(MouseInput input) {
        return session.mouseClicked(input.x(), input.y(), input.rawButton());
    }

    /** 在 onMouseScroll 中调用 / Call from onMouseScroll. */
    public boolean mouseScroll(ScrollInput input) {
        return session.mouseScrolled(input.deltaY());
    }
}
