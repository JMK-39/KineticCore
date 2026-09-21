package dev.xyat.kineticcore.internal.client.screen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Guards registration failure recovery and tooltip replacement at the shared GUI registration layer. */
public final class KineticControlRegistrationContractRegression {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/xyat/kineticcore/internal/client/screen/KineticScreenControls.java"));
        String renderable = body(source, "private void registerRenderable(AbstractWidget widget)",
                "private void registerEvent(ObjectSelectionList<?> widget)");
        check(renderable.contains("if (!registeredRenderables.add(widget)) return;"),
                "Repeated successful renderable registrations must stay idempotent");
        check(Pattern.compile("try\\s*\\{\\s*addRenderable\\.accept\\(widget\\);\\s*\\}\\s*catch\\s*\\(RuntimeException\\s*\\|\\s*Error\\s+\\w+\\)").matcher(renderable).find(),
                "A failing renderable registration must roll back its reservation");
        check(renderable.contains("registeredRenderables.remove(widget);"),
                "A failed renderable must be eligible for retry");
        check(renderable.indexOf("addRenderable.accept(widget);") < renderable.indexOf("tooltipHitOrder.add(widget);"),
                "A failed host registration must not leave tooltip hit state");
        check(renderable.indexOf("addRenderable.accept(widget);") < renderable.indexOf("autoCompleteBoxes.add(box);"),
                "A failed host registration must not enter autocomplete routing");

        String event = body(source, "private void registerEvent(ObjectSelectionList<?> widget)",
                "private void openContextMenu(");
        check(event.contains("if (!registeredEventLists.add(widget)) return;"),
                "Repeated successful event list registrations must stay idempotent");
        check(Pattern.compile("try\\s*\\{\\s*addEvent\\.accept\\(widget\\);\\s*\\}\\s*catch\\s*\\(RuntimeException\\s*\\|\\s*Error\\s+\\w+\\)").matcher(event).find(),
                "A failing event list registration must roll back its reservation");
        check(event.contains("registeredEventLists.remove(widget);"),
                "A failed event list must be eligible for retry");

        String generic = body(source, "public final <T extends AbstractWidget> T registerWidget(T widget, Component tooltip)",
                "public final void unregisterWidget(AbstractWidget widget)");
        check(generic.contains("registerWidgetTooltip(widget, tooltip);"),
                "Explicit null or blank tooltip must clear a previous tooltip on re-registration");
        check(!generic.contains("if (tooltip != null && !tooltip.getString().isBlank())"),
                "Tooltip re-registration must not silently keep a stale tooltip");
        System.out.println("PASS: " + assertions + " registration lifecycle contracts");
    }

    private static String body(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        if (start < 0) throw new AssertionError("Missing method " + startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        if (end < 0) throw new AssertionError("Missing method following " + endMarker);
        return source.substring(start, end);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
