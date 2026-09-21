package dev.xyat.kineticcore.api.client.registry;

import dev.xyat.kineticcore.internal.client.registry.KineticClientMenuRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for client menus. */
public final class KineticClientMenus {
    /** Factory contract for creating screen instances. */
    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> {
        S create(M menu, Inventory inventory, Component title);
    }

    private KineticClientMenus() {
    }

    /**
     * 在客户端初始化前登记容器 Screen。每个工厂独立提交给 Forge；某项失败不会跳过其余工厂。
     * 初始化事件开始后禁止新增登记。
     */
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
