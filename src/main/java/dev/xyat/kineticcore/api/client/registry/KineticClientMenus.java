package dev.xyat.kineticcore.api.client.registry;

import dev.xyat.kineticcore.api.client.gui.page.KineticContainerPage;
import dev.xyat.kineticcore.internal.client.registry.KineticClientMenuRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.function.Supplier;

/** 容器菜单的客户端页面登记 / Client page registration for container menus. */
public final class KineticClientMenus {
    /** 由菜单与标题创建容器页面 / Creates a container page from a menu and title. */
    @FunctionalInterface
    public interface PageFactory<M extends AbstractContainerMenu> {
        /** 创建页面 / Creates the page. */
        KineticContainerPage<M> create(M menu, Component title);
    }

    private KineticClientMenus() {
    }

    /**
     * 在客户端初始化前登记容器页面。每个工厂独立提交给 Forge；某项失败不会跳过其余工厂。初始化事件开始后禁止新增登记。
     * Registers a container page before client setup. Each factory is submitted to Forge independently; one failure
     * does not skip the others. Registration is rejected once client setup has started.
     */
    public static <M extends AbstractContainerMenu> void register(
            Supplier<? extends MenuType<M>> menuType,
            PageFactory<M> factory
    ) {
        KineticClientMenuRuntime.register(
                Objects.requireNonNull(menuType, "menuType"),
                Objects.requireNonNull(factory, "factory")
        );
    }
}
