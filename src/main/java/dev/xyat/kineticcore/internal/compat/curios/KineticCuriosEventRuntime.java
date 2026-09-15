package dev.xyat.kineticcore.internal.compat.curios;

import dev.xyat.kineticcore.api.compat.curios.KineticCuriosEvents;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticCuriosEventRuntime {
    private static final CopyOnWriteArrayList<KineticCuriosEvents.ChangeHandler> CHANGE = new CopyOnWriteArrayList<>();
    private static boolean initialized;

    private KineticCuriosEventRuntime() {
    }

    public static synchronized HookRegistration registerChange(KineticCuriosEvents.ChangeHandler handler) {
        initialize();
        CHANGE.add(handler);
        return () -> CHANGE.remove(handler);
    }

    private static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        MinecraftForge.EVENT_BUS.addListener(KineticCuriosEventRuntime::onChange);
    }

    private static void onChange(CurioChangeEvent event) {
        ChangeContextImpl context = new ChangeContextImpl(event);
        for (KineticCuriosEvents.ChangeHandler handler : CHANGE) {
            handler.handle(context);
        }
    }

    private record ChangeContextImpl(CurioChangeEvent event) implements KineticCuriosEvents.ChangeContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public String identifier() {
            return event.getIdentifier();
        }

        @Override
        public int slotIndex() {
            return event.getSlotIndex();
        }

        @Override
        public ItemStack from() {
            return event.getFrom();
        }

        @Override
        public ItemStack to() {
            return event.getTo();
        }
    }
}
