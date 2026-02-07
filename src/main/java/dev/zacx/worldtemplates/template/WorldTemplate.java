package dev.zacx.worldtemplates.template;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a world template loaded from YAML.
 * Templates define world settings, spawn point, and prefabs to place.
 */
public class WorldTemplate {

    private String name;
    private String displayName;
    private WorldSettings worldSettings;
    private SpawnPoint spawnPoint;
    private InstanceSettings instanceSettings;
    private List<PrefabPlacement> prefabs;

    public WorldTemplate() {
        this.worldSettings = new WorldSettings();
        this.spawnPoint = new SpawnPoint();
        this.instanceSettings = new InstanceSettings();
        this.prefabs = new ArrayList<>();
    }

    @Nonnull
    public String getName() {
        return name != null ? name : "unnamed";
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName != null ? displayName : getName();
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Nonnull
    public WorldSettings getWorldSettings() {
        return worldSettings;
    }

    public void setWorldSettings(WorldSettings worldSettings) {
        this.worldSettings = worldSettings != null ? worldSettings : new WorldSettings();
    }

    @Nonnull
    public SpawnPoint getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(SpawnPoint spawnPoint) {
        this.spawnPoint = spawnPoint != null ? spawnPoint : new SpawnPoint();
    }

    @Nonnull
    public InstanceSettings getInstanceSettings() {
        return instanceSettings;
    }

    public void setInstanceSettings(InstanceSettings instanceSettings) {
        this.instanceSettings = instanceSettings != null ? instanceSettings : new InstanceSettings();
    }

    @Nonnull
    public List<PrefabPlacement> getPrefabs() {
        return prefabs != null ? prefabs : new ArrayList<>();
    }

    public void setPrefabs(List<PrefabPlacement> prefabs) {
        this.prefabs = prefabs != null ? prefabs : new ArrayList<>();
    }

    /**
     * Get spawn point as a Transform for WorldConfig.
     */
    @Nonnull
    public Transform getSpawnTransform() {
        return new Transform(
            new Vector3d(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ()),
            new Vector3f(spawnPoint.getPitch(), spawnPoint.getYaw(), 0.0f)
        );
    }

    /**
     * World settings that map to WorldConfig fields.
     */
    public static class WorldSettings {
        private String gameMode;
        private String forcedWeather;
        private Boolean gameTimePaused;
        private String gameTime;  // "HH:MM" format
        private Boolean spawningNPC;
        private Boolean spawnMarkersEnabled;
        private Boolean allNPCFrozen;
        private Boolean pvpEnabled;
        private Boolean fallDamageEnabled;
        private Boolean blockTicking;
        private Boolean ticking;
        private Integer daytimeDurationSeconds;
        private Integer nighttimeDurationSeconds;
        private String gameplayConfig;

        // Getters with null-safe defaults
        @Nullable public String getGameMode() { return gameMode; }
        @Nullable public String getForcedWeather() { return forcedWeather; }
        @Nullable public Boolean isGameTimePaused() { return gameTimePaused; }
        @Nullable public String getGameTime() { return gameTime; }
        @Nullable public Boolean isSpawningNPC() { return spawningNPC; }
        @Nullable public Boolean isSpawnMarkersEnabled() { return spawnMarkersEnabled; }
        @Nullable public Boolean isAllNPCFrozen() { return allNPCFrozen; }
        @Nullable public Boolean isPvpEnabled() { return pvpEnabled; }
        @Nullable public Boolean isFallDamageEnabled() { return fallDamageEnabled; }
        @Nullable public Boolean isBlockTicking() { return blockTicking; }
        @Nullable public Boolean isTicking() { return ticking; }
        @Nullable public Integer getDaytimeDurationSeconds() { return daytimeDurationSeconds; }
        @Nullable public Integer getNighttimeDurationSeconds() { return nighttimeDurationSeconds; }
        @Nullable public String getGameplayConfig() { return gameplayConfig; }

        // Setters for YAML mapping
        public void setGameMode(String gameMode) { this.gameMode = gameMode; }
        public void setForcedWeather(String forcedWeather) { this.forcedWeather = forcedWeather; }
        public void setGameTimePaused(Boolean gameTimePaused) { this.gameTimePaused = gameTimePaused; }
        public void setGameTime(String gameTime) { this.gameTime = gameTime; }
        public void setSpawningNPC(Boolean spawningNPC) { this.spawningNPC = spawningNPC; }
        public void setSpawnMarkersEnabled(Boolean spawnMarkersEnabled) { this.spawnMarkersEnabled = spawnMarkersEnabled; }
        public void setAllNPCFrozen(Boolean allNPCFrozen) { this.allNPCFrozen = allNPCFrozen; }
        public void setPvpEnabled(Boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
        public void setFallDamageEnabled(Boolean fallDamageEnabled) { this.fallDamageEnabled = fallDamageEnabled; }
        public void setBlockTicking(Boolean blockTicking) { this.blockTicking = blockTicking; }
        public void setTicking(Boolean ticking) { this.ticking = ticking; }
        public void setDaytimeDurationSeconds(Integer daytimeDurationSeconds) { this.daytimeDurationSeconds = daytimeDurationSeconds; }
        public void setNighttimeDurationSeconds(Integer nighttimeDurationSeconds) { this.nighttimeDurationSeconds = nighttimeDurationSeconds; }
        public void setGameplayConfig(String gameplayConfig) { this.gameplayConfig = gameplayConfig; }
    }

    /**
     * Spawn point configuration.
     */
    public static class SpawnPoint {
        private double x = 0.0;
        private double y = 64.0;
        private double z = 0.0;
        private float yaw = 0.0f;
        private float pitch = 0.0f;

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }

        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
        public void setZ(double z) { this.z = z; }
        public void setYaw(float yaw) { this.yaw = yaw; }
        public void setPitch(float pitch) { this.pitch = pitch; }
    }

    /**
     * Instance-specific settings.
     */
    public static class InstanceSettings {
        private String removalCondition = "WorldEmpty";
        private Boolean preventReconnection = false;
        private Boolean deleteOnRemove = true;

        @Nonnull
        public String getRemovalCondition() {
            return removalCondition != null ? removalCondition : "WorldEmpty";
        }

        public Boolean getPreventReconnection() {
            return preventReconnection;
        }

        public boolean isPreventReconnection() {
            return preventReconnection != null && preventReconnection;
        }

        public Boolean getDeleteOnRemove() {
            return deleteOnRemove;
        }

        public boolean isDeleteOnRemove() {
            return deleteOnRemove == null || deleteOnRemove;
        }

        public void setRemovalCondition(String removalCondition) { this.removalCondition = removalCondition; }
        public void setPreventReconnection(Boolean preventReconnection) { this.preventReconnection = preventReconnection; }
        public void setDeleteOnRemove(Boolean deleteOnRemove) { this.deleteOnRemove = deleteOnRemove; }
    }
}
