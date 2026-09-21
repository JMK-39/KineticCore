package dev.xyat.kineticcore.feature.farmland.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.world.event.KineticWorldEvents;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class FarmlandProtectionHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(() -> KineticWorldEvents.onFarmlandTrample(KineticEventPriority.NORMAL, FarmlandProtectionHandler::onFarmlandTrample));
    }


    public static void onFarmlandTrample(KineticWorldEvents.FarmlandTrampleContext context) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "farmland", true)) return;
        if (context.entity() instanceof Player player) {
            ItemStack boots = player.getInventory().getArmor(0);
            if (!boots.isEmpty() && EnchantmentHelper.getTagEnchantmentLevel(Enchantments.FALL_PROTECTION, boots) > 0) {
                context.cancel();
            }
        }
    }
}
