package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.HitResult;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Provides the single client-runtime facade for screen, input, window, and client-state operations.
 */
public final class KineticClientRuntime {
    private KineticClientRuntime() {
    }

    /** Initializes the client-side Kinetic runtime before GUI or feature setup. */
    public static void ensureReady() {
        KineticClientRuntimeImpl.initialize();
    }

    /** Executes the supplied action on the client runtime. */
    public static void execute(Runnable action) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.execute(action);
    }

    /** Returns the currently open client screen, or {@code null} when no screen is open. */
    public static Screen currentScreen() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.currentScreen();
    }

    /** Returns the current client level, or {@code null} while no world is loaded. */
    public static ClientLevel currentLevel() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.currentLevel();
    }

    /** Returns whether the client is connected to a server. */
    public static boolean connected() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.connected();
    }

    /** Returns the current remote server address, when available. */
    public static String currentServerAddress() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.currentServerAddress();
    }

    /** Returns a monotonic revision that changes whenever the active client connection changes. */
    public static long connectionRevision() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.connectionRevision();
    }

    /** Stops the active Minecraft client instance. */
    public static void stopClient() {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.stopClient();
    }

    /** Saves the active client options to disk. */
    public static void saveOptions() {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.saveOptions();
    }

    /** Returns the current local player, or {@code null} when unavailable. */
    public static LocalPlayer localPlayer() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.localPlayer();
    }

    /** Displays a vanilla client message for the local player when available. */
    public static void displayClientMessage(Component message, boolean overlay) {
        if (message == null) return;
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.displayClientMessage(message, overlay);
    }

    /** Returns the active client font. */
    public static Font font() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.font();
    }

    /** Returns the active client resource manager. */
    public static ResourceManager resourceManager() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.resourceManager();
    }

    /** Returns the current client crosshair hit result. */
    public static HitResult hitResult() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.hitResult();
    }

    /** Returns the client render-distance setting in chunks. */
    public static int renderDistanceChunks() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.renderDistanceChunks();
    }

    /** Returns the active client account name. */
    public static String username() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.username();
    }

    /** Returns whether the client is currently paused. */
    public static boolean paused() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.paused();
    }

    /** Returns whether the vanilla HUD is hidden. */
    public static boolean guiHidden() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.guiHidden();
    }

    /** Returns whether the vanilla debug screen is currently visible. */
    public static boolean debugScreenVisible() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.debugScreenVisible();
    }

    /** Returns whether the attack key is currently held. */
    public static boolean attackKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.attackKeyDown();
    }

    /** Sets the client attack key state. */
    public static void setAttackKeyDown(boolean down) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setAttackKeyDown(down);
    }

    /** Returns whether the use key is currently held. */
    public static boolean useKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.useKeyDown();
    }

    /** Returns whether the shift key is currently held. */
    public static boolean shiftKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.shiftKeyDown();
    }

    /** Returns whether the standard forward movement key is held. */
    public static boolean forwardKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.forwardKeyDown();
    }

    /** Returns whether the standard backward movement key is held. */
    public static boolean backKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.backKeyDown();
    }

    /** Returns whether the standard left movement key is held. */
    public static boolean leftKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.leftKeyDown();
    }

    /** Returns whether the standard right movement key is held. */
    public static boolean rightKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.rightKeyDown();
    }

    /** Returns whether the Control modifier is currently held. */
    public static boolean controlModifierDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.controlModifierDown();
    }

    /** Returns whether the Shift modifier is currently held. */
    public static boolean shiftModifierDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.shiftModifierDown();
    }

    /** Returns whether the Alt modifier is currently held. */
    public static boolean altModifierDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.altModifierDown();
    }

    /** Returns whether the supplied key press is the standard select-all shortcut. */
    public static boolean isSelectAllShortcut(int keyCode) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.isSelectAllShortcut(keyCode);
    }

    /** Returns whether the supplied key press is the standard copy shortcut. */
    public static boolean isCopyShortcut(int keyCode) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.isCopyShortcut(keyCode);
    }

    /** Returns whether the supplied key press is the standard paste shortcut. */
    public static boolean isPasteShortcut(int keyCode) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.isPasteShortcut(keyCode);
    }

    /** Returns whether the supplied key press is the standard cut shortcut. */
    public static boolean isCutShortcut(int keyCode) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.isCutShortcut(keyCode);
    }

    /** Returns whether the standard jump key is currently held. */
    public static boolean jumpKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.jumpKeyDown();
    }

    /** Sets the client jump key state. */
    public static void setJumpKeyDown(boolean down) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setJumpKeyDown(down);
    }

    /** Returns the current camera type. */
    public static CameraType cameraType() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.cameraType();
    }

    /** Sets camera type. */
    public static void setCameraType(CameraType cameraType) {
        if (cameraType == null) return;
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setCameraType(cameraType);
    }

    /** Stops the client-side block-destroy action. */
    public static void stopDestroyBlock() {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.stopDestroyBlock();
    }

    /** Returns the current scaled GUI width. */
    public static int guiScaledWidth() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.guiScaledWidth();
    }

    /** Returns the current scaled GUI height. */
    public static int guiScaledHeight() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.guiScaledHeight();
    }

    /** Returns the physical window width in pixels. */
    public static int windowWidth() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.windowWidth();
    }

    /** Returns the physical window height in pixels. */
    public static int windowHeight() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.windowHeight();
    }

    /** Returns the active Minecraft language code, such as {@code en_us}. */
    public static String selectedLanguage() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.selectedLanguage();
    }

    /** Returns whether the active Minecraft language belongs to the English language family. */
    public static boolean isEnglishLanguage() {
        String language = selectedLanguage();
        return language != null && !language.isBlank() && language.toLowerCase(java.util.Locale.ROOT).startsWith("en_");
    }

    /** Immutable cursor coordinates returned by the client-runtime facade. */
    public record CursorPosition(double x, double y) {
    }

    /** Returns the raw window cursor position. */
    public static CursorPosition cursorPosition() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.cursorPosition();
    }

    /** Returns the cursor position in scaled GUI coordinates. */
    public static CursorPosition scaledCursorPosition() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.scaledCursorPosition();
    }

    /** Moves the native window cursor to the supplied raw window coordinates. */
    public static void setCursorPosition(double x, double y) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setCursorPosition(x, y);
    }

    /** Returns the current client clipboard text. */
    public static String clipboard() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.clipboard();
    }

    /** Replaces the current client clipboard text. */
    public static void setClipboard(String value) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setClipboard(value);
    }

    /** Returns the client level keys currently known by the connection. */
    public static Set<ResourceKey<Level>> knownLevels() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.knownLevels();
    }

    /** Returns the resource ids currently exposed by one dynamic registry in the active client level. */
    public static <T> Set<ResourceLocation> registryKeys(ResourceKey<? extends Registry<T>> registryKey) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.registryKeys(registryKey);
    }

    /** Opens the supplied screen; pass {@code null} to close the current screen. */
    public static void openScreen(Screen screen) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.openScreen(screen);
    }

    /** Reinitializes the supplied screen through the client runtime without changing its identity. */
    public static void refreshScreen(Screen screen) {
        if (screen == null) return;
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.refreshScreen(screen);
    }

}
