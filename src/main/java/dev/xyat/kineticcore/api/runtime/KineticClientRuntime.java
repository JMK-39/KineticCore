package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class KineticClientRuntime {
    private KineticClientRuntime() {
    }

    /** 幂等地初始化客户端公共能力；仅在客户端调用。 */
    public static void ensureReady() {
        KineticClientRuntimeImpl.initialize();
    }

    public static void execute(Runnable action) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.execute(action);
    }

    public static Screen currentScreen() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.currentScreen();
    }

    public static <T extends Screen> T currentScreen(Class<T> type) {
        Screen screen = currentScreen();
        return type.isInstance(screen) ? type.cast(screen) : null;
    }

    public static ClientLevel currentLevel() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.currentLevel();
    }

    public static boolean connected() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.connected();
    }

    public static String currentServerAddress() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.currentServerAddress();
    }

    public static void stopClient() {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.stopClient();
    }

    public static LocalPlayer localPlayer() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.localPlayer();
    }

    public static Font font() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.font();
    }

    public static ResourceManager resourceManager() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.resourceManager();
    }

    public static HitResult hitResult() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.hitResult();
    }

    public static int renderDistanceChunks() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.renderDistanceChunks();
    }

    public static ChatComponent chatComponent() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.chatComponent();
    }

    public static double chatScale() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.chatScale();
    }

    public static Collection<PlayerInfo> onlinePlayers() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.onlinePlayers();
    }

    public static PlayerInfo playerInfo(UUID uuid) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.playerInfo(uuid);
    }

    public static Object connectionToken() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.connectionToken();
    }

    public static List<Component> itemTooltip(ItemStack stack) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.itemTooltip(stack);
    }

    public static boolean paused() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.paused();
    }

    public static boolean guiHidden() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.guiHidden();
    }

    public static boolean attackKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.attackKeyDown();
    }

    public static boolean useKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.useKeyDown();
    }

    public static boolean shiftKeyDown() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.shiftKeyDown();
    }

    public static void setJumpKeyDown(boolean down) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setJumpKeyDown(down);
    }

    public static CameraType cameraType() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.cameraType();
    }

    public static void setCameraType(CameraType cameraType) {
        if (cameraType == null) return;
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setCameraType(cameraType);
    }

    public static void stopDestroyBlock() {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.stopDestroyBlock();
    }

    public static boolean clickInventorySlot(int containerId, int slotId, int button, ClickType clickType) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.clickInventorySlot(containerId, slotId, button, clickType);
    }

    public static boolean keyDown(int keyCode) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.keyDown(keyCode);
    }

    public static int guiScaledWidth() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.guiScaledWidth();
    }

    public static int guiScaledHeight() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.guiScaledHeight();
    }

    public static int windowWidth() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.windowWidth();
    }

    public static int windowHeight() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.windowHeight();
    }

    public static String selectedLanguage() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.selectedLanguage();
    }

    public record CursorPosition(double x, double y) {
    }

    public static CursorPosition cursorPosition() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.cursorPosition();
    }

    public static CursorPosition scaledCursorPosition() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.scaledCursorPosition();
    }

    public static void setCursorPosition(double x, double y) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setCursorPosition(x, y);
    }

    public static String clipboard() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.clipboard();
    }

    public static void setClipboard(String value) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.setClipboard(value);
    }

    public static Set<ResourceKey<Level>> knownLevels() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.knownLevels();
    }

    public static void closeScreen() {
        openScreen((Screen) null);
    }

    public static void openScreen(Screen screen) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.openScreen(screen);
    }

    public static void refreshScreen(Screen screen) {
        if (screen == null) return;
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.refreshScreen(screen);
    }

    public static void refreshCurrentScreen() {
        refreshScreen(currentScreen());
    }

    public static void openScreen(Supplier<? extends Screen> screenFactory) {
        if (screenFactory == null) return;
        openScreen(screenFactory.get());
    }
}
