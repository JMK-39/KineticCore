package dev.xyat.kineticcore.feature.copyitem.client;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.minecraft.MinecraftContainers;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import dev.xyat.kineticcore.api.client.item.KineticHoveredItems;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ItemCopyManager {
    private static final long TOOLTIP_FALLBACK_KEEP_MS = 750L;

    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    private static ItemStack tooltipFallbackStack = ItemStack.EMPTY;
    private static long tooltipFallbackTimeMs;

    private ItemCopyManager() {
    }

    public static void register() {
        REGISTRATION.run(
                () -> KineticKeyBindings.builder("key.kineticcore.copyitem.copy_item_id")
                        .category("key.kineticcore.category")
                        .context(KineticKeyBindings.Context.GUI)
                        .modifier(KineticKeyBindings.Modifier.ALT)
                        .keyboard(KineticKeyBindings.Key.C)
                        .onPressed(ItemCopyManager::copyHoveredItem)
                        .register(),
                () -> KineticKeyBindings.builder("key.kineticcore.copyitem.copy_item_info")
                        .category("key.kineticcore.category")
                        .context(KineticKeyBindings.Context.GUI)
                        .modifier(KineticKeyBindings.Modifier.ALT)
                        .keyboard(KineticKeyBindings.Key.F)
                        .onPressed(ItemCopyManager::showHoveredItemDetails)
                        .register(),
                () -> KineticItemTooltips.onRender(ItemCopyManager::onRenderTooltip)
        );
    }

    private static void onRenderTooltip(ItemStack stack) {
        if (stack.isEmpty()) return;

        tooltipFallbackStack = stack.copy();
        tooltipFallbackTimeMs = Util.getMillis();
    }

    private static boolean copyHoveredItem() {
        ItemStack stack = resolveHoveredStack(KineticClientRuntime.currentScreen());
        if (stack.isEmpty()) return false;

        copyItem(stack);
        return true;
    }

    private static boolean showHoveredItemDetails() {
        var player = KineticClientRuntime.localPlayer();
        if (player == null) return false;

        ItemStack stack = resolveHoveredStack(KineticClientRuntime.currentScreen());
        if (stack.isEmpty()) return false;

        ItemDetailPrinter.showItemInfo(player, stack);
        KineticOverlays.toast(null, KineticI18n.translatable("msg.kineticcore.copyitem.copy.chat_output.success"), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        return true;
    }

    private static ItemStack resolveHoveredStack(Screen screen) {
        ItemStack providedStack = KineticHoveredItems.resolve();
        if (!providedStack.isEmpty()) return providedStack;

        ItemStack containerStack = getContainerHoveredStack(screen);
        if (!containerStack.isEmpty()) return containerStack;

        if (!tooltipFallbackStack.isEmpty() && Util.getMillis() - tooltipFallbackTimeMs <= TOOLTIP_FALLBACK_KEEP_MS) {
            return tooltipFallbackStack.copy();
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getContainerHoveredStack(Screen screen) {
        if (!KineticFeatureSwitches.isEnabled("client.copy_item_container_access")) {
            return ItemStack.EMPTY;
        }
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
        KineticClientRuntime.CursorPosition cursor = KineticClientRuntime.scaledCursorPosition();
        double mouseX = cursor.x();
        double mouseY = cursor.y();

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

    private static void copyItem(ItemStack stack) {
        ResourceLocation itemKey = KineticRegistries.items().id(stack.getItem());
        if (itemKey == null) return;

        String itemId = itemKey.toString();
        String result;

        if (stack.hasTag() && stack.getTag() != null) {
            String tag = stack.getTag().toString().replace("\\", "\\\\").replace("'", "\\'");
            result = "Item.of(\"" + itemId + "\", '" + tag + "')";
        } else {
            result = "\"" + itemId + "\"";
        }

        KineticClientRuntime.setClipboard(result);
        KineticOverlays.toast(null, KineticI18n.translatable("msg.kineticcore.copyitem.copy.item_id.success", Component.literal(result)), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
    }
}
