package dev.xyat.kineticcore.api.client.gui.widget.list;



/** Business-state outline of an item grid slot. Colors come from the theme. */
public enum ItemGridOutline {
    /** No state outline; the slot uses the default outline. */
    NONE,
    /** Positive state, for example a satisfied requirement. */
    SUCCESS,
    /** Attention state, for example a partial match. */
    WARNING
}
