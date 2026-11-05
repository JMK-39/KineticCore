package dev.xyat.kineticcoreitemcreatorvalidation;

@net.minecraftforge.fml.common.Mod("kineticcore_itemcreator_validation")
public final class ItemCreatorValidationEntry {
    public ItemCreatorValidationEntry() {
        if (Boolean.getBoolean("kineticcore.spawnerEggValidation")) {
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> SpawnerEggValidation::install);
            return;
        }
        if (Boolean.getBoolean("kineticcore.configGuiValidation")) {
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> ConfigGuiValidation::install);
            return;
        }
        if (Boolean.getBoolean("kineticcore.injectedClickValidation")) {
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> InjectedClickValidation::install);
            return;
        }
        if (Boolean.getBoolean("kineticcore.structureViewValidation")) {
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> StructureViewValidation::install);
            return;
        }
        if (Boolean.getBoolean("kineticcore.layoutEditorValidation")) {
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> LayoutEditorValidation::install);
            return;
        }
        if (Boolean.getBoolean("kineticcore.itemListGuiValidation")) {
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> ItemListGuiValidation::install);
            return;
        }
        if (Boolean.getBoolean("kineticcore.itemCreatorValidation")) {
            dev.xyat.kineticcore.internal.registry.KineticDeferredRegistryRuntime.register(
                    net.minecraft.core.registries.Registries.MOB_EFFECT,
                    dev.xyat.kineticcore.api.resource.KineticResourceIds.of("kineticcore_itemcreator_validation", "fixture_buff"), FixtureBuff::new);
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> ItemCreatorPackRuntime::install);
        }
    }
    private static final class FixtureBuff extends net.minecraft.world.effect.MobEffect {
        FixtureBuff() { super(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x25DD88); }
    }
}
