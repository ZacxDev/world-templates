package dev.zacx.worldtemplates;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import dev.zacx.worldtemplates.command.WTCreateCommand;
import dev.zacx.worldtemplates.command.WTListCommand;
import dev.zacx.worldtemplates.command.WTReloadCommand;
import dev.zacx.worldtemplates.template.TemplateLoader;
import dev.zacx.worldtemplates.template.WorldTemplate;
import dev.zacx.worldtemplates.world.WorldSpawner;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

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

    private TemplateLoader templateLoader;
    private WorldSpawner worldSpawner;

    // Track worlds that have been initialized to avoid duplicate prefab pasting
    private final Set<String> initializedWorlds = new HashSet<>();

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

        getLogger().atInfo().log("WorldTemplates: Started");
    }

    /**
     * Handle AddWorldEvent to auto-apply templates to matching worlds.
     * This enables the default world to be automatically configured from a template.
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
            getLogger().atInfo().log("No matching template for world '%s'", worldName);
            return;
        }

        getLogger().atInfo().log("Auto-applying template '%s' to world '%s'",
            template.getDisplayName(), worldName);

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
