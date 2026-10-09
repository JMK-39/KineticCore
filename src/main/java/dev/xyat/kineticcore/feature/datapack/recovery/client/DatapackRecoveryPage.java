package dev.xyat.kineticcore.feature.datapack.recovery.client;

import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.text.KineticText;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;
import dev.xyat.kineticcore.api.client.gui.widget.list.SelectionItem;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.feature.datapack.PackModule;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackDiagnostics;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackProblem;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.nio.file.Path;
import java.util.List;

/** Same Kinetic API layout on every supported version. */
public final class DatapackRecoveryPage extends KineticPage {
    private static final int MARGIN = 10, GAP = 4, LIST_WIDTH = 224, DETAILS_X = MARGIN + LIST_WIDTH + GAP;
    private final List<DatapackProblem> problems = DatapackDiagnostics.problems();
    private final Path world = DatapackRecovery.clientWorld();
    private final Runnable retry, cancel;
    private int selected, scroll, contentHeight;
    private KineticButton locate;

    public DatapackRecoveryPage(Runnable retry, Runnable cancel) {
        super(text("title"));
        this.retry = retry;
        this.cancel = cancel;
    }

    private static MutableComponent text(String key, Object... arguments) {
        return KineticI18n.translatable("gui.kineticcore.datapack_recovery." + key, arguments);
    }

    @Override
    protected void build(KineticUi ui) {
        var rows = problems.stream().map(problem -> new SelectionItem(
                Component.literal(problem.packId().isEmpty() ? text("unknown_pack").getString() : problem.packId()),
                Component.literal(problem.resourceId()), Component.literal(problem.reason()), true, true)).toList();
        ui.selectionList(MARGIN, 42, LIST_WIDTH, height() - 114, rows).textRows().selected(selected).onSelect(index -> {
            selected = index;
            scroll = 0;
            locate.setEnabled(problem() != null && problem().location() != null);
        }).build();
        int y = height() - 66, buttonWidth = (width() - MARGIN * 2 - GAP * 2) / 3;
        ui.button(MARGIN, y, buttonWidth).text(text("retry")).enabled(DatapackRecovery.canSkipProblems())
                .tooltip(text(DatapackRecovery.canSkipProblems() ? "retry_hint" : "unresolved_hint"))
                .onClick(() -> { if (DatapackRecovery.skipProblems()) retry.run(); }).build();
        locate = ui.button(MARGIN + buttonWidth + GAP, y, buttonWidth).text(text("locate"))
                .enabled(problem() != null && problem().location() != null).tooltip(text("locate_hint"))
                .onClick(() -> { var problem = problem(); if (problem != null && problem.location() != null)
                    DatapackRecoveryClient.locate(Path.of(problem.location().openPath())); }).build();
        ui.button(MARGIN + (buttonWidth + GAP) * 2, y, buttonWidth).text(text("back"))
                .onClick(this::leave).build();
        ui.button(MARGIN, y + CONTROL_HEIGHT + GAP, buttonWidth).text(text("core_folder"))
                .tooltip(Component.literal(PackModule.DATA_PACK_DIR.toAbsolutePath().toString()))
                .onClick(() -> DatapackRecoveryClient.openDirectory(PackModule.DATA_PACK_DIR)).build();
        ui.button(MARGIN + buttonWidth + GAP, y + CONTROL_HEIGHT + GAP, buttonWidth).text(text("world_folder"))
                .tooltip(Component.literal(world.resolve("datapacks").toString()))
                .onClick(() -> DatapackRecoveryClient.openDirectory(world.resolve("datapacks"))).build();
    }

    private DatapackProblem problem() {
        return selected >= 0 && selected < problems.size() ? problems.get(selected) : null;
    }

    private Component details() {
        var problem = problem();
        if (problem == null) return text("unresolved_hint");
        String path = problem.location() == null ? problem.resourceId() : problem.location().displayPath();
        return text("pack", problem.packId().isEmpty() ? text("unknown_pack") : problem.packId()).append("\n\n")
                .append(text("file", path.isEmpty() ? text("unknown_file") : path)).append("\n\n")
                .append(text("reason", problem.reason()));
    }

    @Override
    protected void renderForeground(KineticGraphics g, int mouseX, int mouseY, float partialTick) {
        g.scrollingTextCentered(title(), width() / 2, 8, width() - 2 * MARGIN, 0xFFFFAA00, false);
        g.scrollingText(text("summary", problems.size()), MARGIN, 24, width() - 2 * MARGIN, 0xFFFFAA00, false);
        int top = 42, bottom = height() - 72, right = width() - MARGIN;
        g.fill(DETAILS_X, top, right, bottom, 0xCC202020);
        g.outline(DETAILS_X, top, right - DETAILS_X, bottom - top, 0xFF777777);
        int textWidth = right - DETAILS_X - 8;
        contentHeight = KineticText.wrappedHeight(details(), textWidth);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (bottom - top - 8))));
        g.clipped(DETAILS_X + 4, top + 4, right - 4, bottom - 4,
                () -> g.wrappedText(details(), DETAILS_X + 4, top + 4 - scroll, textWidth, 0xFFFFFFFF));
        g.scrollingText(text("temporary_hint"), MARGIN, height() - 20, width() - 2 * MARGIN, 0xFFAAAAAA, false);
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        if (!input.inside(DETAILS_X, 42, width() - MARGIN - DETAILS_X, height() - 114)) return false;
        scroll = Math.max(0, scroll - (int) (input.deltaY() * 18));
        return true;
    }

    private void leave() { DatapackRecovery.cancel(); cancel.run(); }
    @Override protected boolean onCloseRequested() { leave(); return true; }
}
