package dev.xyat.kineticcore.internal.client.gui.screen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 三种 Kinetic Screen 共用一份实现：共享 API 只存在于 KineticScreenHost 的默认方法与 KineticScreenRuntime 中。
 * <p>
 * The three Kinetic screens share one implementation: the shared API lives only in {@link KineticScreenHost} default
 * methods backed by {@link KineticScreenRuntime}. This check keeps it that way. It fails when
 * <ul>
 *     <li>a host screen stops implementing {@code KineticScreenHost};</li>
 *     <li>a host screen, or any class extending one, redeclares a shared host method (interface defaults cannot be
 *     {@code final}, so this replaces the old {@code public final} guarantee);</li>
 *     <li>a host screen creates its own overlay, control registry or focus state instead of using the runtime;</li>
 *     <li>the screen-specific surfaces that remain drift apart.</li>
 * </ul>
 * Source-based, so it runs without Minecraft on the classpath.
 */
public final class ScreenApiParityRegression {
    private static final Path SCREEN_ROOT = Path.of("src/main/java/dev/xyat/kineticcore/internal/client/gui/screen");
    private static final List<Path> SOURCE_ROOTS = List.of(Path.of("src/main/java"), Path.of("src/test/java"));
    private static final List<String> HOST_SCREENS = List.of("KineticScreen", "KineticContainerScreen", "KineticNativeScreen");
    /** State that only {@link KineticScreenRuntime} may create. */
    private static final List<String> RUNTIME_OWNED_STATE = List.of(
            "new GuiOverlayRuntime(", "new KineticScreenControls(", "new KineticScreenFocus(",
            "new KineticScrollFrameRuntime.Frame(", "new CanvasGuiGraphics("
    );
    private static final Pattern DECLARATION = Pattern.compile(
            "\\n    (?:public|protected)\\s+(?:final\\s+|static\\s+|abstract\\s+)*(?:<[^>]*>\\s*)?[\\w.<>\\[\\]?, ]+?\\s+(\\w+)\\s*\\(([^)]*)\\)"
    );
    private static final Pattern HOST_DEFAULT = Pattern.compile(
            "\\n    default\\s+(?:<[^>]*>\\s*)?[\\w.<>\\[\\]?, ]+?\\s+(\\w+)\\s*\\(([^)]*)\\)"
    );
    private static final Pattern MEMBER_METHOD = Pattern.compile(
            "(?:@\\w+(?:\\([^)]*\\))?\\s+)*(?:(?:public|protected|private|static|final|abstract|synchronized|native|default)\\s+)*"
                    + "(?:<[^>]*>\\s*)?[\\w.<>\\[\\]?, ]+?\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:throws[^{;]*)?[{;]"
    );
    private static final Pattern TOP_LEVEL_TYPE = Pattern.compile(
            "(?m)^(?:public\\s+|abstract\\s+|final\\s+|sealed\\s+|non-sealed\\s+)*class\\s+(\\w+)(?:\\s*<[^{]*?>)?\\s+extends\\s+([\\w.]+)"
    );
    /** Screen-specific hooks and canvas accessors that legitimately differ between the three hosts. */
    private static final Pattern SCREEN_SPECIFIC = Pattern.compile(
            "^(canvas\\w*|renderCanvas\\w*|enableCanvasScissor|disableCanvasScissor|isInsideCanvas|useCanvas|layout|layoutLevel"
                    + "|safeArea|isCompactLayout|isPortraitLayout|isUltrawideLayout|renderTooltips|registerPreviewWheelTarget"
                    + "|addScrollable\\w*|addCompactScrollable\\w*|resetScrollableWidgets"
                    + "|container\\w*|isInsideUi|renderScreenOverlay|renderUiForeground|requestContainerTooltips|ui(Width|Height|Scale)"
                    + "|requestWidgetTooltip"
                    + "|native\\w*|renderNative\\w*|after\\w*|tick|KineticScreen|KineticContainerScreen|KineticNativeScreen)\\(.*"
    );

    private ScreenApiParityRegression() {
    }

    public static void main(String[] args) throws IOException {
        List<String> failures = new ArrayList<>();
        Set<String> hostDefaults = hostDefaults();
        if (hostDefaults.size() < 80) {
            failures.add("  KineticScreenHost declares only " + hostDefaults.size() + " default methods; the shared API moved?");
        }

        Map<String, Path> hostScreens = hostScreensAndSubclasses();
        for (String screen : HOST_SCREENS) {
            String source = masked(Files.readString(SCREEN_ROOT.resolve(screen + ".java"), StandardCharsets.UTF_8));
            if (!Pattern.compile("\\bclass\\s+" + screen + "\\b[^{]*\\bimplements\\b[^{]*\\bKineticScreenHost\\b").matcher(source).find()) {
                failures.add("  " + screen + " no longer implements KineticScreenHost");
            }
            for (String owned : RUNTIME_OWNED_STATE) {
                if (source.contains(owned)) {
                    failures.add("  " + screen + " creates its own state (" + owned + "...); use kineticRuntime() instead");
                }
            }
        }

        for (Map.Entry<String, Path> entry : hostScreens.entrySet()) {
            for (String signature : declaredMethods(Files.readString(entry.getValue(), StandardCharsets.UTF_8))) {
                if (hostDefaults.contains(signature)) {
                    failures.add("  " + entry.getKey() + " redeclares shared host method " + signature
                            + " (" + entry.getValue() + "); change KineticScreenHost or KineticScreenRuntime instead");
                }
            }
        }

        Set<String> screen = remainingSurface("KineticScreen.java");
        Set<String> container = remainingSurface("KineticContainerScreen.java");
        Set<String> nativeScreen = remainingSurface("KineticNativeScreen.java");
        report(failures, "KineticContainerScreen", screen, container);
        report(failures, "KineticNativeScreen", screen, nativeScreen);
        report(failures, "KineticScreen", container, screen);
        report(failures, "KineticScreen", nativeScreen, screen);

        if (!failures.isEmpty()) throw new AssertionError("Screen host drift:\n" + String.join("\n", failures));
        System.out.println("PASS: " + hostDefaults.size() + " shared host methods have one implementation; "
                + hostScreens.size() + " host screen classes checked; " + screen.size()
                + " screen-specific signatures identical across all three screens");
    }

