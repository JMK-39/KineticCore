package dev.xyat.kineticcore.api.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = KineticCore.MODID, value = Dist.CLIENT)
public final class GuiOverlay {
    public enum Position {
        TOP_CENTER,
        BOTTOM_CENTER,
        CENTER,
        TOP_LEFT,
        TOP_RIGHT
    }

    public enum MenuItemStyle {
        NORMAL,
        DANGER,
        SEPARATOR
    }

    public record MenuItem(
            Component label,
            Component tooltip,
            Boolean checked,
            Runnable action,
            boolean enabled,
            MenuItemStyle style
    ) {
        public MenuItem(Component label, Runnable action, boolean enabled) {
            this(label, label, null, action, enabled, MenuItemStyle.NORMAL);
        }

        public MenuItem {
            label = Objects.requireNonNullElse(label, Component.empty());
            tooltip = Objects.requireNonNullElse(tooltip, Component.empty());
            action = action == null ? () -> { } : action;
            style = style == null ? MenuItemStyle.NORMAL : style;
            if (style == MenuItemStyle.SEPARATOR) {
                enabled = false;
            } else if (tooltip.getString().isBlank()) {
                tooltip = label;
            }
        }

        public static MenuItem action(Component label, Runnable action) {
            return action(label, label, action);
        }

        public static MenuItem action(Component label, Component tooltip, Runnable action) {
            return new MenuItem(label, tooltip, null, action, true, MenuItemStyle.NORMAL);
        }

        public static MenuItem toggle(Component label, Component tooltip, boolean checked, Runnable action) {
            return new MenuItem(label, tooltip, checked, action, true, MenuItemStyle.NORMAL);
        }

        public static MenuItem danger(Component label, Runnable action) {
            return danger(label, label, action);
        }

        public static MenuItem danger(Component label, Component tooltip, Runnable action) {
            return new MenuItem(label, tooltip, null, action, true, MenuItemStyle.DANGER);
        }

        public static MenuItem disabled(Component label) {
            return disabled(label, label);
        }

        public static MenuItem disabled(Component label, Component tooltip) {
            return new MenuItem(label, tooltip, null, () -> { }, false, MenuItemStyle.NORMAL);
        }

        public static MenuItem separator() {
            return new MenuItem(Component.empty(), Component.empty(), null, () -> { }, false, MenuItemStyle.SEPARATOR);
        }
    }

    private sealed interface TooltipRequest permits TextTooltip, WrappedTextTooltip, FormattedTooltip, ItemTooltip {
    }

    private record FormattedTooltip(List<FormattedCharSequence> lines) implements TooltipRequest {
    }

    private record TextTooltip(List<Component> lines) implements TooltipRequest {
    }

    private record WrappedTextTooltip(List<Component> lines, int maxWidth) implements TooltipRequest {
    }

    private record ItemTooltip(ItemStack stack) implements TooltipRequest {
    }

    private record MenuControl(MenuItem item, KineticWidgets.MenuButton button) {
    }

    private record ContextMenu(int x, int y, List<MenuControl> controls) {
    }

