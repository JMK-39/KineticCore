package dev.xyat.kineticcore.api.client.gui.editor;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;

import dev.xyat.kineticcore.internal.client.editor.CommandListEditorScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Public Kinetic client API for command list editor.
 */
public final class KineticCommandListEditor {
    private KineticCommandListEditor() {
    }

    /**
     * 以当前界面为父打开标准命令列表编辑器，绑定到指定的读写函数与服务端配置条目。
     * Opens the standard command-list editor as a child of the current screen, bound to the getter, setter and
     * server config entry.
     */
    public static void open(
            Supplier<List<String>> commandGetter,
            Consumer<List<String>> commandSetter,
            String serverPageId,
            String serverEntryId,
            Text text
    ) {
        KineticClientRuntime.openScreen(create(KineticClientRuntime.currentScreen(), commandGetter, commandSetter,
                serverPageId, serverEntryId, text));
    }

    /** 返回打开编辑器的动作，供配置条目按钮使用 / Returns an action opening the editor, for config entry buttons. */
    public static Runnable action(
            Supplier<List<String>> commandGetter,
            Consumer<List<String>> commandSetter,
            String serverPageId,
            String serverEntryId,
            Text text
    ) {
        return () -> open(commandGetter, commandSetter, serverPageId, serverEntryId, text);
    }

    private static Screen create(
            Screen parent,
            Supplier<List<String>> commandGetter,
            Consumer<List<String>> commandSetter,
            String serverPageId,
            String serverEntryId,
            Text text
    ) {
        return new CommandListEditorScreen(
                parent,
                Objects.requireNonNull(commandGetter, "commandGetter"),
                Objects.requireNonNull(commandSetter, "commandSetter"),
                Objects.requireNonNull(serverPageId, "serverPageId"),
                Objects.requireNonNull(serverEntryId, "serverEntryId"),
                Objects.requireNonNull(text, "text")
        );
    }

    /** Immutable translatable/display text bundle used by the command-list editor and its nested editor screen. */
    public record Text(
            Component title,
            Component addEditorTitle,
            Component editEditorTitle,
            Component emptyMessage,
            Component savedMessage,
            Component deletedMessage,
            Component saveFailedMessage,
            Component variableHint
    ) {
        /** Validates the required text bundle used by the reusable command-list editor. */
        public Text {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(addEditorTitle, "addEditorTitle");
            Objects.requireNonNull(editEditorTitle, "editEditorTitle");
            Objects.requireNonNull(emptyMessage, "emptyMessage");
            Objects.requireNonNull(savedMessage, "savedMessage");
            Objects.requireNonNull(deletedMessage, "deletedMessage");
            Objects.requireNonNull(saveFailedMessage, "saveFailedMessage");
        }
    }
}
