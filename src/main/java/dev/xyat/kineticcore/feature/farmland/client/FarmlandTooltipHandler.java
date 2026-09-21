package dev.xyat.kineticcore.feature.farmland.client;


import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class FarmlandTooltipHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(() -> KineticItemTooltips.onBuild(FarmlandTooltipHandler::onTooltip));
    }


    public static void onTooltip(ItemStack stack, List<Component> tooltip) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "farmland", true)) return;

        if (stack.isEmpty()) return;

        if (EnchantmentHelper.getEnchantments(stack).containsKey(Enchantments.FALL_PROTECTION)) {
            String targetName = KineticI18n.translatable(Enchantments.FALL_PROTECTION.getDescriptionId()).getString();
            boolean inserted = false;

            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getString().contains(targetName)) {
                    tooltip.add(i + 1, KineticI18n.translatable("tip.kineticcore.farmland_protection"));
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                tooltip.add(KineticI18n.translatable("tip.kineticcore.farmland_protection"));
            }
        }
    }
}
