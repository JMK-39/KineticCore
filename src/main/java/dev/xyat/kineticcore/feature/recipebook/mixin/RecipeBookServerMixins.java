package dev.xyat.kineticcore.feature.recipebook.mixin;

import com.google.gson.JsonElement;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import dev.xyat.kineticcore.KineticCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RecipeBookServerMixins {

    @Mixin(ServerRecipeBook.class)
    public static class BookData {
        @Inject(method = "toNbt", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onSave(CallbackInfoReturnable<CompoundTag> cir) {
            if (GeneralMechanicsConfig.removeRecipeBook) cir.setReturnValue(new CompoundTag());
        }

        @Inject(method = "fromNbt", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onLoad(CompoundTag pTag, RecipeManager pRecipeManager, CallbackInfo ci) {
            if (GeneralMechanicsConfig.removeRecipeBook) ci.cancel();
        }

        @Inject(method = "sendRecipes", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onSend(ClientboundRecipePacket.State pState, ServerPlayer pPlayer, List<ResourceLocation> pRecipes, CallbackInfo ci) {
            if (GeneralMechanicsConfig.removeRecipeBook) ci.cancel();
        }
    }

    @Mixin(ServerAdvancementManager.class)
    public static class Advancements {
        @Inject(method = "apply*", at = @At("HEAD"))
        private void kineticcore$filterRecipeAdvancements(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller pf, CallbackInfo ci) {
            if (!GeneralMechanicsConfig.removeRecipeBook) return;
            int removed = 0;
            Iterator<Map.Entry<ResourceLocation, JsonElement>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getKey().getPath().startsWith("recipes/")) {
                    it.remove();
                    removed++;
                }
            }
            if (removed > 0) KineticCore.LOGGER.info("RecipeBook Purged: {} advancements.", removed);
        }
    }

    @Mixin(ServerPlayer.class)
    public static class Player {
        @Inject(method = "awardRecipes", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onAward(Collection<Recipe<?>> recipes, CallbackInfoReturnable<Integer> cir) {
            if (GeneralMechanicsConfig.removeRecipeBook) cir.setReturnValue(0);
        }

        @Inject(method = "awardRecipesByKey", at = @At("HEAD"), cancellable = true)
        private void kineticcore$onAwardKey(ResourceLocation[] resourceLocations, CallbackInfo ci) {
            if (GeneralMechanicsConfig.removeRecipeBook) ci.cancel();
        }
    }
}
