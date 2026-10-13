package dev.xyat.kineticcoreitemcreatorvalidation;

import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticToggleActionList;
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
        }else if(step==2){capture("egg-root");next(3);}
        else if(step==3&&captureReady){click(80,108);require((boolean)field(client.screen,"singleSelection"),"entity selector is not single");click(76,101);click(144,101);require(((Set<?>)field(client.screen,"selectedIds")).size()==1,"entity selector selected more than one");click(428,333);require(KineticGui.currentPage()instanceof ItemCreatorEditorPage,"entity apply did not return");draft.entityId="minecraft:zombie";KineticGui.currentPage().rebuild();click(330,60);require(visible().size()==KineticRegistries.mobEffects().ids().size(),"inline effects omitted registry entries");assertOrder();next(4);}
        else if(step==4){capture("effect-catalog");next(5);}
        else if(step==5&&captureReady){click(435,60);textField(180).setTextValue("minecraft:speed");require(visible().size()==1,"inline search failed");next(6);}
        else if(step==6){capture("effect-search");next(7);}
        else if(step==7&&captureReady){var list=list();require((boolean)field(list,"textRows"),"effects are still full-width buttons");int y=list.controlY()+5,x=toggleX(list,y);click(x,y);require(draft.effects().size()==1,"toggle did not deselect");click(x,y);require(draft.effects().size()==2,"multiselect lost existing effect");click(actionX(list(),y),y);require(KineticGui.currentPage()instanceof EffectEditPage,"small Edit button did not open effect settings");click(250,234);require(numberField(100).textValue().equals("1")&&numberField(260).textValue().equals("30.0"),"effect Reset failed");numberField(100).setTextValue("4");numberField(260).setTextValue("60");next(8);}
        else if(step==8){capture("effect-edit");next(9);}
        else if(step==9&&captureReady){click(42,20);require(draft.effect("minecraft:speed").amplifier()==2&&draft.effect("minecraft:speed").durationTicks()==6000,"cancel changed effect");click(actionX(list(),list().controlY()+5),list().controlY()+5);numberField(100).setTextValue("4");numberField(260).setTextValue("60");click(250,262);require(draft.effect("minecraft:speed").amplifier()==3&&draft.effect("minecraft:speed").durationTicks()==1200,"saved effect conversion wrong");int y=list().controlY()+5;click(toggleX(list(),y),y);require(draft.effects().size()==1,"uncheck did not remove");click(toggleX(list(),y),y);require(draft.effect("minecraft:speed").amplifier()==3&&draft.effects().size()==2,"reselect lost parameters");testSavedPin();click(230,60);next(10);}
        else if(step==10){capture("attribute-catalog");next(11);}
        else if(step==11&&captureReady){textField(180).setTextValue(healthId);require(visible().size()==1,"attribute ID search failed");require((boolean)field(list(),"textRows"),"attributes are still full-width buttons");click(actionX(list(),list().controlY()+5),list().controlY()+5);require(KineticGui.currentPage()instanceof AttributeEditPage,"attribute Edit button did not open settings");click(250,220);require(numberField(100).getDoubleValue()==0,"attribute Reset failed");numberField(100).setTextValue("-");KineticGui.currentPage().rebuild();require(numberField(100).textValue().equals("-"),"incomplete numeric text lost on rebuild");click(250,250);require(KineticGui.currentPage()instanceof AttributeEditPage,"invalid input applied");numberField(100).setTextValue("96");next(12);}
        else if(step==12){capture("attribute-edit");next(13);}
        else if(step==13&&captureReady){click(42,20);require(draft.attribute(healthId).amount()==80,"cancel changed attribute");draft.name="Named potion / 命名药水";KineticGui.open(new ItemCreatorEditorPage(true,draft));require(textField(180)!=null&&list()!=null,"potion catalog needs a separate entry");next(14);}
        else if(step==14){capture("potion-root");next(15);}
        else if(step==15&&captureReady){click(90,108);require(((KineticScreenHost)client.screen).overlayBlocksInput(),"form button did not expand choices");next(16);}
        else if(step==16){capture("potion-form-menu");next(17);}
        else if(step==17&&captureReady){((KineticScreenHost)client.screen).closeContextMenu();click(90,288);require((boolean)field(KineticGui.currentPage(),"pending"),"generation did not wait for receipt");require(!button(22,280).isEnabled(),"pending generate button still enabled");next(18);}
        else if(step==18&&!(boolean)field(KineticGui.currentPage(),"pending")){require(((net.minecraft.network.chat.contents.TranslatableContents)((net.minecraft.network.chat.Component)field(KineticGui.currentPage(),"notice")).getContents()).getKey().equals("gui.kineticcore.item_creator.generated"),"success not based on receipt");draft.selectEffect("minecraft:speed",false);draft.selectEffect("minecraft:fire_resistance",false);KineticGui.currentPage().rebuild();click(90,288);next(19);}
        else if(step==19&&!(boolean)field(KineticGui.currentPage(),"pending")){require(draft.effects().isEmpty(),"failed generation modified draft");require(((net.minecraft.network.chat.contents.TranslatableContents)((net.minecraft.network.chat.Component)field(KineticGui.currentPage(),"notice")).getContents()).getKey().equals("gui.kineticcore.item_creator.invalid"),"failure receipt not displayed");testPresets();next(20);}
        else if(step==20){LOG.info("ITEM_CREATOR_CASE_PASS case=gui-input-language-1080p variant={} captures={}",variant,captures);KineticGui.closeScreen();if(++variant<2)next(0);else{LOG.info("ITEM_CREATOR_GUI_PASS captures={}",captures);done.run();}}
        require(wait<900,"GUI phase timed out: "+step);
    }
    private static void testPresets()throws Exception{
        var path=KineticPlatform.gameDirectory().resolve("itemcreator-ui-presets-"+variant+".json");var store=new ItemCreatorPresets(path);
        draft.selectEffect("minecraft:speed",true);store.retain(true,draft);String first=store.activeId(true);
        KineticGui.open(new ItemCreatorEditorPage(true,store));click(90,262);textField(22).setTextValue("Second potion");require(((ItemCreatorDraft)field(KineticGui.currentPage(),"draft")).effects().isEmpty(),"new preset merged old effects");
        store.activate(true,first);var reopened=new ItemCreatorPresets(path);require(reopened.active(true).snapshot().equals(draft.snapshot()),"saved named preset failed switch/reopen");require(reopened.entries(true).size()>=2,"new preset replaced original");
        KineticGui.open(new ItemCreatorEditorPage(true,reopened));click(448,20);require(((KineticScreenHost)Minecraft.getInstance().screen).overlayBlocksInput(),"preset choices did not open");((KineticScreenHost)Minecraft.getInstance().screen).closeContextMenu();
    }
    private static List<?> visible()throws Exception{return (List<?>)field(field(KineticGui.currentPage(),(boolean)field(KineticGui.currentPage(),"showEffects")?"effects":"attributes"),"visible");}
    private static void testSavedPin()throws Exception{
        String id="kineticcore_itemcreator_validation:fixture_buff";textField(180).setTextValue(id);int y=list().controlY()+5;click(toggleX(list(),y),y);require(draft.isEffectSelected(id)&&!draft.isEffectPinned(id),"selection pinned before Save");
        click(actionX(list(),y),y);click(250,262);require(draft.isEffectPinned(id),"saved effect not pinned");textField(180).setTextValue("");assertOrder();draft.selectEffect(id,false);KineticGui.currentPage().rebuild();
    }
    private static void assertOrder()throws Exception{boolean unpinned=false,mod=false;for(var row:visible()){String id=((KineticSuggestion)row).value();boolean pinned=draft.isEffectPinned(id);if(!pinned&&!unpinned){unpinned=true;mod=false;}require(!pinned||!unpinned,"saved effect not pinned");if(!id.startsWith("minecraft:"))mod=true;else require(!mod,"vanilla effect follows modded effect in group");}}
    private static void capture(String name){
        var problems=LayoutCheck.currentScreenProblems();require(problems.isEmpty(),"GUI layout: "+problems);require(LayoutCheck.lastCheckedControls>0,"layout check saw no controls");var client=Minecraft.getInstance();captureReady=false;String file="itemcreator-"+variant+"-"+name+".png";
        //? if >=26.1 {
        /*net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.getMainRenderTarget(),1,message->{captures++;captureReady=true;LOG.info("ITEM_CREATOR_CAPTURE {}",file);});
        *///?} else {
        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.getMainRenderTarget(),message->{captures++;captureReady=true;LOG.info("ITEM_CREATOR_CAPTURE {}",file);});
        //?}
    }
    private static void click(double x,double y){var screen=Minecraft.getInstance().screen;var host=(KineticScreenHost)screen;double sx=host.kineticRuntime().canvas().toScreenX(x),sy=host.kineticRuntime().canvas().toScreenY(y);
        //? if >=26.1 {
        /*var input=new net.minecraft.client.input.MouseButtonEvent(sx,sy,new net.minecraft.client.input.MouseButtonInfo(0,0));screen.mouseClicked(input,false);screen.mouseReleased(input);
        *///?} else {
        screen.mouseClicked(sx,sy,0);screen.mouseReleased(sx,sy,0);
        //?}
    }
    private static KineticTextField textField(int x){return Minecraft.getInstance().screen.children().stream().filter(c->c instanceof KineticTextField f&&f.controlX()==x).map(c->(KineticTextField)c).findFirst().orElseThrow();}
    private static KineticNumberField numberField(int x){return (KineticNumberField)textField(x);}
    private static KineticButton button(int x,int y){return Minecraft.getInstance().screen.children().stream().filter(c->c instanceof KineticButton b&&b.controlX()==x&&b.controlY()==y).map(c->(KineticButton)c).findFirst().orElseThrow();}
    private static KineticToggleActionList list(){return Minecraft.getInstance().screen.children().stream().filter(c->c instanceof KineticToggleActionList).map(c->(KineticToggleActionList)c).findFirst().orElseThrow();}
    private static int toggleX(KineticToggleActionList list,int y){for(int x=list.controlX();x<list.controlX()+list.controlWidth();x++)if(list.toggleAt(x,y)==0)return x+2;throw new AssertionError("No toggle");}
    private static int actionX(KineticToggleActionList list,int y){for(int x=list.controlX();x<list.controlX()+list.controlWidth();x++)if(list.actionAt(x,y)==0)return x+2;throw new AssertionError("No remove action");}
    private static Object field(Object object,String name)throws Exception{Class<?> type=object.getClass();while(type!=null){try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}throw new AssertionError("Missing state "+name);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
