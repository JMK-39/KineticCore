package dev.xyat.kineticcore.api.minecraft;

import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

/**
 * Public Kinetic API for minecraft chat.
 */
public final class MinecraftChat {
    private MinecraftChat() {
    }

    /** Extension contract implemented by the chat mixin without exposing the mixin package to the API. */
    public interface Access {
        List<GuiMessage.Line> kineticcore$getTrimmedMessages();
        int kineticcore$getChatScrollbarPos();
    }

    /** Adds one message to the active client chat component. */
    public static void addMessage(Component message) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.chatComponent().addMessage(Objects.requireNonNull(message, "message"));
    }

    /** Adds one recent input entry to the active client chat component. */
    public static void addRecentChat(String message) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.chatComponent().addRecentChat(Objects.requireNonNull(message, "message"));
    }

    /** Returns the currently trimmed lines from the active client chat component. */
    public static List<GuiMessage.Line> activeTrimmedMessages() {
        KineticClientRuntimeImpl.initialize();
        return trimmedMessages(KineticClientRuntimeImpl.chatComponent());
    }

    /** Returns the number of trimmed lines in the active client chat component. */
    public static int activeTrimmedMessageCount() {
        KineticClientRuntimeImpl.initialize();
        return trimmedMessageCount(KineticClientRuntimeImpl.chatComponent());
    }

    /** Returns the scrollbar position of the active client chat component. */
    public static int activeScrollbarPosition() {
        KineticClientRuntimeImpl.initialize();
        return scrollbarPosition(KineticClientRuntimeImpl.chatComponent());
    }

    /** Returns the number of chat lines visible on the current page. */
    public static int linesPerPage() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.chatComponent().getLinesPerPage();
    }

    /** Returns the configured chat width in chat-local coordinates. */
    public static int width() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.chatComponent().getWidth();
    }

    /** Returns the current chat scale. */
    public static double scale() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.chatScale();
    }

    /** Scrolls the active client chat component by the supplied line delta. */
    public static void scroll(int delta) {
        if (delta == 0) return;
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.chatComponent().scrollChat(delta);
    }

    /** Returns an extension interface implemented by the active chat component, when available. */
    public static <T> T extension(Class<T> extensionType) {
        Objects.requireNonNull(extensionType, "extensionType");
        KineticClientRuntimeImpl.initialize();
        ChatComponent chat = KineticClientRuntimeImpl.chatComponent();
        return extensionType.isInstance(chat) ? extensionType.cast(chat) : null;
    }

    /** Returns the currently trimmed chat message lines. */
    public static List<GuiMessage.Line> trimmedMessages(ChatComponent chat) {
        Objects.requireNonNull(chat, "chat");
        if (chat instanceof Access access) {
            return List.copyOf(access.kineticcore$getTrimmedMessages());
        }
        return List.of();
    }

    /** Returns the number of currently trimmed chat message lines. */
    public static int trimmedMessageCount(ChatComponent chat) {
        Objects.requireNonNull(chat, "chat");
        if (chat instanceof Access access) {
            return access.kineticcore$getTrimmedMessages().size();
        }
        return 0;
    }

    /** Returns the current chat scrollbar position. */
    public static int scrollbarPosition(ChatComponent chat) {
        Objects.requireNonNull(chat, "chat");
        if (chat instanceof Access access) {
            return access.kineticcore$getChatScrollbarPos();
        }
        return 0;
    }
}
