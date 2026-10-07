package dev.xyat.kineticcore;

import dev.xyat.kineticcore.api.client.gui.GuiContractRegression;
import dev.xyat.kineticcore.api.client.gui.layout.GuiLayoutBoundsRegression;
import dev.xyat.kineticcore.api.client.gui.layout.GuiLayoutCanvasCapRegression;
import dev.xyat.kineticcore.api.client.gui.layout.GuiLayoutCoordinateOperationsRegression;
import dev.xyat.kineticcore.api.client.gui.layout.GuiLayoutCoordinateOverflowRegression;
import dev.xyat.kineticcore.api.client.gui.layout.WeightedLayoutZeroWeightRegression;
import dev.xyat.kineticcore.api.client.gui.state.EditedEntryAtomicRefreshRegression;
import dev.xyat.kineticcore.api.client.gui.state.EditedEntryRefreshRegression;
import dev.xyat.kineticcore.api.client.search.SearchPercentTranslationRegression;
import dev.xyat.kineticcore.api.config.common.KineticConfigDecimalPrecisionRegression;
import dev.xyat.kineticcore.api.config.common.KineticConfigNumberConsistencyRegression;
import dev.xyat.kineticcore.api.config.common.KineticConfigNumbersRegression;
import dev.xyat.kineticcore.api.event.SubscriptionIsolationRegression;
import dev.xyat.kineticcore.api.monitoring.ServerTickTrackerBoundaryRegression;
import dev.xyat.kineticcore.api.monitoring.ServerTickTrackerInvalidMsptRegression;
import dev.xyat.kineticcore.api.monitoring.ServerTickTrackerSumOverflowRegression;
import dev.xyat.kineticcore.api.network.KineticCompressionUtf8Regression;
import dev.xyat.kineticcore.internal.client.DefaultOptionsRecoveryRegression;
import dev.xyat.kineticcore.internal.client.config.ServerConfigSubscriptionRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.WidgetLifecycleRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.list.ItemGridLayoutRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.input.AutoCompletePopupClickRegression;
import dev.xyat.kineticcore.internal.client.gui.widget.input.AutoCompleteRoutingContractRegression;
import dev.xyat.kineticcore.internal.client.screen.KineticControlRegistrationContractRegression;
import dev.xyat.kineticcore.internal.client.screen.KineticControlUnregistrationContractRegression;
import dev.xyat.kineticcore.internal.client.search.SupplementarySearchRegression;
import dev.xyat.kineticcore.internal.config.ServerConfigPacketLimitRegression;
import dev.xyat.kineticcore.internal.network.NetworkUtf8Regression;

/**
 * Runs every regression check that needs neither a Minecraft runtime nor a running game, in one JVM.
 * Checks that already have their own Gradle task (screen parity, pinyin, widget state, scroll frame) are not
 * repeated here. Add new Minecraft-free regressions to this list so {@code gradle check} runs them.
 */
public final class HeadlessRegressionSuite {
    private static final String[] NO_ARGS = new String[0];

    private HeadlessRegressionSuite() {
    }

    public static void main(String[] args) throws Exception {
        run("GuiContractRegression", () -> GuiContractRegression.main(NO_ARGS));
        run("ItemGridLayoutRegression", () -> ItemGridLayoutRegression.main(NO_ARGS));
        run("GuiLayoutBoundsRegression", () -> GuiLayoutBoundsRegression.main(NO_ARGS));
        run("GuiLayoutCanvasCapRegression", () -> GuiLayoutCanvasCapRegression.main(NO_ARGS));
        run("GuiLayoutCoordinateOperationsRegression", () -> GuiLayoutCoordinateOperationsRegression.main(NO_ARGS));
        run("GuiLayoutCoordinateOverflowRegression", () -> GuiLayoutCoordinateOverflowRegression.main(NO_ARGS));
        run("WeightedLayoutZeroWeightRegression", () -> WeightedLayoutZeroWeightRegression.main(NO_ARGS));
        run("EditedEntryAtomicRefreshRegression", () -> EditedEntryAtomicRefreshRegression.main(NO_ARGS));
        run("EditedEntryRefreshRegression", () -> EditedEntryRefreshRegression.main(NO_ARGS));
        run("SearchPercentTranslationRegression", () -> SearchPercentTranslationRegression.main(NO_ARGS));
        run("KineticConfigDecimalPrecisionRegression", () -> KineticConfigDecimalPrecisionRegression.main(NO_ARGS));
        run("KineticConfigNumberConsistencyRegression", () -> KineticConfigNumberConsistencyRegression.main(NO_ARGS));
        run("KineticConfigNumbersRegression", () -> KineticConfigNumbersRegression.main(NO_ARGS));
        run("SubscriptionIsolationRegression", () -> SubscriptionIsolationRegression.main(NO_ARGS));
        run("ServerTickTrackerBoundaryRegression", () -> ServerTickTrackerBoundaryRegression.main(NO_ARGS));
        run("ServerTickTrackerInvalidMsptRegression", () -> ServerTickTrackerInvalidMsptRegression.main(NO_ARGS));
        run("ServerTickTrackerSumOverflowRegression", () -> ServerTickTrackerSumOverflowRegression.main(NO_ARGS));
        run("KineticCompressionUtf8Regression", () -> KineticCompressionUtf8Regression.main(NO_ARGS));
        run("DefaultOptionsRecoveryRegression", () -> DefaultOptionsRecoveryRegression.main(NO_ARGS));
        run("ServerConfigSubscriptionRegression", () -> ServerConfigSubscriptionRegression.main(NO_ARGS));
        run("WidgetLifecycleRegression", () -> WidgetLifecycleRegression.main(NO_ARGS));
        run("AutoCompletePopupClickRegression", () -> AutoCompletePopupClickRegression.main(NO_ARGS));
        run("AutoCompleteRoutingContractRegression", () -> AutoCompleteRoutingContractRegression.main(NO_ARGS));
        run("KineticControlRegistrationContractRegression", () -> KineticControlRegistrationContractRegression.main(NO_ARGS));
        run("KineticControlUnregistrationContractRegression", () -> KineticControlUnregistrationContractRegression.main(NO_ARGS));
        run("SupplementarySearchRegression", () -> SupplementarySearchRegression.main(NO_ARGS));
        run("ServerConfigPacketLimitRegression", () -> ServerConfigPacketLimitRegression.main(NO_ARGS));
        run("NetworkUtf8Regression", () -> NetworkUtf8Regression.main(NO_ARGS));
        System.out.println("Headless regression suite passed.");
    }

    private static void run(String name, Check check) throws Exception {
        System.out.println("== " + name);
        check.run();
    }

    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }
}
