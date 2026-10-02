package dev.xyat.kineticcore.internal.client.overlay;

import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;

import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.MenuButton;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.MenuItem;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.MenuItemStyle;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.Position;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticContainerScreen;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticNativeScreen;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GuiOverlayRuntime {
    private static final int TOOLTIP_Z = 1200;

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

    private record MenuControl(MenuItem item, MenuButton button) {
    }

    private record ContextMenu(int x, int y, List<MenuControl> controls, int preferredWidth) {
    }

    private record Dialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel,
            MenuButton confirmButton,
            MenuButton cancelButton
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
    // 菜单最多显示 MAX_VISIBLE_MENU_ITEMS 行，超出部分用滚轮 / 滚动条浏览。
    // Menus show at most MAX_VISIBLE_MENU_ITEMS rows; the rest scrolls with the wheel or the scrollbar.
    private static final int MAX_VISIBLE_MENU_ITEMS = 10;
    private static final int MENU_SCROLLBAR_WIDTH = 4;
    private int menuScroll;
    private boolean draggingMenuThumb;
    private double menuThumbGrab;
    private Dialog dialog;

    public void beginFrame() {
        tooltip = null;
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
        // Match the public tooltip entry: a partially empty list must not crash rendering.
        List<FormattedCharSequence> clean = lines.stream().filter(Objects::nonNull).toList();
        if (!clean.isEmpty()) tooltip = new FormattedTooltip(clean);
    }

    public void itemTooltip(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        tooltip = new ItemTooltip(stack.copy());
    }

    public static void requestTooltip(Component line, int mouseX, int mouseY) {
        KineticClientRuntimeImpl.initialize();
        if (line == null) return;
        pendingScreenTooltip = new GlobalTooltipRequest(new TextTooltip(List.of(line)), mouseX, mouseY);
    }

    public static void requestTooltip(List<? extends Component> lines, int mouseX, int mouseY) {
        KineticClientRuntimeImpl.initialize();
        if (lines == null || lines.isEmpty()) return;
        List<Component> clean = lines.stream().filter(Objects::nonNull).map(Component.class::cast).toList();
        if (!clean.isEmpty()) pendingScreenTooltip = new GlobalTooltipRequest(new TextTooltip(clean), mouseX, mouseY);
    }

    public static void requestTooltip(Component line, int maxWidth, int mouseX, int mouseY) {
        if (line == null) return;
        requestTooltip(List.of(line), maxWidth, mouseX, mouseY);
    }

    public static void requestTooltip(List<? extends Component> lines, int maxWidth, int mouseX, int mouseY) {
        KineticClientRuntimeImpl.initialize();
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
        KineticClientRuntimeImpl.initialize();
        if (lines == null || lines.isEmpty()) return;
        List<FormattedCharSequence> clean = lines.stream().filter(Objects::nonNull).toList();
        if (!clean.isEmpty()) pendingScreenTooltip = new GlobalTooltipRequest(new FormattedTooltip(clean), mouseX, mouseY);
    }

    public static void requestItemTooltip(ItemStack stack, int mouseX, int mouseY) {
        KineticClientRuntimeImpl.initialize();
        if (stack == null || stack.isEmpty()) return;
        pendingScreenTooltip = new GlobalTooltipRequest(new ItemTooltip(stack.copy()), mouseX, mouseY);
    }

    public void openMenu(int screenX, int screenY, List<MenuItem> items) {
        openMenu(screenX, screenY, items, 0);
    }

    /** Uses a fixed width when positive; menu labels scroll rather than changing the menu width. */
    public void openMenu(int screenX, int screenY, List<MenuItem> items, int preferredWidth) {
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
            MenuButton button = KineticWidgets.createMenuButton(
                    displayMenuLabel(item),
                    item.enabled(),
                    item.style() == MenuItemStyle.DANGER,
                    () -> {
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
        menuScroll = 0;
        draggingMenuThumb = false;
        contextMenu = new ContextMenu(screenX, screenY, List.copyOf(controls), preferredWidth);
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
        MenuButton confirmButton = KineticWidgets.createMenuButton(
                safeConfirmText, true, false, () -> {
                    dialog = null;
                    safeConfirm.run();
                }
        );
        MenuButton cancelButton = KineticWidgets.createMenuButton(
                safeCancelText, true, false, () -> {
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

    public static boolean openCurrentDialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof KineticScreen kineticScreen) {
            kineticScreen.openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
            return true;
        }
        if (screen instanceof KineticContainerScreen<?> kineticContainerScreen) {
            kineticContainerScreen.openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
            return true;
        }
        if (screen instanceof KineticNativeScreen kineticNativeScreen) {
            kineticNativeScreen.openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
            return true;
        }
        return false;
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

        MenuBounds bounds = menuBounds(contextMenu, screenWidth, screenHeight, font, menuScroll);
        layoutMenuButtons(bounds);
        if (!GuiTheme.hovering(mouseX, mouseY, bounds.x, bounds.y, bounds.width, bounds.height)) {
            contextMenu = null;
            return true;
        }
        if (bounds.scrollable() && mouseX >= bounds.scrollbarX()) {
            int thumbY = menuThumbY(bounds);
            int thumbHeight = menuThumbHeight(bounds);
            menuThumbGrab = mouseY >= thumbY && mouseY < thumbY + thumbHeight ? mouseY - thumbY : thumbHeight / 2.0D;
            draggingMenuThumb = true;
            dragMenuThumb(bounds, mouseY);
            return true;
        }
        if (mouseY < bounds.viewportTop() || mouseY >= bounds.viewportTop() + bounds.viewportHeight()) return true;

        for (MenuRow row : bounds.rows()) {
            MenuButton menuButton = row.control().button();
            if (menuButton != null && menuButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (dialog != null) {
            if (keyCode == 256) {
                // ESC is a cancellation, just like clicking the dialog's cancel button.
                // Clear the modal before invoking addon code so a callback may open a new one.
                Dialog cancelled = dialog;
                dialog = null;
                cancelled.onCancel().run();
            }
            return true;
        }
        if (contextMenu != null) {
            if (keyCode == 256) contextMenu = null;
            return true;
        }
        return false;
    }

    /** 菜单打开时滚轮滚动菜单内容；返回 true 表示已处理 / Scrolls an open menu; true when handled. */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta, int screenWidth, int screenHeight, Font font) {
        if (contextMenu == null || delta == 0D) return false;
        MenuBounds bounds = menuBounds(contextMenu, screenWidth, screenHeight, font, menuScroll);
        if (!bounds.scrollable() || !GuiTheme.hovering(mouseX, mouseY, bounds.x, bounds.y, bounds.width, bounds.height)) {
            return false;
        }
        int step = KineticScreen.STANDARD_CONTROL_HEIGHT;
        menuScroll = clampMenuScroll(bounds, menuScroll - (int) Math.signum(delta) * step);
        return true;
    }

    /** 拖动菜单滚动条 / Drags the menu scrollbar thumb. */
    public boolean mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight, Font font) {
        if (contextMenu == null || !draggingMenuThumb) return false;
        dragMenuThumb(menuBounds(contextMenu, screenWidth, screenHeight, font, menuScroll), mouseY);
        return true;
    }

    /** 松开鼠标结束滚动条拖动 / Ends a scrollbar drag. */
    public void mouseReleased() {
        draggingMenuThumb = false;
    }

    private static int clampMenuScroll(MenuBounds bounds, int scroll) {
        return Math.max(0, Math.min(scroll, bounds.maxScroll()));
    }

    private static int menuThumbHeight(MenuBounds bounds) {
        if (bounds.contentHeight() <= 0) return bounds.viewportHeight();
        return Math.max(12, bounds.viewportHeight() * bounds.viewportHeight() / bounds.contentHeight());
    }

    private int menuThumbY(MenuBounds bounds) {
        int track = bounds.viewportHeight() - menuThumbHeight(bounds);
        if (bounds.maxScroll() <= 0 || track <= 0) return bounds.viewportTop();
        return bounds.viewportTop() + (int) Math.round(track * (menuScroll / (double) bounds.maxScroll()));
    }

    private void dragMenuThumb(MenuBounds bounds, double mouseY) {
        int track = bounds.viewportHeight() - menuThumbHeight(bounds);
        if (track <= 0) return;
        double ratio = (mouseY - menuThumbGrab - bounds.viewportTop()) / track;
        menuScroll = clampMenuScroll(bounds, (int) Math.round(ratio * bounds.maxScroll()));
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
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, TOOLTIP_Z);
        try {
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
        } finally {
            graphics.pose().popPose();
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
        MenuBounds bounds = menuBounds(contextMenu, screenWidth, screenHeight, font, menuScroll);
        menuScroll = clampMenuScroll(bounds, menuScroll);
        layoutMenuButtons(bounds);
        MenuItem hoveredItem = null;
        int viewportTop = bounds.viewportTop();
        int viewportBottom = viewportTop + bounds.viewportHeight();
        boolean mouseInViewport = mouseY >= viewportTop && mouseY < viewportBottom;
        // 视口外的行不参与悬停 / Rows outside the viewport never count as hovered.
        int rowMouseY = mouseInViewport ? mouseY : Integer.MIN_VALUE / 2;
        int rowRight = bounds.x + bounds.width - (bounds.scrollable() ? MENU_SCROLLBAR_WIDTH + 2 : 0);

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 900);
            GuiTheme.panel(graphics, bounds.x, bounds.y, bounds.width, bounds.height);
            graphics.enableScissor(bounds.x, viewportTop, rowRight, viewportBottom);
            try {
                for (MenuRow row : bounds.rows()) {
                    if (row.y() + row.height() <= viewportTop || row.y() >= viewportBottom) continue;
                    MenuControl control = row.control();
                    MenuItem item = control.item();
                    if (item.style() == MenuItemStyle.SEPARATOR) {
                        int lineY = row.y() + row.height() / 2;
                        graphics.fill(bounds.x + 5, lineY, rowRight - 5, lineY + 1, GuiTheme.current().border());
                        continue;
                    }
                    MenuButton button = control.button();
                    if (button == null) continue;
                    button.render(graphics, mouseX, rowMouseY, 0f);
                    if (!item.detail().getString().isBlank()) {
                        int detailX = rowRight - 7 - font.width(item.detail());
                        int detailY = row.y() + (row.height() - 8) / 2;
                        graphics.drawString(font, item.detail(), detailX, detailY, GuiTheme.current().text(), false);
                    }
                    if (button.isMouseOver(mouseX, rowMouseY)) {
                        hoveredItem = item;
                    }
                }
            } finally {
                graphics.disableScissor();
            }
            if (bounds.scrollable()) {
                GuiTheme.scrollbar(graphics, mouseX, mouseY, bounds.scrollbarX(), viewportTop,
                        MENU_SCROLLBAR_WIDTH, bounds.viewportHeight(), menuThumbHeight(bounds),
                        bounds.maxScroll(), menuScroll, draggingMenuThumb);
            }
        } finally {
            graphics.pose().popPose();
        }

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
            MenuButton button = row.control().button();
            if (button == null) continue;
            int scrollbarSpace = bounds.scrollable() ? MENU_SCROLLBAR_WIDTH + 2 : 0;
            button.setBounds(bounds.x + 3, row.y(), bounds.width - 6 - scrollbarSpace, row.height());
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
        try {
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
        } finally {
            graphics.pose().popPose();
        }
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
        dialog.cancelButton().mouseClicked(mouseX, mouseY, button);
        return true;
    }

    private static MenuBounds menuBounds(ContextMenu menu, int screenWidth, int screenHeight, Font font, int scroll) {
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
            int rowWidth = font.width(displayMenuLabel(item)) + 20;
            if (!item.detail().getString().isBlank()) {
                rowWidth += font.width(item.detail()) + 12;
            }
            width = Math.max(width, rowWidth);
            contentHeight += itemHeight;
        }
        int viewportHeight = Math.min(contentHeight, Math.min(MAX_VISIBLE_MENU_ITEMS * itemHeight, Math.max(6, screenHeight - 14)));
        boolean scrollable = contentHeight > viewportHeight;
        if (menu.preferredWidth() > 0) {
            width = menu.preferredWidth();
        } else if (scrollable) {
            width += MENU_SCROLLBAR_WIDTH + 2;
        }
        width = Math.min(width, Math.max(20, screenWidth - 8));
        int height = viewportHeight + 6;
        int x = Math.max(4, Math.min(menu.x(), screenWidth - width - 4));
        int y = Math.max(4, Math.min(menu.y(), screenHeight - height - 4));
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        int safeScroll = Math.max(0, Math.min(scroll, maxScroll));

        List<MenuRow> rows = new ArrayList<>();
        int cursorY = y + 3 - safeScroll;
        for (MenuControl control : menu.controls()) {
            int rowHeight = control.item().style() == MenuItemStyle.SEPARATOR ? separatorHeight : itemHeight;
            rows.add(new MenuRow(control, cursorY, rowHeight));
            cursorY += rowHeight;
        }
        return new MenuBounds(x, y, width, height, List.copyOf(rows), y + 3, viewportHeight, contentHeight, maxScroll,
                x + width - 3 - MENU_SCROLLBAR_WIDTH);
    }

    private static Component displayMenuLabel(MenuItem item) {
        if (Boolean.TRUE.equals(item.checked())) {
            return Component.translatable("gui.kineticcore.symbol.checked").append(item.label());
        }
        return item.label();
    }

    private record MenuRow(MenuControl control, int y, int height) {
    }

    private record MenuBounds(int x, int y, int width, int height, List<MenuRow> rows,
                              int viewportTop, int viewportHeight, int contentHeight, int maxScroll, int scrollbarX) {
        boolean scrollable() {
            return maxScroll > 0;
        }
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

    public static void toast(
            String id,
            Component message,
            Position position,
            int durationMs,
            int offsetX,
            int offsetY
    ) {
        KineticClientRuntimeImpl.initialize();
        if (message == null) return;
        Position safePosition = position == null ? Position.BOTTOM_CENTER : position;
        if (id != null) {
            ACTIVE_TOASTS.removeIf(toast -> id.equals(toast.id) && toast.position == safePosition);
        }
        ACTIVE_TOASTS.add(new Toast(id, message, Math.max(1, durationMs), safePosition, offsetX, offsetY));
    }

    public static void removeToast(String id) {
        if (id != null) ACTIVE_TOASTS.removeIf(toast -> id.equals(toast.id));
    }

    public static void clearToasts() {
        ACTIVE_TOASTS.clear();
    }

    private static void renderToasts(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
        if (ACTIVE_TOASTS.isEmpty()) return;
        long currentTime = System.currentTimeMillis();
        int currentBottomY = screenHeight - 40;
        int currentTopY = 25;

        // Snapshot iteration is stable even if another callback clears or replaces toasts
        // during rendering. Remove expired entries by identity, never by a stale index.
        for (Toast toast : ACTIVE_TOASTS) {
            long elapsed = currentTime - toast.startTime;
            if (elapsed > toast.duration) {
                ACTIVE_TOASTS.remove(toast);
                continue;
            }

            MutableComponent bold = toast.message.copy().withStyle(style -> style.withBold(true));
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
                default -> throw new IllegalStateException("Unknown toast position: " + toast.position);
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
            int border = alphaHex | (GuiTheme.indicatorColor(KineticTheme.Indicator.WARNING) & 0x00FFFFFF);
            int text = alphaHex | 0x00FFFFFF;
            int renderY = Math.round(toast.currentY);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 800);
                graphics.fill(targetX, renderY, targetX + width, renderY + height, border);
                graphics.fill(targetX + 2, renderY + 2, targetX + width - 2, renderY + height - 2, background);
                int textY = renderY + 5;
                for (FormattedCharSequence line : lines) {
                    int lineX = targetX + (width - font.width(line)) / 2;
                    graphics.drawString(font, line, lineX, textY, text, false);
                    textY += 10;
                }
            } finally {
                // One failed addon font/render call must not leave subsequent HUD frames translated.
                graphics.pose().popPose();
            }
        }
    }

    public static void renderHudLayer(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            renderToasts(
                    graphics,
                    minecraft.font,
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight()
            );
        }
    }

    public static void beginScreenLayer() {
        pendingScreenTooltip = null;
    }

    public static void renderScreenLayer(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        renderToasts(graphics, minecraft.font, width, height);
        GlobalTooltipRequest request = pendingScreenTooltip;
        if (request != null) {
            renderTooltipRequest(graphics, minecraft.font, request.tooltip(), request.mouseX(), request.mouseY());
            pendingScreenTooltip = null;
        }
    }
}
