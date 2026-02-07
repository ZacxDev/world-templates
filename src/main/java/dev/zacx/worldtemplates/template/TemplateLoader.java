package dev.zacx.worldtemplates.template;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads WorldTemplate objects from YAML files in Server/WorldTemplates/.
 */
public class TemplateLoader {

    // Server directory (relative to working dir, which is Server/)
    private static final String SERVER_TEMPLATES_DIR = "WorldTemplates";
    // Asset pack directory (relative to pack root)
    private static final String ASSET_TEMPLATES_DIR = "Server/WorldTemplates";
    private static final String TEMPLATE_EXTENSION = ".template.yaml";

    private final HytaleLogger logger;
    private final Map<String, WorldTemplate> templates;
    private final Yaml yaml;

    public TemplateLoader(@Nonnull HytaleLogger logger) {
        this.logger = logger;
        this.templates = new HashMap<>();

        // Configure SnakeYAML with safe loader options
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        this.yaml = new Yaml(new Constructor(WorldTemplate.class, options));
    }

    /**
     * Load all templates from server directory and asset packs.
     */
    public void loadAll() {
        templates.clear();

        // First, check the server's working directory (WorldTemplates/)
        Path serverTemplatesPath = Path.of(SERVER_TEMPLATES_DIR);
        if (Files.isDirectory(serverTemplatesPath)) {
            logger.atInfo().log("Loading templates from server directory: %s", serverTemplatesPath.toAbsolutePath());
            loadFromDirectory(serverTemplatesPath, "Server");
        } else {
            logger.atInfo().log("No server templates directory found at: %s", serverTemplatesPath.toAbsolutePath());
        }

        // Also check asset packs (for bundled templates)
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            Path templatesPath = pack.getRoot().resolve(ASSET_TEMPLATES_DIR);
            if (Files.isDirectory(templatesPath)) {
                loadFromDirectory(templatesPath, pack.getName());
            }
        }

        logger.atInfo().log("Loaded %d world template(s)", templates.size());
    }

    /**
     * Load templates from a specific directory.
     */
    private void loadFromDirectory(@Nonnull Path directory, @Nonnull String packName) {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(p -> p.toString().endsWith(TEMPLATE_EXTENSION))
                .forEach(path -> loadTemplate(path, packName));
        } catch (IOException e) {
            logger.atWarning().log("Failed to list templates in %s: %s", directory, e.getMessage());
        }
    }

    /**
     * Load a single template from a YAML file.
     */
    private void loadTemplate(@Nonnull Path path, @Nonnull String packName) {
        String fileName = path.getFileName().toString();
        String templateName = fileName.replace(TEMPLATE_EXTENSION, "");

        try (InputStream is = Files.newInputStream(path)) {
            WorldTemplate template = yaml.load(is);
            if (template == null) {
                logger.atWarning().log("Empty template file: %s", path);
                return;
            }

            // Set name from filename if not specified in YAML
            if (template.getName().equals("unnamed")) {
                template.setName(templateName);
            }

            templates.put(templateName.toLowerCase(), template);
            logger.atInfo().log("Loaded template '%s' from %s (%d prefabs)",
                template.getDisplayName(), packName, template.getPrefabs().size());

        } catch (Exception e) {
            logger.atWarning().log("Failed to load template %s: %s", path, e.getMessage());
        }
    }

    /**
     * Get a template by name (case-insensitive).
     */
    @Nullable
    public WorldTemplate getTemplate(@Nonnull String name) {
        return templates.get(name.toLowerCase());
    }

    /**
     * Get all loaded template names.
     */
    @Nonnull
    public Iterable<String> getTemplateNames() {
        return templates.keySet();
    }

    /**
     * Get all loaded templates.
     */
    @Nonnull
    public Map<String, WorldTemplate> getTemplates() {
        return templates;
    }

    /**
     * Check if a template exists.
     */
    public boolean hasTemplate(@Nonnull String name) {
        return templates.containsKey(name.toLowerCase());
    }

    /**
     * Reload all templates.
     */
    public void reload() {
        loadAll();
    }
}
