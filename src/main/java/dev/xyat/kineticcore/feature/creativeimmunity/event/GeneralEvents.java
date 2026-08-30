package dev.xyat.kineticcore.feature.creativeimmunity.event;

import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "kineticcore")
public class GeneralEvents {

    // 定义虚空伤害和 Kill 指令的资源键
    private static final ResourceKey<DamageType> OUT_OF_WORLD = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("out_of_world"));
    private static final ResourceKey<DamageType> GENERIC_KILL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("generic_kill"));

    // 使用线程安全的 Set 记录当前正在主动自杀的玩家 UUID
    private static final Set<UUID> PENDING_SUICIDES = ConcurrentHashMap.newKeySet();

    /**
     * 拦截命令执行事件，判断玩家是否在显式自杀
     */
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        // 检查配置是否开启
        if (!GeneralMechanicsConfig.enableCreativeVoidImmunity) return;

        CommandSourceStack source = event.getParseResults().getContext().getSource();

        // 确保命令是由玩家执行的
        if (!(source.getEntity() instanceof Player player)) return;

        // 获取玩家输入的原始命令字符串并去除首尾空格
        String command = event.getParseResults().getReader().getString().trim();

        // 兼容带斜杠和不带斜杠的输入
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // 精确匹配常见的自杀指令形式
        // 若使用了 /kill @e，则不满足此条件，玩家不会被加入白名单
        if (command.equals("kill") ||
                command.equals("kill @s") ||
                command.equals("kill " + player.getGameProfile().getName())) {

            // 将该玩家加入允许被 generic_kill 杀死的标记集
            PENDING_SUICIDES.add(player.getUUID());
        }
    }

    /**
     * 拦截生物受到攻击的事件
     * Intercept the event where a living entity is attacked
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        // 检查配置是否开启
        if (!GeneralMechanicsConfig.enableCreativeVoidImmunity) return;

        // 检查目标是否为创造模式玩家[cite: 1]
        if (event.getEntity() instanceof Player player && player.isCreative()) {

            // 使用 ResourceKey 进行判定，避免因映射表导致的编译错误[cite: 1]
            if (event.getSource().is(OUT_OF_WORLD)) {
                // 虚空伤害始终拦截[cite: 1]
                event.setCanceled(true);

            } else if (event.getSource().is(GENERIC_KILL)) {
                // 检查玩家是否在自杀白名单中
                if (PENDING_SUICIDES.contains(player.getUUID())) {
                    // 确认是主动自杀，消耗掉该标记并放行伤害
                    PENDING_SUICIDES.remove(player.getUUID());
                } else {
                    // 不是主动自杀（被 /kill @e 误伤），拦截事件以阻止死亡[cite: 1]
                    event.setCanceled(true);
                }
            }
        }
    }
}