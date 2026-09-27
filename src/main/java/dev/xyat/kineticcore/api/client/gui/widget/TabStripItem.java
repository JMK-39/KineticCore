package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;

/** One variable-width tab description for a {@link KineticTabStrip}. */
public record TabStripItem(Component label, Component selectedLabel, Component tooltip) {
    /**
     * Creates a new scrollable tab instance.
     */
    public TabStripItem(Component label, Component tooltip) {
        this(label, label, tooltip);
    }
}
