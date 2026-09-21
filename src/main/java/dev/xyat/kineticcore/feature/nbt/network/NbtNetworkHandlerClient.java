package dev.xyat.kineticcore.feature.nbt.network;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.selector.KineticSelectors;

import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class NbtNetworkHandlerClient {
    private static final String TOAST_ID = "kineticcore_nbt_editor";
    private static Screen pendingEditorParent;

    private NbtNetworkHandlerClient() {
    }

    public static void requestOpen(byte targetType, String targetId) {
        var level = KineticClientRuntime.currentLevel();
        if (level == null || !KineticClientRuntime.connected()) {
            handleNotify("gui.kineticcore.config.requires_world");
            return;
        }

        pendingEditorParent = KineticClientRuntime.currentScreen();
        try {
            if (!NbtNetwork.sendToServer(new NbtNetwork.OpenNbtEditorRequestPacket(
                    targetType,
                    targetId,
                    level.dimension().location()
            ))) {
                pendingEditorParent = null;
                handleNotify("gui.kineticcore.nbt.error.target_unavailable");
            }
        } catch (RuntimeException failure) {
            pendingEditorParent = null;
            handleNotify("gui.kineticcore.nbt.error.target_unavailable");
        }
    }


    public static void handleCommandOpen(byte commandMode) {
        if (commandMode == NbtNetwork.COMMAND_OPEN_HAND) {
            requestOpen(NbtNetwork.TARGET_HAND, "");
            return;
        }

        if (commandMode == NbtNetwork.COMMAND_OPEN_CROSSHAIR) {
            requestCrosshairTarget();
            return;
        }

        handleNotify("gui.kineticcore.nbt.error.no_target");
    }

    private static void requestCrosshairTarget() {
        HitResult hitResult = KineticClientRuntime.hitResult();

        if (hitResult instanceof EntityHitResult entityHitResult
                && hitResult.getType() == HitResult.Type.ENTITY) {
            requestOpen(
                    NbtNetwork.TARGET_ENTITY,
                    entityHitResult.getEntity().getUUID().toString()
            );
            return;
        }

        if (hitResult instanceof BlockHitResult blockHitResult
                && hitResult.getType() == HitResult.Type.BLOCK
                && KineticClientRuntime.currentLevel() != null) {
            BlockPos pos = blockHitResult.getBlockPos();
            if (KineticClientRuntime.currentLevel().getBlockEntity(pos) != null) {
                requestOpen(
                        NbtNetwork.TARGET_BLOCK_ENTITY,
                        Long.toString(pos.asLong())
                );
                return;
            }
        }

        handleNotify("gui.kineticcore.nbt.error.no_target");
    }

    public static void handleOpenEditor(String nbt) {
        Screen parent = pendingEditorParent != null ? pendingEditorParent : KineticClientRuntime.currentScreen();
        pendingEditorParent = null;
        KineticSelectors.openNbtEditor(
                parent,
                nbt,
                value -> NbtNetwork.sendToServer(new NbtNetwork.SaveNbtPacket(value))
        );
    }

    public static void handleNotify(String translationKey) {
        pendingEditorParent = null;
        KineticOverlays.toast(TOAST_ID, KineticI18n.translatable(translationKey), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
    }
}
