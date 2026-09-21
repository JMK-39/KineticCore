package dev.xyat.kineticcore.feature.creativeimmunity.event;


import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.entity.event.KineticLivingEvents;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GeneralEvents {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticServerEvents.onCommand(KineticEventPriority.NORMAL, GeneralEvents::onCommand),
                () -> KineticLivingEvents.onAttack(KineticEventPriority.NORMAL, GeneralEvents::onLivingAttack)
        );
    }


    // 定义虚空伤害和 Kill 指令的资源键
    private static final ResourceKey<DamageType> OUT_OF_WORLD = ResourceKey.create(Registries.DAMAGE_TYPE, KineticResourceIds.parse("out_of_world"));
    private static final ResourceKey<DamageType> GENERIC_KILL = ResourceKey.create(Registries.DAMAGE_TYPE, KineticResourceIds.parse("generic_kill"));

    // 使用线程安全的 Set 记录当前正在主动自杀的玩家 UUID
    private static final Set<UUID> PENDING_SUICIDES = ConcurrentHashMap.newKeySet();

    /**
     * 拦截命令执行事件，判断玩家是否在显式自杀
     */
    public static void onCommand(KineticServerEvents.CommandContext context) {
        // 检查配置是否开启
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "void_immunity", true)) return;

        // 确保命令是由玩家执行的
        if (!(context.source().getEntity() instanceof Player player)) return;

        // 获取玩家输入的原始命令字符串并去除首尾空格
        String command = context.command().trim();

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
    public static void onLivingAttack(KineticLivingEvents.AttackContext context) {
        // 检查配置是否开启
        if (!KTServerConfigApi.getBoolean("kineticcore:general_mechanics", "void_immunity", true)) return;

        // 检查目标是否为创造模式玩家[cite: 1]
        if (context.entity() instanceof Player player && player.isCreative()) {

            // 使用 ResourceKey 进行判定，避免因映射表导致的编译错误[cite: 1]
            if (context.source().is(OUT_OF_WORLD)) {
                // 虚空伤害始终拦截[cite: 1]
                context.cancel();

            } else if (context.source().is(GENERIC_KILL)) {
                // 检查玩家是否在自杀白名单中
                if (PENDING_SUICIDES.contains(player.getUUID())) {
                    // 确认是主动自杀，消耗掉该标记并放行伤害
                    PENDING_SUICIDES.remove(player.getUUID());
                } else {
                    // 不是主动自杀（被 /kill @e 误伤），拦截事件以阻止死亡[cite: 1]
                    context.cancel();
                }
            }
        }
    }
}
