package dev.xyat.kineticcore.internal.mixin.api.client;

import dev.xyat.kineticcore.internal.client.input.KeyMappingAccess;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyAccess extends KeyMappingAccess {
    @Accessor("defaultKey")
    @Mutable
    void kineticcore$setDefaultKey(InputConstants.Key key);

    @Accessor(value = "keyModifierDefault", remap = false)
    @Mutable
    void kineticcore$setDefaultModifier(KeyModifier modifier);

    @Accessor(value = "keyModifierDefault", remap = false)
    KeyModifier kineticcore$getDefaultModifier();

    @Accessor(value = "keyModifier", remap = false)
    KeyModifier kineticcore$getModifier();
}
