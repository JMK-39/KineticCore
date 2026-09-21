package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.internal.client.config.ServerConfigClientRuntime;
import dev.xyat.kineticcore.internal.client.input.KineticKeyBindingRuntime;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.DoubleBuffer;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class KineticClientRuntimeImpl {
    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static final KineticRegistrationBatch INITIALIZATION = new KineticRegistrationBatch();
    private static Object connectionRevisionOwner;
    private static long connectionRevision;

    private KineticClientRuntimeImpl() {
    }

    public static synchronized void initialize() {
        INITIALIZATION.run(
                KineticKeyBindingRuntime::initialize,
                KineticClientEventRuntime::initialize,
                KineticItemTooltipRuntime::initialize,
                ServerConfigClientRuntime::initialize,
                KineticClientRuntimeImpl::installRuntimeListeners
        );
    }

    private static void installRuntimeListeners() {
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(GuiOverlayBridge::onRenderGui));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, GuiOverlayBridge::onRenderScreenPre));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, GuiOverlayBridge::onRenderScreenPost));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(GuiSessionBridge::onScreenOpening));
        attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(GuiSessionBridge::onPlainScreenEscape));
        attempt.finish();
    }

    public static Minecraft client() {
        return Minecraft.getInstance();
    }

    public static void execute(Runnable action) {
        if (action == null) return;
        Minecraft.getInstance().execute(action);
    }

    public static Screen currentScreen() {
        return Minecraft.getInstance().screen;
    }

    public static ClientLevel currentLevel() {
        return Minecraft.getInstance().level;
    }

    public static boolean connected() {
        return Minecraft.getInstance().getConnection() != null;
    }

    public static String currentServerAddress() {
        var serverData = Minecraft.getInstance().getCurrentServer();
        return serverData == null ? null : serverData.ip;
    }

    public static void stopClient() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.isRunning()) {
                minecraft.stop();
            }
        });
    }

    public static void saveOptions() {
        Minecraft.getInstance().options.save();
    }

    public static LocalPlayer localPlayer() {
        return Minecraft.getInstance().player;
    }

    public static void displayClientMessage(Component message, boolean overlay) {
        if (message == null) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(message, overlay);
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static ResourceManager resourceManager() {
        return Minecraft.getInstance().getResourceManager();
    }

    public static HitResult hitResult() {
        return Minecraft.getInstance().hitResult;
    }

    public static int renderDistanceChunks() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    public static ChatComponent chatComponent() {
        return Minecraft.getInstance().gui.getChat();
    }

    public static double chatScale() {
        return Minecraft.getInstance().options.chatScale().get();
    }

    public static String username() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static Collection<PlayerInfo> onlinePlayers() {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? List.of() : List.copyOf(connection.getOnlinePlayers());
    }

    public static PlayerInfo playerInfo(UUID uuid) {
        if (uuid == null) return null;
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(uuid);
    }

    public static synchronized long connectionRevision() {
        Object currentConnection = Minecraft.getInstance().getConnection();
        if (currentConnection != connectionRevisionOwner) {
            connectionRevisionOwner = currentConnection;
            connectionRevision++;
        }
        return connectionRevision;
    }

    public static boolean paused() {
        return Minecraft.getInstance().isPaused();
    }

    public static boolean guiHidden() {
        return Minecraft.getInstance().options.hideGui;
    }

    public static boolean debugScreenVisible() {
        return Minecraft.getInstance().options.renderDebug;
    }

    public static boolean attackKeyDown() {
        return Minecraft.getInstance().options.keyAttack.isDown();
    }

    public static void setAttackKeyDown(boolean down) {
        Minecraft.getInstance().options.keyAttack.setDown(down);
    }

    public static boolean useKeyDown() {
        return Minecraft.getInstance().options.keyUse.isDown();
    }

    public static boolean shiftKeyDown() {
        return Minecraft.getInstance().options.keyShift.isDown();
    }

    public static boolean forwardKeyDown() {
        return Minecraft.getInstance().options.keyUp.isDown();
    }

    public static boolean backKeyDown() {
        return Minecraft.getInstance().options.keyDown.isDown();
    }

    public static boolean leftKeyDown() {
        return Minecraft.getInstance().options.keyLeft.isDown();
    }

    public static boolean rightKeyDown() {
        return Minecraft.getInstance().options.keyRight.isDown();
    }

    public static boolean controlModifierDown() {
        return Screen.hasControlDown();
    }

    public static boolean shiftModifierDown() {
        return Screen.hasShiftDown();
    }

    public static boolean altModifierDown() {
        return Screen.hasAltDown();
    }

    public static boolean isSelectAllShortcut(int keyCode) {
        return Screen.isSelectAll(keyCode);
    }

    public static boolean isCopyShortcut(int keyCode) {
        return Screen.isCopy(keyCode);
    }

    public static boolean isPasteShortcut(int keyCode) {
        return Screen.isPaste(keyCode);
    }

    public static boolean isCutShortcut(int keyCode) {
        return Screen.isCut(keyCode);
    }

    public static boolean jumpKeyDown() {
        return Minecraft.getInstance().options.keyJump.isDown();
    }

    public static void setJumpKeyDown(boolean down) {
        Minecraft.getInstance().options.keyJump.setDown(down);
    }

    public static CameraType cameraType() {
        return Minecraft.getInstance().options.getCameraType();
    }

    public static void setCameraType(CameraType cameraType) {
        Minecraft.getInstance().options.setCameraType(cameraType);
    }

    public static void stopDestroyBlock() {
        var gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null) {
            gameMode.stopDestroyBlock();
        }
    }

    public static boolean clickInventorySlot(int containerId, int slotId, int button, ClickType clickType) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || minecraft.player == null || clickType == null) {
            return false;
        }
        minecraft.gameMode.handleInventoryMouseClick(containerId, slotId, button, clickType, minecraft.player);
        return true;
    }

    public static boolean keyDown(int keyCode) {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), keyCode) == GLFW.GLFW_PRESS;
    }

    public static int guiScaledWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int guiScaledHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static int windowWidth() {
        return Minecraft.getInstance().getWindow().getScreenWidth();
    }

    public static int windowHeight() {
        return Minecraft.getInstance().getWindow().getScreenHeight();
    }

    public static String selectedLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }

    public static KineticClientRuntime.CursorPosition cursorPosition() {
        DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer y = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(Minecraft.getInstance().getWindow().getWindow(), x, y);
        return new KineticClientRuntime.CursorPosition(x.get(0), y.get(0));
    }

    public static KineticClientRuntime.CursorPosition scaledCursorPosition() {
        Minecraft minecraft = Minecraft.getInstance();
        double x = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
        double y = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
        return new KineticClientRuntime.CursorPosition(x, y);
    }

    public static void setCursorPosition(double x, double y) {
        GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().getWindow(), x, y);
    }

    public static String clipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    public static void setClipboard(String value) {
        Minecraft.getInstance().keyboardHandler.setClipboard(value == null ? "" : value);
    }

    public static Set<ResourceKey<Level>> knownLevels() {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? Set.of() : Set.copyOf(connection.levels());
    }

    public static <T> Set<ResourceLocation> registryKeys(ResourceKey<? extends Registry<T>> registryKey) {
        if (registryKey == null) return Set.of();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return Set.of();
        return level.registryAccess().registry(registryKey)
                .map(registry -> Set.copyOf(registry.keySet()))
                .orElseGet(Set::of);
    }

    public static void openScreen(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static void refreshScreen(Screen screen) {
        if (screen == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        screen.resize(
                minecraft,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight()
        );
    }
}
