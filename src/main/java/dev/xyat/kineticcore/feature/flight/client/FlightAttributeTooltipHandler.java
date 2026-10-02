package dev.xyat.kineticcore.feature.flight.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.flight.KineticFlightAttributes;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;

/** Adds concise explanations for the generic Kinetic super-flight attributes. */
public final class FlightAttributeTooltipHandler {
    private FlightAttributeTooltipHandler() {
    }

    public static void register() {
        KineticClientEvents.onItemTooltip(context -> {
            ItemStack stack = context.itemStack();
            if (stack.isEmpty()) return;

            boolean hasSpeed = hasModifier(stack, KineticFlightAttributes.flightSpeed());
            boolean hasDamping = hasModifier(stack, KineticFlightAttributes.turnDamping());
            if (hasSpeed) {
                context.tooltip().add(KineticI18n.translatable("tooltip.kineticcore.flight.speed"));
            }
            if (hasDamping) {
                context.tooltip().add(KineticI18n.translatable("tooltip.kineticcore.flight.damping"));
            }
        });
    }

    private static boolean hasModifier(ItemStack stack, Attribute attribute) {
        //? if >=1.20.5 {
        /*// Item attribute modifiers are one data component covering every slot since 1.20.5.
        return stack.getAttributeModifiers().modifiers().stream().anyMatch(entry -> entry.attribute().value() == attribute);
        *///?} else {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (stack.getAttributeModifiers(slot).containsKey(attribute)) return true;
        }
        return false;
        //?}
    }
}
