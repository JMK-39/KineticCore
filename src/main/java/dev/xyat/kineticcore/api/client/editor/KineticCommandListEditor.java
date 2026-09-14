package dev.xyat.kineticcore.api.client.editor;

import dev.xyat.kineticcore.internal.client.editor.CommandListEditorScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public final class KineticCommandListEditor {
    private KineticCommandListEditor() {
    }

    public static Screen create(
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
