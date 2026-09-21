package dev.xyat.kineticcore.feature.copyitem.client;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.inventory.KineticItemFluids;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;


public class ItemDetailPrinter {
    public static void showItemInfo(Player player, ItemStack stack) {
        player.sendSystemMessage(KineticText.translatable("msg.kineticcore.copyitem.copy.chat_output.header"));

        ResourceLocation itemReg = KineticRegistries.items().id(stack.getItem());
        String itemId = itemReg != null ? itemReg.toString() : "minecraft:air";
        player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.id", Component.literal(itemId)), "\"" + itemId + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.item_id")));

        stack.getItemHolder().tags().toList().forEach(tag -> {
            int size = KineticRegistries.items().valuesInTag(tag).size();
            String tagStr = "#" + tag.location();
            player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.tag", Component.literal(tagStr)), "\"" + tagStr + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.item_tag", Component.literal(String.valueOf(size)))));
        });

        String modId = itemReg != null ? itemReg.getNamespace() : "minecraft";
        long modItemCount = KineticRegistries.items().entries().keySet().stream().filter(id -> id.getNamespace().equals(modId)).count();
        String modStr = "@" + modId;
        player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.mod", Component.literal(modStr)), "\"" + modStr + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.mod", Component.literal(String.valueOf(modItemCount)))));

        if (stack.getItem() instanceof BlockItem blockItem) {
            player.sendSystemMessage(KineticText.translatable("msg.kineticcore.copyitem.copy.chat_output.block"));
            ResourceLocation blockReg = KineticRegistries.blocks().id(blockItem.getBlock());
            String blockId = blockReg != null ? blockReg.toString() : "minecraft:air";
            player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.id", Component.literal(blockId)), "\"" + blockId + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.block_id")));

            blockItem.getBlock().builtInRegistryHolder().tags().toList().forEach(tag -> {
                int size = KineticRegistries.blocks().valuesInTag(tag).size();
                String tagStr = "#" + tag.location();
                player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.tag", Component.literal(tagStr)), "\"" + tagStr + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.block_tag", Component.literal(String.valueOf(size)))));
            });
        }

        KineticItemFluids.containedFluid(stack).ifPresent(fluid -> {
            player.sendSystemMessage(KineticText.translatable("msg.kineticcore.copyitem.copy.chat_output.fluid"));
            ResourceLocation fluidReg = KineticRegistries.fluids().id(fluid);
            String fluidId = fluidReg != null ? fluidReg.toString() : "minecraft:empty";
            player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.id", Component.literal(fluidId)), "\"" + fluidId + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.fluid_id")));

            fluid.builtInRegistryHolder().tags().toList().forEach(tag -> {
                int size = KineticRegistries.fluids().valuesInTag(tag).size();
                String tagStr = "#" + tag.location();
                player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.tag", Component.literal(tagStr)), "\"" + tagStr + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.fluid_tag", Component.literal(String.valueOf(size)))));
            });
        });
    }

    private static Component copy(Component display, String clipboard, Component hoverInfo) {
        return display.copy().withStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, clipboard))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverInfo)));
    }
}