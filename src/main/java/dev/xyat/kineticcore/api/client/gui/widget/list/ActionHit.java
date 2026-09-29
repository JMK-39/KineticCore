package dev.xyat.kineticcore.api.client.gui.widget.list;



/**
 * Identifies the trailing button under the pointer.
 *
 * @param rowIndex row index
 * @param actionIndex button index within the row, from left to right
 */
public record ActionHit(int rowIndex, int actionIndex) {
}
