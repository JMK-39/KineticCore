package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridDensity;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Exercises the geometry used for drawing and picking, including fractional scroll and clipped rows. */
public final class ItemGridLayoutRegression {
    private static int checks;

    public static void main(String[] args) throws Exception {
        ItemGridLayout layout = new ItemGridLayout(154, 34, 475, 304, ItemGridDensity.COMPACT);
        check(layout.columns() == 19 && layout.visibleRows() == 12, "fixed viewport fits 19 columns and 12 full rows");
        check(layout.clipLeft() == 156 && layout.clipTop() == 36
                && layout.clipRight() == 627 && layout.clipBottom() == 336, "viewport retains two pixels of padding");
        for (int row = 0; row < 12; row++) {
            for (int col = 0; col < 19; col++) {
                int index = row * 19 + col;
                int x = layout.slotX(index);
                int y = layout.slotY(index, 0, 0);
                check(x >= 156 && x + 22 <= 627 && y >= 36 && y + 22 <= 336, "full slots stay inside the padded clip");
                check(layout.itemAt(x + 3, y + 3, 0, 0, 1000) == index, "rendered native icon selects its own model");
                check(layout.itemAt(x + 21.999, y + 21.999, 0, 0, 1000) == index, "slot includes its last pixel");
                check(layout.itemAt(x + 22, y + 3, 0, 0, 1000) == -1
                        && layout.itemAt(x + 23.999, y + 3, 0, 0, 1000) == -1, "both horizontal gap pixels reject selection");
                check(layout.itemAt(x + 3, y + 22, 0, 0, 1000) == -1
                        && layout.itemAt(x + 3, y + 23.999, 0, 0, 1000) == -1, "both vertical gap pixels reject selection");
            }
        }
        check(layout.slotX(1) - layout.slotX(0) == 24, "neighbour origins give a two pixel gap");
        check(layout.slotY(19, 0, 0) - layout.slotY(0, 0, 0) == 24, "row origins use the same pitch as picking");
        int shiftedIndex = 3 * 19 + 5;
        int shiftedX = layout.slotX(shiftedIndex);
        int shiftedY = layout.slotY(shiftedIndex, 3, 7);
        check(shiftedY == 29 && layout.rowIntersects(shiftedIndex, 3, 7), "fractional scroll clips the first row");
        check(layout.itemAt(shiftedX + 3, 35.999, 3, 7, 1000) == -1, "clipped top pixels cannot select");
        check(layout.itemAt(shiftedX + 3, 36, 3, 7, 1000) == shiftedIndex, "visible part of the scrolled slot selects correctly");
        check(layout.itemAt(shiftedX + 3, shiftedY + 22, 3, 7, 1000) == -1, "scrolled gap remains empty");
        check(layout.rowIntersects(12 * 19, 0, 0), "partly visible next row participates in rendering");
        check(!layout.rowIntersects(13 * 19, 0, 0), "rows wholly below the clip are skipped");
        check(layout.itemAt(159, 335.999, 0, 0, 1000) == 12 * 19, "visible part of bottom row can select");
        check(layout.itemAt(159, 336, 0, 0, 1000) == -1, "bottom padding cannot select hidden pixels");
        check(layout.itemAt(627, 40, 0, 0, 1000) == -1
                && layout.itemAt(155.999, 40, 0, 0, 1000) == -1, "viewport side padding cannot select");
        check(layout.itemAt(156, 36, 0, 0, 0) == -1, "empty lists have no selectable tile");
        check(layout.itemAt(Double.NaN, 36, 0, 0, 1000) == -1, "invalid pointer has no tile");
        for (int shift : new int[]{0, 7, 23}) {
            for (double y = layout.clipTop() + 0.5; y < layout.clipBottom(); y++) {
                int hit = layout.itemAt(159, y, 3, shift, 1000);
                check(hit < 0 || hit < layout.renderEndIndex(3, shift, 1000),
                        "every selectable pixel belongs to a row included in rendering at scroll shift " + shift);
            }
        }
        for (ItemGridDensity density : ItemGridDensity.values()) {
            ItemGridLayout other = new ItemGridLayout(11, 13, 317, 211, density);
            int index = other.columns() + 1;
            int x = other.slotX(index), y = other.slotY(index, 0, 0);
            check(other.itemAt(x + 0.5, y + 0.5, 0, 0, 1000) == index, density + " keeps placement and picking aligned");
        }
        var tile = ImageIO.read(Path.of("src/main/resources/assets/kineticcore/textures/gui/item_slot.png").toFile());
        check(tile.getWidth() == 8 && tile.getHeight() == 8, "asset is one complete 8x8 reusable checker tile");
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int expected = ((x / 4 + y / 4) & 1) == 0 ? 0xFFCFCFCF : 0xFFBBBBBB;
                check(tile.getRGB(x, y) == expected, "tile repeats without seams and retains the original two colours");
            }
        }
        System.out.println("PASS: " + checks + " item grid placement, hit and clipping checks");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
