package dev.zacx.worldtemplates.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.zacx.worldtemplates.WorldTemplatesPlugin;
import dev.zacx.worldtemplates.template.WorldTemplate;

import javax.annotation.Nonnull;
import java.awt.Color;

/**
 * Command to create a world instance from a template.
 * Usage: /wt:create <template_name>
 */
public class WTCreateCommand extends AbstractPlayerCommand {

    private final WorldTemplatesPlugin plugin;

    @Nonnull
    private final RequiredArg<String> templateNameArg = this.withRequiredArg(
        "template", "Name of the template to use", ArgTypes.STRING
    );

    public WTCreateCommand(@Nonnull WorldTemplatesPlugin plugin) {
        super("wt:create", "Create a world from a template and teleport into it");
        this.plugin = plugin;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        String name = templateNameArg.get(context);

        WorldTemplate template = plugin.getTemplateLoader().getTemplate(name);
        if (template == null) {
            context.sendMessage(Message.raw("Template not found: " + name).color(Color.RED));
            context.sendMessage(Message.raw("Use /wt:list to see available templates").color(Color.GRAY));
            return;
        }

        context.sendMessage(Message.raw("Creating world from template: " + template.getDisplayName()).color(Color.GREEN));

        plugin.getWorldSpawner().spawnAndTeleport(template, playerRef, world, store, entityRef)
            .thenAccept(instanceWorld -> {
                context.sendMessage(Message.raw("Teleported to " + template.getDisplayName()).color(Color.GREEN));
                context.sendMessage(Message.raw("Use /instance:exit to return").color(Color.GRAY));
            })
            .exceptionally(ex -> {
                context.sendMessage(Message.raw("Failed to create world: " + ex.getMessage()).color(Color.RED));
                plugin.getLogger().atWarning().log("Failed to create world from template: %s", ex.getMessage());
                return null;
            });
    }
}
