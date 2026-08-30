package dev.xyat.kineticcore.feature.setspawn.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class SetSpawnData extends SavedData {
    private static final String DATA_NAME = "kineticcore_setspawn";
    public static final int CURRENT_DATA_VERSION = 4;

    private int dataVersion = 0;
    private boolean initialized = false;
    private boolean loadedFromDisk = false;
    private boolean spawnCalculated = false;
    private boolean adminSpawn = false;
    private String spawnDim = "minecraft:overworld";
    private int spawnX = 0;
    private int spawnY = 0;
    private int spawnZ = 0;

    private boolean setSpawnWorldChecked = false;
    private boolean setSpawnWorldEnabled = false;
    private boolean automaticSpawnWorld = false;

    private boolean originalSpawnCaptured = false;
    private String originalSpawnDim = "minecraft:overworld";
    private int originalSpawnX = 0;
    private int originalSpawnY = 0;
    private int originalSpawnZ = 0;

    public static SetSpawnData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SetSpawnData::load, SetSpawnData::new, DATA_NAME);
    }

    public static SetSpawnData load(CompoundTag tag) {
        SetSpawnData data = new SetSpawnData();
        data.loadedFromDisk = true;
        data.dataVersion = tag.contains("dataVersion") ? tag.getInt("dataVersion") : 0;
        data.initialized = tag.getBoolean("initialized");
        data.spawnCalculated = tag.getBoolean("spawnCalculated");
        data.adminSpawn = tag.getBoolean("adminSpawn");
        data.spawnDim = tag.contains("spawnDim") ? tag.getString("spawnDim") : "minecraft:overworld";
        data.spawnX = tag.getInt("spawnX");
        data.spawnY = tag.getInt("spawnY");
        data.spawnZ = tag.getInt("spawnZ");
        data.setSpawnWorldChecked = tag.getBoolean("setSpawnWorldChecked");
        data.setSpawnWorldEnabled = tag.getBoolean("setSpawnWorldEnabled");
        data.automaticSpawnWorld = tag.getBoolean("automaticSpawnWorld");
        data.originalSpawnCaptured = tag.getBoolean("originalSpawnCaptured");
        data.originalSpawnDim = tag.contains("originalSpawnDim") ? tag.getString("originalSpawnDim") : "minecraft:overworld";
        data.originalSpawnX = tag.getInt("originalSpawnX");
        data.originalSpawnY = tag.getInt("originalSpawnY");
        data.originalSpawnZ = tag.getInt("originalSpawnZ");
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putInt("dataVersion", dataVersion);
        tag.putBoolean("initialized", initialized);
        tag.putBoolean("spawnCalculated", spawnCalculated);
        tag.putBoolean("adminSpawn", adminSpawn);
        tag.putString("spawnDim", spawnDim);
        tag.putInt("spawnX", spawnX);
        tag.putInt("spawnY", spawnY);
        tag.putInt("spawnZ", spawnZ);
        tag.putBoolean("setSpawnWorldChecked", setSpawnWorldChecked);
        tag.putBoolean("setSpawnWorldEnabled", setSpawnWorldEnabled);
        tag.putBoolean("automaticSpawnWorld", automaticSpawnWorld);
        tag.putBoolean("originalSpawnCaptured", originalSpawnCaptured);
        tag.putString("originalSpawnDim", originalSpawnDim);
        tag.putInt("originalSpawnX", originalSpawnX);
        tag.putInt("originalSpawnY", originalSpawnY);
        tag.putInt("originalSpawnZ", originalSpawnZ);
        return tag;
    }

    public boolean isLoadedFromDisk() {
        return loadedFromDisk;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
        setDirty();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        setDirty();
    }

    public boolean isSpawnCalculated() {
        return spawnCalculated;
    }

    public void setSpawnCalculated(boolean spawnCalculated) {
        this.spawnCalculated = spawnCalculated;
        setDirty();
    }

    public boolean isAdminSpawn() {
        return adminSpawn;
    }

    public void setAdminSpawn(boolean adminSpawn) {
        this.adminSpawn = adminSpawn;
        setDirty();
    }

    public String getSpawnDim() {
        return spawnDim;
    }

    public void setSpawnDim(String spawnDim) {
        this.spawnDim = spawnDim;
        setDirty();
    }

    public int getSpawnX() {
        return spawnX;
    }

    public void setSpawnX(int spawnX) {
        this.spawnX = spawnX;
        setDirty();
    }

    public int getSpawnY() {
        return spawnY;
    }

    public void setSpawnY(int spawnY) {
        this.spawnY = spawnY;
        setDirty();
    }

    public int getSpawnZ() {
        return spawnZ;
    }

    public void setSpawnZ(int spawnZ) {
        this.spawnZ = spawnZ;
        setDirty();
    }

    public boolean isSetSpawnWorldChecked() {
        return setSpawnWorldChecked;
    }

    public void setSetSpawnWorldChecked(boolean setSpawnWorldChecked) {
        this.setSpawnWorldChecked = setSpawnWorldChecked;
        setDirty();
    }

    public boolean isSetSpawnWorldEnabled() {
        return setSpawnWorldEnabled;
    }

    public void setSetSpawnWorldEnabled(boolean setSpawnWorldEnabled) {
        this.setSpawnWorldEnabled = setSpawnWorldEnabled;
        setDirty();
    }

    public boolean isAutomaticSpawnWorld() {
        return automaticSpawnWorld;
    }

    public void setAutomaticSpawnWorld(boolean automaticSpawnWorld) {
        this.automaticSpawnWorld = automaticSpawnWorld;
        setDirty();
    }

    public boolean isOriginalSpawnCaptured() {
        return originalSpawnCaptured;
    }

    public void setOriginalSpawnCaptured(boolean originalSpawnCaptured) {
        this.originalSpawnCaptured = originalSpawnCaptured;
        setDirty();
    }

    public String getOriginalSpawnDim() {
        return originalSpawnDim;
    }

    public void setOriginalSpawnDim(String originalSpawnDim) {
        this.originalSpawnDim = originalSpawnDim;
        setDirty();
    }

    public int getOriginalSpawnX() {
        return originalSpawnX;
    }

    public void setOriginalSpawnX(int originalSpawnX) {
        this.originalSpawnX = originalSpawnX;
        setDirty();
    }

    public int getOriginalSpawnY() {
        return originalSpawnY;
    }

    public void setOriginalSpawnY(int originalSpawnY) {
        this.originalSpawnY = originalSpawnY;
        setDirty();
    }

    public int getOriginalSpawnZ() {
        return originalSpawnZ;
    }

    public void setOriginalSpawnZ(int originalSpawnZ) {
        this.originalSpawnZ = originalSpawnZ;
        setDirty();
    }
}
