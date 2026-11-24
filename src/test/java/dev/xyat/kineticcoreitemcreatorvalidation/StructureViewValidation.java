package dev.xyat.kineticcoreitemcreatorvalidation;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticStructureView;
import dev.xyat.kineticcore.api.world.structure.KineticBlockSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Real-client check of the structure preview (-Dkineticcore.structureViewValidation=true): builds a small house
 * snapshot with a floor, walls, windows, a stair roof, a chest, a pool, a tree, flowers and four blocks of an
 * unknown mod, sends it through the encoder and decoder, shows it in a {@link KineticStructureView} and captures it
 * from the default angle, turned, cut open, zoomed and upside down. Then drives the view with mouse events sent to the
 * screen as the mouse sends them: a left drag must turn it by the dragged distance, a right drag move it and the wheel
 * zoom in until the camera is inside the house (captured). Last, a 160×40×160 town is shown and turned every frame
 * while the time each frame takes is logged (STRUCTURE_VIEW_PERF), and the client stops.
 */
public final class StructureViewValidation {
    private static final Logger LOG = LogUtils.getLogger();
    private static int phase, ticks, worldTicks, captures, checks;
    private static boolean captureReady = true;
    private static float dragYaw;
    private static TestPage page;
    private static double renderMouseX, renderMouseY;
    private static double pickX, pickY;
    private static int framesSeen;
    private static final java.util.List<String> clicked = new java.util.ArrayList<>(), activated = new java.util.ArrayList<>();
    private static KineticBlockSnapshot town;
    // Frame timing while the town turns: the render time of each frame and the time between frames.
    private static boolean measuring;
    private static long frameStart, lastFrameEnd;
    private static final java.util.List<Long> frameNanos = new java.util.ArrayList<>(), intervalNanos = new java.util.ArrayList<>();

    private StructureViewValidation() {}

