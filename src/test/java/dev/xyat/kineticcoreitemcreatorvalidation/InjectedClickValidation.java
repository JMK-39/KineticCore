package dev.xyat.kineticcoreitemcreatorvalidation;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Clicks what other mods put into the inventory and chat screens (ItemControl's trash bin button and its
 * "/kt clean bin" chat link, TextStudio's chat history button) through the loader's mouse events, as the mouse would,
 * and logs what each click did, with a screenshot after each. Opt-in with
 * {@code -Dkineticcore.injectedClickValidation=true}; meant for the modpack copy.
 */
public final class InjectedClickValidation {
    // The loader mouse events and GL probes used here are the 1.20.1 Forge ones; the check runs in the 1.20.1 pack.
    //? if forge {
    private static final Logger LOG = LogUtils.getLogger();
    private static int phase, ticks, worldTicks, wait;
    private static String before;

    private InjectedClickValidation() {
    }

    public static void install() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, InjectedClickValidation::tick);
        // Frames the open Kinetic page screen finished drawing since the last reset.
        KineticClientEvents.onScreenRenderBefore((screen, graphics, mouseX, mouseY, partialTick) -> {
            if (screen instanceof dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost && frames == 30) glState("before");
        });
        KineticClientEvents.onScreenRenderAfter((screen, graphics, mouseX, mouseY, partialTick) -> {
            if (!(screen instanceof dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost)) return;
            if (frames == 30) glState("after");
            frames++;
        });
    }

    private static int frames;

    /** Logs the GL state that decides whether GUI drawing reaches the screen. */
    private static void glState(String when) {
        int[] box = new int[4], viewport = new int[4];
        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_SCISSOR_BOX, box);
        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_VIEWPORT, viewport);
        java.nio.ByteBuffer mask = org.lwjgl.BufferUtils.createByteBuffer(16);
        org.lwjgl.opengl.GL11.glGetBooleanv(org.lwjgl.opengl.GL11.GL_COLOR_WRITEMASK, mask);
        float[] color = com.mojang.blaze3d.systems.RenderSystem.getShaderColor();
        LOG.info("INJECTED_CLICK_GL phase={} when={} scissor={} box={} viewport={} colorMask={},{},{},{} depthTest={} depthFunc={} depthMask={} blend={} fbo={} shaderColor={},{},{},{} cull={}",
                phase, when, org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST), java.util.Arrays.toString(box),
                java.util.Arrays.toString(viewport), mask.get(0), mask.get(1), mask.get(2), mask.get(3),
                org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST), org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_DEPTH_FUNC),
                org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK), org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND),
                org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING), color[0], color[1], color[2], color[3],
                org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE));
    }

    private static void tick() {
        if (phase == 99) return;
        var client = Minecraft.getInstance();
        try {
            if (++ticks > 12000) throw new IllegalStateException("timed out in phase " + phase);
            if (client.getOverlay() != null || client.player == null || client.level == null) return;
            if (wait > 0) {
                wait--;
                return;
            }
            switch (phase) {
                case 0 -> {
                    if (client.screen != null || ++worldTicks < 100) return;
                    // The trash button is on the survival inventory only.
                    client.player.connection.sendCommand("gamemode survival");
                    wait = 20;
                    phase = 1;
                }
                case 1 -> {
                    client.setScreen(new InventoryScreen(client.player));
                    wait = 20;
                    phase = 2;
                }
                case 2 -> {
                    clickControl(client.screen, "inventory");
                    wait = 60;
                    phase = 3;
                }
                case 3 -> {
                    LOG.info("INJECTED_CLICK_RESULT where=inventory before={} after={}", before, name(client.screen));
                    shot("inventory-after");
                    client.setScreen(null);
                    wait = 20;
                    phase = 4;
                }
                case 4 -> {
                    client.setScreen(new ChatScreen(""));
                    wait = 20;
                    phase = 5;
                }
                case 5 -> {
                    frames = 0;
                    clickControl(client.screen, "chat");
                    wait = 60;
                    phase = 6;
                }
                case 6 -> {
                    LOG.info("INJECTED_CLICK_RESULT where=chat before={} after={} frames={}", before, name(client.screen), frames);
                    shot("chat-after");
                    client.setScreen(null);
                    // The cleaner help lists "/kt clean bin" as a clickable chat line.
                    client.player.connection.sendCommand("kt clean");
                    wait = 40;
                    phase = 7;
                }
                case 7 -> {
                    client.setScreen(new ChatScreen(""));
                    wait = 20;
                    phase = 8;
                }
                case 8 -> {
                    clickChatLink(client, "clean bin");
                    wait = 60;
                    phase = 9;
                }
                case 9 -> {
                    LOG.info("INJECTED_CLICK_RESULT where=chat-link before={} after={}", before, name(client.screen));
                    shot("chat-link-after");
                    client.setScreen(null);
                    // Same chat button in the classic style, to tell a modern-style drawing problem from the page.
                    dev.xyat.kineticcore.internal.client.gui.theme.GuiStyle.setModern(false);
                    wait = 20;
                    phase = 10;
                }
                case 10 -> {
                    client.setScreen(new ChatScreen(""));
                    wait = 20;
                    phase = 11;
                }
                case 11 -> {
                    clickControl(client.screen, "chat-classic");
                    wait = 60;
                    phase = 12;
                }
                case 12 -> {
                    LOG.info("INJECTED_CLICK_RESULT where=chat-classic after={}", name(client.screen));
                    shot("chat-classic-after");
                    client.setScreen(null);
                    dev.xyat.kineticcore.internal.client.gui.theme.GuiStyle.setModern(true);
                    wait = 20;
                    phase = 13;
                }
                case 13 -> {
                    // The chat page opened as a child of the chat screen, from a tick instead of a mouse event.
                    client.setScreen(new ChatScreen(""));
                    wait = 20;
                    phase = 15;
                }
                case 15 -> {
                    Object page = Class.forName("dev.xyat.textstudio.chat.client.ChatCopyCanvasPage")
                            .getConstructor(List.class).newInstance(dev.xyat.kineticcore.api.minecraft.MinecraftChat.activeTrimmedMessages());
                    frames = 0;
                    dev.xyat.kineticcore.api.client.gui.KineticGui.openChild((dev.xyat.kineticcore.api.client.gui.page.KineticPage) page);
                    wait = 60;
                    phase = 16;
                }
                case 16 -> {
                    LOG.info("INJECTED_CLICK_RESULT where=chat-child-tick after={} frames={}", name(client.screen), frames);
                    shot("chat-child-tick-after");
                    client.setScreen(null);
                    wait = 20;
                    phase = 17;
                }
                case 17 -> {
                    // The chat page opened straight from the game, without the chat screen as its parent.
                    Object page = Class.forName("dev.xyat.textstudio.chat.client.ChatCopyCanvasPage")
                            .getConstructor(List.class).newInstance(dev.xyat.kineticcore.api.minecraft.MinecraftChat.activeTrimmedMessages());
                    frames = 0;
                    dev.xyat.kineticcore.api.client.gui.KineticGui.open((dev.xyat.kineticcore.api.client.gui.page.KineticPage) page);
                    wait = 60;
                    phase = 14;
                }
                case 14 -> {
                    LOG.info("INJECTED_CLICK_RESULT where=chat-direct after={}", name(client.screen));
                    shot("chat-direct-after");
                    LOG.info("INJECTED_CLICK_DONE");
                    phase = 99;
                    dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
                }
                default -> { }
            }
        } catch (Throwable failure) {
            LOG.error("INJECTED_CLICK_FAIL phase={}", phase, failure);
            phase = 99;
            dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
        }
    }

    /** Presses and releases the left button on the middle of the first injected control. */
    @SuppressWarnings("unchecked")
    private static void clickControl(Screen screen, String where) throws Exception {
        before = name(screen);
        var field = dev.xyat.kineticcore.internal.client.ScreenOverlayControls.class.getDeclaredField("CONTROLS");
        field.setAccessible(true);
        var controls = ((Map<Screen, List<AbstractWidget>>) field.get(null)).get(screen);
        LOG.info("INJECTED_CLICK_CONTROLS where={} screen={} count={}", where, before, controls == null ? 0 : controls.size());
        if (controls == null || controls.isEmpty()) return;
        for (AbstractWidget widget : controls) {
            LOG.info("INJECTED_CLICK_CONTROL where={} type={} at={},{} size={}x{} visible={} active={} message={}", where,
                    widget.getClass().getName(), widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                    widget.visible, widget.active, widget.getMessage().getString());
        }
        AbstractWidget target = controls.get(0);
        click(screen, where, target.getX() + target.getWidth() / 2.0D, target.getY() + target.getHeight() / 2.0D);
    }

    /** Finds the chat line whose click runs a command containing {@code command} and clicks it. */
    private static void clickChatLink(Minecraft client, String command) {
        Screen screen = client.screen;
        before = name(screen);
        for (int y = screen.height - 40; y > screen.height / 4; y--) {
            for (int x = 2; x < 320; x += 2) {
                Style style = client.gui.getChat().getClickedComponentStyleAt(x, y);
                ClickEvent event = style == null ? null : style.getClickEvent();
                if (event != null && event.getValue().contains(command)) {
                    LOG.info("INJECTED_CLICK_LINK found={} action={} at={},{}", event.getValue(), event.getAction(), x, y);
                    click(screen, "chat-link", x, y);
                    return;
                }
            }
        }
        LOG.info("INJECTED_CLICK_LINK found=none");
    }

    private static void click(Screen screen, String where, double x, double y) {
        boolean pressCanceled = net.minecraftforge.client.ForgeHooksClient.onScreenMouseClickedPre(screen, x, y, 0);
        boolean screenHandled = !pressCanceled && screen.mouseClicked(x, y, 0);
        boolean releaseCanceled = net.minecraftforge.client.ForgeHooksClient.onScreenMouseReleasedPre(screen, x, y, 0);
        if (!releaseCanceled) screen.mouseReleased(x, y, 0);
        LOG.info("INJECTED_CLICK_PRESS where={} at={},{} pressCanceled={} screenHandled={} releaseCanceled={} screenNow={}",
                where, x, y, pressCanceled, screenHandled, releaseCanceled, name(Minecraft.getInstance().screen));
    }

    private static void shot(String name) {
        var client = Minecraft.getInstance();
        net.minecraft.client.Screenshot.grab(client.gameDirectory, "injected-click-" + name + ".png", client.getMainRenderTarget(),
                message -> LOG.info("INJECTED_CLICK_SHOT {}", message.getString()));
    }

    private static String name(Screen screen) {
        return screen == null ? "none" : screen.getClass().getName();
    }
    //?} else {
    /*public static void install() {
    }
    *///?}
}
