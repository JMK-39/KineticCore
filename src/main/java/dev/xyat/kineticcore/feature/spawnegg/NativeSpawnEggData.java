package dev.xyat.kineticcore.feature.spawnegg;

import dev.xyat.kineticcore.api.itemcreator.ItemCreatorStackData;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.feature.spawnegg.event.SpawnEggTemplateHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** Writes templates into the vanilla entity snapshot; ordinary spawner ticks and entity loading do the rest. */
public final class NativeSpawnEggData {
    private NativeSpawnEggData() { }

    public static boolean configure(SpawnerBlockEntity spawner, ItemStack stack, ServerLevel level, Player player) {
        var template = ItemCreatorStackData.readTemplate(stack).orElse(null);
        if (template == null) return false;
        var type = KineticRegistries.entityTypes().get(KineticResourceIds.tryParse(template.entityId()));
        if (type == null || !(type.create(level) instanceof LivingEntity living)) return false;
        BlockPos pos = spawner.getBlockPos();
        living.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0, 0);
        if (living instanceof Mob mob) {
            // Native spawners skip finalizeSpawn for a populated entity snapshot. Initialize that snapshot once.
            //? if >=1.21 {
            /*mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            *///?} else {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null, null);
            //?}
        }
        CompoundTag entity = saveEntity(living);
        entity.merge(eggEntityData(stack));
        loadEntity(living, entity);
        SpawnEggTemplateHandler.applyTemplate(living, template, player);
        entity = saveEntity(living);
        // A snapshot supplies entity state, never a shared UUID or a fixed spawn position.
        for (String key : new String[] {"UUID", "uuid", "Pos", "pos", "Motion", "motion", "Rotation", "rotation", "Passengers", "passengers"}) {
            entity.remove(key);
        }
        entity.putString("id", template.entityId());
        BaseSpawner base = spawner.getSpawner();
        CompoundTag next = withEntityData(saveSpawner(base, level), entity);
        //? if >=26.1 {
        /*base.load(level, pos, net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), next));
        *///?} else {
        base.load(level, pos, next);
        //?}
        spawner.setChanged();
        return true;
    }

    /** Preserves all existing timing, range, custom spawn rules and extension data. */
    public static CompoundTag withEntityData(CompoundTag current, CompoundTag entity) {
        CompoundTag result = current.copy();
        //? if >=26.1 {
        /*CompoundTag data = current.getCompoundOrEmpty("SpawnData").copy();
        *///?} else {
        CompoundTag data = current.getCompound("SpawnData").copy();
        //?}
        data.put("entity", entity.copy());
        result.put("SpawnData", data);
        // Vanilla load builds a single weighted entry from SpawnData when this field is absent. Old entries must
        // not replace the new template after the first wave.
        result.remove("SpawnPotentials");
        return result;
    }

    private static CompoundTag eggEntityData(ItemStack stack) {
        //? if >=26.1 {
        /*var data = stack.get(net.minecraft.core.component.DataComponents.ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTagWithoutId();
        *///?} else if >=1.20.5 {
        /*var data = stack.get(net.minecraft.core.component.DataComponents.ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
        *///?} else {
        CompoundTag data = stack.getTagElement("EntityTag");
        return data == null ? new CompoundTag() : data.copy();
        //?}
    }

    private static CompoundTag saveEntity(Entity entity) {
        //? if >=26.1 {
        /*var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
        *///?} else {
        return entity.saveWithoutId(new CompoundTag());
        //?}
    }

    private static void loadEntity(Entity entity, CompoundTag data) {
        //? if >=26.1 {
        /*entity.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, entity.registryAccess(), data));
        *///?} else {
        entity.load(data);
        //?}
    }

    public static CompoundTag saveSpawner(BaseSpawner spawner, ServerLevel level) {
        //? if >=26.1 {
        /*var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        spawner.save(output);
        return output.buildResult();
        *///?} else {
        return spawner.save(new CompoundTag());
        //?}
    }
}
