package dev.xyat.kineticcore.feature.datapack.mixin.client;

import dev.xyat.kineticcore.feature.datapack.recovery.client.FailureCallbacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DatapackLoadFailureScreen.class)
public abstract class DatapackFailureCallbacksMixin implements FailureCallbacks {
    //? if >=1.21 {
    /*@Shadow @Final private Runnable cancelCallback;
    @Shadow @Final private Runnable safeModeCallback;
    public Runnable kineticcore$retry() { return safeModeCallback; }
    public Runnable kineticcore$cancel() { return cancelCallback; }
    *///?} else {
    @Shadow @Final private Runnable callback;
    public Runnable kineticcore$retry() { return callback; }
    public Runnable kineticcore$cancel() { return () -> Minecraft.getInstance().setScreen(null); }
    //?}
}
