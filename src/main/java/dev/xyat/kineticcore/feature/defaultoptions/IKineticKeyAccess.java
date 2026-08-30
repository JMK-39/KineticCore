package dev.xyat.kineticcore.feature.defaultoptions;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.settings.KeyModifier;

public interface IKineticKeyAccess {
    void setkineticcore$defaultKey(InputConstants.Key key);
    void setkineticcore$keyModifierDefault(KeyModifier modifier);
    KeyModifier getkineticcore$keyModifierDefault();
    KeyModifier getkineticcore$keyModifier();
}