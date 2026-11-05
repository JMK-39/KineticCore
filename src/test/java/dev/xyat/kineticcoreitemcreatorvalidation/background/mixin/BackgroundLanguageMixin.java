package dev.xyat.kineticcoreitemcreatorvalidation.background.mixin;

import dev.xyat.kineticcoreitemcreatorvalidation.background.BackgroundValidationMode;
import net.minecraft.client.resources.language.LanguageManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** All versions: with -Dkineticcore.guiValidation.language every capture fixture renders in that one language. */
@Mixin(LanguageManager.class)
public abstract class BackgroundLanguageMixin {
    @ModifyVariable(method = "setSelected", at = @At("HEAD"), argsOnly = true)
    private String kineticValidation$forcedLanguage(String code) {
        String forced = BackgroundValidationMode.language();
        return forced == null ? code : forced;
    }
}
