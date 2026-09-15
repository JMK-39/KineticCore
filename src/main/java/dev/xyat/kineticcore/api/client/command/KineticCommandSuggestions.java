package dev.xyat.kineticcore.api.client.command;

import dev.xyat.kineticcore.internal.client.command.KineticCommandSuggestionRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

public final class KineticCommandSuggestions {
    private KineticCommandSuggestions() {
    }

    public record Options(
            boolean commandsOnly,
            boolean onlyShowIfCursorPastError,
            int lineStartOffset,
            int suggestionLineLimit,
            boolean anchorToBottom,
            int fillColor
    ) {
        public Options {
            suggestionLineLimit = Math.max(1, suggestionLineLimit);
        }
    }

    public interface Session {
        void setAllowSuggestions(boolean allow);

        void update();

        void render(GuiGraphics graphics, int mouseX, int mouseY);

        boolean keyPressed(int keyCode, int scanCode, int modifiers);

        boolean mouseClicked(double mouseX, double mouseY, int button);

        boolean mouseScrolled(double delta);
    }

    public static Session create(EditBox input, int hostWidth, int hostHeight, Options options) {
        return KineticCommandSuggestionRuntime.create(input, hostWidth, hostHeight, options);
    }
}
