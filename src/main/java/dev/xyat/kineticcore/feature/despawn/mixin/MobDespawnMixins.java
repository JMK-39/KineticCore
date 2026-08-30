package dev.xyat.kineticcore.feature.despawn.mixin;

import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import dev.xyat.kineticcore.feature.despawn.LetMeDespawnLogic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class MobDespawnMixins {

    @Mixin(Mob.class)
    public static abstract class MobTweaks {
        @Shadow private boolean persistenceRequired;

        @Unique
        private byte kineticcore$despawnCache = 0;

        @Inject(method = "requiresCustomPersistence", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onRequiresCustomPersistence(CallbackInfoReturnable<Boolean> cir) {
            if (GeneralMechanicsConfig.enableLetMeDespawn) {
                if (this.kineticcore$despawnCache == 0) {
                    Mob entity = (Mob) (Object) this;
                    this.kineticcore$despawnCache = (byte) (LetMeDespawnLogic.shouldForceDespawn(entity) ? 1 : 2);
                }
                if (this.kineticcore$despawnCache == 1) {
                    cir.setReturnValue(false);
                }
            }
        }

        @Inject(method = "checkDespawn", at = @At("HEAD"))
        private void kineticcore$beforeCheckDespawn(CallbackInfo ci) {
            if (GeneralMechanicsConfig.enableLetMeDespawn) {
                if (this.kineticcore$despawnCache == 0) {
                    Mob entity = (Mob) (Object) this;
                    this.kineticcore$despawnCache = (byte) (LetMeDespawnLogic.shouldForceDespawn(entity) ? 1 : 2);
                }
                if (this.kineticcore$despawnCache == 1) {
                    this.persistenceRequired = false;
                }
            }
        }

        @Inject(method = "setItemSlot", at = @At("TAIL"))
        private void kineticcore$onSetItemSlot(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
            this.kineticcore$despawnCache = 0;
        }

        @Inject(method = "setItemSlotAndDropWhenKilled", at = @At("TAIL"))
        private void kineticcore$onSetItemSlotAndDropWhenKilled(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
            this.kineticcore$despawnCache = 0;
            if (GeneralMechanicsConfig.enableLetMeDespawn) {
                Mob entity = (Mob) (Object) this;
                LetMeDespawnLogic.processPersistence(entity, slot);
            }
        }

        @Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
        private void kineticcore$redirectDiscard(Mob instance) {
            if (GeneralMechanicsConfig.enableLetMeDespawn) {
                LetMeDespawnLogic.dropPickedEquipment(instance);
            }
            instance.discard();
        }

        @Inject(method = "remove", at = @At("HEAD"))
        private void kineticcore$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
            if (GeneralMechanicsConfig.enableLetMeDespawn) {
                LetMeDespawnLogic.dropPickedEquipment((Mob) (Object) this);
            }
        }
    }

    @Mixin(EnderMan.class)
    public static abstract class EnderManTweaks {
        @Unique
        private byte kineticcore$enderCache = 0;

        @Inject(method = "requiresCustomPersistence", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onEndermanCustomPersistence(CallbackInfoReturnable<Boolean> cir) {
            if (GeneralMechanicsConfig.enableLetMeDespawn) {
                if (this.kineticcore$enderCache == 0) {
                    EnderMan enderman = (EnderMan) (Object) this;
                    this.kineticcore$enderCache = (byte) (LetMeDespawnLogic.shouldForceDespawn(enderman) ? 1 : 2);
                }
                if (this.kineticcore$enderCache == 1) {
                    cir.setReturnValue(false);
                }
            }
        }

        @Inject(method = "setCarriedBlock", at = @At("TAIL"))
        private void kineticcore$onSetCarriedBlock(BlockState state, CallbackInfo ci) {
            this.kineticcore$enderCache = 0;
        }
    }
}