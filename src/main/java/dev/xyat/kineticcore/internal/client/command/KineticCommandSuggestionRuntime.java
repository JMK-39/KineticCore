package dev.xyat.kineticcore.internal.client.command;

import dev.xyat.kineticcore.api.client.command.KineticCommandSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KineticCommandSuggestionRuntime {
    private KineticCommandSuggestionRuntime() {
    }

    public static KineticCommandSuggestions.Session create(
            EditBox input,
            int hostWidth,
            int hostHeight,
            KineticCommandSuggestions.Options options
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen host = new Screen(Component.empty()) {
        };
        host.init(minecraft, Math.max(1, hostWidth), Math.max(1, hostHeight));
        CommandSuggestions delegate = new CommandSuggestions(
                minecraft,
                host,
                input,
                minecraft.font,
                options.commandsOnly(),
                options.onlyShowIfCursorPastError(),
                options.lineStartOffset(),
                options.suggestionLineLimit(),
                options.anchorToBottom(),
                options.fillColor()
        );
        return new KineticCommandSuggestions.Session() {
            @Override
            public void setAllowSuggestions(boolean allow) {
                delegate.setAllowSuggestions(allow);
            }

            @Override
            public void update() {
                delegate.updateCommandInfo();
            }

            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY) {
                delegate.render(graphics, mouseX, mouseY);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return delegate.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return delegate.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseScrolled(double delta) {
                return delegate.mouseScrolled(delta);
            }
        };
    }
}
