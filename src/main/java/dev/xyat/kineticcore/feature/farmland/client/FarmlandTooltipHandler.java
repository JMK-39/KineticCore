package dev.xyat.kineticcore.feature.farmland.client;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KineticCore.MODID, value = Dist.CLIENT)
public class FarmlandTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!GeneralMechanicsConfig.enableFarmlandProtection) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        if (EnchantmentHelper.getEnchantments(stack).containsKey(Enchantments.FALL_PROTECTION)) {
            List<Component> tooltip = event.getToolTip();
            String targetName = Component.translatable(Enchantments.FALL_PROTECTION.getDescriptionId()).getString();
            boolean inserted = false;

            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getString().contains(targetName)) {
                    tooltip.add(i + 1, Component.translatable("tip.kineticcore.farmland_protection"));
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                tooltip.add(Component.translatable("tip.kineticcore.farmland_protection"));
            }
        }
    }
}