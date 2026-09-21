package dev.xyat.kineticcore.internal.client.screen;

import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class GuiTestScreens {
    private GuiTestScreens() {
    }

    static final class Plain extends Screen {
        Plain() {
            super(Component.empty());
        }
    }

    static final class Kinetic extends KineticScreen {
        Kinetic() {
            super(Component.empty());
        }

        @Override
        protected void buildUi() {
        }
    }
}
