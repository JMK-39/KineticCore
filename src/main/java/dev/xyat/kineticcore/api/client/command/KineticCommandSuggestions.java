package dev.xyat.kineticcore.api.client.command;

import dev.xyat.kineticcore.internal.client.command.KineticCommandSuggestionRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

/** Public Kinetic API facade for command suggestions. */
public final class KineticCommandSuggestions {
    private KineticCommandSuggestions() {
    }

    /** Immutable options data exposed by this API. */
    public enum Placement {
        VANILLA,
        FIELD_ABOVE
    }

    public record Options(
            boolean commandsOnly,
            boolean onlyShowIfCursorPastError,
            int lineStartOffset,
            int suggestionLineLimit,
            boolean anchorToBottom,
            int fillColor,
            Placement placement
    ) {
        /**
         * Validates and normalizes this options value.
         */
        public Options {
            suggestionLineLimit = Math.max(1, suggestionLineLimit);
            placement = placement == null ? Placement.VANILLA : placement;
        }

        public Options(
                boolean commandsOnly,
                boolean onlyShowIfCursorPastError,
                int lineStartOffset,
                int suggestionLineLimit,
                boolean anchorToBottom,
                int fillColor
        ) {
            this(commandsOnly, onlyShowIfCursorPastError, lineStartOffset, suggestionLineLimit,
                    anchorToBottom, fillColor, Placement.VANILLA);
        }

        /**
         * Standard options for a command input placed inside a Kinetic GUI. The API owns the
         * text inset and uses field-relative suggestions instead of the vanilla chat-bottom anchor.
         */
        public static Options fieldAligned(
                boolean commandsOnly,
                boolean onlyShowIfCursorPastError,
                int suggestionLineLimit,
                int fillColor
        ) {
            return new Options(commandsOnly, onlyShowIfCursorPastError, 4, suggestionLineLimit,
                    false, fillColor, Placement.FIELD_ABOVE);
        }
    }

    /** Public API contract for session. */
    public interface Session {
        void setAllowSuggestions(boolean allow);

        void update();

        void render(GuiGraphics graphics, int mouseX, int mouseY);

        boolean keyPressed(int keyCode, int scanCode, int modifiers);

        boolean mouseClicked(double mouseX, double mouseY, int button);

        boolean mouseScrolled(double delta);
    }

    /**
     * Performs the create API operation.
     */
    public static Session create(EditBox input, int hostWidth, int hostHeight, Options options) {
        return KineticCommandSuggestionRuntime.create(input, hostWidth, hostHeight, options);
    }
}
