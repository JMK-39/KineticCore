package dev.xyat.kineticcore.internal.client.gui.theme;

import dev.xyat.kineticcore.api.client.gui.widget.list.KineticEntityGrid;
import net.minecraft.network.chat.Component;

/** Actual theme policy and the source/binary-compatible entity-card entry constructors. */
public final class HoverAndEntityStateRegression {
    public static void main(String[] args) throws Exception {
        var border = GuiTheme.class.getDeclaredMethod("stateBorder", boolean.class, boolean.class, boolean.class, long.class);
        border.setAccessible(true);
        int peak = (int) border.invoke(null, false, true, false, 0L);
        int dim = (int) border.invoke(null, false, true, false, 700L);
        require(peak == 0xff4da6ff && dim != peak && dim >>> 24 == 255, "hover must breathe in opaque blue");
        require((int)border.invoke(null,false,true,false,1400L)==peak, "hover pulse must wrap smoothly");
        require((int)border.invoke(null,true,true,false,0L)==0xffffaa00, "current selection must keep its own color");
        require((int)border.invoke(null,false,false,true,900L)==0xffff5555, "error-only border must remain red");
        require((int)border.invoke(null,false,false,false,900L)==0xffffffff, "idle border must remain white");
        var legacy = new KineticEntityGrid.Entry("minecraft:pig",Component.literal("pig"),true);
        var current = KineticEntityGrid.Entry.class.getMethod("current");
        var error = KineticEntityGrid.Entry.class.getMethod("error");
        require(legacy.selected() && !(boolean)current.invoke(legacy) && !(boolean)error.invoke(legacy), "legacy green selection must not become current/error");
        var constructor=KineticEntityGrid.Entry.class.getConstructor(String.class,Component.class,boolean.class,boolean.class,boolean.class);
        var invalid=(KineticEntityGrid.Entry)constructor.newInstance("missing:mob",Component.literal("missing"),false,true,true);
        require(!invalid.selected() && (boolean)current.invoke(invalid) && (boolean)error.invoke(invalid), "current/error must be independent of green configuration state");
        System.out.println("PASS breathing hover, stable state colors and independent compatible entity-card states");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
