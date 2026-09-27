package dev.xyat.kineticcore.api.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.internal.client.gui.page.ContainerPageHost;
import dev.xyat.kineticcore.internal.client.gui.page.PageBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.Objects;

/**
 * 容器（菜单）页面：在 Kinetic 逻辑画布中显示原版容器槽位，并可叠加 Kinetic 控件。通过
 * {@code KineticClientMenus.register(menuType, factory)} 注册。容器区域坐标以 {@link #leftPos()}/{@link #topPos()} 为原点。
 * Container (menu) page: shows vanilla container slots inside the Kinetic logical canvas and can add Kinetic
 * controls on top. Register it with {@code KineticClientMenus.register(menuType, factory)}. Container-relative
 * coordinates start at {@link #leftPos()}/{@link #topPos()}.
 */
public abstract class KineticContainerPage<M extends AbstractContainerMenu> extends KineticPage {
    private final M menu;
    int layoutImageWidth = 176;
    int layoutImageHeight = 166;
    int layoutTitleX = 8;
    int layoutTitleY = 6;
    int layoutInventoryX = 8;
    int layoutInventoryY = Integer.MIN_VALUE;

    /** 为菜单创建页面 / Creates a page for a menu. */
    protected KineticContainerPage(M menu, Component title) {
        super(title, PageLayout.CANVAS);
        this.menu = Objects.requireNonNull(menu, "menu");
    }

    /** 容器菜单 / The container menu. */
    public final M menu() {
        return menu;
    }

    private ContainerPageHost containerHost() {
        Object host = PageBridge.access().host(this);
        if (host instanceof ContainerPageHost containerHost) return containerHost;
        throw new IllegalStateException("Container page is not open: " + getClass().getName());
    }

    private ContainerPageHost attachedHost() {
        Object host = PageBridge.access().host(this);
        return host instanceof ContainerPageHost containerHost ? containerHost : null;
    }

    /** 当前悬停的槽位，无则 null / The hovered slot, or null. */
    protected final Slot hoveredSlot() {
        return containerHost().hoveredSlot();
    }

    /** 容器区域左边界 / Left edge of the container area. */
    protected final int leftPos() {
        return containerHost().leftPos();
    }

    /** 容器区域上边界 / Top edge of the container area. */
    protected final int topPos() {
        return containerHost().topPos();
    }

    /** 容器区域宽度 / Container area width. */
    protected final int imageWidth() {
        return containerHost().imageWidth();
    }

    /** 容器区域高度 / Container area height. */
    protected final int imageHeight() {
        return containerHost().imageHeight();
    }

    /** 容器区域尺寸，请在构造函数中调用 / Container area size; call from the constructor. */
    protected final void setImageSize(int width, int height) {
        layoutImageWidth = Math.max(0, width);
        layoutImageHeight = Math.max(0, height);
        ContainerPageHost host = attachedHost();
        if (host != null) host.setImageSize(layoutImageWidth, layoutImageHeight);
    }

    /** 标题文字位置（相对容器区域）/ Title label position relative to the container area. */
    protected final void setTitleLabelPosition(int x, int y) {
        layoutTitleX = x;
        layoutTitleY = y;
        ContainerPageHost host = attachedHost();
        if (host != null) host.setTitleLabelPosition(x, y);
    }

    /** 背包文字位置（相对容器区域）/ Inventory label position relative to the container area. */
    protected final void setInventoryLabelPosition(int x, int y) {
        layoutInventoryX = x;
        layoutInventoryY = y;
        ContainerPageHost host = attachedHost();
        if (host != null) host.setInventoryLabelPosition(x, y);
    }

    /** 绘制容器背景（槽位之下，页面坐标）/ Draws the container background below slots, in page coordinates. */
    protected void renderContainerBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /**
     * 在整个屏幕之上绘制（原生屏幕坐标，位于画布之外也可见）。
     * Draws above the whole screen in native screen coordinates (visible outside the canvas too).
     */
    protected void renderScreenOverlay(KineticGraphics graphics, int screenMouseX, int screenMouseY, float partialTick) {
    }

    /** 默认显示悬停槽位物品提示 / By default shows the hovered slot's item tooltip. */
    @Override
    protected void renderTooltips(int mouseX, int mouseY) {
        Slot slot = hoveredSlot();
        if (slot != null && slot.hasItem()) showItemTooltip(slot.getItem());
    }
}
