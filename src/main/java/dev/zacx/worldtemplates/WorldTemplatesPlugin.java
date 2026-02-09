package dev.zacx.worldtemplates;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.zacx.worldtemplates.command.WTCreateCommand;
import dev.zacx.worldtemplates.command.WTListCommand;
import dev.zacx.worldtemplates.command.WTReloadCommand;
import dev.zacx.worldtemplates.template.TemplateLoader;
import dev.zacx.worldtemplates.template.WorldTemplate;
import dev.zacx.worldtemplates.world.WorldSpawner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * World Templates Plugin
 *
 * Create world instances from YAML template files.
 * Templates define world settings, spawn point, and prefabs to place.
 *
 * Commands:
 *   /wt:create <name> - Create world from template and teleport
 *   /wt:list          - List available templates
 *   /wt:reload        - Reload templates from disk
 *
 * Template files go in: Server/WorldTemplates/*.template.yaml
 */
public class WorldTemplatesPlugin extends JavaPlugin {

    private static WorldTemplatesPlugin instance;

    private static final String LOBBY_TEMPLATE = "lobby";

    private TemplateLoader templateLoader;
    private WorldSpawner worldSpawner;

    // Track worlds that have been initialized to avoid duplicate prefab pasting
    private final Set<String> initializedWorlds = new HashSet<>();

    // Shared lobby instance (created once, reused for all players)
    @Nullable
    private World lobbyWorld;
    private CompletableFuture<World> lobbyCreationFuture;

    public WorldTemplatesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Nonnull
    public static WorldTemplatesPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        getLogger().atInfo().log("WorldTemplates: Setting up...");

        // Initialize template loader
        templateLoader = new TemplateLoader(getLogger());

        // Initialize world spawner
        worldSpawner = new WorldSpawner(getLogger());

        // Register commands
        getCommandRegistry().registerCommand(new WTCreateCommand(this));
        getCommandRegistry().registerCommand(new WTListCommand(this));
        getCommandRegistry().registerCommand(new WTReloadCommand(this));

