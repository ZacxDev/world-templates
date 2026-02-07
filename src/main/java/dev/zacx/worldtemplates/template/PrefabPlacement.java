package dev.zacx.worldtemplates.template;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines a prefab to place in the world.
 * Positions are relative to the spawn point.
 */
public class PrefabPlacement {

    private String id;
    private String path;
    private double x = 0.0;
    private double y = 0.0;
    private double z = 0.0;
    private String rotation = "None";

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Nonnull
    public String getPath() {
        return path != null ? path : "";
    }

    public void setPath(String path) {
        this.path = path;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }

    /**
     * Get the rotation string value (for YAML serialization).
     */
    @Nonnull
    public String getRotation() {
        return rotation != null ? rotation : "None";
    }

    public void setRotation(String rotation) {
        this.rotation = rotation;
    }

    /**
     * Get the absolute world position given a spawn point.
     * Prefab positions are relative to spawn.
     */
    @Nonnull
    public Vector3i getWorldPosition(WorldTemplate.SpawnPoint spawn) {
        return new Vector3i(
            (int) (spawn.getX() + x),
            (int) (spawn.getY() + y),
            (int) (spawn.getZ() + z)
        );
    }

    /**
     * Parse rotation string to Rotation enum.
     * Values: None, Ninety (90), OneEighty (180), TwoSeventy (270)
     */
    @Nonnull
    public Rotation getRotationEnum() {
        return switch (getRotation().toLowerCase()) {
            case "ninety", "90", "cw90" -> Rotation.Ninety;
            case "oneeighty", "180", "cw180" -> Rotation.OneEighty;
            case "twoseventy", "270", "cw270" -> Rotation.TwoSeventy;
            default -> Rotation.None;
        };
    }

    @Override
    public String toString() {
        return String.format("PrefabPlacement{id='%s', path='%s', pos=(%.1f,%.1f,%.1f), rot=%s}",
            id, path, x, y, z, rotation);
    }
}
