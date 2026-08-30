package dev.xyat.kineticcore.feature.worldinit.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class WorldInitData extends SavedData {
    private static final String DATA_NAME = "kineticcore_world_init_commands";

    private boolean commandsExecuted = false;

    public static WorldInitData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WorldInitData::load, WorldInitData::new, DATA_NAME);
    }

    public static WorldInitData load(CompoundTag tag) {
        WorldInitData data = new WorldInitData();
        data.commandsExecuted = tag.getBoolean("commandsExecuted");
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putBoolean("commandsExecuted", commandsExecuted);
        return tag;
    }

    public boolean isCommandsExecuted() {
        return commandsExecuted;
    }

    public void setCommandsExecuted(boolean commandsExecuted) {
        this.commandsExecuted = commandsExecuted;
        setDirty();
    }
}