        getLogger().atInfo().log("WorldTemplates: Setup complete");
    }

    @Override
    protected void start() {
        getLogger().atInfo().log("WorldTemplates: Starting...");

        // Load all templates
        templateLoader.loadAll();

        // Register event listener for auto-applying templates to matching worlds
        getEventRegistry().registerGlobal(AddWorldEvent.class, this::onWorldAdded);

        // Register player ready handler for lobby teleportation
        if (templateLoader.hasTemplate(LOBBY_TEMPLATE)) {
            getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
            getLogger().atInfo().log("Lobby template found - players will be teleported to lobby when ready");
        }

        getLogger().atInfo().log("WorldTemplates: Started");
    }

    /**
     * Handle PlayerReadyEvent to teleport players to the lobby.
     * This fires after the player is fully ready in the world.
     * Note: PlayerReadyEvent fires on Scheduler thread, so store access must be on world thread.
     */
    private void onPlayerReady(@Nonnull PlayerReadyEvent event) {
        com.hypixel.hytale.server.core.entity.entities.Player player = event.getPlayer();
        World currentWorld = player.getWorld();

        // Only teleport if in the default world (not returning from instances)
        if (currentWorld == null || !currentWorld.getName().equals("default")) {
            return;
        }

        Ref<EntityStore> entityRef = event.getPlayerRef();
        if (entityRef == null || !entityRef.isValid()) {
            getLogger().atWarning().log("Player has invalid entity ref at ready time");
            return;
        }

        // Must access store on world thread - schedule the entire operation
        currentWorld.execute(() -> {
            // Get username from PlayerRef component (now on world thread)
            Store<EntityStore> store = entityRef.getStore();
            PlayerRef playerRefComponent = store.getComponent(entityRef, PlayerRef.getComponentType());
            String username = playerRefComponent != null ? playerRefComponent.getUsername() : "Unknown";

            getLogger().atInfo().log("Player %s ready in default world, will teleport to lobby after delay...", username);

            // Delay teleport to let client fade-in complete (prevents "Cannot start fade out" crash)
            // Use 2 second delay to ensure fade animation completes
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                // Get or create the lobby instance, then teleport
                getOrCreateLobby(currentWorld).thenAccept(lobby -> {
                    if (lobby == null) {
                        getLogger().atWarning().log("Failed to get/create lobby for player %s", username);
                        return;
                    }

                    // Teleport player to lobby on the world thread
                    currentWorld.execute(() -> {
                        // Check if entity ref is still valid
                        if (!entityRef.isValid()) {
                            getLogger().atWarning().log("Player %s entity ref became invalid before teleport", username);
                            return;
                        }

                        Store<EntityStore> teleportStore = entityRef.getStore();
                        TransformComponent transform = teleportStore.getComponent(entityRef, TransformComponent.getComponentType());
                        if (transform == null) {
                            getLogger().atWarning().log("Player %s has no transform component", username);
                            return;
                        }

                        Transform returnTransform = new Transform(
                            transform.getPosition().clone(),
                            transform.getRotation().clone()
                        );

                        InstancesPlugin.teleportPlayerToInstance(entityRef, teleportStore, lobby, returnTransform);
                        getLogger().atInfo().log("Teleported %s to lobby", username);
                    });
                });
            }, 2000, TimeUnit.MILLISECONDS);
        });
    }

    /**
     * Get or create the shared lobby instance.
     */
    @Nonnull
    private synchronized CompletableFuture<World> getOrCreateLobby(@Nonnull World originWorld) {
        // Return existing lobby if alive
        if (lobbyWorld != null && lobbyWorld.isAlive()) {
            return CompletableFuture.completedFuture(lobbyWorld);
        }

        // Return in-progress creation if one is running
        if (lobbyCreationFuture != null && !lobbyCreationFuture.isDone()) {
            return lobbyCreationFuture;
        }

        // Create new lobby
        WorldTemplate template = templateLoader.getTemplate(LOBBY_TEMPLATE);
        if (template == null) {
            return CompletableFuture.completedFuture(null);
        }

        getLogger().atInfo().log("Creating lobby instance...");

        // Get player's position for return transform (not really used since lobby persists)
        Transform returnTransform = new Transform(
            new Vector3d(0, 65, 0),
            new Vector3f(0, 0, 0)
        );

        lobbyCreationFuture = worldSpawner.spawnInstance(template, originWorld, returnTransform)
            .thenApply(world -> {
                lobbyWorld = world;
                getLogger().atInfo().log("Lobby instance created: %s", world.getName());
                return world;
            });

        return lobbyCreationFuture;
    }

    /**
     * Apply a template to a world (shared logic for startup and event handling).
     */
    private void applyTemplateToWorld(@Nonnull World world, @Nonnull WorldTemplate template) {
        String worldName = world.getName();

        // Mark as initialized before applying to prevent race conditions
        initializedWorlds.add(worldName);

        // Apply template settings and paste prefabs
        worldSpawner.applyToExistingWorld(world, template)
            .whenComplete((result, error) -> {
                if (error != null) {
                    getLogger().atWarning().log("Failed to apply template to '%s': %s",
                        worldName, error.getMessage());
                    // Remove from initialized so it can be retried
                    initializedWorlds.remove(worldName);
                } else {
                    getLogger().atInfo().log("Successfully applied template to world '%s'", worldName);
                }
            });
    }

    /**
     * Handle AddWorldEvent to auto-apply templates to matching worlds.
     * This enables dynamically created worlds to be configured from a template.
     */
    private void onWorldAdded(@Nonnull AddWorldEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();

        // Skip if already initialized (prevents duplicate prefab pasting)
        if (initializedWorlds.contains(worldName)) {
            return;
        }

        // Check if there's a template matching this world name
        WorldTemplate template = templateLoader.getTemplate(worldName);
        if (template == null) {
            return;
        }

        getLogger().atInfo().log("Auto-applying template '%s' to new world '%s'",
            template.getDisplayName(), worldName);

        applyTemplateToWorld(world, template);
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("WorldTemplates: Shutting down...");
        // Nothing to clean up
    }

    @Nonnull
    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    @Nonnull
    public WorldSpawner getWorldSpawner() {
        return worldSpawner;
    }
}
