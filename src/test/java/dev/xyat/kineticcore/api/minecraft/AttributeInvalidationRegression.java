package dev.xyat.kineticcore.api.minecraft;

import dev.xyat.kineticcore.api.registry.KineticEntityAttributes;
import dev.xyat.kineticcore.internal.registry.RuntimeAttributeInstanceAccess;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.concurrent.atomic.AtomicInteger;

/** Real vanilla computed-value caching with the loader's protected-method invoker supplied by a subclass. */
public final class AttributeInvalidationRegression {
    public static void main(String[] args) {
        var attribute = AttributeRangeRegression.attribute();
        AtomicInteger dirtyNotifications = new AtomicInteger();
        var instance = new TestInstance(attribute, dirtyNotifications);
        instance.setBaseValue(10);
        //? if >=1.20.5 {
        /*var modifier = new AttributeModifier(dev.xyat.kineticcore.api.resource.KineticResourceIds.of("range_regression", "bonus"), 30, AttributeModifier.Operation.ADD_VALUE);
        *///?} else {
        var modifier = new AttributeModifier(java.util.UUID.fromString("37b14669-c59d-4c93-bf16-bd405ce5823f"), "bonus", 30, AttributeModifier.Operation.ADDITION);
        //?}
        instance.addPermanentModifier(modifier);
        value(instance, 20);
        MinecraftAttributes.setRange(attribute, 0, 100);
        value(instance, 20); // A definition edit alone leaves vanilla's computed cache intact.
        int before = dirtyNotifications.get();
        KineticEntityAttributes.invalidate(instance);
        value(instance, 40);
        if (dirtyNotifications.get() != before + 1) throw new AssertionError("native tracking callback was not called once");
        MinecraftAttributes.setRange(attribute, 0, 15);
        KineticEntityAttributes.invalidate(instance);
        value(instance, 15);
        MinecraftAttributes.setRange(attribute, 50, 100);
        KineticEntityAttributes.invalidate(instance);
        value(instance, 50);
        MinecraftAttributes.setRange(attribute, 0, 100);
        KineticEntityAttributes.invalidate(instance);
        value(instance, 40);
        if (instance.getBaseValue() != 10 || instance.getModifiers().size() != 1 || !instance.getModifiers().contains(modifier)) {
            throw new AssertionError("invalidation changed base or modifiers");
        }
        AttributeRangeRegression.expect(NullPointerException.class, () -> KineticEntityAttributes.invalidate(null));
        System.out.println("Attribute invalidation regression passed.");
    }

    private static void value(AttributeInstance instance, double expected) {
        if (instance.getValue() != expected) throw new AssertionError("cached value: " + instance.getValue() + ", expected " + expected);
    }

    private static final class TestInstance extends AttributeInstance implements RuntimeAttributeInstanceAccess {
        TestInstance(AttributeRangeRegression.TestAttribute attribute, AtomicInteger notifications) {
            //? if >=1.20.5 {
            /*super(net.minecraft.core.Holder.direct(attribute), ignored -> notifications.incrementAndGet());
            *///?} else {
            super(attribute, ignored -> notifications.incrementAndGet());
            //?}
        }

        @Override
        public void kineticcore$invalidate() { setDirty(); }
    }
}
