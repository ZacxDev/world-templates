package dev.zacx.worldtemplates.world;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.builtin.instances.config.InstanceWorldConfig;
import com.hypixel.hytale.builtin.instances.removal.RemovalCondition;
import com.hypixel.hytale.builtin.instances.removal.TimeoutCondition;
import com.hypixel.hytale.builtin.instances.removal.WorldEmptyCondition;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PrefabUtil;
import dev.zacx.worldtemplates.template.PrefabPlacement;
import dev.zacx.worldtemplates.template.WorldTemplate;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Spawns world instances from templates.
 * Creates void worlds, applies settings, pastes prefabs, and teleports players.
 */
public class WorldSpawner {

    // Base instance template for void worlds (must exist in Server/Instances/)
    private static final String VOID_INSTANCE_TEMPLATE = "VoidTemplate";

    private final HytaleLogger logger;

    public WorldSpawner(@Nonnull HytaleLogger logger) {
        this.logger = logger;
    }

    /**
     * Spawn a world from a template and teleport the player into it.
     *
     * @param template   The world template to use
     * @param playerRef  The player to teleport
     * @param originWorld The player's current world
     * @param store      The entity store
     * @param entityRef  The player's entity reference
     * @return CompletableFuture that completes with the created world
     */
    @Nonnull
    public CompletableFuture<World> spawnAndTeleport(
        @Nonnull WorldTemplate template,
        @Nonnull PlayerRef playerRef,
        @Nonnull World originWorld,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> entityRef
    ) {
        // Get player's current position as return point
        TransformComponent transformComponent = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transformComponent == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Player has no transform component")
            );
        }

        Transform returnTransform = new Transform(
            transformComponent.getPosition().clone(),
            transformComponent.getRotation().clone()
        );

        logger.atInfo().log("Creating world from template '%s' for player %s",
            template.getDisplayName(), playerRef.getUsername());

        // Spawn the instance (includes settings and prefabs)
        return spawnInstance(template, originWorld, returnTransform)
            .thenApply(world -> {
                // Teleport player to instance
                teleportPlayer(playerRef, originWorld, world, returnTransform);
                return world;
            });
    }

    /**
     * Spawn an instance world from template settings (without prefabs, without teleport).
     * Used internally and for creating shared instances like lobby.
     *
     * @param template       The template to use
     * @param originWorld    The origin world for instance creation
     * @param returnTransform The return transform for instance exit
     * @return CompletableFuture with the created world (settings applied, prefabs pasted)
     */
    @Nonnull
    public CompletableFuture<World> spawnInstance(
        @Nonnull WorldTemplate template,
        @Nonnull World originWorld,
        @Nonnull Transform returnTransform
    ) {
        // Generate unique world name
        String worldName = "template-" + template.getName().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);

        return InstancesPlugin.get()
            .spawnInstance(VOID_INSTANCE_TEMPLATE, worldName, originWorld, returnTransform)
            .thenApply(world -> {
                applyTemplateSettings(world, template);
                return world;
            })
            .thenCompose(world -> {
                // Paste prefabs and return the world when done
                return pastePrefabs(world, template).thenApply(v -> world);
            });
    }

    /**
     * Apply template settings and paste prefabs to an existing world.
     * Used for auto-setup of default/named worlds that match a template.
     *
     * @param world    The existing world to configure
     * @param template The template to apply
     * @return CompletableFuture that completes when prefabs are pasted
     */
    @Nonnull
    public CompletableFuture<Void> applyToExistingWorld(@Nonnull World world, @Nonnull WorldTemplate template) {
        logger.atInfo().log("Applying template '%s' to existing world '%s'",
            template.getDisplayName(), world.getName());

        applyTemplateSettings(world, template);

        // Paste prefabs on world thread
        return pastePrefabs(world, template);
    }

    /**
     * Apply template settings to the world config.
     */
    private void applyTemplateSettings(@Nonnull World world, @Nonnull WorldTemplate template) {
        WorldConfig config = world.getWorldConfig();
        WorldTemplate.WorldSettings settings = template.getWorldSettings();

        // Display name
        config.setDisplayName(template.getDisplayName());

        // Spawn provider
        config.setSpawnProvider(new GlobalSpawnProvider(template.getSpawnTransform()));

        // Game mode
        if (settings.getGameMode() != null) {
            try {
                GameMode mode = GameMode.valueOf(settings.getGameMode());
                config.setGameMode(mode);
            } catch (IllegalArgumentException e) {
                logger.atWarning().log("Invalid game mode: %s", settings.getGameMode());
            }
        }

        // Weather
        if (settings.getForcedWeather() != null) {
            config.setForcedWeather(settings.getForcedWeather());
        }

        // Time settings
        if (settings.isGameTimePaused() != null) {
            config.setGameTimePaused(settings.isGameTimePaused());
        }

        if (settings.getGameTime() != null) {
            Instant gameTime = parseGameTime(settings.getGameTime());
            if (gameTime != null) {
                config.setGameTime(gameTime);
            }
        }

        // NPC/spawning settings
        if (settings.isSpawningNPC() != null) {
            config.setSpawningNPC(settings.isSpawningNPC());
        }
        if (settings.isSpawnMarkersEnabled() != null) {
            config.setIsSpawnMarkersEnabled(settings.isSpawnMarkersEnabled());
        }
        if (settings.isAllNPCFrozen() != null) {
            config.setIsAllNPCFrozen(settings.isAllNPCFrozen());
        }

        // Combat settings
        if (settings.isPvpEnabled() != null) {
            config.setPvpEnabled(settings.isPvpEnabled());
        }

        // Ticking
        if (settings.isTicking() != null) {
            config.setTicking(settings.isTicking());
        }
        if (settings.isBlockTicking() != null) {
            config.setBlockTicking(settings.isBlockTicking());
        }

        // Gameplay config
        if (settings.getGameplayConfig() != null) {
            config.setGameplayConfig(settings.getGameplayConfig());
        }

        // Instance-specific settings
        InstanceWorldConfig instanceConfig = InstanceWorldConfig.ensureAndGet(config);
        WorldTemplate.InstanceSettings instanceSettings = template.getInstanceSettings();

        // Removal conditions
        RemovalCondition[] conditions = parseRemovalConditions(instanceSettings.getRemovalCondition());
        instanceConfig.setRemovalConditions(conditions);

        // Delete on remove
        config.setDeleteOnRemove(instanceSettings.isDeleteOnRemove());

        config.markChanged();

        logger.atInfo().log("Applied template settings to world '%s'", config.getDisplayName());
    }

    /**
     * Paste all prefabs from template into the world.
     */
    @Nonnull
    private CompletableFuture<Void> pastePrefabs(@Nonnull World world, @Nonnull WorldTemplate template) {
        if (template.getPrefabs().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        WorldTemplate.SpawnPoint spawn = template.getSpawnPoint();

        world.execute(() -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                Random random = new Random();
                int placed = 0;

                Path prefabsRoot = PrefabStore.get().getServerPrefabsPath();

                for (PrefabPlacement placement : template.getPrefabs()) {
                    if (placement.getPath().isEmpty()) {
                        logger.atWarning().log("Skipping prefab with empty path: %s", placement.getId());
                        continue;
                    }

                    // Convert string path to Path object for PrefabBufferUtil
                    Path prefabPath = prefabsRoot.resolve(placement.getPath());
                    IPrefabBuffer buffer = PrefabBufferUtil.getCached(prefabPath);
                    if (buffer == null) {
                        logger.atWarning().log("Failed to load prefab: %s", placement.getPath());
                        continue;
                    }

                    // Get absolute position (prefab position is relative to spawn)
                    Vector3i position = placement.getWorldPosition(spawn);

                    PrefabUtil.paste(
                        buffer,
                        world,
                        position,
                        placement.getRotationEnum(),
                        true,  // spawn entities
                        random,
                        store
                    );

                    placed++;
                    logger.atInfo().log("Placed prefab '%s' at %s",
                        placement.getId() != null ? placement.getId() : placement.getPath(),
                        position);
                }

                logger.atInfo().log("Placed %d/%d prefabs in world",
                    placed, template.getPrefabs().size());

                future.complete(null);
            } catch (Exception e) {
                logger.atWarning().log("Failed to paste prefabs: %s", e.getMessage());
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Teleport player to the instance world.
     */
    private void teleportPlayer(
        @Nonnull PlayerRef playerRef,
        @Nonnull World currentWorld,
        @Nonnull World instanceWorld,
        @Nonnull Transform returnTransform
    ) {
        currentWorld.execute(() -> {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null || !entityRef.isValid()) {
                logger.atWarning().log("Player entity ref invalid during teleport");
                return;
            }

            Store<EntityStore> store = entityRef.getStore();
            InstancesPlugin.teleportPlayerToInstance(
                entityRef,
                store,
                instanceWorld,
                returnTransform
            );

            logger.atInfo().log("Teleported %s to template world", playerRef.getUsername());
        });
    }

    /**
     * Parse game time from "HH:MM" format to Instant.
     */
    private Instant parseGameTime(@Nonnull String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length != 2) return null;

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            // Base time is year 0, add hours and minutes
            return Instant.EPOCH
                .plus(hours, ChronoUnit.HOURS)
                .plus(minutes, ChronoUnit.MINUTES);
        } catch (NumberFormatException e) {
            logger.atWarning().log("Invalid game time format: %s", timeStr);
            return null;
        }
    }

    /**
     * Parse removal condition string to RemovalCondition array.
     * Supported values:
     *   - "worldempty" or "empty" - Remove when all players leave
     *   - "never" or "none" - Never auto-remove
     *   - "timeout:300" - Remove after 300 seconds
     *
     * Note: IdleTimeoutCondition exists but doesn't accept configuration,
     * so "idle:N" is treated as a timeout instead.
     */
    @Nonnull
    private RemovalCondition[] parseRemovalConditions(@Nonnull String condition) {
        String lower = condition.toLowerCase();

        if (lower.equals("worldempty") || lower.equals("empty")) {
            return new RemovalCondition[]{ WorldEmptyCondition.INSTANCE };
        }

        if (lower.equals("never") || lower.equals("none")) {
            return RemovalCondition.EMPTY;
        }

        // Try to parse as timeout (e.g., "timeout:300" for 5 minutes)
        if (lower.startsWith("timeout:")) {
            try {
                int seconds = Integer.parseInt(condition.substring(8));
                return new RemovalCondition[]{ new TimeoutCondition(seconds) };
            } catch (NumberFormatException e) {
                logger.atWarning().log("Invalid timeout format: %s", condition);
            }
        }

        // "idle:N" is treated as timeout since IdleTimeoutCondition isn't configurable
        if (lower.startsWith("idle:")) {
            try {
                int seconds = Integer.parseInt(condition.substring(5));
                logger.atInfo().log("Note: 'idle' mapped to timeout since IdleTimeoutCondition is not configurable");
                return new RemovalCondition[]{ new TimeoutCondition(seconds) };
            } catch (NumberFormatException e) {
                logger.atWarning().log("Invalid idle timeout format: %s", condition);
            }
        }

        // Default to WorldEmpty
        return new RemovalCondition[]{ WorldEmptyCondition.INSTANCE };
    }
}
