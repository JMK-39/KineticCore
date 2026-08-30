package dev.xyat.kineticcore.api.client;

import dev.xyat.kineticcore.api.client.layout.GuiSafeArea;
import dev.xyat.kineticcore.api.client.layout.ResponsiveLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public abstract class ScaledScreen extends Screen {
    private enum CanvasMode {
        FIT_CANVAS,
        FLUID
    }

    protected int vWidth;
    protected int vHeight;
    protected float guiScale;
    protected int offsetX;
    protected int offsetY;

    protected float scaleMultiplier = 1.0f;
    protected float minScale = 0.1f;
    protected float maxScale = Float.MAX_VALUE;
    protected boolean renderRenderablesOnly = false;

    private float designWidth = 640f;
    private float designHeight = 360f;
    private CanvasMode canvasMode =
            CanvasMode.FIT_CANVAS;
    private int safeMargin;

    private GuiSafeArea safeArea =
            GuiSafeArea.of(
                    1,
                    1,
                    0
            );

    private ResponsiveLayout responsiveLayout =
            ResponsiveLayout.evaluate(
                    1,
                    1,
                    640f,
                    360f
            );

    protected ScaledScreen(Component title) {
        super(title);
    }

    protected final void configureResponsiveCanvas(
            float designWidth,
            float designHeight,
            int safeMargin
    ) {
        this.designWidth =
                Math.max(
                        1f,
                        designWidth
                );

        this.designHeight =
                Math.max(
                        1f,
                        designHeight
                );

        this.safeMargin =
                Math.max(
                        0,
                        safeMargin
                );

        this.canvasMode =
                CanvasMode.FIT_CANVAS;
    }

    protected final void configureResponsiveFluid(
            float preferredWidth,
            float preferredHeight,
            int safeMargin
    ) {
        this.designWidth =
                Math.max(
                        1f,
                        preferredWidth
                );

        this.designHeight =
                Math.max(
                        1f,
                        preferredHeight
                );

        this.safeMargin =
                Math.max(
                        0,
                        safeMargin
                );

        this.canvasMode =
                CanvasMode.FLUID;
    }

    protected final GuiSafeArea safeArea() {
        return safeArea;
    }

    protected final ResponsiveLayout responsiveLayout() {
        return responsiveLayout;
    }

    protected final ResponsiveLayout.Level layoutLevel() {
        return responsiveLayout.level();
    }

    protected final boolean isPortraitLayout() {
        return responsiveLayout.isPortrait();
    }

    protected final boolean isUltrawideLayout() {
        return responsiveLayout.isUltrawide();
    }

    protected final boolean isCompactLayout() {
        return responsiveLayout.isCompact();
    }

    @Override
    protected void init() {
        super.init();
        updateResponsiveMetrics();
        clearWidgets();
        initScaled();
    }

    protected abstract void initScaled();

    private void updateResponsiveMetrics() {
        safeArea =
                GuiSafeArea.of(
                        width,
                        height,
                        safeMargin
                );

        responsiveLayout =
                ResponsiveLayout.evaluate(
                        safeArea.width(),
                        safeArea.height(),
                        designWidth,
                        designHeight
                );

        if (canvasMode == CanvasMode.FLUID) {
            updateFluidMetrics();
            return;
        }

        updateFixedCanvasMetrics();
    }

    private void updateFixedCanvasMetrics() {
        float fitScale =
                Math.max(
                        0.0001f,
                        responsiveLayout.fitScale()
                );

        float boundedScale = getBoundedScale(fitScale);

        guiScale =
                Math.min(
                        fitScale,
                        boundedScale
                );

        vWidth =
                Math.max(
                        1,
                        Math.round(
                                designWidth
                        )
                );

        vHeight =
                Math.max(
                        1,
                        Math.round(
                                designHeight
                        )
                );

        offsetX =
                safeArea.left()
                        + Math.round(
                        (
                                safeArea.width()
                                        - designWidth
                                        * guiScale
                        ) / 2f
                );

        offsetY =
                safeArea.top()
                        + Math.round(
                        (
                                safeArea.height()
                                        - designHeight
                                        * guiScale
                        ) / 2f
                );
    }

    private float getBoundedScale(float fitScale) {
        float requestedScale =
                fitScale
                        * Math.max(
                        0.0001f,
                        scaleMultiplier
                );

        float lowerBound =
                Math.max(
                        0.0001f,
                        minScale
                );

        float upperBound =
                Math.max(
                        lowerBound,
                        maxScale
                );

        return Math.max(
                lowerBound,
                Math.min(
                        requestedScale,
                        upperBound
                )
        );
    }

    private void updateFluidMetrics() {
        float lowerBound =
                Math.max(
                        0.0001f,
                        minScale
                );

        float upperBound =
                Math.max(
                        lowerBound,
                        maxScale
                );

        guiScale =
                Math.max(
                        lowerBound,
                        Math.min(
                                Math.max(
                                        0.0001f,
                                        scaleMultiplier
                                ),
                                upperBound
                        )
                );

        offsetX =
                safeArea.left();

        offsetY =
                safeArea.top();

        vWidth =
                Math.max(
                        1,
                        (int) Math.floor(
                                safeArea.width()
                                        / guiScale
                        )
                );

        vHeight =
                Math.max(
                        1,
                        (int) Math.floor(
                                safeArea.height()
                                        / guiScale
                        )
                );
    }

    @Override
    public void render(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        int scaledMouseX =
                (int) Math.floor(
                        toVirtualX(
                                mouseX
                        )
                );

        int scaledMouseY =
                (int) Math.floor(
                        toVirtualY(
                                mouseY
                        )
                );

        graphics.pose().pushPose();

        graphics.pose().translate(
                offsetX,
                offsetY,
                0
        );

        graphics.pose().scale(
                guiScale,
                guiScale,
                1.0f
        );

        try {
            renderScaledBackground(
                    graphics,
                    scaledMouseX,
                    scaledMouseY,
                    partialTick
            );

            if (renderRenderablesOnly) {
                for (Renderable renderable : renderables) {
                    renderable.render(
                            graphics,
                            scaledMouseX,
                            scaledMouseY,
                            partialTick
                    );
                }
            } else {
                super.render(
                        graphics,
                        scaledMouseX,
                        scaledMouseY,
                        partialTick
                );
            }

            renderScaledForeground(
                    graphics,
                    scaledMouseX,
                    scaledMouseY,
                    partialTick
            );
        } finally {
            graphics.pose().popPose();
        }

        renderTooltips(
                graphics,
                scaledMouseX,
                scaledMouseY,
                mouseX,
                mouseY
        );
    }

    protected void renderScaledBackground(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    protected void renderScaledForeground(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    protected void renderTooltips(
            GuiGraphics graphics,
            int scaledMouseX,
            int scaledMouseY,
            int mouseX,
            int mouseY
    ) {
    }

    protected final double toVirtualX(
            double screenX
    ) {
        return (
                screenX
                        - offsetX
        ) / guiScale;
    }

    protected final double toVirtualY(
            double screenY
    ) {
        return (
                screenY
                        - offsetY
        ) / guiScale;
    }

    protected final int toScreenX(
            double virtualX
    ) {
        return offsetX
                + (int) Math.floor(
                virtualX
                        * guiScale
        );
    }

    protected final int toScreenY(
            double virtualY
    ) {
        return offsetY
                + (int) Math.floor(
                virtualY
                        * guiScale
        );
    }

    protected final int toScreenRight(
            double virtualX
    ) {
        return offsetX
                + (int) Math.ceil(
                virtualX
                        * guiScale
        );
    }

    protected final int toScreenBottom(
            double virtualY
    ) {
        return offsetY
                + (int) Math.ceil(
                virtualY
                        * guiScale
        );
    }

    protected final boolean isInsideVirtualCanvas(
            double screenX,
            double screenY
    ) {
        double virtualX =
                toVirtualX(
                        screenX
                );

        double virtualY =
                toVirtualY(
                        screenY
                );

        return virtualX >= 0
                && virtualX < vWidth
                && virtualY >= 0
                && virtualY < vHeight;
    }

    protected final void enableVirtualScissor(
            GuiGraphics graphics,
            int left,
            int top,
            int right,
            int bottom
    ) {
        graphics.enableScissor(
                toScreenX(left),
                toScreenY(top),
                toScreenRight(right),
                toScreenBottom(bottom)
        );
    }

    protected void renderScissorCorrectedList(
            ObjectSelectionList<?> list,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (list == null
                || minecraft == null) {
            return;
        }

        GuiGraphics proxy =
                new GuiGraphics(
                        minecraft,
                        graphics.bufferSource()
                ) {
                    @Override
                    public void enableScissor(
                            int left,
                            int top,
                            int right,
                            int bottom
                    ) {
                        super.enableScissor(
                                toScreenX(left),
                                toScreenY(top),
                                toScreenRight(right),
                                toScreenBottom(bottom)
                        );
                    }
                };

        proxy.pose().translate(
                offsetX,
                offsetY,
                0
        );

        proxy.pose().scale(
                guiScale,
                guiScale,
                1.0f
        );

        list.render(
                proxy,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        boolean handled =
                universalMouseClicked(
                        toVirtualX(mouseX),
                        toVirtualY(mouseY),
                        button
                );

        if (!handled) {
            setFocused(null);
        }

        return handled;
    }

    protected boolean universalMouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        return universalMouseReleased(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button
        );
    }

    protected boolean universalMouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        return super.mouseReleased(
                mouseX,
                mouseY,
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
        return universalMouseDragged(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button,
                dragX / guiScale,
                dragY / guiScale
        );
    }

    protected boolean universalMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        return super.mouseDragged(
                mouseX,
                mouseY,
                button,
                dragX,
                dragY
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        return universalMouseScrolled(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                delta
        );
    }

    protected boolean universalMouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        return super.mouseScrolled(
                mouseX,
                mouseY,
                delta
        );
    }

    @Override
    public void mouseMoved(
            double mouseX,
            double mouseY
    ) {
        universalMouseMoved(
                toVirtualX(mouseX),
                toVirtualY(mouseY)
        );
    }

    protected void universalMouseMoved(
            double mouseX,
            double mouseY
    ) {
        super.mouseMoved(
                mouseX,
                mouseY
        );
    }
}
