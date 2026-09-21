package dev.xyat.kineticcore.api.client.widget.input;

import java.nio.file.Files;
import java.nio.file.Path;

/** Ensures secondary clicks never reach a widget hidden behind a suggestion popup. */
public final class AutoCompletePopupClickRegression {
    public static void main(String[] args) throws Exception {
        String group = Files.readString(Path.of("src/main/java/dev/xyat/kineticcore/api/client/widget/input/KineticAutoComplete.java"));
        String controls = Files.readString(Path.of("src/main/java/dev/xyat/kineticcore/internal/client/screen/KineticScreenControls.java"));
        require(group.contains("public boolean isAnySuggestionPopupHovered(double mouseX, double mouseY)"), "popup occlusion check missing");
        require(group.contains("boxes.get(index).isSuggestionPopupHovered(mouseX, mouseY)"), "occlusion check does not inspect open popup geometry");
        int start = controls.indexOf("public boolean handleAutoCompleteClick(double mouseX, double mouseY, int button)");
        int end = controls.indexOf("public boolean handleAutoCompleteDragged(", start);
        require(start >= 0 && end > start, "click router missing");
        String method = controls.substring(start, end);
        require(method.contains("button == 0") && method.contains("handleSuggestionClick(mouseX, mouseY)"), "primary click no longer selects suggestions");
        require(method.contains("isAnySuggestionPopupHovered(mouseX, mouseY)"), "non-primary click penetrates suggestion popup");
        require(!method.contains("button == 0 &&"), "non-primary click still returns false without checking occlusion");
        System.out.println("PASS: 5 popup click routing contracts");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
