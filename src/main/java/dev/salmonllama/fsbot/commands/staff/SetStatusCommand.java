package dev.salmonllama.fsbot.commands.staff;

import com.vdurmont.emoji.EmojiParser;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class SetStatusCommand extends Command {
    @Override public String name() { return "Set Status"; }
    @Override public String description() { return "Updates the bot's current status"; }
    @Override public String usage()  { return "updatestatus <String status>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("setstatus", "status"); }

    @Override
    public void onCommand(CommandContext ctx) {
        String[] args = ctx.getArgs();

        // argument checking
        if (args.length == 0) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.YELLOW)
                    .addField("WARNING", "Not enough arguments supplied");
            ctx.getChannel().sendMessage(embed);
            return;
        }

        String status = String.join(" ", args);

        ctx.getApi().updateActivity(status);

        ctx.getMessage().addReaction(EmojiParser.parseToUnicode(":white_check_mark:"));
    }
}
