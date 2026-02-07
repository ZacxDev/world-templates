package dev.zacx.worldtemplates.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import dev.zacx.worldtemplates.WorldTemplatesPlugin;
import dev.zacx.worldtemplates.template.WorldTemplate;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Command to list all available world templates.
 * Usage: /wt:list
 */
public class WTListCommand extends AbstractCommand {

    private final WorldTemplatesPlugin plugin;

    public WTListCommand(@Nonnull WorldTemplatesPlugin plugin) {
        super("wt:list", "List all available world templates");
        this.plugin = plugin;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        Map<String, WorldTemplate> templates = plugin.getTemplateLoader().getTemplates();

        if (templates.isEmpty()) {
            ctx.sendMessage(Message.raw("No world templates available.").color(Color.YELLOW));
            ctx.sendMessage(Message.raw("Add .template.yaml files to Server/WorldTemplates/").color(Color.GRAY));
            return CompletableFuture.completedFuture(null);
        }

        ctx.sendMessage(Message.raw("=== World Templates ===").color(Color.GREEN));

        for (Map.Entry<String, WorldTemplate> entry : templates.entrySet()) {
            WorldTemplate template = entry.getValue();
            String prefabCount = String.valueOf(template.getPrefabs().size());
            ctx.sendMessage(Message.raw(String.format(
                "  %s - %s (%s prefabs)",
                entry.getKey(),
                template.getDisplayName(),
                prefabCount
            )));
        }

        ctx.sendMessage(Message.raw(""));
        ctx.sendMessage(Message.raw("Use /wt:create <name> to create a world").color(Color.GRAY));

        return CompletableFuture.completedFuture(null);
    }
}
