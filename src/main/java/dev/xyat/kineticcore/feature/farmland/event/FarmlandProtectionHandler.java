package dev.xyat.kineticcore.feature.farmland.event;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KineticCore.MODID)
public class FarmlandProtectionHandler {

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!GeneralMechanicsConfig.enableFarmlandProtection) return;
        if (event.getEntity() instanceof Player player) {
            ItemStack boots = player.getInventory().getArmor(0);
            if (!boots.isEmpty() && EnchantmentHelper.getTagEnchantmentLevel(Enchantments.FALL_PROTECTION, boots) > 0) {
                event.setCanceled(true);
            }
        }
    }
}