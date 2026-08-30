package dev.xyat.kineticcore.feature.defaultoptions.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xyat.kineticcore.feature.defaultoptions.IKineticKeyAccess;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyAccess extends IKineticKeyAccess {
    @Accessor("defaultKey")
    @Mutable
    void setkineticcore$defaultKey(InputConstants.Key key);

    @Accessor(value = "keyModifierDefault", remap = false)
    @Mutable
    void setkineticcore$keyModifierDefault(KeyModifier modifier);

    @Accessor(value = "keyModifierDefault", remap = false)
    KeyModifier getkineticcore$keyModifierDefault();

    @Accessor(value = "keyModifier", remap = false)
    KeyModifier getkineticcore$keyModifier();
}