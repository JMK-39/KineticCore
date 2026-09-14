package dev.xyat.kineticcore.feature.mining.event;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.feature.mining.client.MiningModeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class MiningInputHandler {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.START, MiningInputHandler::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(MiningInputHandler::onLeftClickBlock);
    }

    private static BlockPos miningPos = null;

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (miningPos != null) {
            if (!mc.options.keyAttack.isDown()) {
                miningPos = null;
            } else {
                boolean targetChanged = true;
                if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
                    if (blockHit.getBlockPos().equals(miningPos)) {
                        targetChanged = false;
                    }
                }

                if (targetChanged) {
                    mc.options.keyAttack.setDown(false);
                    miningPos = null;
                }
            }
        }
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide()) return;

        // 核心修改：不再判断手持物品和附魔，直接读取全局客户端模式状态
        if (MiningModeClient.isSingleModeClientSide) {
            Minecraft mc = Minecraft.getInstance();
            if (miningPos != null && !miningPos.equals(event.getPos())) {
                event.setCanceled(true);
                mc.options.keyAttack.setDown(false);
                return;
            }
            miningPos = event.getPos();
        }
    }
}
