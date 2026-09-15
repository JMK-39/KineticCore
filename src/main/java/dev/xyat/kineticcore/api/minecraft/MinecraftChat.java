package dev.xyat.kineticcore.api.minecraft;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;

import java.util.List;
import java.util.Objects;

public final class MinecraftChat {
    private MinecraftChat() {
    }

    public interface Access {
        List<GuiMessage.Line> kineticcore$getTrimmedMessages();

        int kineticcore$getChatScrollbarPos();
    }

    public static List<GuiMessage.Line> trimmedMessages(ChatComponent chat) {
        Objects.requireNonNull(chat, "chat");
        if (chat instanceof Access access) {
            return List.copyOf(access.kineticcore$getTrimmedMessages());
        }
        return List.of();
    }

    public static int trimmedMessageCount(ChatComponent chat) {
        Objects.requireNonNull(chat, "chat");
        if (chat instanceof Access access) {
            return access.kineticcore$getTrimmedMessages().size();
        }
        return 0;
    }

    public static int scrollbarPosition(ChatComponent chat) {
        Objects.requireNonNull(chat, "chat");
        if (chat instanceof Access access) {
            return access.kineticcore$getChatScrollbarPos();
        }
        return 0;
    }
}
