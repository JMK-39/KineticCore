package dev.xyat.kineticcore.feature.copyitem.client;

import dev.xyat.kineticcore.api.client.text.KineticText;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public class ItemDetailPrinter {
    public static void showItemInfo(Player player, ItemStack stack) {
        player.sendSystemMessage(KineticText.translatable("msg.kineticcore.copyitem.copy.chat_output.header"));

        ResourceLocation itemReg = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = itemReg != null ? itemReg.toString() : "minecraft:air";
        player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.id", Component.literal(itemId)), "\"" + itemId + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.item_id")));

        stack.getItemHolder().tags().toList().forEach(tag -> {
            int size = BuiltInRegistries.ITEM.getTag(tag).map(HolderSet::size).orElse(0);
            String tagStr = "#" + tag.location();
            player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.tag", Component.literal(tagStr)), "\"" + tagStr + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.item_tag", Component.literal(String.valueOf(size)))));
        });

        String modId = itemReg != null ? itemReg.getNamespace() : "minecraft";
        long modItemCount = ForgeRegistries.ITEMS.getValues().stream().filter(item -> ForgeRegistries.ITEMS.getKey(item) != null && Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).getNamespace().equals(modId)).count();
        String modStr = "@" + modId;
        player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.mod", Component.literal(modStr)), "\"" + modStr + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.mod", Component.literal(String.valueOf(modItemCount)))));

        if (stack.getItem() instanceof BlockItem blockItem) {
            player.sendSystemMessage(KineticText.translatable("msg.kineticcore.copyitem.copy.chat_output.block"));
            ResourceLocation blockReg = ForgeRegistries.BLOCKS.getKey(blockItem.getBlock());
            String blockId = blockReg != null ? blockReg.toString() : "minecraft:air";
            player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.id", Component.literal(blockId)), "\"" + blockId + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.block_id")));

            blockItem.getBlock().builtInRegistryHolder().tags().toList().forEach(tag -> {
                int size = BuiltInRegistries.BLOCK.getTag(tag).map(HolderSet::size).orElse(0);
                String tagStr = "#" + tag.location();
                player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.tag", Component.literal(tagStr)), "\"" + tagStr + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.block_tag", Component.literal(String.valueOf(size)))));
            });
        }

        FluidUtil.getFluidContained(stack).ifPresent(fluidStack -> {
            if (!fluidStack.isEmpty()) {
                player.sendSystemMessage(KineticText.translatable("msg.kineticcore.copyitem.copy.chat_output.fluid"));
                ResourceLocation fluidReg = ForgeRegistries.FLUIDS.getKey(fluidStack.getFluid());
                String fluidId = fluidReg != null ? fluidReg.toString() : "minecraft:empty";
                player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.id", Component.literal(fluidId)), "\"" + fluidId + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.fluid_id")));

                fluidStack.getFluid().builtInRegistryHolder().tags().toList().forEach(tag -> {
                    int size = BuiltInRegistries.FLUID.getTag(tag).map(HolderSet::size).orElse(0);
                    String tagStr = "#" + tag.location();
                    player.sendSystemMessage(copy(KineticText.translatable("msg.kineticcore.copyitem.copy.format.tag", Component.literal(tagStr)), "\"" + tagStr + "\"", KineticText.translatable("msg.kineticcore.copyitem.copy.hover.fluid_tag", Component.literal(String.valueOf(size)))));
                });
            }
        });
    }

    private static Component copy(Component display, String clipboard, Component hoverInfo) {
        return display.copy().withStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, clipboard))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverInfo)));
    }
}