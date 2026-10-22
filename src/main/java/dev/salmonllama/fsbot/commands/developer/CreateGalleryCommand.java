package dev.salmonllama.fsbot.commands.developer;

import com.vdurmont.emoji.EmojiParser;
import dev.salmonllama.fsbot.database.controllers.GalleryController;
import dev.salmonllama.fsbot.database.models.GalleryChannel;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class CreateGalleryCommand extends Command {
    @Override public String name() { return "Create Gallery"; }
    @Override public String description() { return "Creates a channel gallery, tracking any posted images"; }
    @Override public String usage() { return "creategallery <String tag>"; }
    @Override public CommandCategory category() { return CommandCategory.DEVELOPER; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "owner"); }
    @Override public List<String> aliases() { return Arrays.asList("creategallery", "addgallery", "newgallery"); }

    @Override
    public void onCommand(CommandContext ctx) {
        if (ctx.isPrivateMessage()) {
            ctx.reply("This command can only be used in a server!");
            return;
        }
        if (ctx.getArgs().length < 1) {
            ctx.reply("Args are incorrect");
            return;
        }

        String[] args = ctx.getArgs();
        // TODO: Check if the tag already exists
        // Check if the channel is already a registered gallery channel.
        // Create a gallery channel of the current channel.
        // Store the gallery channel in the database.

        String channelId = ctx.getChannel().getIdAsString();
        if (GalleryController.galleryExists(channelId).join()) { // This is a value that is needed immediately.
            ctx.reply("A gallery already exists in this channel, can not create a new one.");
            return;
        }

        String tag = args[0];

        String emoji;
        if (args.length == 2) {
            emoji = EmojiParser.parseToAliases(args[1]);
        } else {
            emoji = ":heartpulse:";
        }

        GalleryChannel.GalleryBuilder galleryBuilder = new GalleryChannel.GalleryBuilder();
        galleryBuilder.setChannelId(channelId);
        galleryBuilder.setTag(tag);
        galleryBuilder.setEmoji(emoji);

        ctx.getServer().ifPresent(server -> {
            galleryBuilder.setServerId(server.getIdAsString());
        });

        GalleryChannel gallery = galleryBuilder.build();
        GalleryController.insert(gallery).thenAcceptAsync((Void) -> {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.GREEN)
                    .addField("Success", "Gallery has been created:")
                    .addField("Channel Id:", gallery.getChannelId())
                    .addField("Tag:", tag)
                    .addField("Emoji:", EmojiParser.parseToUnicode(gallery.getEmoji()))
                    .addField("End:", String.format("This channel is now being tracked under: %s", tag));
            ctx.reply(embed);
        });
    }
}

