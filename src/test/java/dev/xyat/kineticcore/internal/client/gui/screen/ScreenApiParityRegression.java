package dev.xyat.kineticcore.internal.client.gui.screen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 三种 Kinetic Screen 的公共控件/草稿/提示 API 必须保持一致；只有各自坐标系的钩子允许不同。
 * The three Kinetic screens must expose the same shared API surface; only coordinate-specific
 * hooks may differ. Source-based, so it runs without Minecraft on the classpath.
 */
public final class ScreenApiParityRegression {
    private static final Path ROOT = Path.of("src/main/java/dev/xyat/kineticcore/internal/client/gui/screen");
    private static final Pattern DECLARATION = Pattern.compile(
            "\\n    (?:public|protected)\\s+(?:final\\s+|static\\s+|abstract\\s+)*(?:<[^>]*>\\s*)?[\\w.<>\\[\\]?, ]+?\\s+(\\w+)\\s*\\(([^)]*)\\)"
    );
    /** 仅 KineticScreen 的虚拟画布与画布内滚动控件 / Canvas-only APIs of KineticScreen. */
    private static final Pattern SCREEN_SPECIFIC = Pattern.compile(
            "^(canvas\\w*|renderCanvas\\w*|enableCanvasScissor|disableCanvasScissor|isInsideCanvas|useCanvas|layout|layoutLevel"
                    + "|safeArea|isCompactLayout|isPortraitLayout|isUltrawideLayout|renderTooltips|registerPreviewWheelTarget"
                    + "|addScrollable\\w*|addCompactScrollable\\w*|resetScrollableWidgets"
                    + "|container\\w*|isInsideUi|renderScreenOverlay|renderUiForeground|requestContainerTooltips|ui(Width|Height|Scale)"
                    + "|native\\w*|renderNative\\w*|after\\w*|requestWidgetTooltip|tick|KineticScreen|KineticContainerScreen|KineticNativeScreen)\\(.*"
    );

    private ScreenApiParityRegression() {
    }

    public static void main(String[] args) throws IOException {
        Set<String> screen = shared("KineticScreen.java");
        Set<String> container = shared("KineticContainerScreen.java");
        Set<String> nativeScreen = shared("KineticNativeScreen.java");
        List<String> failures = new ArrayList<>();
        report(failures, "KineticContainerScreen", screen, container);
        report(failures, "KineticNativeScreen", screen, nativeScreen);
        report(failures, "KineticScreen", container, screen);
        report(failures, "KineticScreen", nativeScreen, screen);
        if (!failures.isEmpty()) throw new AssertionError("Screen API drift:\n" + String.join("\n", failures));
        System.out.println("PASS: " + screen.size() + " shared screen API signatures are identical across all three screens");
    }

    private static void report(List<String> failures, String target, Set<String> expected, Set<String> actual) {
        for (String signature : expected) {
            if (!actual.contains(signature)) failures.add("  " + target + " is missing " + signature);
        }
    }

    private static Set<String> shared(String file) throws IOException {
        String source = Files.readString(ROOT.resolve(file), StandardCharsets.UTF_8)
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("//[^\\n]*", "");
        Set<String> signatures = new TreeSet<>();
        Matcher matcher = DECLARATION.matcher(source);
        while (matcher.find()) {
            String signature = matcher.group(1) + "(" + normalizeParameters(matcher.group(2)) + ")";
            if (!SCREEN_SPECIFIC.matcher(signature).matches()) signatures.add(signature);
        }
        return signatures;
    }

    private static String normalizeParameters(String parameters) {
        List<String> types = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char ch : parameters.toCharArray()) {
            if (ch == '<') depth++;
            if (ch == '>') depth--;
            if (ch == ',' && depth == 0) {
                types.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (!current.toString().isBlank()) types.add(current.toString());
        List<String> normalized = new ArrayList<>();
        for (String raw : types) {
            String type = raw.replaceAll("@\\w+\\s*", "").trim();
            int lastSpace = type.lastIndexOf(' ');
            if (lastSpace > 0) type = type.substring(0, lastSpace);
            type = type.replaceAll("\\bjava\\.util\\.(function\\.)?", "")
                    .replaceAll("\\b[A-Z]\\b", "T")
                    .replace(" ", "");
            normalized.add(type);
        }
        return String.join(",", normalized);
    }
}
