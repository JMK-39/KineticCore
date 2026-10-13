package dev.xyat.kineticcoreitemcreatorvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.api.spawnegg.KineticSpawnEggs;
import dev.xyat.kineticcore.api.world.event.KineticWorldEvents;
import dev.xyat.kineticcore.feature.networklimit.config.NetworkConfig;
import dev.xyat.kineticcore.feature.spawnegg.config.SpawnEggConfig;
import dev.xyat.kineticcore.feature.spawnegg.entity.ThrowSpawnEgg;
import dev.xyat.kineticcore.internal.itemcreator.ItemCreatorProtocol;
import dev.xyat.kineticcore.internal.itemcreator.ItemCreatorService;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Actual client/server fixture. All waits are for physical flight/rendering, not template application. */
public final class ItemCreatorPackRuntime {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(ItemCreatorPackRuntime.class);
    private static final BlockPos DISPENSER=new BlockPos(1000,120,1000);
    private static final AABB AREA=new AABB(995,116,995,1015,132,1010);
    private static final Set<UUID> JOINED=ConcurrentHashMap.newKeySet();
    private static volatile Throwable failure;
    private static volatile boolean serverDone;
    private static volatile LivingEntity spawned;
    private static volatile boolean ownerless;
    private static volatile int callbackCount;
    private static volatile KineticItemCreator.Result result;
    private static int phase,ticks,cases,potionIndex;
    private static String healthId,armorId,attackId;
    private static SpawnEggTemplate template;
    private static boolean originalThrow,reload,guiOnly;
    private static double originalInaccuracy;
    private static com.mojang.authlib.GameProfile testOwner;
    public static void install(){
        reload="reload".equals(System.getProperty("kineticcore.itemCreatorValidation.case"));
        guiOnly="gui".equals(System.getProperty("kineticcore.itemCreatorValidation.case"));
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END,ItemCreatorPackRuntime::tick);
        LOG.info("ITEM_CREATOR_VALIDATION_INSTALL case={} game={}",reload?"reload":"all",KineticPlatform.gameDirectory());
        try{Files.writeString(evidencePath(),"ITEM_CREATOR_VALIDATION_INSTALL case="+testCase()+" time="+java.time.Instant.now()+"\n");}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    private static void tick(){
        var client=Minecraft.getInstance();if(phase==99)return;
        try{
            if(failure!=null)throw new AssertionError("Server validation failed",failure);
            if(client.player==null||client.getSingleplayerServer()==null){
                if(++ticks%200==0)LOG.info("ITEM_CREATOR_WAIT screen={}",client.screen==null?"none":client.screen.getClass().getSimpleName());
                if(ticks>2400)throw new AssertionError("Test world did not open");return;
            }
            ticks++;
            if(phase==0){dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfig.errorsOnly=false;selectZero(client.player);phase=1;ticks=0;runServer(ItemCreatorPackRuntime::setup);}
            else if(phase==1&&serverDone){
                if(reload){phase=90;ticks=0;runServer(ItemCreatorPackRuntime::checkReload);return;}
                if(guiOnly){KineticGui.closeScreen();phase=20;ticks=0;ItemCreatorGuiValidation.start(()->{phase=21;ticks=0;});return;}
                KineticGui.closeScreen();phase=2;ticks=0;KineticItemCreator.requestEgg(template,r->result=r);
            }else if(phase==2&&result!=null){
                require(result.success(),"egg generation denied: "+result.translationKey());pass("network-give-egg");result=null;phase=3;ticks=0;
                runServer(()->{var stack=player().getInventory().getItem(0);require(ItemCreatorStackData.readTemplate(stack).orElseThrow().equals(template),"server changed template");stack.setCount(2);player().containerMenu.broadcastChanges();});
            }else if(phase==3&&serverDone&&ticks>12){
                require(ItemCreatorStackData.hasTemplate(client.player.getMainHandItem()),"egg did not sync");client.player.setYRot(-90);client.player.setXRot(0);
                spawned=null;ownerless=false;phase=4;ticks=0;client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND);
            }else if(phase==4&&spawned!=null&&ticks>10){
                require(!ownerless,"player owner lost");pass("player-throw-sync-completion");phase=5;ticks=0;
                runServer(()->{
                    require(player().getMainHandItem().getCount()==1,"throw did not consume one egg");var level=level();
                    level.setBlockAndUpdate(DISPENSER,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));
                    var block=(DispenserBlockEntity)level.getBlockEntity(DISPENSER);var eggs=KineticItemCreator.buildEgg(level,template);eggs.setCount(2);block.setItem(0,eggs);
                    spawned=null;int count=callbackCount;level.setBlockAndUpdate(DISPENSER.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());require(callbackCount==count,"dispenser bypassed physical flight");
                });
            }else if(phase==5&&serverDone&&spawned!=null&&ticks>10){
                require(ownerless,"dispenser fabricated owner");phase=6;ticks=0;
                runServer(()->{
                    var level=level();var block=(DispenserBlockEntity)level.getBlockEntity(DISPENSER);require(block.getItem(0).getCount()==1,"redstone did not consume one egg");
                    level.setBlockAndUpdate(DISPENSER.below(),Blocks.STONE.defaultBlockState());spawned.getPersistentData().putBoolean("itemcreator_saved",true);
                    Files.writeString(KineticPlatform.gameDirectory().resolve("itemcreator-saved-entity.txt"),spawned.getUUID().toString());pass("redstone-dispenser-template");ordinaryDispenser();
                });
            }else if(phase==6&&serverDone){phase=7;ticks=0;runServer(ItemCreatorPackRuntime::permissionsAndConfig);}
            else if(phase==7&&serverDone){phase=8;ticks=0;runServer(ItemCreatorPackRuntime::runtimeAttributeRoundTrip);}
            else if(phase==8&&serverDone){phase=81;ticks=0;KineticItemCreator.requestEgg(new SpawnEggTemplate("missing:entity",List.of(),List.of()),r->result=r);}
            else if(phase==81&&result!=null){require(!result.success(),"unknown entity request succeeded");result=null;pass("network-unknown-id-rejected");phase=82;ticks=0;runServer(()->{
                var server=client.getSingleplayerServer();
                testOwner=server.getSingleplayerProfile();server.setSingleplayerProfile(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"fixture_owner"));
                //? if >=1.21 {
                /*server.getPlayerList().setAllowCommandsForAllPlayers(false);
                *///?} else {
                server.getPlayerList().setAllowCheatsForAllPlayers(false);
                //?}
                //? if >=26.1 {
                /*server.getPlayerList().deop(player().nameAndId());
                *///?} else {
                server.getPlayerList().deop(player().getGameProfile());
                //?}
            });}
            else if(phase==82&&serverDone&&ticks>12){phase=83;ticks=0;KineticItemCreator.requestEgg(template,r->result=r);}
            else if(phase==83&&result!=null){require(!result.success(),"real unprivileged request succeeded");result=null;pass("network-unprivileged-rejected");phase=84;ticks=0;runServer(()->{
                var server=client.getSingleplayerServer();
                server.setSingleplayerProfile(testOwner);
                //? if >=26.1 {
                /*server.getPlayerList().op(player().nameAndId());
                *///?} else {
                server.getPlayerList().op(player().getGameProfile());
                //?}
                for(int slot=0;slot<36;slot++)player().getInventory().setItem(slot,new ItemStack(Items.STONE,64));player().containerMenu.broadcastChanges();
            });}
            else if(phase==84&&serverDone&&ticks>12){phase=85;ticks=0;KineticItemCreator.requestEgg(template,r->result=r);}
            else if(phase==85&&result!=null){require(result.success(),"full inventory delivery denied");result=null;phase=86;ticks=0;runServer(()->{require(level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,AREA).stream().anyMatch(e->ItemCreatorStackData.hasTemplate(e.getItem())),"full inventory item not dropped");pass("network-full-inventory-drop");});}
            else if(phase==86&&serverDone){potionIndex=0;phase=10;ticks=0;preparePotion();}
            else if(phase==10&&result!=null){require(result.success(),"potion generation denied");result=null;phase=11;ticks=0;}
            else if(phase==11&&ticks>12){client.player.setXRot(potionIndex==0?0:90);client.player.setYRot(0);client.options.keyUse.setDown(true);client.gameMode.useItem(client.player,InteractionHand.MAIN_HAND);phase=12;ticks=0;}
            else if(phase==12&&ticks>70){client.options.keyUse.setDown(false);phase=13;ticks=0;runServer(()->{var effect=player().getEffect(speed());require(effect!=null&&effect.getAmplifier()==1&&effect.getDuration()>0,"real potion use missing custom effect: "+potionIndex);var fire=player().getEffect(effect("minecraft:fire_resistance"));require(fire!=null&&fire.getDuration()==-1,"permanent potion effect lost: "+potionIndex);require(player().getHealth()>5,"instant potion did not heal: "+potionIndex);var modded=player().getEffect(effect("kineticcore_itemcreator_validation:fixture_buff"));require(modded!=null&&modded.getAmplifier()==2,"modded potion effect missing: "+potionIndex);pass("multi-effect-instant-permanent-potion-use-"+KineticItemCreator.PotionKind.values()[potionIndex]);});}
            else if(phase==13&&serverDone){if(++potionIndex<3){phase=10;ticks=0;preparePotion();}else{phase=20;ticks=0;ItemCreatorGuiValidation.start(()->{phase=21;ticks=0;});}}
            else if(phase==20){ItemCreatorGuiValidation.tick();}
            else if(phase==21){phase=22;runServer(()->{SpawnEggConfig.enableSpawnEggThrow=originalThrow;SpawnEggConfig.spawnEggThrowInaccuracy=originalInaccuracy;var server=client.getSingleplayerServer();server.getPlayerList().saveAll();server.saveEverything(false,true,true);pass("world-save-for-reload");});}
            else if((phase==22||phase==90)&&serverDone){phase=99;String proof="ITEM_CREATOR_VALIDATION_PASS case="+testCase()+" cases="+cases+" callbacks="+callbackCount+" mods="+KineticPlatform.loadedMods().size();evidence(proof);LOG.info(proof);client.stop();}
            if(phase!=20&&ticks>600)throw new AssertionError("Phase timed out: "+phase);
        }catch(Throwable error){int failed=phase;phase=99;client.options.keyUse.setDown(false);evidence("ITEM_CREATOR_VALIDATION_FAIL phase="+failed+" cases="+cases+" error="+error);LOG.error("ITEM_CREATOR_VALIDATION_FAIL phase={} cases={}",failed,cases,error);client.stop();}
    }
    private static void setup()throws Exception{
        var player=player();var server=Minecraft.getInstance().getSingleplayerServer();var level=level();
        //? if >=26.1 {
        /*server.getPlayerList().op(player.nameAndId());
        *///?} else {
        server.getPlayerList().op(player.getGameProfile());
        //?}

        server.getCommands().getDispatcher().execute("tp @s 1000 120 1002 -90 0",player.createCommandSourceStack().withPermission(4));
        if(level.getDifficulty()!=net.minecraft.world.Difficulty.NORMAL)server.getCommands().getDispatcher().execute("difficulty normal",player.createCommandSourceStack().withPermission(4));selectZero(player);
        //? if >=1.21 {
        /*healthId=KineticRegistries.attributes().id(Attributes.MAX_HEALTH.value()).toString();armorId=KineticRegistries.attributes().id(Attributes.ARMOR.value()).toString();attackId=KineticRegistries.attributes().id(Attributes.ATTACK_DAMAGE.value()).toString();
        *///?} else {
        healthId=KineticRegistries.attributes().id(Attributes.MAX_HEALTH).toString();armorId=KineticRegistries.attributes().id(Attributes.ARMOR).toString();attackId=KineticRegistries.attributes().id(Attributes.ATTACK_DAMAGE).toString();
        //?}
        originalThrow=SpawnEggConfig.enableSpawnEggThrow;originalInaccuracy=SpawnEggConfig.spawnEggThrowInaccuracy;
        if(reload)return;
        if(guiOnly){player.setGameMode(GameType.CREATIVE);player.getInventory().clearContent();player.containerMenu.broadcastChanges();return;}
        player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
        for(int x=996;x<=1012;x++)for(int z=996;z<=1007;z++){level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());for(int y=120;y<=127;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),x==1008?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());}
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,AREA))if(entity!=player)entity.discard();
        originalThrow=SpawnEggConfig.enableSpawnEggThrow;originalInaccuracy=SpawnEggConfig.spawnEggThrowInaccuracy;SpawnEggConfig.enableSpawnEggThrow=true;SpawnEggConfig.spawnEggThrowInaccuracy=0;
        player.getPersistentData().putBoolean("DisableEggThrow",false);
        template=new SpawnEggTemplate("minecraft:zombie",List.of(new AttributeSpec(healthId,80,AttributeSpec.Operation.SET),new AttributeSpec(armorId,6,AttributeSpec.Operation.ADDITION),new AttributeSpec(attackId,1.5,AttributeSpec.Operation.MULTIPLY_TOTAL)),List.of(new EffectSpec("minecraft:speed",2,6000,false,true),new EffectSpec("minecraft:fire_resistance",0,-1,false,true)),"Validation Zombie");
        KineticWorldEvents.onEntityJoin(KineticEventPriority.LOWEST,c->{if(c.entity()instanceof LivingEntity living&&AREA.contains(living.position())&&living.getType()==net.minecraft.world.entity.EntityType.ZOMBIE){JOINED.add(living.getUUID());attribute(living,healthId).setBaseValue(30);attribute(living,armorId).setBaseValue(7);attribute(living,attackId).setBaseValue(4);addExisting(attribute(living,armorId),4);addExisting(attribute(living,attackId),2);living.addEffect(new MobEffectInstance(speed(),6000,4));living.addEffect(new MobEffectInstance(effect("minecraft:invisibility"),6000,0));if(living instanceof Mob mob)mob.setNoAi(true);}});
        KineticSpawnEggs.onSpawned(KineticEventPriority.HIGHEST,c->{if(!ItemCreatorStackData.hasTemplate(c.stack())||c.entity().getType()!=net.minecraft.world.entity.EntityType.ZOMBIE)return;require(JOINED.contains(c.entity().getUUID()),"completion before entity join");c.entity().getPersistentData().putLong("itemcreator_join_tick",c.entity().level().getGameTime());var copy=c.stack();copy.setCount(99);require(c.stack().getCount()==1,"source stack aliases callbacks");LOG.info("ITEM_CREATOR_SPAWN_BEFORE base={}",attribute((LivingEntity)c.entity(),healthId).getBaseValue());});
        KineticSpawnEggs.onSpawned(KineticEventPriority.LOWEST,c->{
            if(!ItemCreatorStackData.hasTemplate(c.stack())||c.entity().getType()!=net.minecraft.world.entity.EntityType.ZOMBIE)return;var living=(LivingEntity)c.entity();require(attribute(living,healthId).getBaseValue()==80,"late SET overwritten");require(living.getName().getString().equals("Validation Zombie"),"custom entity name lost");
            require(attribute(living,armorId).getValue()==17&&attribute(living,attackId).getValue()==15,"addition/multiplier lost existing modifier");require(living.hasEffect(effect("minecraft:invisibility")),"unselected effect removed");
            var effect=living.getEffect(speed());require(effect!=null&&effect.getAmplifier()==2&&effect.getDuration()==6000&&!effect.isVisible()&&effect.showIcon(),"effect parameters incorrect");
            require(living.getPersistentData().getLong("itemcreator_join_tick")==living.level().getGameTime(),"template crossed a tick");require(living.getHealth()==living.getMaxHealth(),"new health not initialized");spawned=living;ownerless=c.player()==null;callbackCount++;
        });
    }
    private static void ordinaryDispenser(){
        var level=level();var block=(DispenserBlockEntity)level.getBlockEntity(DISPENSER);block.setItem(0,new ItemStack(Items.ZOMBIE_SPAWN_EGG,2));var before=level.getEntitiesOfClass(ThrowSpawnEgg.class,AREA).size();
        var state=Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST);state.tick(level,DISPENSER,level.getRandom());
        require(block.getItem(0).getCount()==1,"ordinary dispenser consumption changed");require(level.getEntitiesOfClass(ThrowSpawnEgg.class,AREA).size()==before,"ordinary egg became projectile");pass("ordinary-dispenser-preserved");
        SpawnEggConfig.enableSpawnEggThrow=false;block.setItem(0,KineticItemCreator.buildEgg(level,template));state.tick(level,DISPENSER,level.getRandom());require(level.getEntitiesOfClass(ThrowSpawnEgg.class,AREA).size()==before,"disabled throw intercepted dispenser");SpawnEggConfig.enableSpawnEggThrow=true;pass("disabled-dispenser-fallback");
        block.setItem(0,KineticItemCreator.buildEgg(level,template));
        try(var cancel=KineticWorldEvents.onEntityJoin(KineticEventPriority.HIGHEST,c->{if(c.entity()instanceof ThrowSpawnEgg)c.cancel();})){
            state.tick(level,DISPENSER,level.getRandom());require(block.getItem(0).getCount()==1,"cancelled launch consumed egg");require(!dev.xyat.kineticcore.feature.spawnegg.SpawnEggDispenseBehavior.INSTANCE.isSuccess(),"failed launch reported success");
        }pass("cancelled-dispenser-launch-no-consumption");
    }
    private static void permissionsAndConfig(){
        var client=Minecraft.getInstance();var server=client.getSingleplayerServer();var player=player();var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"itemcreator_guest");
        //? if >=1.21 {
        /*var guest=new ServerPlayer(server,level(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        *///?} else {
        var guest=new ServerPlayer(server,level(),profile);
        //?}
        require(!ItemCreatorService.give(guest,new ItemCreatorProtocol.Request(999,template,null,List.of())).success(),"guest bypassed server permissions");pass("server-permission-check");
        long old=NetworkConfig.nbtMaxSize;NetworkConfig.nbtMaxSize=32L*1024*1024;NetworkConfig.save();NetworkConfig.load();require(KineticNetwork.transportLimits().nbtBytes()==32L*1024*1024,"NBT >16 MiB rejected");NetworkConfig.nbtMaxSize=old;NetworkConfig.save();NetworkConfig.load();pass("nbt-config-above-default");player.setGameMode(GameType.CREATIVE);
        try{KineticItemCreator.buildPotion(KineticItemCreator.PotionKind.NORMAL,List.of(new EffectSpec("minecraft:instant_health",0,-1,true,true)));throw new AssertionError("permanent instant effect accepted");}catch(IllegalArgumentException expected){pass("instant-effect-validation");}
        for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
        try(var cancel=KineticWorldEvents.onEntityJoin(KineticEventPriority.HIGHEST,c->{if(c.entity()instanceof net.minecraft.world.entity.item.ItemEntity)c.cancel();})){
            require(!ItemCreatorService.give(player,new ItemCreatorProtocol.Request(998,template,null,List.of())).success(),"cancelled item delivery reported success");
        }player.getInventory().clearContent();player.containerMenu.broadcastChanges();pass("cancelled-full-inventory-delivery-failed");
    }
    private static void preparePotion(){runServer(()->{var player=player();player.getInventory().clearContent();selectZero(player);player.removeAllEffects();player.setHealth(5);player.containerMenu.broadcastChanges();Minecraft.getInstance().execute(()->KineticItemCreator.requestPotion(KineticItemCreator.PotionKind.values()[potionIndex],List.of(new EffectSpec("minecraft:speed",1,1200,false,true),new EffectSpec("minecraft:fire_resistance",0,-1,false,true),new EffectSpec("minecraft:instant_health",0,1,false,true),new EffectSpec("kineticcore_itemcreator_validation:fixture_buff",2,1200,false,true)),"Validation Potion",r->result=r));});}
    private static void runtimeAttributeRoundTrip(){
        var level=level();var type=net.minecraft.world.entity.EntityType.COW;
        var cow=type.create(level);require(cow!=null,"cow create failed");
        attribute(cow,attackId).setBaseValue(13);addExisting(attribute(cow,attackId),2);
        cow.moveTo(1005,120,1005,0,0);cow.setNoAi(true);require(level.addFreshEntity(cow),"cow did not enter world");
        var cowTemplate=new SpawnEggTemplate("minecraft:cow",List.of(new AttributeSpec(attackId,1.5,AttributeSpec.Operation.MULTIPLY_TOTAL)),List.of());
        KineticSpawnEggs.notifySpawned(cow,KineticItemCreator.buildEgg(level,cowTemplate),null);
        require(attribute(cow,attackId).getValue()==37.5,"runtime template did not preserve existing modifier");
        var restored=type.create(level);
        //? if >=26.1 {
        /*restored.getAttributes().apply(cow.getAttributes().pack());
        *///?} else {
        restored.getAttributes().load(cow.getAttributes().save());
        //?}
        require(attribute(restored,attackId).getBaseValue()==13&&attribute(restored,attackId).getValue()==37.5,"runtime-added attribute lost on persistence round trip");
        try{Files.writeString(KineticPlatform.gameDirectory().resolve("itemcreator-saved-cow.txt"),cow.getUUID().toString());}catch(java.io.IOException e){throw new RuntimeException(e);}pass("runtime-attribute-persistence-roundtrip");
        boolean[] called={false};try(var subscription=KineticSpawnEggs.onSpawned(KineticEventPriority.LOWEST,c->{if(c.entity()==restored)called[0]=true;})){KineticSpawnEggs.notifySpawned(restored,new ItemStack(Items.COW_SPAWN_EGG),null);require(!called[0],"unregistered entity published completion");}pass("cancelled-entity-completion-suppressed");
    }
    private static void checkReload()throws Exception{
        UUID id=UUID.fromString(Files.readString(KineticPlatform.gameDirectory().resolve("itemcreator-saved-entity.txt")).trim());var level=level();level.getChunkAt(new BlockPos(1007,120,1000));var entity=level.getEntity(id);require(entity instanceof LivingEntity,"saved entity missing after reopen");var living=(LivingEntity)entity;
        require(living.getPersistentData().getBoolean("itemcreator_saved"),"save marker missing");require(attribute(living,healthId).getBaseValue()==80,"SET base changed on reopen");var effect=living.getEffect(speed());require(effect!=null&&effect.getAmplifier()==2&&effect.getDuration()<6000,"buff refreshed on reopen");pass("real-world-save-reopen");
        require(attribute(living,armorId).getValue()==17&&attribute(living,attackId).getValue()==15,"permanent modifiers duplicated/lost on reopen");
        UUID cowId=UUID.fromString(Files.readString(KineticPlatform.gameDirectory().resolve("itemcreator-saved-cow.txt")).trim());var cow=(LivingEntity)level.getEntity(cowId);require(cow!=null&&attribute(cow,attackId).getBaseValue()==13&&attribute(cow,attackId).getValue()==37.5,"runtime added cow attribute lost/duplicated on reopen");pass("runtime-added-attribute-world-reopen");
    }
    private static net.minecraft.world.entity.ai.attributes.AttributeInstance attribute(LivingEntity living,String id){return dev.xyat.kineticcore.api.registry.KineticEntityAttributes.ensureInstance(living,KineticRegistries.attributes().get(KineticResourceIds.tryParse(id)));}
    private static void addExisting(net.minecraft.world.entity.ai.attributes.AttributeInstance attribute,double amount){
        //? if >=1.21 {
        /*var id=KineticResourceIds.of("itemcreator_validation","existing");attribute.removeModifier(id);attribute.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,amount,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        *///?} else {
        var id=UUID.fromString("8a581177-c191-438d-952c-ae889cfb0d3f");attribute.removeModifier(id);attribute.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,"validation-existing",amount,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        //?}
    }
    //? if >=1.21 {
    /*private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect(String id){return KineticRegistries.mobEffects().holder(KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(id)));}
    *///?} else {
    private static net.minecraft.world.effect.MobEffect effect(String id){return KineticRegistries.mobEffects().get(KineticResourceIds.tryParse(id));}
    //?}
    private static void selectZero(Player player){
        //? if >=26.1 {
        /*player.getInventory().setSelectedSlot(0);
        *///?} else {
        player.getInventory().selected=0;
        //?}
    }
    private static net.minecraft.server.level.ServerLevel level(){return (net.minecraft.server.level.ServerLevel)player().level();}
    private static ServerPlayer player(){var client=Minecraft.getInstance();return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());}
    private static void runServer(Checked action){serverDone=false;Minecraft.getInstance().getSingleplayerServer().execute(()->{try{action.run();serverDone=true;}catch(Throwable e){failure=e;}});}
    private static String testCase(){return reload?"reload":guiOnly?"gui":"all";}
    private static java.nio.file.Path evidencePath(){return KineticPlatform.gameDirectory().resolve("itemcreator-validation-"+testCase()+".log");}
    private static synchronized void evidence(String line){try{Files.writeString(evidencePath(),line+"\n",java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}}
    private static void pass(String name){cases++;evidence("ITEM_CREATOR_CASE_PASS case="+name);LOG.info("ITEM_CREATOR_CASE_PASS case={}",name);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private interface Checked{void run()throws Exception;}
    //? if >=1.21 {
    /*private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> speed(){return KineticRegistries.mobEffects().holder(KineticRegistries.mobEffects().get(KineticResourceIds.of("minecraft","speed")));}
    *///?} else {
    private static net.minecraft.world.effect.MobEffect speed(){return KineticRegistries.mobEffects().get(KineticResourceIds.of("minecraft","speed"));}
    //?}
}
