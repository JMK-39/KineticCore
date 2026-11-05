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
 * from the default angle, turned, cut open and zoomed, then stops the client.
 */
public final class StructureViewValidation {
    private static final Logger LOG = LogUtils.getLogger();
    private static int phase, ticks, worldTicks, captures, checks;
    private static boolean captureReady = true;
    private static TestPage page;

    private StructureViewValidation() {}

    public static void install() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, StructureViewValidation::tick);
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
            require(++ticks < (phase == 0 ? 12000 : 600), "structure view validation timed out in phase " + phase);
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

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
