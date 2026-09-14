package dev.xyat.kineticcore.internal.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.settings.KeyModifier;

public interface KeyMappingAccess {
    void kineticcore$setDefaultKey(InputConstants.Key key);

    void kineticcore$setDefaultModifier(KeyModifier modifier);

    KeyModifier kineticcore$getDefaultModifier();

    KeyModifier kineticcore$getModifier();
}
