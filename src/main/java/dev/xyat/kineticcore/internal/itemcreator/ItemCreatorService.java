package dev.xyat.kineticcore.internal.itemcreator;

import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import java.util.Optional;
*///?} else {
import net.minecraft.world.item.alchemy.PotionUtils;
//?}

public final class ItemCreatorService {
    private ItemCreatorService() { }
    public static KineticItemCreator.Result give(ServerPlayer player, ItemCreatorProtocol.Request request) {
        if (!player.hasPermissions(2)) return new KineticItemCreator.Result(request.requestId(), false, "gui.kineticcore.item_creator.permission");
        try {
            ItemStack stack = request.template() != null ? buildEgg(player.level(), request.template()) : buildPotion(request.kind(), request.effects(), request.name());
            var inventory = player.getInventory();
            int slot = inventory.getSlotWithRemainingSpace(stack);
            if (slot < 0) slot = inventory.getFreeSlot();
            // Avoid Inventory.add(-1, ...) silently deleting undeliverable items for creative players.
            if (slot < 0 || !inventory.add(slot, stack)) {
                var dropped = player.drop(stack, false);
                if (dropped == null || dropped.isRemoved() || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)
                        || level.getEntity(dropped.getUUID()) != dropped)
                    return new KineticItemCreator.Result(request.requestId(), false, "gui.kineticcore.item_creator.delivery_failed");
            }
            player.containerMenu.broadcastChanges();
            return new KineticItemCreator.Result(request.requestId(), true, "gui.kineticcore.item_creator.generated");
        } catch (RuntimeException exception) {
            dev.xyat.kineticcore.api.runtime.KineticRuntime.logger().warn("Cannot create requested item", exception);
            return new KineticItemCreator.Result(request.requestId(), false, "gui.kineticcore.item_creator.invalid");
        }
    }
    public static ItemStack buildEgg(Level level, SpawnEggTemplate template) {
        ItemCreatorValidation.validateTemplate(template);
        var id = KineticResourceIds.tryParse(template.entityId());
        if (!KineticRegistries.entityTypes().contains(id)) throw new IllegalArgumentException("Unknown entity");
        var type = KineticRegistries.entityTypes().get(id);
        if (!(type.create(level) instanceof LivingEntity)) throw new IllegalArgumentException("Not a living entity");
        ItemStack stack;
        //? if >=26.1 {
        /*stack = SpawnEggItem.byId(type).map(holder -> new ItemStack(holder)).orElseThrow(() -> new IllegalArgumentException("Missing spawn egg"));
        *///?} else {
        var egg = SpawnEggItem.byId(type);
        if (egg == null) throw new IllegalArgumentException("Missing spawn egg");
        stack = new ItemStack(egg);
        //?}
        for (var spec : template.attributes()) if (!KineticRegistries.attributes().contains(KineticResourceIds.tryParse(spec.id()))) throw new IllegalArgumentException("Unknown attribute");
        validateRegisteredEffects(template.effects());
        ItemCreatorStackData.writeTemplate(stack, template);
        name(stack, template.name().isEmpty() ? Component.translatable("gui.kineticcore.item_creator.special_egg", type.getDescription()) : Component.literal(template.name()));
        return stack;
    }
    public static ItemStack buildPotion(KineticItemCreator.PotionKind kind, List<EffectSpec> effects) {
        return buildPotion(kind, effects, "");
    }
    public static ItemStack buildPotion(KineticItemCreator.PotionKind kind, List<EffectSpec> effects, String customName) {
        customName = ItemCreatorValidation.validateName(customName);
        if (effects.isEmpty()) throw new IllegalArgumentException("Select an effect");
        ItemCreatorValidation.validateEffects(effects);
        validateRegisteredEffects(effects);
        ItemStack stack = new ItemStack(switch (kind) { case NORMAL -> Items.POTION; case SPLASH -> Items.SPLASH_POTION; case LINGERING -> Items.LINGERING_POTION; });
        List<MobEffectInstance> instances = new ArrayList<>();
        for (var spec : effects) {
            var effect = KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(spec.id()));
            //? if >=1.21 {
            /*var holder = KineticRegistries.mobEffects().holder(effect);
            *///?} else {
            var holder = effect;
            //?}
            instances.add(new MobEffectInstance(holder, spec.durationTicks(), spec.amplifier(), false, spec.particles(), spec.icon()));
        }
        //? if >=26.1 {
        /*stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), instances, Optional.empty()));
        *///?} else if >=1.20.5 {
        /*stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), instances));
        *///?} else {
        PotionUtils.setCustomEffects(stack, instances);
        //?}
        name(stack, customName.isEmpty() ? Component.translatable("gui.kineticcore.item_creator.custom_potion") : Component.literal(customName));
        return stack;
    }
    private static void validateRegisteredEffects(List<EffectSpec> effects) {
        for (var spec : effects) {
            var id = KineticResourceIds.tryParse(spec.id());
            if (!KineticRegistries.mobEffects().contains(id)) throw new IllegalArgumentException("Unknown effect");
            if (spec.durationTicks() == -1 && KineticRegistries.mobEffects().get(id).isInstantenous()) throw new IllegalArgumentException("Instant effect cannot be permanent");
        }
    }
    private static void name(ItemStack stack, Component name) {
        //? if >=1.20.5 {
        /*stack.set(DataComponents.CUSTOM_NAME, name);
        *///?} else {
        stack.setHoverName(name);
        //?}
    }
}
