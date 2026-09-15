package dev.xyat.kineticcore.internal.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public final class KineticInventoryRuntime {
    private KineticInventoryRuntime() {
    }

    public static boolean isPlayerInventorySlot(Slot slot, Player player) {
        if (slot.container == player.getInventory()) return true;
        if (slot instanceof SlotItemHandler handlerSlot) {
            Object handler = handlerSlot.getItemHandler();
            if (handler instanceof PlayerMainInvWrapper || handler instanceof PlayerArmorInvWrapper) return true;
            if (handler instanceof InvWrapper invWrapper) return invWrapper.getInv() == player.getInventory();
        }
        return false;
    }

    public static boolean isPlayerInventoryHandler(Object handler) {
        if (handler instanceof PlayerMainInvWrapper || handler instanceof PlayerArmorInvWrapper) return true;
        return handler instanceof InvWrapper invWrapper && invWrapper.getInv() instanceof Inventory;
    }
}
