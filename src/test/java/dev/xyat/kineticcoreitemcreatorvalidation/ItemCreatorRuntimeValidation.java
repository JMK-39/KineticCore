package dev.xyat.kineticcoreitemcreatorvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in fixture runs in a disposable actual-client world, never in a released mod. */
public final class ItemCreatorRuntimeValidation {
    private static final Logger LOG = LoggerFactory.getLogger(ItemCreatorRuntimeValidation.class);
    private static boolean finished;
    private static int waiting;
    public static void install() { KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, ItemCreatorRuntimeValidation::tick); }
    private static void tick() {
        var client = Minecraft.getInstance();
        if (finished) return;
        if (client.player == null || client.getSingleplayerServer() == null) {
            if (++waiting % 100 == 0) LOG.info("ITEM_CREATOR_WAIT screen={}", client.screen == null ? "none" : client.screen.getClass().getSimpleName());
            if (waiting == 100) {
                //? if >=26.1 {
                /*net.minecraft.client.Screenshot.grab(client.gameDirectory, "bootstrap.png", client.getMainRenderTarget(), 1, message -> LOG.info("ITEM_CREATOR_BOOTSTRAP_CAPTURE {}", message.getString()));
                *///?} else {
                net.minecraft.client.Screenshot.grab(client.gameDirectory, "bootstrap.png", client.getMainRenderTarget(), message -> LOG.info("ITEM_CREATOR_BOOTSTRAP_CAPTURE {}", message.getString()));
                //?}
                try { Class.forName("dev.xyat.kineticcore.api.client.gui.editor.KineticItemCreatorEditors"); }
                catch (ClassNotFoundException e) {
                    LOG.error("ITEM_CREATOR_VALIDATION_FAIL", new AssertionError("Missing in-game item creator editors", e));
                    finished = true; client.stop();
                }
            }
            return;
        }
        finished = true;
        try {
            try { Class.forName("dev.xyat.kineticcore.api.client.gui.editor.KineticItemCreatorEditors"); }
            catch (ClassNotFoundException e) { throw new AssertionError("Missing in-game item creator editors", e); }
            LOG.info("ITEM_CREATOR_EDITOR_PRESENT");
        } catch (Throwable failure) { LOG.error("ITEM_CREATOR_VALIDATION_FAIL", failure); }
        client.stop();
    }
}
