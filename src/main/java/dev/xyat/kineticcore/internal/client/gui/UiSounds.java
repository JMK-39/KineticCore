package dev.xyat.kineticcore.internal.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** 界面音效 / UI sounds. */
public final class UiSounds {
    private UiSounds() {
    }

    /** 原版按钮点击音效 / Vanilla button click sound. */
    public static void buttonClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.0F));
    }
}
