package dev.xyat.kineticcore;

import dev.xyat.kineticcore.api.client.search.SearchComparatorRecoveryRegression;
import dev.xyat.kineticcore.api.client.search.SearchModelAtomicRegression;
import dev.xyat.kineticcore.api.config.client.KTClientConfigDecimalMetadataRegression;
import dev.xyat.kineticcore.api.config.client.KTClientConfigDuplicatePathRegression;
import dev.xyat.kineticcore.api.config.client.KTClientConfigFiniteRegression;
import dev.xyat.kineticcore.api.config.client.KTClientConfigIntegerMetadataRegression;
import dev.xyat.kineticcore.api.config.client.KTConfigChoiceUniquenessRegression;
import dev.xyat.kineticcore.api.config.client.KTConfigEntryAtomicMirrorRegression;
import dev.xyat.kineticcore.api.config.client.KTConfigEntryDirectWriteRegression;
import dev.xyat.kineticcore.api.config.client.KTConfigEntryPublicRollbackRegression;
import dev.xyat.kineticcore.api.config.client.KTConfigEntrySingleDecodeRegression;
import dev.xyat.kineticcore.api.config.client.KTConfigPageBuilderAtomicRegression;
import dev.xyat.kineticcore.api.config.server.KTServerConfigAttemptedRollbackRegression;
import dev.xyat.kineticcore.api.config.server.KTServerConfigDependencyRollbackRegression;
import dev.xyat.kineticcore.api.config.server.KTServerConfigPartialSaveRegression;
import dev.xyat.kineticcore.api.config.server.KTServerConfigPrecisionRegression;
import dev.xyat.kineticcore.api.config.server.KTServerConfigRollbackPersistenceRegression;
import dev.xyat.kineticcore.api.monitoring.KineticServerPerformanceAbsentTrackerRegression;
import dev.xyat.kineticcore.internal.client.config.ServerConfigMirrorPrecisionRegression;
import dev.xyat.kineticcore.internal.client.config.ServerConfigMirrorUnderflowRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.button.ButtonCallbackRollbackRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.input.AutoCompleteMatcherRollbackRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.GridScrollPointerValidationRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.RawScrollbarNonFiniteRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.selection.DropdownSingleValidationRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.selection.DropdownValidationRegression;
import dev.xyat.kineticcore.internal.client.input.NumericBoundsRegression;
import dev.xyat.kineticcore.internal.client.input.NumericDecimalPrecisionRegression;
import dev.xyat.kineticcore.internal.client.screen.GuiWheelDispatchRegression;
import dev.xyat.kineticcore.internal.client.screen.GuiWheelGenericOcclusionRegression;
import dev.xyat.kineticcore.internal.client.screen.GuiWheelOcclusionRegression;
import dev.xyat.kineticcore.internal.config.client.KineticConfigBuildAtomicRegression;
import dev.xyat.kineticcore.internal.config.client.KineticConfigStartupRegression;
import dev.xyat.kineticcore.internal.config.client.KineticNativeSetRollbackRegression;
import dev.xyat.kineticcore.internal.network.ForgeNetworkUtf8Regression;
import dev.xyat.kineticcore.internal.runtime.FeatureSwitchPersistenceRegression;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Runs the regression checks that need Minecraft and Forge classes on the classpath but no bootstrap and no mod
 * loading context. Checks that keep static startup state run in their own JVM. Every check runs even when an earlier
 * one fails, and the task fails at the end with the list of failed checks.
 *
 * <p>Not run here, because they need a real game host: Minecraft bootstrap ({@code KineticScrollStateRegression},
 * {@code SmoothSelectionListLifecycleRegression}) or a Forge mod loading context ({@code SearchEnglishDisplayRegression},
 * {@code KTServerConfigGetterRegression}, {@code ServerConfigDecimalWireRegression},
 * {@code HookRegistrationIntegrationRegression}, {@code GuiDraftConfigureFailureRegression},
 * {@code GuiRootNavigationRegression}, {@code GuiSessionLifecycleRegression}, {@code GuiSessionNavigationRegression}).
 */
public final class MinecraftRegressionSuite {
    private static final String[] NO_ARGS = new String[0];
    private static final List<String> FAILED = new ArrayList<>();

    private MinecraftRegressionSuite() {
    }

