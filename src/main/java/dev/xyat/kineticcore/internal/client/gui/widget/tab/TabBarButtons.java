package dev.xyat.kineticcore.internal.client.gui.widget.tab;

import dev.xyat.kineticcore.api.client.gui.widget.KineticTabBar;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;

import java.util.List;

/** Internal view of a tab bar exposing its button widgets for host registration. */
public interface TabBarButtons extends KineticTabBar {
    /** Returns the managed tab buttons for host registration. */
    List<StateButton> buttons();
}