    private record Dialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel,
            KineticWidgets.MenuButton confirmButton,
            KineticWidgets.MenuButton cancelButton
    ) {
    }

    private static final class Toast {
        private final String id;
        private final Component message;
        private final long startTime;
        private final int duration;
        private final Position position;
        private final int offsetX;
        private final int offsetY;
        private float currentY = -1000f;

        private Toast(String id, Component message, int duration, Position position, int offsetX, int offsetY) {
            this.id = id;
            this.message = message;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.position = position;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    private static final List<Toast> ACTIVE_TOASTS = new CopyOnWriteArrayList<>();
    private static GlobalTooltipRequest pendingScreenTooltip;

    private record GlobalTooltipRequest(TooltipRequest tooltip, int mouseX, int mouseY) {
    }

    private TooltipRequest tooltip;
    private ContextMenu contextMenu;
    private Dialog dialog;

    public void beginFrame() {
        tooltip = null;
    }

    public void tooltip(Component line) {
        if (line == null) return;
        tooltip = new TextTooltip(List.of(line));
    }

    public void tooltip(List<? extends Component> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<Component> clean = lines.stream().filter(Objects::nonNull).map(Component.class::cast).toList();
        if (!clean.isEmpty()) tooltip = new TextTooltip(clean);
    }

    public void tooltip(Component line, int maxWidth) {
        if (line == null) return;
        tooltip(List.of(line), maxWidth);
    }

    public void tooltip(List<? extends Component> lines, int maxWidth) {
        if (lines == null || lines.isEmpty()) return;
        List<Component> clean = lines.stream().filter(Objects::nonNull).map(Component.class::cast).toList();
        if (!clean.isEmpty()) tooltip = new WrappedTextTooltip(clean, Math.max(1, maxWidth));
    }

    public void formattedTooltip(List<FormattedCharSequence> lines) {
        if (lines == null || lines.isEmpty()) return;
        tooltip = new FormattedTooltip(List.copyOf(lines));
    }

    public void itemTooltip(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        tooltip = new ItemTooltip(stack.copy());
    }

    public static void requestTooltip(Component line, int mouseX, int mouseY) {
        if (line == null) return;
        pendingScreenTooltip = new GlobalTooltipRequest(new TextTooltip(List.of(line)), mouseX, mouseY);
    }

    public static void requestTooltip(List<? extends Component> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;
        List<Component> clean = lines.stream().filter(Objects::nonNull).map(Component.class::cast).toList();
        if (!clean.isEmpty()) pendingScreenTooltip = new GlobalTooltipRequest(new TextTooltip(clean), mouseX, mouseY);
    }

    public static void requestTooltip(Component line, int maxWidth, int mouseX, int mouseY) {
        if (line == null) return;
        requestTooltip(List.of(line), maxWidth, mouseX, mouseY);
    }

    public static void requestTooltip(List<? extends Component> lines, int maxWidth, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;
        List<Component> clean = lines.stream().filter(Objects::nonNull).map(Component.class::cast).toList();
        if (!clean.isEmpty()) {
            pendingScreenTooltip = new GlobalTooltipRequest(
                    new WrappedTextTooltip(clean, Math.max(1, maxWidth)),
                    mouseX,
                    mouseY
            );
        }
    }

    public static void requestFormattedTooltip(List<FormattedCharSequence> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;
        List<FormattedCharSequence> clean = lines.stream().filter(Objects::nonNull).toList();
        if (!clean.isEmpty()) pendingScreenTooltip = new GlobalTooltipRequest(new FormattedTooltip(clean), mouseX, mouseY);
    }

    public static void requestItemTooltip(ItemStack stack, int mouseX, int mouseY) {
        if (stack == null || stack.isEmpty()) return;
        pendingScreenTooltip = new GlobalTooltipRequest(new ItemTooltip(stack.copy()), mouseX, mouseY);
    }

    public void openMenu(int screenX, int screenY, List<MenuItem> items) {
        if (items == null || items.isEmpty()) {
            contextMenu = null;
            return;
        }
        List<MenuControl> controls = new ArrayList<>();
        for (MenuItem item : items) {
            if (item == null) continue;
            if (item.style() == MenuItemStyle.SEPARATOR) {
                controls.add(new MenuControl(item, null));
                continue;
            }
            KineticWidgets.MenuButton button = KineticWidgets.createMenuButton(
                    displayMenuLabel(item),
                    item.enabled(),
                    item.style() == MenuItemStyle.DANGER,
                    pressed -> {
                        contextMenu = null;
                        item.action().run();
                    }
            );
            button.setSelected(Boolean.TRUE.equals(item.checked()));
            controls.add(new MenuControl(item, button));
        }
        if (controls.isEmpty()) {
            contextMenu = null;
            return;
        }
        contextMenu = new ContextMenu(screenX, screenY, List.copyOf(controls));
        dialog = null;
    }

    public void closeMenu() {
        contextMenu = null;
    }

    public void openDialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        Component safeTitle = Objects.requireNonNullElse(title, Component.empty());
        Component safeMessage = Objects.requireNonNullElse(message, Component.empty());
        Component safeConfirmText = Objects.requireNonNullElse(confirmText, Component.empty());
        Component safeCancelText = Objects.requireNonNullElse(cancelText, Component.empty());
        Runnable safeConfirm = onConfirm == null ? () -> { } : onConfirm;
        Runnable safeCancel = onCancel == null ? () -> { } : onCancel;
        KineticWidgets.MenuButton confirmButton = KineticWidgets.createMenuButton(
                safeConfirmText, true, false, ignored -> {
                    dialog = null;
                    safeConfirm.run();
                }
        );
        KineticWidgets.MenuButton cancelButton = KineticWidgets.createMenuButton(
                safeCancelText, true, false, ignored -> {
                    dialog = null;
                    safeCancel.run();
                }
        );
        dialog = new Dialog(
                safeTitle,
                safeMessage,
                safeConfirmText,
                safeCancelText,
                safeConfirm,
                safeCancel,
                confirmButton,
                cancelButton
        );
        contextMenu = null;
    }

    public void closeDialog() {
        dialog = null;
    }

    public boolean blocksInput() {
        return contextMenu != null || dialog != null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth, int screenHeight, Font font) {
        if (dialog != null) {
            return handleDialogClick(mouseX, mouseY, button, screenWidth, screenHeight, font);
        }
        if (contextMenu == null) return false;
        if (button != 0) {
            contextMenu = null;
            return true;
        }

        MenuBounds bounds = menuBounds(contextMenu, screenWidth, screenHeight, font);
        layoutMenuButtons(bounds);
        if (!GuiTheme.hovering(mouseX, mouseY, bounds.x, bounds.y, bounds.width, bounds.height)) {
            contextMenu = null;
            return true;
        }

        for (MenuRow row : bounds.rows()) {
            KineticWidgets.MenuButton menuButton = row.control().button();
            if (menuButton != null && menuButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode != 256) return false;
        if (dialog != null) {
            dialog = null;
            return true;
        }
        if (contextMenu != null) {
            contextMenu = null;
            return true;
        }
        return false;
    }

    public void render(
            GuiGraphics graphics,
            Font font,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY
    ) {
        if (contextMenu != null || dialog != null) {
            pendingScreenTooltip = null;
        }
        if (contextMenu != null) renderMenu(graphics, font, screenWidth, screenHeight, mouseX, mouseY);
        if (dialog != null) renderDialog(graphics, font, screenWidth, screenHeight, mouseX, mouseY);
        if (dialog == null && contextMenu == null && tooltip != null) renderTooltip(graphics, font, mouseX, mouseY);
    }

    private void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        renderTooltipRequest(graphics, font, tooltip, mouseX, mouseY);
    }

    private static void renderTooltipRequest(
            GuiGraphics graphics,
            Font font,
            TooltipRequest request,
            int mouseX,
            int mouseY
    ) {
        if (request instanceof ItemTooltip item) {
            graphics.renderTooltip(font, item.stack(), mouseX, mouseY);
            return;
        }
        if (request instanceof TextTooltip text) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : text.lines()) lines.add(line.getVisualOrderText());
            graphics.renderTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (request instanceof WrappedTextTooltip text) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : text.lines()) lines.addAll(font.split(line, text.maxWidth()));
            graphics.renderTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (request instanceof FormattedTooltip text) {
            graphics.renderTooltip(font, text.lines(), mouseX, mouseY);
        }
    }

    private void renderMenu(
            GuiGraphics graphics,
            Font font,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY
    ) {
        MenuBounds bounds = menuBounds(contextMenu, screenWidth, screenHeight, font);
        layoutMenuButtons(bounds);
        MenuItem hoveredItem = null;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 900);
        GuiTheme.panel(graphics, bounds.x, bounds.y, bounds.width, bounds.height);
        for (MenuRow row : bounds.rows()) {
            MenuControl control = row.control();
            MenuItem item = control.item();
            if (item.style() == MenuItemStyle.SEPARATOR) {
                int lineY = row.y() + row.height() / 2;
                graphics.fill(bounds.x + 5, lineY, bounds.x + bounds.width - 5, lineY + 1, GuiTheme.current().border());
                continue;
            }
            KineticWidgets.MenuButton button = control.button();
            if (button == null) continue;
            button.render(graphics, mouseX, mouseY, 0f);
            if (button.isMouseOver(mouseX, mouseY)) {
                hoveredItem = item;
            }
        }
        graphics.pose().popPose();

        if (hoveredItem != null && !hoveredItem.tooltip().getString().isBlank()) {
            renderTooltipRequest(
                    graphics,
                    font,
                    new WrappedTextTooltip(List.of(hoveredItem.tooltip()), 320),
                    mouseX,
                    mouseY
            );
        }
    }

    private void layoutMenuButtons(MenuBounds bounds) {
        for (MenuRow row : bounds.rows()) {
            KineticWidgets.MenuButton button = row.control().button();
            if (button == null) continue;
            button.setBounds(bounds.x + 3, row.y(), bounds.width - 6, row.height());
        }
    }

    private void renderDialog(
            GuiGraphics graphics,
            Font font,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY
    ) {
        DialogBounds bounds = dialogBounds(screenWidth, screenHeight, font);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 950);
        graphics.fill(0, 0, screenWidth, screenHeight, GuiTheme.current().shadow());
        GuiTheme.panel(graphics, bounds.x, bounds.y, bounds.width, bounds.height);
        GuiTheme.stateOutline(graphics, bounds.x, bounds.y, bounds.width, bounds.height, true, false, false);
        GuiTheme.stateOutline(graphics, bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4, true, false, false);
        graphics.drawCenteredString(font, dialog.title(), bounds.x + bounds.width / 2, bounds.y + 12, GuiTheme.current().text());

        List<FormattedCharSequence> lines = font.split(dialog.message(), bounds.width - 24);
        int lineY = bounds.y + 34;
        for (FormattedCharSequence line : lines) {
            int lineX = bounds.x + (bounds.width - font.width(line)) / 2;
            graphics.drawString(font, line, lineX, lineY, GuiTheme.current().text(), false);
            lineY += 10;
        }

        dialog.confirmButton().setBounds(bounds.confirmX, bounds.buttonY, bounds.buttonWidth, bounds.buttonHeight);
        dialog.cancelButton().setBounds(bounds.cancelX, bounds.buttonY, bounds.buttonWidth, bounds.buttonHeight);
        dialog.confirmButton().render(graphics, mouseX, mouseY, 0f);
        dialog.cancelButton().render(graphics, mouseX, mouseY, 0f);
        graphics.pose().popPose();
    }

    private boolean handleDialogClick(
            double mouseX,
            double mouseY,
            int button,
            int screenWidth,
            int screenHeight,
            Font font
    ) {
        if (button != 0) return true;
        DialogBounds bounds = dialogBounds(screenWidth, screenHeight, font);
        dialog.confirmButton().setBounds(bounds.confirmX, bounds.buttonY, bounds.buttonWidth, bounds.buttonHeight);
        dialog.cancelButton().setBounds(bounds.cancelX, bounds.buttonY, bounds.buttonWidth, bounds.buttonHeight);
        if (dialog.confirmButton().mouseClicked(mouseX, mouseY, button)) return true;
        if (dialog.cancelButton().mouseClicked(mouseX, mouseY, button)) return true;
        return true;
    }

    private static MenuBounds menuBounds(ContextMenu menu, int screenWidth, int screenHeight, Font font) {
        int itemHeight = KineticScreen.STANDARD_CONTROL_HEIGHT;
        int separatorHeight = 5;
        int width = 126;
        int contentHeight = 0;
        for (MenuControl control : menu.controls()) {
            MenuItem item = control.item();
            if (item.style() == MenuItemStyle.SEPARATOR) {
                contentHeight += separatorHeight;
                continue;
            }
            width = Math.max(width, font.width(displayMenuLabel(item)) + 20);
            contentHeight += itemHeight;
        }
        width = Math.min(width, Math.max(20, screenWidth - 8));
        int height = Math.min(contentHeight + 6, Math.max(12, screenHeight - 8));
        int x = Math.max(4, Math.min(menu.x(), screenWidth - width - 4));
        int y = Math.max(4, Math.min(menu.y(), screenHeight - height - 4));

        List<MenuRow> rows = new ArrayList<>();
        int cursorY = y + 3;
        for (MenuControl control : menu.controls()) {
            int rowHeight = control.item().style() == MenuItemStyle.SEPARATOR ? separatorHeight : itemHeight;
            rows.add(new MenuRow(control, cursorY, rowHeight));
            cursorY += rowHeight;
        }
        return new MenuBounds(x, y, width, height, List.copyOf(rows));
    }

    private static Component displayMenuLabel(MenuItem item) {
        if (Boolean.TRUE.equals(item.checked())) {
            return Component.literal("✓ ").append(item.label());
        }
        return item.label();
    }

    private record MenuRow(MenuControl control, int y, int height) {
    }

    private record MenuBounds(int x, int y, int width, int height, List<MenuRow> rows) {
    }

    private DialogBounds dialogBounds(int screenWidth, int screenHeight, Font font) {
        int width = Math.min(320, Math.max(220, screenWidth - 40));
        int messageHeight = Math.max(30, font.split(dialog.message(), width - 24).size() * 10);
        int height = Math.min(screenHeight - 20, 78 + messageHeight);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;
        int buttonWidth = Math.max(70, (width - 36) / 2);
        int buttonHeight = KineticScreen.STANDARD_CONTROL_HEIGHT;
        int buttonY = y + height - buttonHeight - 10;
        int confirmX = x + 10;
        int cancelX = x + width - 10 - buttonWidth;
        return new DialogBounds(x, y, width, height, confirmX, cancelX, buttonY, buttonWidth, buttonHeight);
    }


    private record DialogBounds(
            int x,
            int y,
            int width,
            int height,
            int confirmX,
            int cancelX,
            int buttonY,
            int buttonWidth,
            int buttonHeight
    ) {
    }

    public static void toast(Component message) {
        if (message == null) return;
        toast(message.getString(), message, Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    public static void toast(String id, Component message) {
        toast(id, message, Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    public static void toast(
            String id,
            Component message,
            Position position,
            int durationMs,
            int offsetX,
            int offsetY
    ) {
        if (id == null || message == null) return;
        Position safePosition = position == null ? Position.BOTTOM_CENTER : position;
        ACTIVE_TOASTS.removeIf(toast -> toast.id.equals(id) && toast.position == safePosition);
        ACTIVE_TOASTS.add(new Toast(id, message, Math.max(1, durationMs), safePosition, offsetX, offsetY));
    }

    public static void removeToast(String id) {
        if (id != null) ACTIVE_TOASTS.removeIf(toast -> toast.id.equals(id));
    }

    public static void clearToasts() {
        ACTIVE_TOASTS.clear();
    }

    public static void clearAllToasts() {
        clearToasts();
    }

    private static void renderToasts(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
        if (ACTIVE_TOASTS.isEmpty()) return;
        long currentTime = System.currentTimeMillis();
        int currentBottomY = screenHeight - 40;
        int currentTopY = 25;

        for (int index = 0; index < ACTIVE_TOASTS.size(); index++) {
            Toast toast = ACTIVE_TOASTS.get(index);
            long elapsed = currentTime - toast.startTime;
            if (elapsed > toast.duration) {
                ACTIVE_TOASTS.remove(index--);
                continue;
            }

            MutableComponent bold = toast.message.copy().withStyle(ChatFormatting.BOLD);
            String raw = bold.getString();
            int wrapWidth = Integer.MAX_VALUE;
            if (raw.length() > 64) wrapWidth = Math.max(40, Math.round(font.width(bold) * 64f / raw.length()));
            List<FormattedCharSequence> lines = font.split(bold, wrapWidth);
            int maxLineWidth = 0;
            for (FormattedCharSequence line : lines) maxLineWidth = Math.max(maxLineWidth, font.width(line));
            int width = maxLineWidth + 16;
            int height = 8 + lines.size() * 10;

            int targetX;
            int targetY;
            switch (toast.position) {
                case TOP_CENTER -> {
                    targetX = (screenWidth - width) / 2;
                    targetY = currentTopY;
                    currentTopY += height + 4;
                }
                case BOTTOM_CENTER -> {
                    targetX = (screenWidth - width) / 2;
                    targetY = currentBottomY;
                    currentBottomY -= height + 4;
                }
                case CENTER -> {
                    targetX = (screenWidth - width) / 2;
                    targetY = (screenHeight - height) / 2;
                }
                case TOP_LEFT -> {
                    targetX = 10;
                    targetY = currentTopY;
                    currentTopY += height + 4;
                }
                case TOP_RIGHT -> {
                    targetX = screenWidth - width - 10;
                    targetY = currentTopY;
                    currentTopY += height + 4;
                }
                default -> throw new IllegalStateException();
            }

            targetX += toast.offsetX;
            targetY += toast.offsetY;
            if (toast.currentY == -1000f) {
                toast.currentY = toast.position == Position.BOTTOM_CENTER ? targetY + 15 : targetY - 15;
            }
            toast.currentY += (targetY - toast.currentY) * 0.25f;

            long remaining = toast.duration - elapsed;
            float alpha = 1f;
            if (remaining < 1200) alpha = remaining / 1200f;
            else if (elapsed < 300) alpha = elapsed / 300f;
            alpha = Math.max(0f, Math.min(1f, alpha));
            if (alpha <= 0.02f) continue;

            int alphaHex = (int) (alpha * 255f) << 24;
            int background = alphaHex | (GuiTheme.current().panel() & 0x00FFFFFF);
            int border = alphaHex | (GuiTheme.current().accent() & 0x00FFFFFF);
            int text = alphaHex | 0x00FFFFFF;
            int renderY = Math.round(toast.currentY);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 800);
            graphics.fill(targetX, renderY, targetX + width, renderY + height, border);
            graphics.fill(targetX + 2, renderY + 2, targetX + width - 2, renderY + height - 2, background);
            int textY = renderY + 5;
            for (FormattedCharSequence line : lines) {
                int lineX = targetX + (width - font.width(line)) / 2;
                graphics.drawString(font, line, lineX, textY, text, false);
                textY += 10;
            }
            graphics.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            renderToasts(
                    event.getGuiGraphics(),
                    minecraft.font,
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight()
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderScreenPre(ScreenEvent.Render.Pre event) {
        pendingScreenTooltip = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        renderToasts(event.getGuiGraphics(), minecraft.font, event.getScreen().width, event.getScreen().height);
        GlobalTooltipRequest request = pendingScreenTooltip;
        if (request != null) {
            renderTooltipRequest(event.getGuiGraphics(), minecraft.font, request.tooltip(), request.mouseX(), request.mouseY());
            pendingScreenTooltip = null;
        }
    }
}
