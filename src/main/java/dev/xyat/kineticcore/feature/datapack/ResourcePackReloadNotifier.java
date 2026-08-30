package dev.xyat.kineticcore.feature.datapack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ResourcePackReloadNotifier {
    // 拦截标志，当处于 PackSelectionScreen 退出期间时为 true
    public static boolean isClosing = false;
    // 文本显示的截止时间戳
    public static long showTextUntil = 0L;

    /**
     * 渲染提示文本
     * 在屏幕底部中心位置，间距30，无淡出，纯色背景避免看不清
     */
    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (System.currentTimeMillis() > showTextUntil) return;

        Minecraft mc = Minecraft.getInstance();

        Font font = mc.font;
        // 使用您自己的 I18N 键，请在 lang 文件中添加相应内容，如："资源包已保存，请按 F3+T 或重启游戏生效"
        Component text = Component.translatable("datapack.kineticcore.reload_prompt");
        int textWidth = font.width(text);

        // 居中，靠底部间距30
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 30;

        // 绘制一层半透明黑色背景以便阅读文本 (ARGB)
        guiGraphics.fill(x - 4, y - 4, x + textWidth + 4, y + font.lineHeight + 4, 0xCC000000);
        // 绘制文字 (false 代表不添加阴影)
        guiGraphics.drawString(font, text, x, y, 0xFFFFFF, false);
    }
}