    public static void install() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, StructureViewValidation::tick);
        KineticClientEvents.onScreenRenderBefore((screen, graphics, mouseX, mouseY, partialTick) -> {
            renderMouseX = mouseX;
            renderMouseY = mouseY;
            framesSeen++;
            if (!measuring || page == null || page.view == null) return;
            // A little more turn every frame, as a drag does.
            page.view.setView(page.view.yaw() + 3F, 35F, 1F);
            frameStart = System.nanoTime();
        });
        KineticClientEvents.onScreenRenderAfter((screen, graphics, mouseX, mouseY, partialTick) -> {
            if (!measuring || frameStart == 0L) return;
            long now = System.nanoTime();
            frameNanos.add(now - frameStart);
            if (lastFrameEnd != 0L) intervalNanos.add(now - lastFrameEnd);
            lastFrameEnd = now;
        });
    }

    /** The preview with the snapshot's counts beside it. */
    public static final class TestPage extends KineticPage {
        final KineticBlockSnapshot snapshot;
        KineticStructureView view;

        TestPage(KineticBlockSnapshot snapshot) {
            super(Component.literal("Structure preview validation"));
            this.snapshot = snapshot;
            useCanvas(640, 360, 4);
        }

        @Override protected void build(KineticUi ui) {
            view = ui.add(new KineticStructureView(200, 24, 436, 332));
            view.setSnapshot(snapshot);
        }

        @Override protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
            KineticTheme.panel(g, 0, 0, width(), height());
            g.text(title(), 8, 8, 0xFFFFFFFF);
            int y = 30;
            for (String line : new String[]{
                    "Size " + snapshot.sizeX() + "×" + snapshot.sizeY() + "×" + snapshot.sizeZ(),
                    "Blocks " + snapshot.nonAirCount(),
                    "Missing " + snapshot.missingCount() + " " + snapshot.missingNames(),
                    "Containers " + snapshot.containerCount(),
                    "Block entities " + snapshot.blockEntityCount(),
                    "Cut " + (view == null || view.cutY() == Integer.MAX_VALUE ? "-" : view.cutY())}) {
                g.scrollingText(Component.literal(line), 8, y, 184, 0xFFFFFFFF, false);
                y += 12;
            }
        }
    }

    private static void tick() {
        if (phase == 99) return;
        var client = Minecraft.getInstance();
        try {
            require(++ticks < (phase == 0 ? 12000 : phase == 9 ? 2400 : 600), "structure view validation timed out in phase " + phase);
            if (client.getOverlay() != null || client.player == null || client.level == null) return;
            switch (phase) {
                case 0 -> {
                    if (client.screen != null || ++worldTicks < 100) return;
                    KineticBlockSnapshot built = house();
                    KineticBlockSnapshot decoded = KineticBlockSnapshot.decode(built.encode());
                    require(decoded.nonAirCount() == built.nonAirCount(), "the codec keeps every block: "
                            + decoded.nonAirCount() + " vs " + built.nonAirCount());
                    require(decoded.missingCount() == 4, "four cells of an unknown mod stay missing: " + decoded.missingCount());
                    require(decoded.missingNames().containsKey("absentmod:mystery_block"), "the missing block keeps its id");
                    require(decoded.containerCount() == 1 && decoded.blockEntityCount() == 1, "container and chest data survive");
                    require(decoded.state(10, 1, 8).is(Blocks.CHEST), "the chest is back where it was");
                    KineticBlockSnapshot coarse = decoded.downsample(2);
                    require(coarse.sizeX() == 11 && coarse.scale() == 2 && coarse.nonAirCount() > 0, "downsampling halves the size");
                    checks += 6;
                    page = new TestPage(decoded);
                    KineticGui.open(page);
                    next();
                }
                case 1 -> {
                    // Meshing a slice per frame finishes this small house within a few frames; wait one second.
                    if (page.view == null || ticks < 20) return;
                    capture("default");
                    next();
                }
                case 2 -> {
                    if (!captureReady) return;
                    page.view.setView(135F, 55F, 1F);
                    if (ticks < 10) return;
                    capture("turned");
                    next();
                }
                case 3 -> {
                    if (!captureReady) return;
                    page.view.setView(KineticStructureView.DEFAULT_YAW, KineticStructureView.DEFAULT_PITCH, 1F);
                    page.view.setCutY(3);
                    if (ticks < 20) return;
                    capture("cut");
                    next();
                }
                case 4 -> {
                    if (!captureReady) return;
                    page.view.setCutY(Integer.MAX_VALUE);
                    page.view.setView(200F, 20F, 2.5F);
                    if (ticks < 10) return;
                    capture("zoomed");
                    next();
                }
                case 5 -> {
                    if (!captureReady) return;
                    // Over the top: the view keeps turning past straight down and shows the structure upside down.
                    page.view.setView(KineticStructureView.DEFAULT_YAW, 160F, 1F);
                    if (ticks < 10) return;
                    require(Math.abs(page.view.pitch() - 160F) < 0.01F, "the tilt is not held at 90 degrees: " + page.view.pitch());
                    checks++;
                    capture("upside_down");
                    next();
                }
                case 6 -> {
                    if (!captureReady) return;
                    if (ticks < 5) {
                        page.view.resetView();
                        return;
                    }
                    // A left drag across 80 screen units and then a right drag down 40, one move per tick as a mouse
                    // sends them over time: the drag has to outlast the screen's per-tick focus check.
                    var screen = client.screen;
                    var host = (dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost) screen;
                    double x = host.toScreenX(page.view.controlX() + page.view.controlWidth() / 2D);
                    double y = host.toScreenY(page.view.controlY() + page.view.controlHeight() / 2D);
                    if (ticks == 5) {
                        dragYaw = page.view.yaw();
                        press(screen, x, y, 0);
                        return;
                    }
                    if (ticks <= 15) {
                        drag(screen, x + 8 * (ticks - 5), y, 0, 8, 0);
                        return;
                    }
                    if (ticks == 16) {
                        release(screen, x + 80, y, 0);
                        float expected = (float) (host.toVirtualX(x + 80) - host.toVirtualX(x)) * 0.8F;
                        float turned = angle(page.view.yaw() - dragYaw);
                        LOG.info("STRUCTURE_VIEW_DRAG turned={} expected={}", turned, expected);
                        require(Math.abs(turned - expected) < 1F, "a left drag turns the view by the dragged distance: " + turned + " vs " + expected);
                        press(screen, x, y, 1);
                        return;
                    }
                    if (ticks <= 26) {
                        drag(screen, x, y + 4 * (ticks - 16), 1, 0, 4);
                        return;
                    }
                    release(screen, x, y + 40, 1);
                    Object target = targetOf(page.view);
                    require(target != null, "a right drag moves the view");
                    // From the default view, the wheel at the middle: closer and closer, then on into the house.
                    page.view.resetView();
                    for (int i = 0; i < 24; i++) scroll(screen, x, y, 1);
                    require(page.view.zoom() > 10F, "the wheel zooms in: " + page.view.zoom());
                    checks += 3;
                    next();
                }
                case 7 -> {
                    // The zoom eases in over a few frames.
                    if (ticks < 20) return;
                    capture("inside");
                    next();
                }
                case 8 -> {
                    if (!captureReady) return;
                    town = town();
                    page.view.setSnapshot(town);
                    page.view.resetView();
                    next();
                }
                case 9 -> {
                    // The town is meshed a slice per frame; then it is turned every frame for 200 frames.
                    if (dev.xyat.kineticcore.internal.client.render.structure.StructurePreviewRuntime.progress(town, Integer.MAX_VALUE) < 1F) return;
                    if (!measuring) {
                        LOG.info("STRUCTURE_VIEW_TOWN built in {} ticks, {} blocks", ticks, town.nonAirCount());
                        measuring = true;
                        return;
                    }
                    if (frameNanos.size() < 200) return;
                    measuring = false;
                    java.util.List<Long> sorted = new java.util.ArrayList<>(frameNanos);
                    java.util.Collections.sort(sorted);
                    double average = sorted.stream().mapToLong(Long::longValue).average().orElse(0) / 1e6;
                    double p95 = sorted.get((int) (sorted.size() * 0.95)) / 1e6, worst = sorted.get(sorted.size() - 1) / 1e6;
                    double interval = intervalNanos.stream().mapToLong(Long::longValue).average().orElse(0) / 1e6;
                    LOG.info(String.format(java.util.Locale.ROOT, "STRUCTURE_VIEW_PERF frames=%d renderAvgMs=%.2f renderP95Ms=%.2f renderMaxMs=%.2f frameIntervalMs=%.2f",
                            sorted.size(), average, p95, worst, interval));
                    require(average < 16D, "turning the town takes " + average + " ms a frame");
                    checks++;
                    page.view.setView(200F, 30F, 1F);
                    capture("town");
                    next();
                }
                case 10 -> {
                    // The controls hint belongs to the corner badge only: none over the structure, shown on the badge.
                    if (!captureReady) return;
                    var host = (dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost) client.screen;
                    int badgeX = page.view.controlX() + page.view.controlWidth() - 14 + 5;
                    int badgeY = page.view.controlY() + 3 + 5;
                    if (ticks == 1) {
                        pointAt(host.toScreenX(page.view.controlX() + page.view.controlWidth() / 2D),
                                host.toScreenY(page.view.controlY() + page.view.controlHeight() / 2D));
                        return;
                    }
                    if (ticks == 10) {
                        require(!helpHovered(page.view), "no controls hint while the pointer is over the structure");
                        pointAt(host.toScreenX(badgeX), host.toScreenY(badgeY));
                        return;
                    }
                    if (ticks == 30) {
                        require(helpHovered(page.view), "the controls hint shows on the corner badge; render mouse " + renderMouseX + "," + renderMouseY
                                + " badge " + badgeX + "," + badgeY + " screen " + host.toScreenX(badgeX) + "," + host.toScreenY(badgeY));
                        checks += 2;
                        capture("hint");
                        next();
                    }
                }
                case 11 -> {
                    // Markers on the chest and the villager; the pointer rests on the chest.
                    // Counting starts once the last capture is written.
                    if (!captureReady) {
                        ticks = 0;
                        return;
                    }
                    if (ticks == 1) {
                        page.view.setSnapshot(page.snapshot);
                        page.view.resetView();
                        // The walls hide the chest; cut them down to the floor's layer.
                        page.view.setCutY(2);
                        page.view.setMarkers(java.util.List.of(
                                new KineticStructureView.Marker("chest", Component.literal("Chest"), 10, 1, 8, 11, 2, 9),
                                new KineticStructureView.Marker("villager", Component.literal("Villager"), 5.2, 1, 0.2, 5.8, 2.95, 0.8)));
                        page.view.setPickListener(new KineticStructureView.PickListener() {
                            @Override
                            public void clicked(@javax.annotation.Nullable KineticStructureView.Marker marker,
                                                dev.xyat.kineticcore.api.client.gui.input.MouseInput input) {
                                clicked.add(marker == null ? "-" : marker.key());
                            }

                            @Override
                            public void activated(KineticStructureView.Marker marker) {
                                activated.add(marker.key());
                            }
                        });
                        return;
                    }
                    if (ticks < 20) return;
                    require(figureCount(page.view) == 2, "the villager and the cow are drawn: " + figureCount(page.view) + " of "
                            + page.snapshot.entityCount() + " entities, made directly: "
                            + dev.xyat.kineticcore.internal.client.render.structure.PreviewOverlay.figures(page.snapshot.entities(), java.util.List.of()).size()
                            + "; view shows the house " + (page.view.snapshot() == page.snapshot) + ", scale " + page.view.snapshot().scale()
                            + ", built " + dev.xyat.kineticcore.internal.client.render.structure.StructurePreviewRuntime.progress(page.snapshot, Integer.MAX_VALUE)
                            + ", frames " + framesSeen);
                    double[] chest = find(page.view, "chest");
                    require(chest != null, "the chest can be picked somewhere in the view");
                    pickX = chest[0];
                    pickY = chest[1];
                    var host = (dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost) client.screen;
                    pointAt(host.toScreenX(pickX), host.toScreenY(pickY));
                    checks += 2;
                    next();
                }
                case 12 -> {
                    // Hovering names the chest; a click selects it and a double-click opens it.
                    var screen = client.screen;
                    var host = (dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost) screen;
                    double x = host.toScreenX(pickX), y = host.toScreenY(pickY);
                    if (ticks == 5) {
                        var hoveredMarker = page.view.hoveredMarker();
                        require(hoveredMarker != null && hoveredMarker.key().equals("chest"), "the pointer names the chest: " + hoveredMarker);
                        press(screen, x, y, 0);
                    } else if (ticks == 6) {
                        release(screen, x, y, 0);
                        require(clicked.equals(java.util.List.of("chest")), "a click picks the chest: " + clicked);
                    } else if (ticks == 7) {
                        press(screen, x, y, 0);
                    } else if (ticks == 8) {
                        release(screen, x, y, 0);
                        require(activated.equals(java.util.List.of("chest")), "a double-click opens the chest: " + activated);
                        require(Math.abs(angle(page.view.yaw() - KineticStructureView.DEFAULT_YAW)) < 0.01F, "clicks do not turn the view");
                        page.view.setSelection("chest", java.util.Set.of("chest", "villager"));
                    } else if (ticks == 20) {
                        checks += 4;
                        capture("pick");
                        next();
                    }
                }
                case 13 -> {
                    // A double-click on empty space resets the view and opens nothing.
                    // Counting starts once the last capture is written.
                    if (!captureReady) {
                        ticks = 0;
                        return;
                    }
                    var screen = client.screen;
                    var host = (dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost) screen;
                    int emptyX = page.view.controlX() + 12, emptyY = page.view.controlY() + page.view.controlHeight() - 20;
                    double x = host.toScreenX(emptyX), y = host.toScreenY(emptyY);
                    if (ticks == 1) {
                        page.view.setView(100F, 50F, 1F);
                        require(page.view.markerAt(emptyX, emptyY) == null, "the bottom-left corner of the view is empty");
                        press(screen, x, y, 0);
                    } else if (ticks == 2) {
                        release(screen, x, y, 0);
                    } else if (ticks == 3) {
                        press(screen, x, y, 0);
                    } else if (ticks == 4) {
                        release(screen, x, y, 0);
                        require(Math.abs(angle(page.view.yaw() - KineticStructureView.DEFAULT_YAW)) < 0.01F, "a double-click on empty space resets: " + page.view.yaw());
                        require(activated.size() == 1 && clicked.size() == 4 && clicked.get(3).equals("-"), "empty clicks open nothing: " + clicked + " " + activated);
                        checks += 3;
                        next();
                    }
                }
                case 14 -> {
                    // The letters of the gizmo turn the view: Y looks down from above and, clicked again, up from
                    // below; X looks from the east and Z from the south.
                    var screen = client.screen;
                    var host = (dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost) screen;
                    int axis = ticks < 12 ? 1 : ticks < 17 ? 0 : 2;
                    if (ticks == 3 || ticks == 8 || ticks == 13 || ticks == 18) {
                        int[] letter = axisLabel(page.view, axis);
                        require(letter[0] > page.view.controlX() + page.view.controlWidth() - 52 && letter[1] > page.view.controlY() + page.view.controlHeight() - 52,
                                "the gizmo is in the bottom-right corner: " + letter[0] + ", " + letter[1]);
                        pickX = letter[0];
                        pickY = letter[1];
                        press(screen, host.toScreenX(pickX), host.toScreenY(pickY), 0);
                    } else if (ticks == 4 || ticks == 9 || ticks == 14 || ticks == 19) {
                        release(screen, host.toScreenX(pickX), host.toScreenY(pickY), 0);
                        float yaw = page.view.yaw(), pitch = page.view.pitch();
                        String view = "yaw " + yaw + " pitch " + pitch;
                        if (ticks == 4) require(Math.abs(pitch - 90F) < 0.01F && Math.abs(angle(yaw - 270F)) < 0.01F, "Y looks down from above, square to the axes: " + view);
                        if (ticks == 9) require(Math.abs(pitch + 90F) < 0.01F && Math.abs(angle(yaw - 270F)) < 0.01F, "Y again looks up from below: " + view);
                        if (ticks == 14) require(Math.abs(pitch) < 0.01F && Math.abs(angle(yaw + 90F)) < 0.01F, "X looks from the east: " + view);
                        if (ticks == 19) require(Math.abs(pitch) < 0.01F && Math.abs(angle(yaw)) < 0.01F, "Z looks from the south: " + view);
                        require(activated.size() == 1 && clicked.size() == 4, "a click on the gizmo picks nothing: " + clicked + " " + activated);
                    } else if (ticks == 30) {
                        checks += 5;
                        capture("axis");
                        next();
                    }
                }
                case 15 -> {
                    if (!captureReady) return;
                    page.view.release();
                    LOG.info("STRUCTURE_VIEW_PASS checks={} captures={}", checks, captures);
                    phase = 99;
                    client.setScreen(null);
                    dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
                }
                default -> { }
            }
        } catch (Throwable failure) {
            LOG.error("STRUCTURE_VIEW_FAIL phase={}", phase, failure);
            phase = 99;
            dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
        }
    }

    /** A 21 × 9 × 17 house: stone brick floor, plank walls with glass, stair roof, chest, pool, tree and flowers. */
    private static KineticBlockSnapshot house() {
        KineticBlockSnapshot.Builder b = KineticBlockSnapshot.builder(21, 9, 17);
        for (int x = 0; x < 21; x++) for (int z = 0; z < 17; z++) b.set(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int x = 2; x <= 13; x++) for (int z = 2; z <= 12; z++) b.set(x, 0, z, Blocks.STONE_BRICKS.defaultBlockState());
        for (int y = 1; y <= 4; y++) {
            for (int x = 2; x <= 13; x++) {
                for (int z = 2; z <= 12; z++) {
                    boolean wall = x == 2 || x == 13 || z == 2 || z == 12;
                    if (!wall) continue;
                    boolean corner = (x == 2 || x == 13) && (z == 2 || z == 12);
                    boolean window = y >= 2 && y <= 3 && !corner && (x % 3 == 0 || z % 3 == 0);
                    boolean door = z == 2 && x == 7 && y <= 2;
                    if (door) continue;
                    b.set(x, y, z, corner ? Blocks.OAK_LOG.defaultBlockState()
                            : window ? Blocks.GLASS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }
        // A stepped roof: stairs facing out on each long side, planks along the ridge.
        for (int layer = 0; layer <= 5; layer++) {
            int y = 5 + layer / 2;
            for (int x = 1; x <= 14; x++) {
                int north = 1 + layer, south = 13 - layer;
                if (north > south) break;
                b.set(x, y, north, stair(Direction.SOUTH));
                b.set(x, y, south, stair(Direction.NORTH));
                if (north + 1 >= south) for (int z = north; z <= south; z++) b.set(x, y, z, Blocks.OAK_PLANKS.defaultBlockState());
            }
        }
        b.set(10, 1, 8, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        CompoundTag chest = new CompoundTag();
        chest.putString("id", "minecraft:chest");
        b.blockEntity(10, 1, 8, chest).containers(1);
        // A villager by the door and a cow by the pool, drawn as their models.
        CompoundTag villager = new CompoundTag();
        villager.putString("id", "minecraft:villager");
        b.entity(5.5D, 1D, 0.5D, villager);
        CompoundTag cow = new CompoundTag();
        cow.putString("id", "minecraft:cow");
        b.entity(16.5D, 1D, 8.5D, cow);
        b.set(4, 1, 10, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.set(5, 1, 10, Blocks.FURNACE.defaultBlockState());
        b.set(4, 3, 4, Blocks.LANTERN.defaultBlockState());
        for (int x = 15; x <= 18; x++) for (int z = 3; z <= 6; z++) b.set(x, 0, z, Blocks.WATER.defaultBlockState());
        for (int y = 1; y <= 4; y++) b.set(17, y, 12, Blocks.OAK_LOG.defaultBlockState());
        for (int x = 15; x <= 19; x++) for (int z = 10; z <= 14; z++) for (int y = 4; y <= 6; y++) {
            if (Math.abs(x - 17) + Math.abs(z - 12) + Math.abs(y - 5) <= 3 && !(x == 17 && z == 12 && y < 5)) {
                b.set(x, y, z, Blocks.OAK_LEAVES.defaultBlockState());
            }
        }
        b.set(1, 1, 14, Blocks.POPPY.defaultBlockState());
        b.set(3, 1, 15, Blocks.DANDELION.defaultBlockState());
        b.set(19, 1, 1, Blocks.TALL_GRASS.defaultBlockState());
        for (int i = 0; i < 4; i++) b.setMissing(15 + i, 1, 15, "absentmod:mystery_block[glowing=true]");
        return b.build();
    }

    /** A 160 × 40 × 160 town: 64 houses of brick, planks and glass with floors and a chest each, trees and a pond. */
    private static KineticBlockSnapshot town() {
        KineticBlockSnapshot.Builder b = KineticBlockSnapshot.builder(160, 40, 160);
        for (int x = 0; x < 160; x++) for (int z = 0; z < 160; z++) b.set(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
        CompoundTag chest = new CompoundTag();
        chest.putString("id", "minecraft:chest");
        int chests = 0;
        for (int bx = 0; bx < 8; bx++) {
            for (int bz = 0; bz < 8; bz++) {
                int x0 = bx * 20 + 2, z0 = bz * 20 + 2, x1 = x0 + 13, z1 = z0 + 13;
                int height = 8 + (bx * 3 + bz * 5) % 28;
                BlockState wall = (bx + bz) % 2 == 0 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
                for (int y = 1; y <= height; y++) {
                    for (int x = x0; x <= x1; x++) {
                        for (int z = z0; z <= z1; z++) {
                            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                            boolean floor = y % 5 == 0 || y == height;
                            if (!edge && !floor) continue;
                            boolean window = edge && !floor && y % 5 >= 2 && y % 5 <= 3 && (x + z) % 3 != 0;
                            b.set(x, y, z, window ? Blocks.GLASS.defaultBlockState() : wall);
                        }
                    }
                }
                b.set(x0 + 3, 1, z0 + 3, Blocks.CHEST.defaultBlockState());
                b.blockEntity(x0 + 3, 1, z0 + 3, chest.copy());
                chests++;
                b.set(x0 + 6, 1, z0 + 6, Blocks.LANTERN.defaultBlockState());
                // A tree beside each house.
                int tx = x0 + 16, tz = z0 + 16;
                if (tx < 159 && tz < 159) {
                    for (int y = 1; y <= 4; y++) b.set(tx, y, tz, Blocks.OAK_LOG.defaultBlockState());
                    for (int x = tx - 1; x <= tx + 1; x++) for (int z = tz - 1; z <= tz + 1; z++) for (int y = 4; y <= 5; y++) {
                        if (x != tx || z != tz || y == 5) b.set(x, y, z, Blocks.OAK_LEAVES.defaultBlockState());
                    }
                }
            }
        }
        b.containers(chests);
        for (int x = 70; x < 90; x++) for (int z = 70; z < 90; z++) b.set(x, 0, z, Blocks.WATER.defaultBlockState());
        return b.build();
    }

    private static BlockState stair(Direction facing) {
        return Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static void next() {
        phase++;
        ticks = 0;
    }

    private static void capture(String name) {
        var client = Minecraft.getInstance();
        captureReady = false;
        String file = "core-structure-view-" + name + ".png";
        //? if >=26.1 {
        /*net.minecraft.client.Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), 1, message -> {
        *///?} else {
        net.minecraft.client.Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), message -> {
        //?}
            captures++; captureReady = true; LOG.info("STRUCTURE_VIEW_CAPTURE {}", file);
        });
    }

    /** The angle in degrees, from -180 to 180. */
    private static float angle(float degrees) {
        float wrapped = degrees % 360F;
        if (wrapped > 180F) wrapped -= 360F;
        if (wrapped <= -180F) wrapped += 360F;
        return wrapped;
    }

    private static int figureCount(KineticStructureView view) throws ReflectiveOperationException {
        java.lang.reflect.Field field = KineticStructureView.class.getDeclaredField("figures");
        field.setAccessible(true);
        return ((java.util.List<?>) field.get(view)).size();
    }

    /** The middle of the points in the view where a marker is picked, in page units; {@code null} if there are none. */
    @javax.annotation.Nullable
    private static double[] find(KineticStructureView view, String key) {
        double sumX = 0, sumY = 0;
        int found = 0;
        for (int y = view.controlY() + 1; y < view.controlY() + view.controlHeight() - 1; y += 2) {
            for (int x = view.controlX() + 1; x < view.controlX() + view.controlWidth() - 1; x += 2) {
                KineticStructureView.Marker marker = view.markerAt(x, y);
                if (marker != null && marker.key().equals(key)) {
                    sumX += x;
                    sumY += y;
                    found++;
                }
            }
        }
        return found == 0 ? null : new double[] {sumX / found, sumY / found};
    }

    /** Where the view last drew a letter of its gizmo (0 X, 1 Y, 2 Z), in page units. */
    private static int[] axisLabel(KineticStructureView view, int axis) throws ReflectiveOperationException {
        java.lang.reflect.Field field = KineticStructureView.class.getDeclaredField("axisLabels");
        field.setAccessible(true);
        return ((int[][]) field.get(view))[axis].clone();
    }

    private static boolean helpHovered(KineticStructureView view) throws ReflectiveOperationException {
        java.lang.reflect.Field field = KineticStructureView.class.getDeclaredField("helpHovered");
        field.setAccessible(true);
        return field.getBoolean(view);
    }

    /** Puts the pointer at a point in screen units without moving the real cursor; the hidden window gets no moves. */
    private static void pointAt(double x, double y) throws ReflectiveOperationException {
        var client = Minecraft.getInstance();
        var window = client.getWindow();
        // The mouse handler's first two double fields are its window position (xpos, ypos).
        java.util.List<java.lang.reflect.Field> position = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : client.mouseHandler.getClass().getDeclaredFields()) {
            if (field.getType() == double.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) position.add(field);
        }
        require(position.size() >= 2, "the mouse handler keeps its position in two doubles");
        position.get(0).setAccessible(true);
        position.get(1).setAccessible(true);
        position.get(0).setDouble(client.mouseHandler, x * window.getScreenWidth() / window.getGuiScaledWidth());
        position.get(1).setDouble(client.mouseHandler, y * window.getScreenHeight() / window.getGuiScaledHeight());
    }

    private static Object targetOf(KineticStructureView view) throws ReflectiveOperationException {
        java.lang.reflect.Field field = KineticStructureView.class.getDeclaredField("target");
        field.setAccessible(true);
        return field.get(view);
    }

    // Mouse events as the game's mouse handler sends them to the screen, in GUI units.
    private static void press(net.minecraft.client.gui.screens.Screen screen, double x, double y, int button) {
        //? if >=26.1 {
        /*screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false);
        *///?} else {
        screen.mouseClicked(x, y, button);
        //?}
    }

    private static void drag(net.minecraft.client.gui.screens.Screen screen, double x, double y, int button, double dx, double dy) {
        //? if >=26.1 {
        /*screen.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(button, 0)), dx, dy);
        *///?} else {
        screen.mouseDragged(x, y, button, dx, dy);
        //?}
    }

    private static void release(net.minecraft.client.gui.screens.Screen screen, double x, double y, int button) {
        //? if >=26.1 {
        /*screen.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(button, 0)));
        *///?} else {
        screen.mouseReleased(x, y, button);
        //?}
    }

    private static void scroll(net.minecraft.client.gui.screens.Screen screen, double x, double y, double delta) {
        //? if >=1.20.2 {
        /*screen.mouseScrolled(x, y, 0D, delta);
        *///?} else {
        screen.mouseScrolled(x, y, delta);
        //?}
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
