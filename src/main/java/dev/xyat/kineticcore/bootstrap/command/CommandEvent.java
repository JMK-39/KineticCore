package dev.xyat.kineticcore.bootstrap.command;

import dev.xyat.kineticcore.KineticCore;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KineticCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandEvent {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 注册主指令 /kt
        Command.register(event.getDispatcher());
    }
}