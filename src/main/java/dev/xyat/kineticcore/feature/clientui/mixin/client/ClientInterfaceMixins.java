package dev.xyat.kineticcore.feature.clientui.mixin.client;

import com.mojang.serialization.Lifecycle;
import com.mojang.text2speech.Narrator;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ClientInterfaceMixins {

    // 1. 复述器彻底禁用
    @Mixin(GameNarrator.class)
    public static class NarratorTweaks {
        @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/text2speech/Narrator;getNarrator()Lcom/mojang/text2speech/Narrator;", remap = false))
        private Narrator kineticcore$replaceWithDummy() {
            return new Narrator() {
                @Override public void say(String msg, boolean interrupt) {}
                @Override public void clear() {}
                @Override public boolean active() { return false; }
                @Override public void destroy() {}
            };
        }

        @Inject(method = "getStatus", at = @At("HEAD"), cancellable = true)
        private void kineticcore$forceStatusOff(CallbackInfoReturnable<NarratorStatus> cir) { cir.setReturnValue(NarratorStatus.OFF); }

        @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
        private void kineticcore$forceInactive(CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(false); }

        @Inject(method = "sayNow(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
        private void kineticcore$silenceSayNow(Component pMessage, CallbackInfo ci) { ci.cancel(); }

        @Inject(method = "updateNarratorStatus", at = @At("HEAD"), cancellable = true)
        private void kineticcore$blockStatusUpdate(NarratorStatus pStatus, CallbackInfo ci) { ci.cancel(); }
    }

    // 2. 移除多人游戏列表的 Forge 详情
    @Mixin(value = ForgeHooksClient.class, remap = false)
    public static class ForgePingTweaks {
        /** @author xyat @reason 清理界面 */
        @Overwrite
        public static void drawForgePingInfo(JoinMultiplayerScreen gui, ServerData target, GuiGraphics guiGraphics, int x, int y, int width, int relativeMouseX, int relativeMouseY) {
            // 不进行任何渲染
        }
    }

    // 3. 将“未经验证服务器”弹窗改为聊天提示
    @Mixin(ToastComponent.class)
    public static class ToastTweaks {
        @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
        private void interceptUnsecureToast(Toast toast, CallbackInfo ci) {
            if (toast instanceof SystemToast systemToast) {
                if (systemToast.getToken() == SystemToast.SystemToastIds.UNSECURE_SERVER_WARNING) {
                    ci.cancel();
                    Minecraft mc = Minecraft.getInstance();
                    mc.gui.getChat().addMessage(Component.translatable("msg.kineticcore.validation.warning"));
                }
            }
        }
    }

    // 4. 世界加载自动化 (跳过警告、稳定生命周期)
    @Mixin(CreateWorldScreen.class)
    public static class CreateWorldTweaks {
        @ModifyVariable(method = "tryApplyNewDataPacks", at = @At("HEAD"), argsOnly = true)
        private boolean kineticcore$dontShowWarning(boolean showWarning) { return false; }
    }

    @Mixin(WorldOpenFlows.class)
    public static class OpenFlowsTweaks {
        @ModifyVariable(method = "confirmWorldCreation", at = @At("HEAD"), argsOnly = true)
        private static Lifecycle kineticcore$alwaysStable(Lifecycle cycle) { return Lifecycle.stable(); }
    }

    @Mixin(PrimaryLevelData.class)
    public static class LevelDataTweaks {
        @Inject(method = "hasConfirmedExperimentalWarning", at = @At("HEAD"), cancellable = true, remap = false)
        private void kineticcore$hasConfirmedExperimentalWarning(CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(true);
        }
    }

    // 5. 阻止 UI 强制跳转
    @Mixin(WorldSelectionList.class)
    public static class SelectionListTweaks {
        @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openFresh(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V"))
        private void kineticcore$stopAutoJump(Minecraft mc, Screen screen) {
            // 阻止自动跳转
        }
    }

}