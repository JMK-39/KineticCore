package dev.xyat.kineticcore.api.client.gui.widget.list;



/**
 * Identifies the toggle under the pointer.
 *
 * @param rowIndex row index
 * @param toggleIndex toggle index within the row, from left to right
 */
public record ToggleHit(int rowIndex, int toggleIndex) {
}
