package dev.xyat.kineticcoreitemcreatorvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.feature.spawnegg.NativeSpawnEggData;
import dev.xyat.kineticcore.feature.spawnegg.entity.ThrowSpawnEgg;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Real egg use and projectile collision followed by ordinary world ticks, with no test-driven spawning. */
public final class SpawnerEggValidation {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SpawnerEggValidation.class);
    private static final List<String> evidence = new ArrayList<>();
    private static final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private static final Set<UUID> originalEntities = new HashSet<>();
    private static volatile boolean pending, finished;
    private static int phase, clientTicks;
    private static long deadline, phaseStarted;
    private static ServerLevel level;
    private static ServerPlayer player;
    private static BlockPos origin, spawnerPos;
    private static AABB area;
    private static Vec3 playerPosition;
    private static ItemStack previousHand;
    private static String name;

    public static void install() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, SpawnerEggValidation::tick);
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (finished || pending || ++clientTicks % 5 != 0 || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 180_000;
        pending = true;
        mc.getSingleplayerServer().execute(() -> {
            try {
                require(System.currentTimeMillis() < deadline, "Timed out waiting for native spawner waves, phase=" + phase);
                if (phase == 0) {
                    prepare();
                    configure(false);
                    phase = 1;
                } else {
                    var spawned = level.getEntitiesOfClass(LivingEntity.class, area).stream()
                            .filter(entity -> name.equals(entity.getName().getString())).toList();
                    for (var entity : spawned) verifyEntity(entity);
                    require(level.getGameTime() - phaseStarted < 500, "Native spawner did not produce two template mobs: " + name);
                    if (spawned.size() >= 2) {
                        pass((phase == 1 ? "direct" : "thrown") + "-native-two-waves count=" + spawned.size());
                        removeNewEntities();
                        if (phase == 1) {
                            configure(true);
                            phase = 2;
                        } else finish(null);
                    }
                }
            } catch (Throwable error) {
                finish(error);
            } finally {
                pending = false;
            }
        });
    }

    private static void prepare() throws Exception {
        player = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0);
        level = (ServerLevel) player.level();
        origin = new BlockPos(Integer.getInteger("kineticcore.spawnerEggValidation.x", 1000),
                Integer.getInteger("kineticcore.spawnerEggValidation.y", 200), Integer.getInteger("kineticcore.spawnerEggValidation.z", 1000));
        spawnerPos = origin.offset(3, 0, 0);
        area = new AABB(origin.getX() - 6, origin.getY() - 2, origin.getZ() - 6,
                origin.getX() + 7, origin.getY() + 6, origin.getZ() + 7);
        playerPosition = player.position();
        previousHand = player.getMainHandItem().copy();
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, area)) originalEntities.add(entity.getUUID());
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -1, -5), origin.offset(5, 4, 5))) {
            require(level.getBlockEntity(pos) == null, "Fixture region contains a block entity; select another fixture origin");
            blocks.put(pos.immutable(), level.getBlockState(pos));
        }
        for (var pos : blocks.keySet()) level.setBlockAndUpdate(pos, pos.getY() == origin.getY() - 1
                ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        teleport(origin.getX() - 1.5D, origin.getY(), origin.getZ() + 0.5D);
        pass("arena-ready nativeWorldTicks=true");
    }

    private static void configure(boolean thrown) throws Exception {
        level.setBlockAndUpdate(spawnerPos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(spawnerPos, Blocks.SPAWNER.defaultBlockState());
        var spawner = (SpawnerBlockEntity) level.getBlockEntity(spawnerPos);
        CompoundTag initial = NativeSpawnEggData.saveSpawner(spawner.getSpawner(), level);
        initial.putShort("Delay", (short) 10);
        initial.putShort("MinSpawnDelay", (short) 40);
        initial.putShort("MaxSpawnDelay", (short) 60);
        initial.putShort("SpawnCount", (short) 1);
        initial.putShort("MaxNearbyEntities", (short) 8);
        initial.putShort("RequiredPlayerRange", (short) 32);
        initial.putShort("SpawnRange", (short) 2);
        var entity = new CompoundTag(); entity.putString("id", "minecraft:pig");
        var data = new CompoundTag(); data.put("entity", entity); data.put("custom_spawn_rules", new CompoundTag());
        initial.put("SpawnData", data); initial.remove("SpawnPotentials");
        loadSpawner(spawner, initial);
        name = thrown ? "Native Spawner Thrown Cow" : "Native Spawner Direct Cow";
        //? if >=1.21 {
        /*String health = KineticRegistries.attributes().id(Attributes.MAX_HEALTH.value()).toString();
        *///?} else {
        String health = KineticRegistries.attributes().id(Attributes.MAX_HEALTH).toString();
        //?}
        var template = new SpawnEggTemplate("minecraft:cow", List.of(new AttributeSpec(health, 60, AttributeSpec.Operation.SET)),
                List.of(new EffectSpec("minecraft:speed", 2, 1200, false, false),
                        new EffectSpec("minecraft:fire_resistance", 0, -1, false, true)), name, equipment());
        ItemStack egg = KineticItemCreator.buildEgg(level, template);
        if (thrown) {
            var projectile = new ThrowSpawnEgg(level, player);
            projectile.setItem(egg);
            projectile.setPos(spawnerPos.getX() - 3, spawnerPos.getY() + 0.5D, spawnerPos.getZ() + 0.5D);
            projectile.shoot(1, 0, 0, 1, 0);
            require(level.addFreshEntity(projectile), "Real egg projectile was rejected");
        } else {
            player.setItemInHand(InteractionHand.MAIN_HAND, egg);
            var hit = new BlockHitResult(Vec3.atCenterOf(spawnerPos), Direction.WEST, spawnerPos, false);
            require(egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Real spawn egg use was rejected");
            CompoundTag saved = NativeSpawnEggData.saveSpawner(spawner.getSpawner(), level);
            for (String key : new String[] {"Delay", "MinSpawnDelay", "MaxSpawnDelay", "SpawnCount", "MaxNearbyEntities", "RequiredPlayerRange", "SpawnRange"}) {
                require(Objects.equals(initial.get(key), saved.get(key)), "Native spawner setting changed: " + key);
            }
            var spawnData = compound(saved, "SpawnData");
            var snapshot = compound(spawnData, "entity");
            //? if >=26.1 {
            /*require("minecraft:cow".equals(snapshot.getStringOr("id", "")), "SpawnData did not select the egg's entity");
            *///?} else {
            require("minecraft:cow".equals(snapshot.getString("id")), "SpawnData did not select the egg's entity");
            //?}
            require(snapshot.size() > 1 && !snapshot.contains("UUID") && !snapshot.contains("Pos"), "SpawnData is missing state or fixes entity identity/position");
            require(spawnData.contains("custom_spawn_rules"), "Existing custom spawn rules were lost");
            evidence.add("SPAWN_DATA_DIRECT " + spawnData);
            pass("direct-native-data settingsPreserved=true");
        }
        phaseStarted = level.getGameTime();
    }

    private static void verifyEntity(LivingEntity entity) {
        verifyEquipment(entity);
        require(entity.getMaxHealth() == 60, "Native attribute snapshot was not loaded");
        //? if >=1.21 {
        /*var speed = KineticRegistries.mobEffects().holder(KineticRegistries.mobEffects().get(KineticResourceIds.parse("minecraft:speed")));
        var fire = KineticRegistries.mobEffects().holder(KineticRegistries.mobEffects().get(KineticResourceIds.parse("minecraft:fire_resistance")));
        *///?} else {
        var speed = KineticRegistries.mobEffects().get(KineticResourceIds.parse("minecraft:speed"));
        var fire = KineticRegistries.mobEffects().get(KineticResourceIds.parse("minecraft:fire_resistance"));
        //?}
        var effect = entity.getEffect(speed);
        require(effect != null && effect.getAmplifier() == 2 && effect.getDuration() > 0 && effect.getDuration() <= 1200
                && !effect.isVisible() && !effect.showIcon(), "Native effect parameters were not loaded");
        effect = entity.getEffect(fire);
        require(effect != null && effect.getDuration() == -1 && effect.showIcon(), "Native permanent effect was not loaded");
    }

    static List<EquipmentSpec> equipment() {
        //? if >=1.20.5 {
        /*String sword = "minecraft:diamond_sword[minecraft:damage=7]";
        *///?} else {
        String sword = "minecraft:diamond_sword{Damage:7}";
        //?}
        return List.of(new EquipmentSpec(EquipmentSpec.Slot.MAINHAND, sword),
                new EquipmentSpec(EquipmentSpec.Slot.OFFHAND, "minecraft:shield"),
                new EquipmentSpec(EquipmentSpec.Slot.HEAD, "minecraft:diamond_helmet"),
                new EquipmentSpec(EquipmentSpec.Slot.CHEST, "minecraft:diamond_chestplate"),
                new EquipmentSpec(EquipmentSpec.Slot.LEGS, "minecraft:diamond_leggings"),
                new EquipmentSpec(EquipmentSpec.Slot.FEET, "minecraft:diamond_boots"));
    }

    static void verifyEquipment(LivingEntity entity) {
        for (var spec : equipment()) {
            var stack = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.valueOf(spec.slot().name()));
            var expected = dev.xyat.kineticcore.api.inventory.KineticItemText.parse(spec.item());
            //? if >=1.20.5 {
            /*require(net.minecraft.world.item.ItemStack.isSameItemSameComponents(expected, stack), "Equipment/data missing from " + spec.slot());
            *///?} else {
            require(net.minecraft.world.item.ItemStack.isSameItemSameTags(expected, stack), "Equipment/NBT missing from " + spec.slot());
            //?}
        }
    }

    private static void loadSpawner(SpawnerBlockEntity spawner, CompoundTag data) {
        //? if >=26.1 {
        /*spawner.getSpawner().load(level, spawnerPos, net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), data));
        *///?} else {
        spawner.getSpawner().load(level, spawnerPos, data);
        //?}
    }

    private static void removeNewEntities() {
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity != player && !originalEntities.contains(entity.getUUID())) entity.discard();
        }
    }

    private static void teleport(double x, double y, double z) throws Exception {
        var source = player.createCommandSourceStack().withPermission(4);
        level.getServer().getCommands().getDispatcher().execute(String.format(Locale.ROOT, "tp @s %.3f %.3f %.3f", x, y, z), source);
    }

    private static void finish(Throwable error) {
        if (finished) return;
        finished = true;
        try {
            if (level != null) {
                removeNewEntities();
                for (var entry : blocks.entrySet()) level.setBlockAndUpdate(entry.getKey(), entry.getValue());
            }
            if (player != null && previousHand != null) {
                player.setItemInHand(InteractionHand.MAIN_HAND, previousHand);
                player.containerMenu.broadcastChanges();
                teleport(playerPosition.x, playerPosition.y, playerPosition.z);
            }
        } catch (Throwable restoreError) {
            if (error == null) error = restoreError;
            else error.addSuppressed(restoreError);
        }
        String result = "SPAWNER_EGG_VALIDATION_" + (error == null ? "PASS" : "FAIL") + " nativeSpawner=true direct=true thrown=true waves=2";
        evidence.add(result + (error == null ? "" : " error=" + error));
        if (error == null) LOG.info(result); else LOG.error(result, error);
        try {
            Path output = Path.of(System.getProperty("kineticcore.spawnerEggValidation.output", "spawner-egg-validation.txt"));
            if (output.toAbsolutePath().getParent() != null) Files.createDirectories(output.toAbsolutePath().getParent());
            Files.write(output, evidence);
        } catch (Exception writeError) { LOG.error("SPAWNER_EGG_RESULT_WRITE_FAIL", writeError); }
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }

    private static void pass(String message) { evidence.add("PASS " + message); LOG.info("SPAWNER_EGG_CASE_PASS {}", message); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static CompoundTag compound(CompoundTag tag, String key) {
        //? if >=26.1 {
        /*return tag.getCompoundOrEmpty(key);
        *///?} else {
        return tag.getCompound(key);
        //?}
    }
}
