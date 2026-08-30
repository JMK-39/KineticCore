package dev.xyat.kineticcore.api.client.gui;

import dev.xyat.kineticcore.api.client.layout.GuiSafeArea;
import dev.xyat.kineticcore.api.client.layout.ResponsiveLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public abstract class ResponsiveContainerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {
    private float preferredWidth = 640f;
    private float preferredHeight = 360f;
    private int safeMargin = 6;

    private float responsiveScale = 1f;
    private int responsiveOffsetX;
    private int responsiveOffsetY;
    private int responsiveWidth = 1;
    private int responsiveHeight = 1;

    protected ResponsiveContainerScreen(
            T menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
    }

    protected final void configureResponsiveContainer(
            ) {
        this.preferredWidth =
                Math.max(
                        1f,
                        (float) 640.0
                );

        this.preferredHeight =
                Math.max(
                        1f,
                        (float) 360.0
                );

        this.safeMargin =
                Math.max(
                        0,
                        6
                );
    }

    protected final int responsiveWidth() {
        return responsiveWidth;
    }

    protected final int responsiveHeight() {
        return responsiveHeight;
    }

    protected final float responsiveScale() {
        return responsiveScale;
    }

    protected final double toVirtualX(double screenX) {
        return (
                screenX
                        - responsiveOffsetX
        ) / responsiveScale;
    }

    protected final double toVirtualY(double screenY) {
        return (
                screenY
                        - responsiveOffsetY
        ) / responsiveScale;
    }

    @Override
    protected void init() {
        updateResponsiveMetrics();

        int realWidth = width;
        int realHeight = height;

        width = responsiveWidth;
        height = responsiveHeight;

        try {
            super.init();
        } finally {
            width = realWidth;
            height = realHeight;
        }
    }

    private void updateResponsiveMetrics() {
        GuiSafeArea safeArea =
                GuiSafeArea.of(
                        width,
                        height,
                        safeMargin
                );

        ResponsiveLayout layout =
                ResponsiveLayout.evaluate(
                        safeArea.width(),
                        safeArea.height(),
                        preferredWidth,
                        preferredHeight
                );

        responsiveScale =
                Math.max(
                        0.05f,
                        Math.min(
                                1f,
                                layout.fitScale()
                        )
                );

        responsiveOffsetX =
                safeArea.left();

        responsiveOffsetY =
                safeArea.top();

        responsiveWidth =
                Math.max(
                        1,
                        (int) Math.floor(
                                safeArea.width()
                                        / responsiveScale
                        )
                );

        responsiveHeight =
                Math.max(
                        1,
                        (int) Math.floor(
                                safeArea.height()
                                        / responsiveScale
                        )
                );
    }

    @Override
    public final void render(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        int virtualMouseX =
                (int) Math.floor(
                        toVirtualX(mouseX)
                );

        int virtualMouseY =
                (int) Math.floor(
                        toVirtualY(mouseY)
                );

        graphics.pose().pushPose();

        graphics.pose().translate(
                responsiveOffsetX,
                responsiveOffsetY,
                0
        );

        graphics.pose().scale(
                responsiveScale,
                responsiveScale,
                1f
        );

        try {
            super.render(
                    graphics,
                    virtualMouseX,
                    virtualMouseY,
                    partialTick
            );

            renderTooltip(
                    graphics,
                    virtualMouseX,
                    virtualMouseY
            );

            renderResponsiveForeground(
                    graphics,
                    virtualMouseX,
                    virtualMouseY,
                    partialTick
            );
        } finally {
            graphics.pose().popPose();
        }

        renderResponsiveOverlay(
                graphics,
                virtualMouseX,
                virtualMouseY,
                mouseX,
                mouseY,
                partialTick
        );
    }

    protected void renderResponsiveForeground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    protected void renderResponsiveOverlay(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        return super.mouseClicked(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button
        );
    }

    @Override
    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        return super.mouseReleased(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button
        );
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        return super.mouseDragged(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button,
                dragX / responsiveScale,
                dragY / responsiveScale
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        return super.mouseScrolled(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                delta
        );
    }

    @Override
    public void mouseMoved(
            double mouseX,
            double mouseY
    ) {
        super.mouseMoved(
                toVirtualX(mouseX),
                toVirtualY(mouseY)
        );
    }
}
