package dev.xyat.kineticcore.api.client.gui.theme;

/**
 * 所有 Kinetic 界面共用的固定尺寸。物品格与生物格在每个附属里都必须一样大，除非是特意放大或缩小的展示。
 * Fixed sizes every Kinetic screen shares. Item slots and mob cells must be the same size in every add-on, except for
 * deliberately larger or smaller showcase displays.
 */
public final class KineticSizes {
    /** 物品格边长：16 px 原版物品图标居中 / Item slot side: the 16 px vanilla item icon, centred. Same as ItemGridDensity.COMPACT. */
    public static final int ITEM_SLOT = 22;
    /** 相邻物品格间距 / Gap between neighbouring item slots. */
    public static final int ITEM_GAP = 2;
    /** 物品格步距（格子 + 间距）/ Item slot pitch (slot + gap). */
    public static final int ITEM_PITCH = ITEM_SLOT + ITEM_GAP;
    /** 物品图标缩放（1 = 原版 16 px）/ Item icon scale (1 = the vanilla 16 px). */
    public static final float ITEM_SCALE = 1.0F;

    /** 3D 生物格边长 / 3D mob cell side. */
    public static final int ENTITY_CELL = dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_SIZE;
    /** 相邻生物格间距 / Gap between neighbouring mob cells. */
    public static final int ENTITY_GAP = dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_GAP;
    /** 生物格步距 / Mob cell pitch. */
    public static final int ENTITY_PITCH = ENTITY_CELL + ENTITY_GAP;

    private KineticSizes() {
    }
}
