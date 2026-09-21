package dev.xyat.kineticcore.api.client.widget.input;

import java.nio.file.Files;
import java.nio.file.Path;

/** Verifies that the input chain preserves scan codes/modifiers and targets the top popup. */
public final class AutoCompleteRoutingContractRegression {
    private static int checks;
    private static final Path SOURCE = Path.of("src/main/java/dev/xyat/kineticcore");

    public static void main(String[] args) throws Exception {
        String autocomplete = source("api/client/widget/input/KineticAutoComplete.java");
        String controls = source("internal/client/screen/KineticScreenControls.java");
        String signature = "handleKeyPressed(int keyCode, int scanCode, int modifiers)";
        check(autocomplete.contains("public boolean " + signature), "field input handler loses keyboard metadata");
        check(autocomplete.contains("super.keyPressed(keyCode, scanCode, modifiers)"), "field input loses Ctrl/Shift/scan code");
        check(autocomplete.contains("box.handleKeyPressed(keyCode, scanCode, modifiers)"), "input group loses keyboard metadata");
        check(controls.contains("handleAutoCompleteKey(int keyCode, int scanCode, int modifiers)"), "controls loses keyboard metadata");
        check(controls.contains("autoCompleteGroup.handleKeyPressed(keyCode, scanCode, modifiers)"), "controls not forwarding keyboard metadata");
        check(!autocomplete.contains("super.keyPressed(keyCode, 0, 0)"), "old shortcut-breaking keyboard forwarding remains");
        for (String screen : new String[] {"KineticScreen", "KineticNativeScreen", "KineticContainerScreen"}) {
            check(source("api/client/screen/" + screen + ".java")
                    .contains("controls.handleAutoCompleteKey(keyCode, scanCode, modifiers)"),
                    screen + " does not forward keyboard metadata");
        }
        // Last popup is drawn on top: mouse input must use reverse order, not first-registered order.
        String start = autocomplete.substring(autocomplete.indexOf("public static class AutoCompleteBoxGroup"));
        for (String method : new String[] {"handleHoveredMouseScrolled", "handleSuggestionClick", "handleMouseDragged"}) {
            int at = start.indexOf("public boolean " + method + "(");
            check(at >= 0, "missing popup input handler: " + method);
            int until = start.indexOf("public boolean ", at + 12);
            String body = start.substring(at, until < 0 ? start.length() : until);
            check(body.contains("for (int index = boxes.size() - 1; index >= 0; index--)"),
                    "background popup takes priority for " + method);
        }
        // The pointer-free API still routes to the top open popup, rather than lower scrollable ones.
        int directWheelStart = start.indexOf("public boolean handleMouseScrolled(double delta)");
        int hoveredWheelStart = start.indexOf("public boolean handleHoveredMouseScrolled(");
        String directWheel = start.substring(directWheelStart, hoveredWheelStart);
        check(directWheel.contains("if (box.isSuggestionPopupOpen())"),
                "pointer-free wheel must select the topmost visible popup");
        check(directWheel.contains("box.handleMouseScrolled(delta);\n                    return true;"),
                "pointer-free wheel must not leak through the top popup at its scroll limit");
        // An open upper popup must consume pointer input even when it cannot scroll or has an empty edge.
        int scrollStart = start.indexOf("public boolean handleHoveredMouseScrolled(");
        int clickStart = start.indexOf("public boolean handleSuggestionClick(");
        int dragStart = start.indexOf("public boolean handleMouseDragged(");
        String scrollBody = start.substring(scrollStart, clickStart);
        String clickBody = start.substring(clickStart, dragStart);
        check(scrollBody.contains("if (box.isSuggestionPopupHovered(mouseX, mouseY))"),
                "scroll must select the foremost hovered popup");
        check(scrollBody.contains("box.handleMouseScrolled(delta);\n                    return true;"),
                "scroll must not leak through an upper popup at its scroll limit");
        check(clickBody.contains("if (box.isSuggestionPopupHovered(mouseX, mouseY))"),
                "click must select the foremost hovered popup");
        check(clickBody.contains("box.handleMouseClick(mouseX, mouseY);\n                    return true;"),
                "click must not leak through an upper popup's empty region");
        System.out.println("PASS: " + checks + " autocomplete routing contract assertions");
    }

    private static String source(String relativePath) throws Exception {
        return Files.readString(SOURCE.resolve(relativePath));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
