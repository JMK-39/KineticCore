package dev.xyat.kineticcore.internal.client.screen;

import dev.xyat.kineticcore.internal.client.gui.theme.SelectionPulse;

public final class DeleteAndSelectionRegression {
    public static void main(String[] args) {
        var binding = new KineticDeleteAction();
        int[] count = {0}; boolean[] enabled = {false};
        require(!binding.handle(261, false), "unbound Delete must do nothing");
        binding.bind(() -> enabled[0], () -> count[0]++);
        require(!binding.handle(261, false), "unavailable selection must do nothing");
        enabled[0] = true;
        require(!binding.handle(261, true) && count[0] == 0, "text deletion must not delete an entry");
        require(!binding.handle(257, false), "Enter outside a dialog must not delete");
        require(binding.handle(261, false) && count[0] == 1, "Delete invokes the registered operation once");
        int color = 0xff55dd88;
        require(SelectionPulse.color(color, 0) == color, "pulse peak retains the exact semantic color");
        int dim = SelectionPulse.color(color, 700);
        require(dim != color && dim >>> 24 == 255, "pulse brightens while staying opaque");
        require(SelectionPulse.color(color, 1400) == color, "pulse wraps without a discontinuity");
        require((dim >>> 8 & 255) >= 140, "selected green border remains readable");
        System.out.println("PASS registered Delete admission, text editing priority and opaque selection breathing");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
