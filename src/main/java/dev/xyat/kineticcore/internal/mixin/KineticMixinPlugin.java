package dev.xyat.kineticcore.internal.mixin;

import dev.xyat.kineticcore.internal.runtime.FeatureSwitchRuntime;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class KineticMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        FeatureSwitchRuntime.initialize();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("MiniEffectsMixins$InventoryEffectRendererGuiHandlerMixin")
                && classMissing("mezz.jei.api.IModPlugin")) {
            return false;
        }
        return FeatureSwitchRuntime.shouldApplyMixin(mixinClassName);
    }

    private static boolean classMissing(String className) {
        try {
            Class.forName(className, false, KineticMixinPlugin.class.getClassLoader());
            return false;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }
}