    private static void report(List<String> failures, String target, Set<String> expected, Set<String> actual) {
        for (String signature : expected) {
            if (!actual.contains(signature)) failures.add("  " + target + " is missing " + signature);
        }
    }

    private static Set<String> hostDefaults() throws IOException {
        String source = masked(Files.readString(SCREEN_ROOT.resolve("KineticScreenHost.java"), StandardCharsets.UTF_8));
        Set<String> signatures = new TreeSet<>();
        Matcher matcher = HOST_DEFAULT.matcher(source);
        while (matcher.find()) signatures.add(matcher.group(1) + "(" + normalizeParameters(matcher.group(2)) + ")");
        return signatures;
    }

    /** The three host screens plus every top-level class in main or test sources that extends one of them. */
    private static Map<String, Path> hostScreensAndSubclasses() throws IOException {
        Map<String, String> superclassOf = new LinkedHashMap<>();
        Map<String, Path> fileOf = new LinkedHashMap<>();
        for (Path root : SOURCE_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    Matcher matcher = TOP_LEVEL_TYPE.matcher(masked(Files.readString(file, StandardCharsets.UTF_8)));
                    if (!matcher.find()) continue;
                    String superclass = matcher.group(2);
                    superclassOf.put(matcher.group(1), superclass.substring(superclass.lastIndexOf('.') + 1));
                    fileOf.put(matcher.group(1), file);
                }
            }
        }
        Set<String> hosts = new LinkedHashSet<>(HOST_SCREENS);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Map.Entry<String, String> entry : superclassOf.entrySet()) {
                if (hosts.contains(entry.getValue()) && hosts.add(entry.getKey())) grew = true;
            }
        }
        Map<String, Path> result = new LinkedHashMap<>();
        for (String host : hosts) {
            Path file = fileOf.get(host);
            if (file != null) result.put(host, file);
        }
        return result;
    }

    /** Methods declared directly in the top-level class body; nested and anonymous classes are skipped. */
    private static Set<String> declaredMethods(String rawSource) {
        String source = masked(rawSource);
        Set<String> signatures = new TreeSet<>();
        Matcher type = TOP_LEVEL_TYPE.matcher(source);
        if (!type.find()) return signatures;
        int open = source.indexOf('{', type.end());
        if (open < 0) return signatures;
        int depth = 0;
        int segmentStart = open + 1;
        for (int index = open; index < source.length(); index++) {
            char ch = source.charAt(index);
            if (ch == '{') {
                if (depth == 1) addMember(signatures, source.substring(segmentStart, index + 1));
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 1) segmentStart = index + 1;
                if (depth == 0) break;
            } else if (ch == ';' && depth == 1) {
                addMember(signatures, source.substring(segmentStart, index + 1));
                segmentStart = index + 1;
            }
        }
        return signatures;
    }

    private static void addMember(Set<String> signatures, String member) {
        Matcher matcher = MEMBER_METHOD.matcher(member.trim());
        if (matcher.matches()) signatures.add(matcher.group(1) + "(" + normalizeParameters(matcher.group(2)) + ")");
    }

    private static Set<String> remainingSurface(String file) throws IOException {
        String source = masked(Files.readString(SCREEN_ROOT.resolve(file), StandardCharsets.UTF_8));
        Set<String> signatures = new TreeSet<>();
        Matcher matcher = DECLARATION.matcher(source);
        while (matcher.find()) {
            String signature = matcher.group(1) + "(" + normalizeParameters(matcher.group(2)) + ")";
            if (!SCREEN_SPECIFIC.matcher(signature).matches()) signatures.add(signature);
        }
        return signatures;
    }

    /** Blanks comments and string/char literals so braces and keywords inside them are ignored. */
    private static String masked(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            char ch = source.charAt(index);
            if (source.startsWith("//", index)) {
                while (index < source.length() && source.charAt(index) != '\n') {
                    out.append(' ');
                    index++;
                }
            } else if (source.startsWith("/*", index)) {
                int end = source.indexOf("*/", index + 2);
                end = end < 0 ? source.length() : end + 2;
                for (; index < end; index++) out.append(source.charAt(index) == '\n' ? '\n' : ' ');
            } else if (ch == '"' || ch == '\'') {
                out.append(' ');
                index++;
                while (index < source.length() && source.charAt(index) != ch) {
                    if (source.charAt(index) == '\\') {
                        out.append(' ');
                        index++;
                    }
                    if (index < source.length()) {
                        out.append(source.charAt(index) == '\n' ? '\n' : ' ');
                        index++;
                    }
                }
                if (index < source.length()) {
                    out.append(' ');
                    index++;
                }
            } else {
                out.append(ch);
                index++;
            }
        }
        return out.toString();
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
            String type = raw.replaceAll("@\\w+\\s*", "").replaceAll("\\bfinal\\s+", "").trim();
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
