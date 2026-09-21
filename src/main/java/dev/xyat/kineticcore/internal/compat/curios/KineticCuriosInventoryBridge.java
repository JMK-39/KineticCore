package dev.xyat.kineticcore.internal.compat.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Objects;
import java.util.function.Consumer;

public final class KineticCuriosInventoryBridge {
    private KineticCuriosInventoryBridge() {
    }

    public static void appendPlayerStacks(Player player, Consumer<ItemStack> consumer) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(consumer, "consumer");
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getCurios().values().forEach(stackHandler -> {
                    var stacks = stackHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        consumer.accept(stacks.getStackInSlot(i));
                    }
                })
        );
    }
}
