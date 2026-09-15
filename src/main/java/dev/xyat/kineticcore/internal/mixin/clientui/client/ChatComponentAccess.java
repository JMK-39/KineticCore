package dev.xyat.kineticcore.internal.mixin.clientui.client;

import dev.xyat.kineticcore.api.minecraft.MinecraftChat;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccess extends MinecraftChat.Access {
    @Override
    @Accessor("trimmedMessages")
    List<GuiMessage.Line> kineticcore$getTrimmedMessages();

    @Override
    @Accessor("chatScrollbarPos")
    int kineticcore$getChatScrollbarPos();
}
