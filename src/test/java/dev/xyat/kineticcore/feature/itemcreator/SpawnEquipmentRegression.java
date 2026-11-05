package dev.xyat.kineticcore.feature.itemcreator;

import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.network.NetworkBuffers;
import dev.xyat.kineticcore.internal.client.editor.itemcreator.ItemCreatorDraft;
import dev.xyat.kineticcore.internal.client.editor.itemcreator.ItemCreatorPresets;
import dev.xyat.kineticcore.internal.itemcreator.ItemCreatorProtocol;
import java.util.*;
import java.lang.reflect.*;

/** Equipment data survives the complete editor/template/wire/preset path without a game host. */
public final class SpawnEquipmentRegression {
    @SuppressWarnings({"unchecked","rawtypes"})
    public static void main(String[] args) throws Exception {
        var specType=Class.forName("dev.xyat.kineticcore.api.itemcreator.EquipmentSpec");
        var slotType=(Class<? extends Enum>)Class.forName("dev.xyat.kineticcore.api.itemcreator.EquipmentSpec$Slot");
        var head=Enum.valueOf(slotType,"HEAD");var hand=Enum.valueOf(slotType,"MAINHAND");
        var constructor=specType.getConstructor(slotType,String.class);
        String sword="minecraft:diamond_sword{Enchantments:[{id:'minecraft:sharpness',lvl:3s}],RepairCost:2}";
        Object weapon=constructor.newInstance(hand,sword);
        Object helmet=constructor.newInstance(head,"minecraft:diamond_helmet");
        var source=new ArrayList<>(List.of(weapon,helmet));
        var template=SpawnEggTemplate.class.getConstructor(String.class,List.class,List.class,String.class,List.class)
                .newInstance("minecraft:zombie",List.of(),List.of(),"Equipped mob",source);
        source.clear();var equipment=SpawnEggTemplate.class.getMethod("equipment");
        require(((List<?>)equipment.invoke(template)).size()==2,"equipment aliases its source");
        require(template.equals(ItemCreatorStackData.decode(ItemCreatorStackData.encode(template))),"template equipment round trip lost native item data");
        var legacy=ItemCreatorStackData.encode(new SpawnEggTemplate("minecraft:zombie",List.of(),List.of()));legacy.remove("equipment");
        require(((List<?>)equipment.invoke(ItemCreatorStackData.decode(legacy))).isEmpty(),"legacy templates did not default to empty equipment");
        var request=new ItemCreatorProtocol.Request(99,template,null,List.of());
        ItemCreatorProtocol.validateRequestSize(request);
        var wire=NetworkBuffers.encode(buffer->ItemCreatorProtocol.writeRequest(buffer,request));
        require(request.equals(NetworkBuffers.decode(wire,ItemCreatorProtocol::readRequest)),"equipment request lost its slot or NBT");
        reject(()->SpawnEggTemplate.class.getConstructor(String.class,List.class,List.class,String.class,List.class)
                .newInstance("minecraft:zombie",List.of(),List.of(),"",List.of(weapon,weapon)),"duplicate equipment slots");
        reject(()->constructor.newInstance(head,"bad item"),"invalid equipment item ID");
        reject(()->constructor.newInstance(head,"minecraft:diamond{"+"x".repeat(32768)+"}"),"unbounded equipment data");
        var large=new SpawnEggTemplate("minecraft:zombie",List.of(),List.of(),"",List.of(
                new EquipmentSpec(EquipmentSpec.Slot.MAINHAND,"minecraft:stick{data:'"+"x".repeat(17000)+"'}"),
                new EquipmentSpec(EquipmentSpec.Slot.HEAD,"minecraft:stone{data:'"+"x".repeat(17000)+"'}")));
        reject(()->ItemCreatorProtocol.validateRequestSize(new ItemCreatorProtocol.Request(1,large,null,List.of())),"aggregate request above vanilla transport limit");
        var unicode=new SpawnEggTemplate("minecraft:zombie",List.of(),List.of(),"",List.of(
                new EquipmentSpec(EquipmentSpec.Slot.MAINHAND,"minecraft:stick{data:'"+"\u4e2d".repeat(11000)+"'}")));
        reject(()->ItemCreatorProtocol.validateRequestSize(new ItemCreatorProtocol.Request(1,unicode,null,List.of())),"UTF-8 bytes above vanilla transport limit");
        var draft=new ItemCreatorDraft();var edit=ItemCreatorDraft.class.getMethod("setEquipment",slotType,String.class);
        edit.invoke(draft,hand,sword);require(!draft.isBlank(),"equipment-only draft is blank");
        var copy=draft.copy();edit.invoke(copy,hand,"");
        require(((List<?>)equipment.invoke(draft.template())).size()==1,"draft equipment copies alias");
        require(((List<?>)equipment.invoke(copy.template())).isEmpty(),"clearing equipment did not remove its override");
        var file=java.nio.file.Files.createTempDirectory("spawn-equipment-").resolve("presets.json");
        var store=new ItemCreatorPresets(file);String id=store.save(false,null,draft);
        var restored=new ItemCreatorPresets(file).entry(id);
        require(draft.snapshot().equals(restored.draft()),"saved equipment did not reopen");
        require(((List<?>)equipment.invoke(ItemCreatorDraft.from(restored.draft()).template())).size()==1,"reopened equipment was omitted");
        java.nio.file.Files.delete(file);java.nio.file.Files.delete(file.getParent());
        System.out.println("PASS: spawn equipment slots, native item data, legacy templates, wire and preset persistence");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void reject(Check check,String message) throws Exception {
        try{check.run();}catch(IllegalArgumentException error){return;}catch(InvocationTargetException error){if(error.getCause() instanceof IllegalArgumentException)return;throw error;}
        throw new AssertionError("Accepted "+message);
    }
    private interface Check {void run() throws Exception;}
}
