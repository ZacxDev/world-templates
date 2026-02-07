package dev.zacx.worldtemplates;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.zacx.worldtemplates.command.WTCreateCommand;
import dev.zacx.worldtemplates.command.WTListCommand;
import dev.zacx.worldtemplates.command.WTReloadCommand;
import dev.zacx.worldtemplates.template.TemplateLoader;
import dev.zacx.worldtemplates.world.WorldSpawner;

import javax.annotation.Nonnull;

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

        getLogger().atInfo().log("WorldTemplates: Started");
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
