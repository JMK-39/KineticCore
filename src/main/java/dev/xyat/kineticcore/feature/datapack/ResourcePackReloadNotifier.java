package dev.xyat.kineticcore.feature.datapack;

import dev.xyat.kineticcore.api.hook.ClientHooks;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ResourcePackReloadNotifier {
    private static final KineticRegistrationBatch HOOK_REGISTRATION = new KineticRegistrationBatch();

    public static synchronized void registerHook() {
        HOOK_REGISTRATION.run(() -> ClientHooks.onResourceReloadUi(new ClientHooks.ResourceReloadUi() {
            @Override
            public void setPackScreenClosing(boolean closing) {
                ResourcePackReloadNotifier.setPackScreenClosing(closing);
            }

            @Override
            public boolean interceptReloadStart() {
                return ResourcePackReloadNotifier.interceptReloadStart();
            }

            @Override
            public void render(GuiGraphics graphics, int width, int height) {
                ResourcePackReloadNotifier.render(graphics, width, height);
            }
        }));
    }

    // 拦截标志，当处于 PackSelectionScreen 退出期间时为 true
    public static boolean isClosing = false;
    // 文本显示的截止时间戳
    public static long showTextUntil = 0L;

    public static void setPackScreenClosing(boolean closing) {
        isClosing = closing;
    }

    public static boolean interceptReloadStart() {
        if (isClosing) {
            showTextUntil = System.currentTimeMillis() + 3000L;
            return true;
        }
        showTextUntil = 0L;
        return false;
    }

    /**
     * 渲染提示文本
     * 在屏幕底部中心位置，间距30，无淡出，纯色背景避免看不清
     */
    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (System.currentTimeMillis() > showTextUntil) return;

        Font font = KineticClientRuntime.font();
        // 使用您自己的 I18N 键，请在 lang 文件中添加相应内容，如："资源包已保存，请按 F3+T 或重启游戏生效"
        Component text = KineticI18n.translatable("datapack.kineticcore.reload_prompt");
        int textWidth = font.width(text);

        // 居中，靠底部间距30
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 30;

        // 绘制一层半透明黑色背景以便阅读文本 (ARGB)
        GuiTheme.surface(guiGraphics, x - 4, y - 4, textWidth + 8, font.lineHeight + 8, GuiTheme.Surface.PANEL);
        // 绘制文字 (false 代表不添加阴影)
        guiGraphics.drawString(font, text, x, y, GuiTheme.current().text(), false);
    }
}