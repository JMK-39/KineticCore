package dev.xyat.kineticcore.feature.mining.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.player.event.KineticPlayerEvents;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.feature.mining.client.MiningModeClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MiningInputHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticClientEvents.onTick(KineticClientEvents.TickPhase.START, MiningInputHandler::onClientTick),
                () -> KineticPlayerEvents.onLeftClickBlock(KineticEventPriority.NORMAL, MiningInputHandler::onLeftClickBlock)
        );
    }

    private static BlockPos miningPos = null;

    public static void onClientTick() {
        if (KineticClientRuntime.localPlayer() == null || KineticClientRuntime.currentLevel() == null) return;

        if (miningPos != null) {
            if (!KineticClientRuntime.attackKeyDown()) {
                miningPos = null;
            } else {
                boolean targetChanged = true;
                HitResult hitResult = KineticClientRuntime.hitResult();
                if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hitResult;
                    if (blockHit.getBlockPos().equals(miningPos)) {
                        targetChanged = false;
                    }
                }

                if (targetChanged) {
                    KineticClientRuntime.setAttackKeyDown(false);
                    miningPos = null;
                }
            }
        }
    }

    public static void onLeftClickBlock(KineticPlayerEvents.LeftClickBlockContext context) {
        if (!context.player().level().isClientSide()) return;

        // 核心修改：不再判断手持物品和附魔，直接读取全局客户端模式状态
        if (MiningModeClient.isSingleModeClientSide) {
            if (miningPos != null && !miningPos.equals(context.pos())) {
                context.cancel();
                KineticClientRuntime.setAttackKeyDown(false);
                return;
            }
            miningPos = context.pos();
        }
    }
}
