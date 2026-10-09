package dev.xyat.kineticcoredatapackvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackDiagnostics;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import dev.xyat.kineticcore.feature.datapack.recovery.client.DatapackRecoveryPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in validation in an owned save containing intentionally invalid data. */
public final class DatapackRecoveryValidation {
    private static final Logger LOG = LoggerFactory.getLogger(DatapackRecoveryValidation.class);
    private static boolean finished;
    private static int ticks, phase, wait;
    private static boolean canceled;
    private static volatile boolean verified;
    private static volatile Throwable failure;

    public static void install() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, DatapackRecoveryValidation::tick);
    }

    private static void tick() {
        if (finished) return;
        var client = Minecraft.getInstance();
        try {
            if (client.screen instanceof DatapackLoadFailureScreen) {
                throw new AssertionError("Invalid datapack still opens vanilla failure screen without file diagnosis or temporary retry");
            }
            if (failure != null) throw new AssertionError("server-side save validation failed", failure);
            if (++ticks > 2400) throw new AssertionError("loading or retry timed out");
            if (phase == 0 && client.player != null && Boolean.getBoolean("kineticcore.datapackValidation.allowNormal")) {
                require(DatapackRecovery.skippedPacks().isEmpty(), "normal loading incorrectly excluded a pack");
                String pack = System.getProperty("kineticcore.datapackValidation.pack", "kineticcore_recovery");
                require(client.getSingleplayerServer().getWorldData().getDataConfiguration().dataPacks().getEnabled().contains(pack), "valid pack was not restored on the next load");
                LOG.info("DATAPACK_RECOVERY_PASS normal loading keeps the pack enabled without temporary exclusions");
                finished = true; client.stop(); return;
            }
            if (phase == 0 && client.player != null && !Boolean.getBoolean("kineticcore.datapackValidation.allowNormal"))
                throw new AssertionError("Expected fatal resource was accepted without diagnosis");
            if (phase == 0 && client.getOverlay() == null && KineticGui.currentPage() instanceof DatapackRecoveryPage) {
                require(!DatapackDiagnostics.problems().isEmpty(), "missing diagnosis");
                if (Boolean.getBoolean("kineticcore.datapackValidation.unresolved")) {
                    require(!DatapackRecovery.canSkipProblems(), "unknown metadata error disabled unrelated packs");
                    require(DatapackDiagnostics.problems().stream().allMatch(value -> value.location() == null), "metadata error invented a resource file");
                    phase = 1; wait = 30;
                    return;
                }
                var problem = DatapackDiagnostics.problems().stream().filter(value -> value.packId().equals(System.getProperty("kineticcore.datapackValidation.pack", "kineticcore_recovery"))
                        && value.resourceId().endsWith(System.getProperty("kineticcore.datapackValidation.resource", "worldgen/biome/broken.json"))).findFirst()
                        .orElseThrow(() -> new AssertionError("wrong source or resource: " + DatapackDiagnostics.problems()));
                require(problem.location() != null, "missing exact path: " + problem);
                require(DatapackDiagnostics.problems().size() >= Integer.getInteger("kineticcore.datapackValidation.count", 1), "not every failure was reported");
                require(DatapackRecovery.canSkipProblems(), "identified local pack cannot be skipped");
                phase = 1; wait = 30;
                LOG.info("DATAPACK_RECOVERY_DIAGNOSIS {}", problem);
            }
            if (phase == 1 && --wait <= 0) {
                var imagePath = client.gameDirectory.toPath().resolve("数据包失败界面.png");
                //? if >=26.1 {
                /*Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> {
                    try (image) { image.writeToFile(imagePath); } catch (Exception error) { failure = error; }
                });
                *///?} else {
                try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) { image.writeToFile(imagePath); }
                //?}
                boolean closeFirst = Boolean.getBoolean("kineticcore.datapackValidation.cancel") && !canceled;
                boolean unresolved = Boolean.getBoolean("kineticcore.datapackValidation.unresolved");
                String action = closeFirst || unresolved ? "back" : "retry";
                if (unresolved) {
                    var disabled = client.screen.children().stream().filter(child -> child instanceof Button button
                            && button.getMessage().getContents() instanceof TranslatableContents content
                            && content.getKey().equals("gui.kineticcore.datapack_recovery.retry")).map(Button.class::cast).findFirst().orElseThrow();
                    require(!disabled.active, "unknown source retry is incorrectly enabled");
                }
                Button retry = client.screen.children().stream().filter(child -> child instanceof Button button
                        && button.getMessage().getContents() instanceof TranslatableContents content
                        && content.getKey().equals("gui.kineticcore.datapack_recovery." + action)).map(Button.class::cast).findFirst().orElseThrow();
                require(retry.active, "retry button is disabled");
                phase = unresolved ? 5 : closeFirst ? 4 : 2;
                //? if >=26.1 {
                /*retry.onPress(new net.minecraft.client.input.MouseButtonInfo(0, 0));
                *///?} else {
                retry.onPress();
                //?}
                if (closeFirst) { canceled = true; wait = 20; }
            }
            if (phase == 5) {
                require(DatapackRecovery.clientWorld() == null && DatapackRecovery.skippedPacks().isEmpty(), "unknown source cancel kept a recovery session");
                LOG.info("DATAPACK_RECOVERY_PASS metadata failure shows its cause without invented files or automatic pack exclusion");
                finished = true; client.stop(); return;
            }
            if (phase == 4 && --wait <= 0) {
                require(DatapackRecovery.clientWorld() == null && DatapackRecovery.skippedPacks().isEmpty(), "cancel did not clear the recovery session");
                try (var access = client.getLevelSource().validateAndCreateAccess("codex-datapack-recovery-20261009")) {
                    require(access != null, "cancel left the world locked");
                }
                phase = 0;
                //? if >=1.21 {
                /*client.createWorldOpenFlows().openWorld("codex-datapack-recovery-20261009", () -> { });
                *///?} else {
                client.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(), "codex-datapack-recovery-20261009");
                //?}
            }
            if (phase == 2 && client.player != null && client.getSingleplayerServer() != null) {
                phase = 3;
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var server = client.getSingleplayerServer();
                        var data = (PrimaryLevelData) server.getWorldData();
                        String badPack = System.getProperty("kineticcore.datapackValidation.pack", "kineticcore_recovery");
                        require(!data.getDataConfiguration().dataPacks().getEnabled().contains(badPack), "bad pack still active in runtime configuration");
                        //? if >=26.1 {
                        /*var serialized = data.createTag(client.player.getUUID());
                        *///?} else {
                        var serialized = data.createTag(server.registryAccess(), null);
                        //?}
                        require(serialized.toString().contains(badPack), "saved pack choices permanently lost the bad pack: " + serialized);
                        require(DatapackRecovery.skippedPacks().contains(badPack), "session skip set missing");
                        require(data.getDataConfiguration().dataPacks().getEnabled().contains("kineticcore_recovery_keep"), "an unrelated normal pack was removed");
                        require(DatapackRecovery.skippedPacks().size() >= Integer.getInteger("kineticcore.datapackValidation.count", 1), "not every bad pack was skipped");
                        if (badPack.equals("override_bad")) {
                            require(server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                                    .containsKey(net.minecraft.resources.ResourceLocation.tryParse("kineticcore_recovery:broken")), "lower-priority valid resource did not replace the excluded override");
                        }
                        if (Boolean.getBoolean("kineticcore.datapackValidation.reloadChecks")) {
                            var issues = new java.util.ArrayList<String>();
                            var marker = net.minecraft.resources.ResourceLocation.tryParse("kineticcore_recovery:function" +
                                    //? if >=1.21 {
                                    /*"/marker.mcfunction");
                                    *///?} else {
                                    "s/marker.mcfunction");
                                    //?}
                            require(server.getResourceManager().getResource(marker).isEmpty(), "skipped pack marker loaded on initial retry");
                            for (int i = 0; i < 3; i++) {
                                server.reloadResources(new java.util.ArrayList<>(server.getPackRepository().getSelectedIds())).join();
                                if (server.getResourceManager().getResource(marker).isPresent()) issues.add("ordinary reload reintroduced the skipped pack");
                                var field = dev.xyat.kineticcore.feature.datapack.recovery.DatapackReferences.class.getDeclaredField("SOURCES");
                                field.setAccessible(true);
                                if (!((java.util.Map<?, ?>) field.get(null)).isEmpty()) issues.add("ordinary reload retained source resources");
                            }
                            var actual = data.getDataConfiguration();
                            var enabled = new java.util.ArrayList<>(actual.dataPacks().getEnabled());
                            enabled.remove("kineticcore_recovery_keep");
                            enabled.add(0, "file/new_user_choice");
                            var disabled = new java.util.ArrayList<>(actual.dataPacks().getDisabled());
                            disabled.add("kineticcore_recovery_keep");
                            var changed = new net.minecraft.world.level.WorldDataConfiguration(new net.minecraft.world.level.DataPackConfig(enabled, disabled), actual.enabledFeatures());
                            var saved = (net.minecraft.world.level.WorldDataConfiguration) DatapackRecovery.savedConfiguration(data, changed);
                            if (saved.dataPacks().getEnabled().contains("kineticcore_recovery_keep") || !saved.dataPacks().getDisabled().contains("kineticcore_recovery_keep")) issues.add("save undid the player's later disable choice");
                            if (!saved.dataPacks().getEnabled().get(0).equals("file/new_user_choice")) issues.add("save undid the player's enable or ordering choice");
                            require(saved.dataPacks().getEnabled().contains(badPack), "save failed to restore the temporary skip");
                            require(issues.isEmpty(), String.join("; ", issues));
                        }
                        verified = true;
                    } catch (Throwable error) { failure = error; }
                });
            }
            if (phase == 3 && verified) {
                LOG.info("DATAPACK_RECOVERY_PASS diagnosis, real retry button, normal load, temporary runtime exclusion and preserved save choices");
                finished = true; client.stop();
            }
            if (ticks % 200 == 0) LOG.info("DATAPACK_RECOVERY_WAIT phase={} screen={}", phase, client.screen == null ? "none" : client.screen.getClass().getSimpleName());
        } catch (Throwable exception) {
            finished = true;
            LOG.error("DATAPACK_RECOVERY_FAIL", exception);
            client.stop();
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
