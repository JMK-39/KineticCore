package dev.xyat.kineticcore.feature.farmland.event;

import net.minecraftforge.common.MinecraftForge;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.level.BlockEvent;

public class FarmlandProtectionHandler {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(FarmlandProtectionHandler::onFarmlandTrample);
    }


    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "farmland", true)) return;
        if (event.getEntity() instanceof Player player) {
            ItemStack boots = player.getInventory().getArmor(0);
            if (!boots.isEmpty() && EnchantmentHelper.getTagEnchantmentLevel(Enchantments.FALL_PROTECTION, boots) > 0) {
                event.setCanceled(true);
            }
        }
    }
}
