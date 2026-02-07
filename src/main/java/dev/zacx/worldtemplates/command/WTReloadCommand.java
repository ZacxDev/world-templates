package dev.zacx.worldtemplates.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import dev.zacx.worldtemplates.WorldTemplatesPlugin;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.concurrent.CompletableFuture;

/**
 * Command to reload world templates from disk.
 * Usage: /wt:reload
 */
public class WTReloadCommand extends AbstractCommand {

    private final WorldTemplatesPlugin plugin;

    public WTReloadCommand(@Nonnull WorldTemplatesPlugin plugin) {
        super("wt:reload", "Reload world templates from disk");
        this.plugin = plugin;
    }

    @Override
    protected boolean canGeneratePermission() {
        return true;  // Requires permission (admin only)
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("Reloading world templates...").color(Color.YELLOW));

        try {
            plugin.getTemplateLoader().reload();
            int count = plugin.getTemplateLoader().getTemplates().size();
            ctx.sendMessage(Message.raw("Loaded " + count + " template(s)").color(Color.GREEN));
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Failed to reload: " + e.getMessage()).color(Color.RED));
        }

        return CompletableFuture.completedFuture(null);
    }
}
