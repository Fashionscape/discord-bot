package dev.salmonllama.fsbot.commands.general;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import dev.salmonllama.fsbot.database.controllers.GalleryController;
import dev.salmonllama.fsbot.database.models.GalleryChannel;
import org.javacord.api.util.logging.ExceptionLogger;

public class ShowGalleriesCommand extends Command {
    @Override public String name() { return "Show Galleries"; }
    @Override public String description() { return "Shows registered gallery channels in the current server"; }
    @Override public String usage() { return "showgalleries"; }
    @Override public CommandCategory category() { return CommandCategory.GENERAL; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return Arrays.asList("showgalleries", "listgalleries"); }

    @Override public void onCommand(CommandContext ctx) {
        if (ctx.isPrivateMessage()) {
            ctx.reply("This command can only be used within a server");
            return;
        }

        ctx.getServer().ifPresent(server -> {
            GalleryController.getGalleriesByServer(server.getIdAsString())
                    .exceptionally(ExceptionLogger.get())
                    .thenAccept(galleries -> {
                ctx.reply(galleryEmbed(galleries, server));
            });
        });
    }

    EmbedBuilder galleryEmbed(Collection<GalleryChannel> galleries, Server server) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Server Gallery Channels");

        Collection<String> mentionTags = galleries.stream().map(
                galleryChannel -> server.getChannelById(
                        galleryChannel.getChannelId()).get() // Can call get safely -> The channel is not stored if it is not a valid ServerTextChannel
                        .asServerTextChannel().get()) // Same as above
                .map(ServerTextChannel::getMentionTag).collect(Collectors.toList());

        Collection<String> tags = galleries.stream().map(GalleryChannel::getTag).collect(Collectors.toList());

        embed.addField("Channels", String.join("\n", mentionTags), true);
        embed.addField("Tags", String.join("\n", tags), true);

        return embed;
    }
}
