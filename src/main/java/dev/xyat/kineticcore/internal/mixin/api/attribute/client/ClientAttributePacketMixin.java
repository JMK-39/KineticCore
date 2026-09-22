package dev.xyat.kineticcore.internal.mixin.api.attribute.client;

import dev.xyat.kineticcore.api.registry.KineticEntityAttributes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Before vanilla reads an attribute snapshot, create any missing registered instances on the client. */
@Mixin(ClientPacketListener.class)
public abstract class ClientAttributePacketMixin {
    @Shadow private ClientLevel level;

    @Inject(method = "handleUpdateAttributes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeMap;getInstance(Lnet/minecraft/world/entity/ai/attributes/Attribute;)Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;"))
    private void kineticcore$prepareDynamicAttributes(ClientboundUpdateAttributesPacket packet, CallbackInfo ci) {
        if (level == null || !(level.getEntity(packet.getEntityId()) instanceof LivingEntity living)) return;
        for (ClientboundUpdateAttributesPacket.AttributeSnapshot snapshot : packet.getValues()) {
            KineticEntityAttributes.ensureInstance(living, snapshot.getAttribute());
        }
    }
}
