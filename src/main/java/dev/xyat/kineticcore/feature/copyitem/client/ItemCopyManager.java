package dev.xyat.kineticcore.feature.copyitem.client;

import dev.xyat.kineticcore.api.client.text.KineticText;
import com.mojang.blaze3d.platform.InputConstants;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.minecraft.MinecraftContainers;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.feature.copyitem.compat.jei.ItemCopyJeiPlugin;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemCopyManager {
    private static final long TOOLTIP_FALLBACK_KEEP_MS = 750L;

    private static boolean registered;
    private static ItemStack tooltipFallbackStack = ItemStack.EMPTY;
    private static long tooltipFallbackTimeMs;

    private ItemCopyManager() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        KineticKeyBindings.builder("key.kineticcore.copyitem.copy_item_id")
                .category("key.kineticcore.category")
                .context(KineticKeyBindings.Context.GUI)
                .modifier(KineticKeyBindings.Modifier.ALT)
                .keyboardKey(InputConstants.KEY_C)
                .onPressed(ItemCopyManager::copyHoveredItem)
                .register();

        KineticKeyBindings.builder("key.kineticcore.copyitem.copy_item_info")
                .category("key.kineticcore.category")
                .context(KineticKeyBindings.Context.GUI)
                .modifier(KineticKeyBindings.Modifier.ALT)
                .keyboardKey(InputConstants.KEY_F)
                .onPressed(ItemCopyManager::showHoveredItemDetails)
                .register();

        KineticItemTooltips.onRender(ItemCopyManager::onRenderTooltip);
    }

    private static void onRenderTooltip(ItemStack stack) {
        if (stack.isEmpty()) return;

        tooltipFallbackStack = stack.copy();
        tooltipFallbackTimeMs = Util.getMillis();
    }

    private static boolean copyHoveredItem() {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = resolveHoveredStack(KineticClientRuntime.currentScreen());
        if (stack.isEmpty()) return false;

        copyItem(mc, stack);
        return true;
    }

    private static boolean showHoveredItemDetails() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        ItemStack stack = resolveHoveredStack(KineticClientRuntime.currentScreen());
        if (stack.isEmpty()) return false;

        ItemDetailPrinter.showItemInfo(mc.player, stack);
        GuiOverlay.toast(KineticText.translatable("msg.kineticcore.copyitem.copy.chat_output.success"));
        return true;
    }

    private static ItemStack resolveHoveredStack(Screen screen) {
        ItemStack jeiStack = getJeiHoveredStack();
        if (!jeiStack.isEmpty()) return jeiStack;

        ItemStack containerStack = getContainerHoveredStack(screen);
        if (!containerStack.isEmpty()) return containerStack;

        if (!tooltipFallbackStack.isEmpty() && Util.getMillis() - tooltipFallbackTimeMs <= TOOLTIP_FALLBACK_KEEP_MS) {
            return tooltipFallbackStack.copy();
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getJeiHoveredStack() {
        if (!ModList.get().isLoaded("jei")) {
            return ItemStack.EMPTY;
        }

        return ItemCopyJeiPlugin.getHoveredItemStack();
    }

    private static ItemStack getContainerHoveredStack(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return ItemStack.EMPTY;
        }

        Slot hoveredSlot = MinecraftContainers.hoveredSlot(containerScreen);
        if (isValidSlot(hoveredSlot)) {
            return hoveredSlot.getItem().copy();
        }

        Slot mouseSlot = findSlotByMouse(containerScreen);
        if (isValidSlot(mouseSlot)) {
            return mouseSlot.getItem().copy();
        }

        ItemStack carried = containerScreen.getMenu().getCarried();
        if (!carried.isEmpty()) {
            return carried.copy();
        }

        return ItemStack.EMPTY;
    }

    private static Slot findSlotByMouse(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();

        double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();

        int left = MinecraftContainers.left(screen);
        int top = MinecraftContainers.top(screen);

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;

            int slotX = left + slot.x;
            int slotY = top + slot.y;

            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                return slot;
            }
        }

        return null;
    }

    private static boolean isValidSlot(Slot slot) {
        return slot != null && slot.isActive() && slot.hasItem() && !slot.getItem().isEmpty();
    }

    private static void copyItem(Minecraft mc, ItemStack stack) {
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemKey == null) return;

        String itemId = itemKey.toString();
        String result;

        if (stack.hasTag() && stack.getTag() != null) {
            String tag = stack.getTag().toString().replace("\\", "\\\\").replace("'", "\\'");
            result = "Item.of(\"" + itemId + "\", '" + tag + "')";
        } else {
            result = "\"" + itemId + "\"";
        }

        mc.keyboardHandler.setClipboard(result);
        GuiOverlay.toast(KineticText.translatable("msg.kineticcore.copyitem.copy.item_id.success", Component.literal(result)));
    }
}
