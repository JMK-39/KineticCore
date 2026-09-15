package dev.xyat.kineticcore.api.client.registry;

import dev.xyat.kineticcore.internal.client.registry.KineticClientMenuRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticClientMenus {
    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> {
        S create(M menu, Inventory inventory, Component title);
    }

    private KineticClientMenus() {
    }

    public static <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void register(
            Supplier<? extends MenuType<M>> menuType,
            ScreenFactory<M, S> factory
    ) {
        KineticClientMenuRuntime.register(
                Objects.requireNonNull(menuType, "menuType"),
                Objects.requireNonNull(factory, "factory")
        );
    }
}
