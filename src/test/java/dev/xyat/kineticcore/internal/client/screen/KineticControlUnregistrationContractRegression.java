package dev.xyat.kineticcore.internal.client.screen;

import java.nio.file.Files;
import java.nio.file.Path;

/** The host must release a widget before Kinetic forgets its input and tooltip state. */
public final class KineticControlUnregistrationContractRegression {
    private static int checks;
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/xyat/kineticcore/internal/client/screen/KineticScreenControls.java"));
        int start = source.indexOf("public final void unregisterWidget(AbstractWidget widget)");
        int end = source.indexOf("/** Registers one Kinetic smooth selection list", start);
        check(start >= 0 && end > start, "missing unregister method");
        String method = source.substring(start, end);
        int host = method.indexOf("removeWidget.accept(widget);");
        check(host >= 0, "host widget removal missing");
        check(method.indexOf("if (widget == null) return;") < host, "null must be rejected first");
        for (String cleanup : new String[]{
                "widgetTooltips.remove(widget);", "registeredRenderables.remove(widget);",
                "tooltipHitOrder.removeIf(", "autoCompleteBoxes.remove(box)"}) {
            check(method.indexOf(cleanup) > host, "cleanup happens before host confirms removal: " + cleanup);
        }
        System.out.println("PASS: " + checks + " unregister ordering contracts");
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
