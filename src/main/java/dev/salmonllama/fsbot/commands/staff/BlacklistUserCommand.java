package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.database.controllers.UserBlacklistController;
import dev.salmonllama.fsbot.database.models.UserBlacklist;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.Arrays;
import java.util.List;

public class BlacklistUserCommand extends Command {
    @Override public String name() { return "Blacklist User"; }
    @Override public String description() { return "Adds the user to the bot's blacklist, preventing them from using any commands or features"; }
    @Override public String usage() { return "blacklistuser <userId> <reason>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "admin"); }
    @Override public List<String> aliases() { return Arrays.asList("blacklistuser", "bluser", "sabusr"); }

    @Override
    public void onCommand(CommandContext ctx) {
        String[] args = ctx.getArgs();

        if (args.length < 1) {
            // Did it wrong
            return;
        }

        // If the user is on the blacklist, remove them, otherwise, add them with the reason.
        UserBlacklistController.get(args[0]).thenAcceptAsync(possibleBlacklist -> possibleBlacklist.ifPresentOrElse(blacklist -> {
            // Remove user from the blacklist
            UserBlacklistController.delete(blacklist).thenAcceptAsync((Void) -> {
                EmbedBuilder response = new EmbedBuilder()
                    .setTitle("Removed User from Blacklist")
                    .addField("User ID:", blacklist.getId())
                    .addField("Reason for Add", blacklist.getReason())
                    .addField("Added:", blacklist.getAdded().toString());

                ctx.reply(response);
            });
        }, () -> {
            // Add user to the blacklist, check args
            UserBlacklist.Builder blBuilder = new UserBlacklist.Builder(args[0]);
            EmbedBuilder response = new EmbedBuilder().setTitle("Added User to Blacklist").addField("User ID:", args[0]);

            if (args.length > 1) {
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                blBuilder.setReason(reason);
                response.addField("With reason:", reason);
            }

            UserBlacklistController.insert(blBuilder.build()).thenAcceptAsync((Void) -> ctx.reply(response));
        }));
    }
}
