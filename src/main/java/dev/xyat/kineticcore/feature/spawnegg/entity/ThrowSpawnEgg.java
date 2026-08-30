package dev.xyat.kineticcore.feature.spawnegg.entity;

import dev.xyat.kineticcore.feature.spawnegg.SpawnEggInit;
import dev.xyat.kineticcore.feature.spawnegg.util.SpawnImpactOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ThrowSpawnEgg extends ThrowableItemProjectile {
    public ThrowSpawnEgg(EntityType<? extends ThrowSpawnEgg> entityType, Level level) {
        super(entityType, level);
    }

    public ThrowSpawnEgg(Level level, LivingEntity owner) {
        super(SpawnEggInit.THROWABLE_SPAWN_EGG.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EGG;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        ItemStack stack = getItem();
        if (!(stack.getItem() instanceof SpawnEggItem spawnEgg)) {
            if (!level().isClientSide) discard();
            return;
        }
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) return;

        CompoundTag stackTag = stack.getTag();
        if (result instanceof BlockHitResult blockHit && handleSpawnerHit(blockHit, spawnEgg, stackTag)) {
            discard();
            return;
        }

        EntityType<?> entityType = spawnEgg.getType(stackTag);
        BlockPos spawnAnchor = result instanceof BlockHitResult blockHit
                ? blockHit.getBlockPos().relative(blockHit.getDirection())
                : BlockPos.containing(result.getLocation());
        Player player = getOwner() instanceof Player ownerPlayer ? ownerPlayer : null;
        Entity entity = entityType.spawn(
                serverLevel,
                stack,
                player,
                spawnAnchor,
                MobSpawnType.SPAWN_EGG,
                false,
                false
        );

        if (entity != null) {
            if (result instanceof BlockHitResult blockHit) {
                moveEntityOutsideHitBlock(serverLevel, entity, blockHit);
            } else {
                moveEntityToEntityImpact(serverLevel, entity, result.getLocation());
            }
            applyExtraNbt(entity, stackTag);
        }

        discard();
    }

    private void moveEntityOutsideHitBlock(ServerLevel level, Entity entity, BlockHitResult hit) {
        BlockPos blockPos = hit.getBlockPos();
        Direction direction = hit.getDirection();
        SpawnImpactOffset.Position base = SpawnImpactOffset.outsideFace(
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ(),
                direction.getStepX(),
                direction.getStepY(),
                direction.getStepZ(),
                entity.getBbWidth(),
                entity.getBbHeight()
        );
        Vec3 safe = findSafePosition(level, entity, new Vec3(base.x(), base.y(), base.z()), direction);
        entity.moveTo(safe.x, safe.y, safe.z, entity.getYRot(), entity.getXRot());
    }

    private Vec3 findSafePosition(ServerLevel level, Entity entity, Vec3 base, Direction direction) {
        if (tryPosition(level, entity, base)) return base;

        int stepX = direction.getStepX();
        int stepY = direction.getStepY();
        int stepZ = direction.getStepZ();

        for (int outward = 0; outward <= 4; outward++) {
            if (stepX != 0) {
                for (int lift = 0; lift <= 3; lift++) {
                    for (int side = 0; side <= 2; side++) {
                        if (side == 0) {
                            Vec3 candidate = base.add(stepX * outward, lift, 0.0D);
                            if (tryPosition(level, entity, candidate)) return candidate;
                        } else {
                            Vec3 positive = base.add(stepX * outward, lift, side);
                            if (tryPosition(level, entity, positive)) return positive;
                            Vec3 negative = base.add(stepX * outward, lift, -side);
                            if (tryPosition(level, entity, negative)) return negative;
                        }
                    }
                }
            } else if (stepZ != 0) {
                for (int lift = 0; lift <= 3; lift++) {
                    for (int side = 0; side <= 2; side++) {
                        if (side == 0) {
                            Vec3 candidate = base.add(0.0D, lift, stepZ * outward);
                            if (tryPosition(level, entity, candidate)) return candidate;
                        } else {
                            Vec3 positive = base.add(side, lift, stepZ * outward);
                            if (tryPosition(level, entity, positive)) return positive;
                            Vec3 negative = base.add(-side, lift, stepZ * outward);
                            if (tryPosition(level, entity, negative)) return negative;
                        }
                    }
                }
            } else {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Vec3 candidate = base.add(x, stepY * outward, z);
                        if (tryPosition(level, entity, candidate)) return candidate;
                    }
                }
            }
        }

        return base;
    }

    private void moveEntityToEntityImpact(ServerLevel level, Entity entity, Vec3 impact) {
        Vec3 base = new Vec3(impact.x, Math.floor(impact.y), impact.z);
        if (tryPosition(level, entity, base)) return;
        for (int lift = 1; lift <= 4; lift++) {
            Vec3 candidate = base.add(0.0D, lift, 0.0D);
            if (tryPosition(level, entity, candidate)) return;
        }
        entity.moveTo(base.x, base.y, base.z, entity.getYRot(), entity.getXRot());
    }

    private boolean tryPosition(ServerLevel level, Entity entity, Vec3 position) {
        entity.moveTo(position.x, position.y, position.z, entity.getYRot(), entity.getXRot());
        return level.noCollision(entity, entity.getBoundingBox());
    }

    private boolean handleSpawnerHit(BlockHitResult hit, SpawnEggItem spawnEgg, CompoundTag stackTag) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level().getBlockState(pos);
        if (!state.is(Blocks.SPAWNER)) return false;

        BlockEntity blockEntity = level().getBlockEntity(pos);
        if (!(blockEntity instanceof SpawnerBlockEntity spawner)) return false;

        EntityType<?> entityType = spawnEgg.getType(stackTag);
        CompoundTag entityData = new CompoundTag();
        entityData.putString("id", EntityType.getKey(entityType).toString());
        if (stackTag != null && stackTag.contains("EntityTag", Tag.TAG_COMPOUND)) {
            entityData.merge(stackTag.getCompound("EntityTag").copy());
        }

        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entityData);
        CompoundTag spawnerData = new CompoundTag();
        spawnerData.put("SpawnData", spawnData);
        spawnerData.putShort("Delay", (short) 20);
        spawner.load(spawnerData);
        spawner.setChanged();
        level().sendBlockUpdated(pos, state, state, 3);
        return true;
    }

    private void applyExtraNbt(Entity entity, CompoundTag stackTag) {
        if (stackTag == null) return;
        CompoundTag entityTag = stackTag.contains("EntityTag", Tag.TAG_COMPOUND)
                ? stackTag.getCompound("EntityTag")
                : stackTag;

        if (entityTag.contains("CustomName", Tag.TAG_STRING)) {
            try {
                Component name = Component.Serializer.fromJson(entityTag.getString("CustomName"));
                if (name != null) entity.setCustomName(name);
            } catch (Exception ignored) {
            }
        }

        if (!(entity instanceof Mob mob)) return;
        if (entityTag.getBoolean("PersistenceRequired")) {
            mob.setPersistenceRequired();
        }

        if (entityTag.contains("ArmorItems", Tag.TAG_LIST)) {
            ListTag armor = entityTag.getList("ArmorItems", Tag.TAG_COMPOUND);
            EquipmentSlot[] slots = {
                    EquipmentSlot.FEET,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.HEAD
            };
            for (int index = 0; index < Math.min(slots.length, armor.size()); index++) {
                mob.setItemSlot(slots[index], ItemStack.of(armor.getCompound(index)));
            }
        }

        if (entityTag.contains("HandItems", Tag.TAG_LIST)) {
            ListTag hands = entityTag.getList("HandItems", Tag.TAG_COMPOUND);
            if (!hands.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.of(hands.getCompound(0)));
            }
            if (hands.size() > 1) {
                mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.of(hands.getCompound(1)));
            }
        }
    }
}
