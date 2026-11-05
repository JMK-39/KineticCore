package dev.xyat.kineticcoreitemcreatorvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;
import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTextField;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.gui.LayoutCheck;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Opt-in real native-selector clicks. Every callback and extra index page belongs only to this fixture. */
public final class ItemListGuiValidation {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ItemListGuiValidation.class);
    private static final String UNKNOWN = "kineticcore_validation:unregistered_item";
    private static final List<String> INITIAL = List.of("minecraft:stone", "minecraft:diamond", UNKNOWN,
            "#kineticcore_validation:unknown_tag", "@kineticcore_validation_unknown_mod");
    private static final List<List<String>> saves = new ArrayList<>(), entitySaves = new ArrayList<>();
    private static final List<String> checks = new ArrayList<>(), registeredPages = new ArrayList<>();
    private static int phase, ticks, captures;
    private static boolean installed, preparing;
    private static Path pendingCapture;
    private static volatile boolean screenshotComplete;
    private static volatile Throwable screenshotFailure;
    private static Screen original, caller, editor;
    private static String tag;
    private ItemListGuiValidation() {}

    public static void install() {
        if (installed) return;
        installed = true;
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, ItemListGuiValidation::tick);
    }
    private static void tick() {
        if (phase == 99) return;
        try {
            var client = Minecraft.getInstance();
            require(++ticks < (phase == 0 ? 12000 : 2400), "selector phase timeout: " + phase);
            if (client.getOverlay() != null || ticks < 12) return;
            if (pendingCapture != null && !screenshotComplete) return;
            switch (phase) {
                case 0 -> {
                    // Tags are verified against the installed world's synchronized registries.
                    if (client.player == null || client.level == null) return;
                    if (!dev.xyat.kineticcore.api.client.search.KineticItemSearch.ready()) {
                        if (!preparing) {
                            preparing=true;
                            dev.xyat.kineticcore.api.client.search.KineticItemSearch.prepare(null);
                        }
                        return;
                    }
                    require(client.getWindow().getWidth()==1920 && client.getWindow().getHeight()==1080, "requires 1920x1080");
                    original=client.screen;
                    caller=new Screen(Component.literal("Selector validation parent")) {};
                    KineticClientRuntime.openScreen(caller);open();next(1);
                }
                case 1 -> {
                    require(editor.getClass().getSuperclass().getSimpleName().equals("ItemSelectorScreen"), "list editor must be the native selector subclass");
                    require(!((List<?>)value(editor,"displayList")).isEmpty(), "all items are visible immediately without Add");
                    require(rules().equals(new LinkedHashSet<>(INITIAL)), "initial unknown/tag/mod rules are retained");
                    require(number(editor,"SLOT_SIZE")==22 && number(editor,"CELL_SIZE")==24, "native item slots use 22 pixels plus two-pixel gaps");
                    var first=slots().get(0);double x=slotNumber(first,"x"),y=slotNumber(first,"y");
                    require(invoke(editor,"findVisibleSlot",x+22.5,y+2D)==null, "item grid gap must not select an item");
                    capture("item-list-direct");search().setTextValue("minecraft:diamond");next(2);
                }
                case 2 -> {
                    clickItem(Items.DIAMOND,1);
                    require(!rules().contains("minecraft:diamond") && rules().contains("minecraft:stone"), "filtered right-click removes only the matching concrete rule");
                    search().setTextValue("minecraft:gold_ingot");next(3);
                }
                case 3 -> {clickItem(Items.GOLD_INGOT,0);search().setTextValue("minecraft:emerald");next(4);}
                case 4 -> {clickItem(Items.EMERALD,0);next(5);}
                case 5 -> {
                    require(rules().containsAll(List.of("minecraft:gold_ingot","minecraft:emerald",UNKNOWN)), "direct grid keeps multiple selections and unknown rules");
                    String file=capture("item-list-multi-green");assertItemGreen(file,Items.EMERALD);
                    click(24,12,0);require(client.screen==caller && saves.isEmpty(), "Back returns to parent without saving the multi-selection");
                    checks.add("PASS item multi-selection Back returns to parent without saving");
                    open();search().setTextValue("minecraft:gold_ingot");next(6);
                }
                case 6 -> {clickItem(Items.GOLD_INGOT,0);search().setTextValue("minecraft:emerald");next(7);}
                case 7 -> {clickItem(Items.EMERALD,0);next(8);}
                case 8 -> {
                    clickButton(topButtons().get(0));
                    require(saves.size()==1 && new LinkedHashSet<>(saves.get(0)).containsAll(INITIAL)
                            && saves.get(0).containsAll(List.of("minecraft:gold_ingot","minecraft:emerald")), "Save callback receives the whole draft while search is filtered");
                    require(client.screen==caller && saves.size()==1, "Save returns to the same parent after one callback");
                    verifySaveCallbackNavigation();
                    open();
                    var tags=(List<String>)value(editor,"allTags");
                    require(!tags.isEmpty(), "world item tags are available");
                    tag=tags.contains("minecraft:logs")?"minecraft:logs":tags.get(0);
                    search().setTextValue("#"+tag);invoke(editor,"applySelectedSuggestion","#"+tag);next(9);
                }
                case 9 -> {
                    require(number(editor,"activeFilterType")==2, "native #tag filter is active");
                    clickButton((KineticButton)value(editor,"applyFilterBtn"));
                    require(rules().contains("#"+tag), "Apply adds a native tag rule without leaving the list");
                    checks.add("PASS native tag rule #"+tag);next(10);
                }
                case 10 -> {
                    String file=capture("item-list-tag-green");assertFirstSlotGreen(file);
                    search().setTextValue("@minecraft");invoke(editor,"applySelectedSuggestion","@minecraft");next(11);
                }
                case 11 -> {
                    require(number(editor,"activeFilterType")==1, "native @mod filter is active");
                    clickButton((KineticButton)value(editor,"applyFilterBtn"));
                    require(rules().contains("@minecraft") && rules().contains(UNKNOWN), "Apply adds a mod rule and preserves unresolved rules");
                    checks.add("PASS native mod rule @minecraft preserves unresolved rules");next(12);
                }
                case 12 -> {
                    String file=capture("item-list-mod-green");assertFirstSlotGreen(file);
                    clickButton(topButtons().get(1));next(13);
                }
                case 13 -> {
                    Object menu=value(overlays(),"contextMenu");require(menu!=null,"Rules menu opens");
                    capture("item-list-rules");
                    KineticButton remove=null;
                    for(Object control:(List<?>)invoke(menu,"controls")) {
                        var button=(KineticButton)invoke(control,"button");
                        if(button!=null && button.text().getString().equals(UNKNOWN)){remove=button;break;}
                    }
                    require(remove!=null,"unregistered rule remains visible and removable in Rules");
                    clickScreen(remove.controlX()+remove.controlWidth()/2D,remove.controlY()+remove.controlHeight()/2D,0);
                    require(!rules().contains(UNKNOWN) && rules().containsAll(List.of("#"+tag,"@minecraft","#kineticcore_validation:unknown_tag")), "menu removal keeps unrelated tag and mod rules");
                    checks.add("PASS Rules menu removes unknown ID while preserving other unresolved/group rules");
                    click(1,359,0);next(14);
                }
                case 14 -> {
                    click(24,12,0);require(client.screen==caller && saves.size()==1,"unsaved rule menu edits are discarded on Back");
                    openIndex();next(15);
                }
                case 15 -> {
                    require(client.screen.getClass().getSimpleName().equals("KTConfigIndexScreen"),"actual config index opened");
                    capture("index-start");invoke(value(client.screen,"listScroll"),"setOffset",Integer.MAX_VALUE);next(16);
                }
                case 16 -> {
                    verifyIndexEnd();capture("index-exact-end");click(24,12,0);
                    require(client.screen==caller,"index Back returns to parent");cleanupPages();
                    KineticSelectors.openEntitySelector(Component.literal("Entity validation"),List.of("minecraft:zombie"),result->entitySaves.add(List.copyOf(result)));
                    next(17);
                }
                case 17 -> {
                    verifyEntityGeometry();capture("entity-52-grid");search().setTextValue("minecraft:cow");next(18);
                }
                case 18 -> {
                    click(5,29,0);require(((Set<?>)value(client.screen,"selectedIds")).contains("minecraft:cow"),"native entity card click adds a second entity");next(19);
                }
                case 19 -> {
                    String file=capture("entity-multi-green");assertGreen(file,4,28,dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_SIZE);
                    clickButton(topButtons().get(0));
                    require(client.screen==caller && entitySaves.size()==1 && entitySaves.get(0).containsAll(List.of("minecraft:zombie","minecraft:cow")), "entity Apply returns all selected IDs to caller");
                    checks.add("PASS entity Apply returns native multi-selection to parent");
                    KineticSelectors.openEntitySelector(Component.literal("Entity cancellation"),List.of("minecraft:zombie"),result->entitySaves.add(List.copyOf(result)));
                    search().setTextValue("minecraft:pig");next(20);
                }
                case 20 -> {click(5,29,0);click(24,12,0);next(21);}
                case 21 -> {
                    Object dialog=value(overlays(),"dialog");require(dialog!=null,"changed entity Back asks whether to apply");
                    capture("entity-cancel-confirm");
                    var cancel=(KineticButton)value(dialog,"cancelButton");clickScreen(cancel.controlX()+cancel.controlWidth()/2D,cancel.controlY()+cancel.controlHeight()/2D,0);
                    require(client.screen==caller && entitySaves.size()==1,"No discards entity edits and returns to parent without callback");
                    checks.add("PASS entity Back/No discards draft without callback");
                    finish(null);
                }
                default -> throw new AssertionError("unknown phase "+phase);
            }
        } catch(CapturePending ignored) {
            // The 26.1 renderer completes readback asynchronously; resume this phase only after the PNG is written.
        } catch(Throwable failure) {finish(failure);}
    }

    private static void open() {
        KineticSelectors.openItemListEditor(Component.literal("Native item multi-selection"),INITIAL,
                KineticSelectors.ItemListMode.ITEMS_TAGS_MODS,result->saves.add(List.copyOf(result)));
        editor=Minecraft.getInstance().screen;
    }
    private static void next(int next) {phase=next;ticks=0;}
    private static LinkedHashSet<String> rules() throws Exception {return new LinkedHashSet<>((Collection<String>)value(editor,"rules"));}
    private static KineticTextField search() throws Exception {
        var screen=Minecraft.getInstance().screen;
        try {return (KineticTextField)value(screen,"searchBox");}
        catch(NoSuchFieldException ignored) {
            return screen.children().stream().filter(child->child instanceof KineticTextField field && field.controlY()==4)
                    .map(child->(KineticTextField)child).findFirst().orElseThrow(()->new AssertionError("native search field missing"));
        }
    }
    private static void verifySaveCallbackNavigation() {
        Screen target=new Screen(Component.literal("Save callback destination")) {};
        int[] calls={0};
        KineticSelectors.openItemListEditor(Component.literal("Save callback navigation"),INITIAL,
                KineticSelectors.ItemListMode.ITEMS_TAGS_MODS,result->{
                    require(result.equals(INITIAL),"callback navigation receives the initial rule draft");
                    calls[0]++;
                    KineticClientRuntime.openScreen(target);
                });
        clickButton(topButtons().get(0));
        require(calls[0]==1 && Minecraft.getInstance().screen==target,"Save preserves the destination opened by its callback");
        checks.add("PASS Save returns to parent and preserves explicit callback navigation");
        KineticClientRuntime.openScreen(caller);
    }
    private static List<?> slots() throws Exception {return (List<?>)value(editor,"visibleSlotCache");}
    private static int slotNumber(Object slot,String name) throws Exception {return (Integer)invoke(slot,name);}
    private static int number(Object object,String name) throws Exception {return (Integer)value(object,name);}
    private static Object overlays() {return ((KineticScreenHost)Minecraft.getInstance().screen).kineticRuntime().overlays();}
    private static List<KineticButton> topButtons() {
        return Minecraft.getInstance().screen.children().stream().filter(child->child instanceof KineticButton button
                        && button.controlY()==4 && button.controlVisible() && button.isEnabled())
                .map(child->(KineticButton)child).sorted(java.util.Comparator.comparingInt(KineticControl::controlX).reversed()).toList();
    }
    private static void clickButton(KineticButton button) {click(button.controlX()+button.controlWidth()/2D,button.controlY()+button.controlHeight()/2D,0);}
    private static void clickItem(net.minecraft.world.item.Item item,int button) throws Exception {
        for(Object slot:slots()) if(((ItemStack)invoke(slot,"stack")).is(item)) {
            click(slotNumber(slot,"x")+10,slotNumber(slot,"y")+10,button);
            require(Minecraft.getInstance().screen==editor,"multi-selection must keep the same screen open");return;
        }
        throw new AssertionError("item is missing from rendered native slots: "+item);
    }
    private static void click(double x,double y,int button) {
        var canvas=((KineticScreenHost)Minecraft.getInstance().screen).kineticRuntime().canvas();
        clickScreen(canvas.toScreenX(x),canvas.toScreenY(y),button);
    }
    private static void clickScreen(double x,double y,int button) {
        Screen screen=Minecraft.getInstance().screen;
        //? if >=26.1 {
        /*var input=new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0));
        screen.mouseClicked(input,false);screen.mouseReleased(input);*/
        //?} else {
        screen.mouseClicked(x,y,button);screen.mouseReleased(x,y,button);
        //?}
    }
    private static void openIndex() {
        for(int i=0;i<20;i++) {
            String id="kineticcore_scroll_validation_"+i+":layout";registeredPages.add(id);
            KTConfigApi.register(KTConfigPage.builder(id,Component.literal("Scroll sample "+i)).scope(KTConfigScope.CLIENT_LOCAL)
                    .intValueUnbounded("sample",Component.literal("Sample"),()->7,value->{},1,null).build());
        }
        KTConfigApi.openIndex();
    }
    private static void cleanupPages() {for(String id:registeredPages)KTConfigApi.unregister(id);registeredPages.clear();}
    private static void verifyIndexEnd() throws Exception {
        Object screen=Minecraft.getInstance().screen;
        var modules=(List<?>)invoke(value(screen,"moduleModel"),"items");
        int count=modules.size(),height=number(screen,"LIST_HEIGHT"),stride=number(screen,"ROW_STRIDE"),row=number(screen,"ROW_HEIGHT"),y=number(screen,"LIST_Y");
        require(count>height/stride,"index has enough isolated sample modules to exercise scrolling");
        double offset=(Double)invoke(value(screen,"listScroll"),"smoothOffset");
        double expected=((count-1D)*stride+row-height)/stride;
        require(Math.abs(offset-expected)<0.001,"index offset clamps to exact content end");
        int top=y+(int)Math.round((count-1D-offset)*stride);
        require(top>=y && top+row==y+height,"last index row is completely visible with no trailing blank row");
        require(invoke(screen,"moduleAt",5D,(double)y+height-1)==modules.get(count-1)
                && invoke(screen,"moduleAt",5D,(double)y+height+1)==null,"actual index row hit testing matches its clipped viewport");
        checks.add("PASS index actual screen exact end modules="+count+" lastTop="+top);
    }
    private static void verifyEntityGeometry() throws Exception {
        Object screen=Minecraft.getInstance().screen;
        require(screen.getClass().getSimpleName().equals("EntitySelectorScreen"),"native entity selector opened");
        int size=dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_SIZE;
        int gap=dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_GAP,pitch=size+gap;
        int columns=number(screen,"COLS"),x=number(screen,"GRID_X"),y=number(screen,"GRID_Y");
        require(size==50 && gap==2 && columns==12,"entity selector uses fifty-pixel cards and twelve columns");
        require(number(screen,"CELL_W")==pitch && number(screen,"CELL_H")==pitch,"entity pitch follows the shared card size and gap");
        require((Integer)invoke(screen,"entityIndex",x+size+.5,y+1D)==-1
                && (Integer)invoke(screen,"entityIndex",x+pitch+1D,y+1D)==1,"two-pixel entity gap is not clickable");
        require((Integer)invoke(screen,"entityIndex",x+(columns-1D)*pitch+1,y+5D*pitch+1)==6*columns-1,"six complete entity rows fit");
        checks.add("PASS entity native selector "+size+"x"+size+" cards, "+gap+"-pixel gaps, "+columns+"x6 complete rows");
    }
    private static String capture(String name) throws Exception {
        require(LayoutCheck.currentScreenProblems().isEmpty() && LayoutCheck.lastCheckedControls>0,"native screen controls pass layout check");
        Path path=directory().resolve(String.format("%02d-%s.png",phase,name));
        var client=Minecraft.getInstance();
        //? if >=26.1 {
        /*if (pendingCapture==null) {
            pendingCapture=path;screenshotComplete=false;screenshotFailure=null;
            net.minecraft.client.Screenshot.takeScreenshot(client.getMainRenderTarget(),image->{
                try(image) {image.writeToFile(path);} catch(Throwable error) {screenshotFailure=error;}
                finally {screenshotComplete=true;}
            });
            throw new CapturePending();
        }
        require(pendingCapture.equals(path),"asynchronous screenshot belongs to the current phase");
        if(screenshotFailure!=null)throw new java.io.IOException("screenshot failed",screenshotFailure);
        pendingCapture=null;screenshotComplete=false;
        *///?} else {
        try(var image=net.minecraft.client.Screenshot.takeScreenshot(client.getMainRenderTarget())) {image.writeToFile(path);}
        //?}
        captures++;checks.add("CAPTURE "+path.getFileName());writeResult("RUNNING");return path.toString();
    }
    private static final class CapturePending extends RuntimeException {
        private CapturePending() {super(null,null,false,false);}
    }
    private static void assertItemGreen(String file,net.minecraft.world.item.Item item) throws Exception {
        for(Object slot:slots()) if(((ItemStack)invoke(slot,"stack")).is(item)) {assertGreen(file,slotNumber(slot,"x"),slotNumber(slot,"y"),22);return;}
        throw new AssertionError("selected visible item missing");
    }
    private static void assertFirstSlotGreen(String file) throws Exception {
        require(!slots().isEmpty(),"active group filter has rendered item choices");var slot=slots().get(0);
        require((Boolean)invoke(editor,"selected",invoke(slot,"stack")),"group rule marks native item selected");
        assertGreen(file,slotNumber(slot,"x"),slotNumber(slot,"y"),22);
    }
    private static void assertGreen(String file,int x,int y,int size) throws Exception {
        var client=Minecraft.getInstance();var screen=client.screen;
        var canvas=((KineticScreenHost)screen).kineticRuntime().canvas();
        var image=javax.imageio.ImageIO.read(Path.of(file).toFile());
        boolean found=false;
        for(double[] point:new double[][]{{x+.3,y+size/2D},{x+size/2D,y+.3},{x+size-.7,y+size/2D}}) {
            int px=(int)Math.floor(canvas.toScreenX(point[0])*image.getWidth()/screen.width),py=(int)Math.floor(canvas.toScreenY(point[1])*image.getHeight()/screen.height);
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++) if(px+dx>=0 && py+dy>=0 && px+dx<image.getWidth() && py+dy<image.getHeight()) {
                int rgb=image.getRGB(px+dx,py+dy),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;
                // The Core success border breathes; verify its rendered green hue rather than one animation frame's RGB.
                if(g>=100 && g>r*1.2 && g>b*1.08 && b>r)found=true;
            }
        }
        require(found,"rendered selected card has the actual green outline");checks.add("PASS rendered green outline "+Path.of(file).getFileName());
    }
    private static Path directory() throws Exception {
        String configured=System.getProperty("kineticcore.itemListGuiValidation.output");
        Path path=configured==null?dev.xyat.kineticcore.api.runtime.KineticPlatform.gameDirectory().resolve("item-list-gui-validation"):Path.of(configured);
        require(path.isAbsolute(),"output must be absolute");Files.createDirectories(path);return path;
    }
    private static void writeResult(String status) throws Exception {Files.writeString(directory().resolve("result.txt"),status+" phase="+phase+" captures="+captures+System.lineSeparator()+String.join(System.lineSeparator(),checks));}
    private static void finish(Throwable error) {
        checks.add("END phase="+phase);phase=99;cleanupPages();
        try {
            if(error!=null){var text=new java.io.StringWriter();error.printStackTrace(new java.io.PrintWriter(text));checks.add(text.toString());LOG.error("ITEM_LIST_GUI_FAIL",error);}
            KineticClientRuntime.openScreen(original);writeResult(error==null?"PASS":"FAIL");
            String next=System.getProperty("kineticcore.itemListGuiValidation.thenValidation","").trim();
            if(error==null && !next.isBlank()) {
                if(next.endsWith(".install()"))next=next.substring(0,next.length()-10);
                Class.forName(next).getMethod("install").invoke(null);return;
            }
        } catch(Throwable failure){
            LOG.error("ITEM_LIST_GUI_FINISH_FAIL",failure);
            var text=new java.io.StringWriter();failure.printStackTrace(new java.io.PrintWriter(text));checks.add(text.toString());
            try {writeResult("FAIL");} catch(Exception writeFailure) {LOG.error("ITEM_LIST_GUI_RESULT_FAIL",writeFailure);}
        }
        if(Boolean.parseBoolean(System.getProperty("kineticcore.itemListGuiValidation.stopAfter","true")))KineticClientRuntime.stopClient();
    }
    private static Object value(Object object,String name) throws Exception {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try {Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static Object invoke(Object object,String name,Object...args) throws Exception {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())for(var method:type.getDeclaredMethods()) {
            if(!method.getName().equals(name)||method.getParameterCount()!=args.length)continue;
            var types=method.getParameterTypes();boolean match=true;
            for(int i=0;i<types.length;i++) if(args[i]!=null && !(types[i].isInstance(args[i])||types[i]==int.class&&args[i] instanceof Integer||types[i]==double.class&&args[i] instanceof Double||types[i]==boolean.class&&args[i] instanceof Boolean))match=false;
            if(match){method.setAccessible(true);return method.invoke(object,args);}
        }
        throw new NoSuchMethodException(name);
    }
    private static void require(boolean valid,String message){if(!valid)throw new AssertionError(message);}
}