    public static void main(String[] args) {
        run("SearchComparatorRecoveryRegression", () -> SearchComparatorRecoveryRegression.main(NO_ARGS));
        run("SearchModelAtomicRegression", () -> SearchModelAtomicRegression.main(NO_ARGS));
        run("KTClientConfigDecimalMetadataRegression", () -> KTClientConfigDecimalMetadataRegression.main(NO_ARGS));
        run("KTClientConfigDuplicatePathRegression", () -> KTClientConfigDuplicatePathRegression.main(NO_ARGS));
        run("KTClientConfigFiniteRegression", () -> KTClientConfigFiniteRegression.main(NO_ARGS));
        run("KTClientConfigIntegerMetadataRegression", () -> KTClientConfigIntegerMetadataRegression.main(NO_ARGS));
        run("KTConfigChoiceUniquenessRegression", () -> KTConfigChoiceUniquenessRegression.main(NO_ARGS));
        run("KTConfigEntryAtomicMirrorRegression", () -> KTConfigEntryAtomicMirrorRegression.main(NO_ARGS));
        run("KTConfigEntryDirectWriteRegression", () -> KTConfigEntryDirectWriteRegression.main(NO_ARGS));
        run("KTConfigEntryPublicRollbackRegression", () -> KTConfigEntryPublicRollbackRegression.main(NO_ARGS));
        run("KTConfigEntrySingleDecodeRegression", () -> KTConfigEntrySingleDecodeRegression.main(NO_ARGS));
        run("KTConfigPageBuilderAtomicRegression", () -> KTConfigPageBuilderAtomicRegression.main(NO_ARGS));
        run("KTServerConfigAttemptedRollbackRegression", () -> KTServerConfigAttemptedRollbackRegression.main(NO_ARGS));
        run("KTServerConfigDependencyRollbackRegression", () -> KTServerConfigDependencyRollbackRegression.main(NO_ARGS));
        run("KTServerConfigPartialSaveRegression", () -> KTServerConfigPartialSaveRegression.main(NO_ARGS));
        run("KTServerConfigPrecisionRegression", () -> KTServerConfigPrecisionRegression.main(NO_ARGS));
        run("KTServerConfigRollbackPersistenceRegression", () -> KTServerConfigRollbackPersistenceRegression.main(NO_ARGS));
        run("KineticServerPerformanceAbsentTrackerRegression", () -> KineticServerPerformanceAbsentTrackerRegression.main(NO_ARGS));
        run("ServerConfigMirrorPrecisionRegression", () -> ServerConfigMirrorPrecisionRegression.main(NO_ARGS));
        run("ServerConfigMirrorUnderflowRegression", () -> ServerConfigMirrorUnderflowRegression.main(NO_ARGS));
        run("ButtonCallbackRollbackRegression", () -> ButtonCallbackRollbackRegression.main(NO_ARGS));
        run("AutoCompleteMatcherRollbackRegression", () -> AutoCompleteMatcherRollbackRegression.main(NO_ARGS));
        run("GridScrollPointerValidationRegression", () -> GridScrollPointerValidationRegression.main(NO_ARGS));
        run("RawScrollbarNonFiniteRegression", () -> RawScrollbarNonFiniteRegression.main(NO_ARGS));
        run("DropdownSingleValidationRegression", () -> DropdownSingleValidationRegression.main(NO_ARGS));
        run("DropdownValidationRegression", () -> DropdownValidationRegression.main(NO_ARGS));
        run("NumericBoundsRegression", () -> NumericBoundsRegression.main(NO_ARGS));
        run("NumericDecimalPrecisionRegression", () -> NumericDecimalPrecisionRegression.main(NO_ARGS));
        run("GuiWheelDispatchRegression", () -> GuiWheelDispatchRegression.main(NO_ARGS));
        run("GuiWheelGenericOcclusionRegression", () -> GuiWheelGenericOcclusionRegression.main(NO_ARGS));
        run("GuiWheelOcclusionRegression", () -> GuiWheelOcclusionRegression.main(NO_ARGS));
        run("KineticConfigBuildAtomicRegression", () -> KineticConfigBuildAtomicRegression.main(NO_ARGS));
        run("KineticConfigStartupRegression", () -> KineticConfigStartupRegression.main(NO_ARGS));
        run("KineticNativeSetRollbackRegression", () -> KineticNativeSetRollbackRegression.main(NO_ARGS));
        run("ForgeNetworkUtf8Regression", () -> ForgeNetworkUtf8Regression.main(NO_ARGS));
        runIsolated(FeatureSwitchPersistenceRegression.class, "invalid");
        runIsolated(FeatureSwitchPersistenceRegression.class, "saveFailure");
        if (!FAILED.isEmpty()) {
            throw new AssertionError(FAILED.size() + " regression checks failed: " + String.join(", ", FAILED));
        }
        System.out.println("Minecraft regression suite passed.");
    }

    private static void run(String name, Check check) {
        System.out.println("== " + name);
        try {
            check.run();
        } catch (Throwable failure) {
            FAILED.add(name);
            failure.printStackTrace(System.out);
        }
    }

    private static void runIsolated(Class<?> mainClass, String caseName) {
        run(mainClass.getSimpleName() + " " + caseName, () -> {
            Path argumentFile = Files.createTempFile("kinetic-regression", ".args");
            try {
                String classpath = System.getProperty("java.class.path").replace("\\", "\\\\");
                Charset launcherCharset = Charset.forName(System.getProperty("native.encoding", Charset.defaultCharset().name()));
                Files.writeString(argumentFile, "-cp \"" + classpath + "\"", launcherCharset);
                Process process = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "@" + argumentFile,
                        mainClass.getName(),
                        caseName
                ).directory(isolatedWorkDirectory(mainClass, caseName).toFile()).inheritIO().start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new AssertionError(mainClass.getSimpleName() + " " + caseName + " exited with " + exitCode);
                }
            } finally {
                deleteQuietly(argumentFile);
            }
        });
    }

    /**
     * Fresh working directory for one isolated case. Those cases write config/ and logs/ relative to their working
     * directory, which must never be the project root. Gradle passes {@code kinetic.regression.workDir}.
     */
    private static Path isolatedWorkDirectory(Class<?> mainClass, String caseName) throws IOException {
        String configured = System.getProperty("kinetic.regression.workDir");
        Path base = configured == null || configured.isBlank()
                ? Files.createTempDirectory("kinetic-regression")
                : Path.of(configured).toAbsolutePath();
        Path directory = base.resolve(mainClass.getSimpleName() + "-" + caseName);
        if (Files.exists(directory)) {
            try (Stream<Path> stale = Files.walk(directory)) {
                for (Path path : stale.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        return Files.createDirectories(directory);
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private interface Check {
        void run() throws Throwable;
    }
}
