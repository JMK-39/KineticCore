package dev.xyat.kineticcoreitemcreatorvalidation;

import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.internal.client.editor.itemcreator.*;
import dev.xyat.kineticcore.internal.client.gui.LayoutCheck;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.lang.reflect.Field;
import java.util.*;

/** Real rendered screens, real mouse clicks/text responders and real generation receipts. */
public final class ItemCreatorGuiValidation {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(ItemCreatorGuiValidation.class);
    private static int variant,step,wait,captures;
    private static volatile boolean resourcesReady,captureReady;
    private static Runnable done;
    private static ItemCreatorDraft draft;
    private static String healthId;
    private static List<EquipmentSpec> previewGear;
    public static void start(Runnable completion){done=completion;variant=0;step=0;wait=0;captures=0;}
    private static void next(int value){step=value;wait=0;}
    public static void tick()throws Exception{
        var client=Minecraft.getInstance();dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.clearToasts();if(++wait<8)return;
        if(client.getOverlay()!=null){require(wait<1200,"Resource overlay did not finish");return;}
        if(step==0){
            String language=variant==0?"en_us":"zh_cn";int width=1920,height=1080;
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(client.getWindow().getWindow(),width,height);
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.getWindow().getWindow(),0,0);
            //? if >=26.1 {
            /*client.resizeGui();
            *///?} else {
            client.resizeDisplay();
            //?}
            client.options.languageCode=language;client.getLanguageManager().setSelected(language);resourcesReady=false;
            client.reloadResourcePacks().whenComplete((nothing,error)->{if(error!=null)LOG.error("ITEM_CREATOR_GUI_RESOURCE_FAIL",error);resourcesReady=error==null;});next(1);
        }else if(step==1&&resourcesReady){
            //? if >=1.21 {
            /*healthId=KineticRegistries.attributes().id(Attributes.MAX_HEALTH.value()).toString();
            *///?} else {
            healthId=KineticRegistries.attributes().id(Attributes.MAX_HEALTH).toString();
            //?}
            draft=new ItemCreatorDraft();draft.name="Named mob / 命名生物";draft.saveAttribute(new AttributeSpec(healthId,80,AttributeSpec.Operation.SET));
            draft.saveEffect(new EffectSpec("minecraft:speed",2,6000,false,true));draft.saveEffect(new EffectSpec("minecraft:fire_resistance",0,-1,false,true));
            KineticGui.open(new ItemCreatorEditorPage(false,draft));next(2);
        }else if(step==2){checkMissingTranslations();capture("egg-root");next(3);}
        else if(step==3&&captureReady){click(566,38);require((boolean)field(client.screen,"singleSelection"),"entity selector is not single");int gx=(int)field(client.screen,"GRID_X"),gy=(int)field(client.screen,"GRID_Y"),cell=(int)field(client.screen,"CELL_W"),right=(int)field(client.screen,"SCROLL_X")+(int)field(client.screen,"SCROLL_W");click(gx+cell/2,gy+cell/2);click(gx+cell+cell/2,gy+cell/2);require(((Set<?>)field(client.screen,"selectedIds")).size()==1,"entity selector selected more than one");click(right-20,12);require(KineticGui.currentPage()instanceof ItemCreatorEditorPage,"entity apply did not return");draft.entityId="minecraft:zombie";KineticGui.currentPage().rebuild();click(304,62);require((boolean)field(KineticGui.currentPage(),"showEffects"),"effects tab did not switch");require(visible().size()==KineticRegistries.mobEffects().ids().size(),"effects list omitted registry entries");assertOrder();next(4);}
        else if(step==4){capture("effect-catalog");next(5);}
        else if(step==5&&captureReady){textField(432).setTextValue("minecraft:sp");next(105);}
        // The rest is typed key by key: the page rebuilds its search box after every key, and each new box must keep
        // the caret after the text. It once kept the caret at the start (or selected everything), so keys landed wrong.
        else if(step>=105&&step<=107){type("eed".charAt(step-105));next(step==107?6:step+1);}
        else if(step==6){require("minecraft:speed".equals(textField(432).textValue()),"typing after a search rebuild misplaced keys: "+textField(432).textValue());require(visible().size()==1,"search failed");require(textField(432).controlFocused(),"search box lost focus while typing");checkRowCenters();capture("effect-search");next(7);}
        else if(step==7&&captureReady){require(numberField(475).textValue().equals("3")&&numberField(519).textValue().equals("300"),"ticked row did not show its level and seconds");numberField(475).setTextValue("4");numberField(519).setTextValue("60");require(draft.effect("minecraft:speed").amplifier()==3&&draft.effect("minecraft:speed").durationTicks()==1200,"row fields did not edit the effect");numberField(519).setTextValue("-");require(draft.effect("minecraft:speed").durationTicks()==1200,"invalid seconds changed the effect");numberField(519).setTextValue("60");click(260,104);require(draft.effects().size()==1,"clicking a ticked row did not untick it");click(260,104);require(draft.effects().size()==2&&draft.effect("minecraft:speed").amplifier()==3,"ticking again lost the values");next(8);}
        else if(step==8){capture("effect-edit");next(9);}
        else if(step==9&&captureReady){click(220,62);require(!(boolean)field(KineticGui.currentPage(),"showEffects"),"attributes tab did not switch");next(10);}
        else if(step==10){capture("attribute-catalog");next(11);}
        else if(step==11&&captureReady){textField(432).setTextValue(healthId);next(12);}
        else if(step==12){require(visible().size()==1,"attribute ID search failed");require(numberField(563).getDoubleValue()==80,"row did not show the saved attribute");numberField(563).setTextValue("-");require(draft.attribute(healthId).amount()==80,"invalid input applied");numberField(563).setTextValue("96");require(draft.attribute(healthId).amount()==96,"row field did not edit the attribute");capture("attribute-edit");next(13);}
        else if(step==13&&captureReady){checkOperationSwitch();checkRowCenters();for(var spec:SpawnerEggValidation.equipment())draft.setEquipment(spec.slot(),spec.item());click(388,62);require((boolean)field(KineticGui.currentPage(),"showEquipment"),"Equipment tab did not open");next(24);}
        else if(step==24){SpawnerEggValidation.verifyEquipment((net.minecraft.world.entity.LivingEntity)field(KineticGui.currentPage(),"previewEntity"));capture("egg-equipment");next(25);}
        else if(step==25&&captureReady){equipmentAction(2);require(draft.equipment().size()==5,"Clear did not remove main-hand equipment");click(193,105);next(28);}
        else if(step==28&&client.screen instanceof dev.xyat.kineticcore.internal.client.selector.ItemSelectorScreen){testEquipment();next(26);}
        else if(step==26){capture("egg-equipment-edited");next(27);}
        else if(step==27&&captureReady){previewGear=draft.equipment();for(var slot:EquipmentSpec.Slot.values())draft.setEquipment(slot,null);KineticGui.currentPage().rebuild();next(29);}
        else if(step==29){var grid=(dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemGrid)field(KineticGui.currentPage(),"equipmentGrid");require(grid.columns()==6&&grid.maxScrollOffset()==0&&grid.controlHeight()==26,"Equipment grid is not compact");for(var item:grid.items()){var icon=item.emptyIcon();require(icon!=null&&client.getResourceManager().getResource(dev.xyat.kineticcore.api.resource.KineticResourceIds.of(icon.namespace(),icon.path())).isPresent(),"Missing native equipment slot icon");}capture("egg-equipment-empty-slots");next(30);}
        else if(step==30&&captureReady){for(var spec:previewGear)draft.setEquipment(spec.slot(),spec.item());draft.name="Named potion / 命名药水";KineticGui.open(new ItemCreatorEditorPage(true,draft));require(textField(180)!=null,"potion page needs its name box");require(!visible().isEmpty(),"potion page needs its effect list");next(14);}
        else if(step==14){checkRowCenters();capture("potion-root");next(15);}
        else if(step==15&&captureReady){click(586,38);require(((KineticScreenHost)client.screen).overlayBlocksInput(),"form dropdown did not expand");next(16);}
        else if(step==16){capture("potion-form-menu");next(17);}
        else if(step==17&&captureReady){((KineticScreenHost)client.screen).closeContextMenu();click(612,14);require((boolean)field(KineticGui.currentPage(),"pending"),"generation did not wait for receipt");require(!button(588,4).isEnabled(),"pending generate button still enabled");next(18);}
        else if(step==18&&!(boolean)field(KineticGui.currentPage(),"pending")){require(((net.minecraft.network.chat.contents.TranslatableContents)((net.minecraft.network.chat.Component)field(KineticGui.currentPage(),"notice")).getContents()).getKey().equals("gui.kineticcore.item_creator.generated"),"success not based on receipt");draft.selectEffect("minecraft:speed",false);draft.selectEffect("minecraft:fire_resistance",false);KineticGui.currentPage().rebuild();click(612,14);next(19);}
        else if(step==19&&!(boolean)field(KineticGui.currentPage(),"pending")){require(draft.effects().isEmpty(),"failed generation modified draft");require(((net.minecraft.network.chat.contents.TranslatableContents)((net.minecraft.network.chat.Component)field(KineticGui.currentPage(),"notice")).getContents()).getKey().equals("gui.kineticcore.item_creator.invalid"),"failure receipt not displayed");testRecords();next(20);}
        else if(step==20){capture("potion-records");next(21);}
        else if(step==21&&captureReady){testEggRecords();next(22);}
        else if(step==22){capture("egg-records");next(23);}
        else if(step==23&&captureReady){LOG.info("ITEM_CREATOR_CASE_PASS case=gui-input-language-1080p variant={} captures={}",variant,captures);KineticGui.closeScreen();if(++variant<Integer.getInteger("kineticcore.itemCreatorGui.variants",1))next(0);else{LOG.info("ITEM_CREATOR_GUI_PASS captures={}",captures);done.run();}}
        require(wait<900,"GUI phase timed out: "+step);
    }
    /** Saved records: Save writes and pins, New asks before dropping edits, records reopen from the file. */
    private static void testRecords()throws Exception{
        var path=KineticPlatform.gameDirectory().resolve("itemcreator-ui-presets-"+variant+".json");java.nio.file.Files.deleteIfExists(path);var store=new ItemCreatorPresets(path);
        KineticGui.open(new ItemCreatorEditorPage(true,store));var page=KineticGui.currentPage();
        require(!button(544,4).isEnabled(),"blank draft can be saved");
        textField(180).setTextValue("First potion");clickRow(rowOf("minecraft:fire_resistance"));
        require(button(544,4).isEnabled(),"edited draft cannot be saved");click(564,14);
        require(store.entries(true).size()==1,"Save did not store a record");String first=(String)field(page,"recordId");require(first!=null,"saved record not kept open");
        require(visible().get(0).value().equals("minecraft:fire_resistance"),"saved effect not moved to the top");
        click(520,14);require(field(page,"recordId")==null&&((ItemCreatorDraft)field(page,"draft")).isBlank(),"New did not start a blank draft");
        textField(180).setTextValue("Second potion");click(520,14);require(((KineticScreenHost)Minecraft.getInstance().screen).overlayBlocksInput(),"New dropped unsaved edits without asking");dismissOverlay();require(((ItemCreatorDraft)field(page,"draft")).name.equals("Second potion"),"cancelling the question dropped the edits");
        KineticGui.currentPage().rebuild();clickRow(rowOf("minecraft:speed"));click(564,14);
        require(store.entries(true).size()==2&&store.entries(true).get(0).draft().name().equals("Second potion"),"newest record not first");
        var reopened=new ItemCreatorPresets(path);require(reopened.entries(true).size()==2&&!reopened.activeId(true).equals(first),"records did not survive reopening");
        KineticGui.open(new ItemCreatorEditorPage(true,reopened));require(((ItemCreatorDraft)field(KineticGui.currentPage(),"draft")).name.equals("Second potion"),"editor did not reopen the last record");
        click(88,64);require(first.equals(field(KineticGui.currentPage(),"recordId")),"clicking a record did not open it");
        click(88,64,1);require(((KineticScreenHost)Minecraft.getInstance().screen).overlayBlocksInput(),"record menu did not open");((KineticScreenHost)Minecraft.getInstance().screen).closeContextMenu();
    }
    /** The spawn-egg editor keeps its own records in the same file, next to the potion records. */
    private static void testEggRecords()throws Exception{
        var store=new ItemCreatorPresets(KineticPlatform.gameDirectory().resolve("itemcreator-ui-presets-"+variant+".json"));
        KineticGui.open(new ItemCreatorEditorPage(false,store));
        require(!button(544,4).isEnabled(),"blank egg can be saved");
        textField(180).setTextValue("Zombie captain");clickRow(rowOf(healthId));var recordDraft=(ItemCreatorDraft)field(KineticGui.currentPage(),"draft");for(var spec:SpawnerEggValidation.equipment())recordDraft.setEquipment(spec.slot(),spec.item());click(564,14);
        require(store.entries(false).size()==1,"Save did not store an egg record");
        require(store.entries(true).size()==2,"egg record mixed into the potion records");
        require(new ItemCreatorPresets(KineticPlatform.gameDirectory().resolve("itemcreator-ui-presets-"+variant+".json")).entries(false).get(0).draft().equipment().size()==6,"Equipment missing after preset reopen");
        KineticGui.currentPage().rebuild();
    }
    private static void checkRowCenters(){
        for(var child:Minecraft.getInstance().screen.children())if(child instanceof KineticControl control && control.controlX()>=180&&control.controlY()>=92&&control.controlY()+control.controlHeight()<=356){
            require((control.controlY()-92)%24==(24-control.controlHeight())/2,"Row control is not vertically centered: "+control.controlX()+","+control.controlY());
        }
    }
    private static void checkMissingTranslations()throws Exception{
        var type=Class.forName("dev.xyat.kineticcore.internal.client.editor.itemcreator.ItemCreatorEntryText");var method=type.getDeclaredMethod("label",boolean.class,String.class);method.setAccessible(true);int count=0;
        for(var entry:KineticRegistries.attributes().entries().entrySet())if(!dev.xyat.kineticcore.api.text.KineticI18n.hasTranslation(entry.getValue().getDescriptionId())){
            String id=entry.getKey().toString();require(((net.minecraft.network.chat.Component)method.invoke(null,false,id)).getString().equals(id),"Untranslated attribute key repeated");count++;
        }
        for(var entry:KineticRegistries.mobEffects().entries().entrySet())if(!dev.xyat.kineticcore.api.text.KineticI18n.hasTranslation(entry.getValue().getDescriptionId())){
            String id=entry.getKey().toString();require(((net.minecraft.network.chat.Component)method.invoke(null,true,id)).getString().equals(id),"Untranslated effect key repeated");count++;
        }
        require(count>0,"No missing translations tested");LOG.info("ITEM_CREATOR_TRANSLATION_PASS checked={}",count);
    }
    private static void testEquipment()throws Exception{
        var selector=Minecraft.getInstance().screen;require(selector instanceof dev.xyat.kineticcore.internal.client.selector.ItemSelectorScreen,"Equipment item selector did not open");
        var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        stack.setDamageValue(7);
        var choose=dev.xyat.kineticcore.internal.client.selector.ItemSelectorScreen.class.getDeclaredMethod("choose",dev.xyat.kineticcore.internal.client.selector.ItemSelectorScreen.Selection.class,int.class);choose.setAccessible(true);choose.invoke(selector,dev.xyat.kineticcore.internal.client.selector.ItemSelectorScreen.Selection.item(stack),0);
        require(KineticGui.currentPage()instanceof ItemCreatorEditorPage,"Item selector did not return to the editor");
        require(dev.xyat.kineticcore.api.inventory.KineticItemText.parse(draft.equipment().get(0).item()).getDamageValue()==7,"Equipment selector lost item data");
        equipmentAction(1);var nbtScreen=Minecraft.getInstance().screen;var widget=field(nbtScreen,"nbtEditor");var set=widget.getClass().getDeclaredMethod("setValue",String.class);set.setAccessible(true);
        //? if >=1.20.5 {
        /*set.invoke(widget,"[minecraft:damage=13]");
        *///?} else {
        set.invoke(widget,"{Damage:13}");
        //?}
        click(616,14);click(24,14);require(KineticGui.currentPage()instanceof ItemCreatorEditorPage,"Equipment data editor did not return");
        require(dev.xyat.kineticcore.api.inventory.KineticItemText.parse(draft.equipment().get(0).item()).getDamageValue()==13,"Equipment data edit did not save");
    }
    private static void equipmentAction(int index)throws Exception{
        click(193,105,1);var host=(KineticScreenHost)Minecraft.getInstance().screen;require(host.overlayBlocksInput(),"Equipment context menu did not open");
        var menu=field(host.kineticRuntime().overlays(),"contextMenu");var controls=(List<?>)field(menu,"controls");var item=field(controls.get(index),"item");var action=(Runnable)field(item,"action");host.closeContextMenu();action.run();
    }
    private static void dismissOverlay(){var host=(KineticScreenHost)Minecraft.getInstance().screen;host.kineticRuntime().overlays().keyPressed(256);require(!host.overlayBlocksInput(),"Escape did not close the question");}
    private static int rowOf(String id)throws Exception{var rows=visible();for(int i=0;i<rows.size();i++)if(rows.get(i).value().equals(id))return i;throw new AssertionError("Missing row "+id);}
    @SuppressWarnings("unchecked")
    private static List<KineticSuggestion> visible()throws Exception{return (List<KineticSuggestion>)field(KineticGui.currentPage(),"visible");}
    private static void assertOrder()throws Exception{var draft=(ItemCreatorDraft)field(KineticGui.currentPage(),"draft");boolean unpinned=false,mod=false;for(var row:visible()){String id=row.value();boolean pinned=draft.isEffectPinned(id);if(!pinned&&!unpinned){unpinned=true;mod=false;}require(!pinned||!unpinned,"saved effect not pinned");if(!id.startsWith("minecraft:"))mod=true;else require(!mod,"vanilla effect follows modded effect in group");}}
    /** Catalog rows are 24 px from y 92; scrolls the row to the top of the list first. */
    private static void clickRow(int index)throws Exception{var scroll=(dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController)field(KineticGui.currentPage(),"scroll");scroll.setOffset(index);click(260,92+(index-scroll.offset())*24+12);}
    private static void capture(String name){
        var problems=LayoutCheck.currentScreenProblems();require(problems.isEmpty(),"GUI layout: "+problems);require(LayoutCheck.lastCheckedControls>0,"layout check saw no controls");var client=Minecraft.getInstance();captureReady=false;String file="itemcreator-"+variant+"-"+name+".png";
        //? if >=26.1 {
        /*net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.getMainRenderTarget(),1,message->{captures++;captureReady=true;LOG.info("ITEM_CREATOR_CAPTURE {}",file);});
        *///?} else {
        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.getMainRenderTarget(),message->{captures++;captureReady=true;LOG.info("ITEM_CREATOR_CAPTURE {}",file);});
        //?}
    }
    /**
     * Cycles the health row's operation with real clicks: Set, Addition, then Final Multiplier while the value is 0.
     * That switch crashed the game (a factor of 0 was stored as -1); it must give the neutral factor 1 and say so.
     * Typing 0 as a factor is refused with its own message. Ends on Set 96.
     */
    private static void checkOperationSwitch()throws Exception{
        click(511,104);require(draft.attribute(healthId).operation()==AttributeSpec.Operation.ADDITION,"operation did not cycle to Addition");
        numberField(563).setTextValue("0");require(draft.attribute(healthId).amount()==0,"addition value not edited");
        require(toastKey()==null,"0 is a valid addition but was refused");
        click(511,104);var spec=draft.attribute(healthId);
        require(spec.operation()==AttributeSpec.Operation.MULTIPLY_TOTAL&&spec.amount()==0&&numberField(563).getDoubleValue()==1,"multiplier from 0 is not the factor 1: "+spec+" shown="+numberField(563).textValue());
        require("gui.kineticcore.item_creator.multiplier_reset".equals(toastKey()),"switching 0 to a multiplier did not tell the player: "+toastKey());
        dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.clearToasts();
        numberField(563).setTextValue("0");
        require("gui.kineticcore.item_creator.multiplier_zero".equals(toastKey()),"typing a factor of 0 was not refused: "+toastKey());
        require(draft.attribute(healthId).amount()==0,"a typed factor of 0 changed the attribute");
        numberField(563).setTextValue("2");require(draft.attribute(healthId).amount()==1,"factor 2 was not stored as +100%");
        click(511,104);numberField(563).setTextValue("96");spec=draft.attribute(healthId);
        require(spec.operation()==AttributeSpec.Operation.SET&&spec.amount()==96,"operation did not return to Set 96: "+spec);
    }
    /** Translation key of the item creator's multiplier toast while it shows, else null. */
    private static String toastKey()throws Exception{
        var list=dev.xyat.kineticcore.internal.client.overlay.GuiOverlayRuntime.class.getDeclaredField("ACTIVE_TOASTS");
        list.setAccessible(true);
        for(Object toast:(List<?>)list.get(null)){
            if(!"kineticcore:item_creator_multiplier".equals(field(toast,"id")))continue;
            var message=(net.minecraft.network.chat.Component)field(toast,"message");
            return message.getContents()instanceof net.minecraft.network.chat.contents.TranslatableContents contents?contents.getKey():null;
        }
        return null;
    }
    /** Types one character through the screen, as the keyboard does. */
    private static void type(char c){
        var screen=Minecraft.getInstance().screen;
        //? if >=26.1 {
        /*screen.charTyped(new net.minecraft.client.input.CharacterEvent(c));
        *///?} else {
        screen.charTyped(c,0);
        //?}
    }
    private static void click(double x,double y){click(x,y,0);}
    private static void click(double x,double y,int button){var screen=Minecraft.getInstance().screen;var host=(KineticScreenHost)screen;double sx=host.kineticRuntime().canvas().toScreenX(x),sy=host.kineticRuntime().canvas().toScreenY(y);
        //? if >=26.1 {
        /*var input=new net.minecraft.client.input.MouseButtonEvent(sx,sy,new net.minecraft.client.input.MouseButtonInfo(button,0));screen.mouseClicked(input,false);screen.mouseReleased(input);
        *///?} else {
        screen.mouseClicked(sx,sy,button);screen.mouseReleased(sx,sy,button);
        //?}
    }
    private static KineticTextField textField(int x){return Minecraft.getInstance().screen.children().stream().filter(c->c instanceof KineticTextField f&&f.controlX()==x).map(c->(KineticTextField)c).findFirst().orElseThrow();}
    private static KineticNumberField numberField(int x){return (KineticNumberField)textField(x);}
    private static KineticButton button(int x,int y){return Minecraft.getInstance().screen.children().stream().filter(c->c instanceof KineticButton b&&b.controlX()==x&&b.controlY()==y).map(c->(KineticButton)c).findFirst().orElseThrow();}
    private static Object field(Object object,String name)throws Exception{Class<?> type=object.getClass();while(type!=null){try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}throw new AssertionError("Missing state "+name);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
