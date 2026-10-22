/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.commands.general;

import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class HelpCommand extends Command {
    @Override public String name() { return "Help"; }
    @Override public String description() { return "Shows all commands, or a specific command's information"; }
    @Override public String usage() { return "help [String command]"; }
    @Override public CommandCategory category() { return CommandCategory.GENERAL; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return Arrays.asList("help", "h"); }

    private final Guthix guthix;

    public HelpCommand(Guthix guthix) {
        this.guthix = guthix;
    }

    @Override
    public void onCommand(CommandContext ctx) {
        String[] args = ctx.getArgs();
        TextChannel channel = ctx.getChannel();

        String prefix = "~";

        if (args.length == 0) {
            channel.sendMessage(buildCategoryHelp(prefix));
        }
        if (args.length == 1) {
            if (isCategory(args[0])) {
                channel.sendMessage(buildTargetCategory(prefix, args[0]));
            }
            else if (isCommand(args[0])) {
                channel.sendMessage(buildCommandHelp(channel, prefix, args[0]));
            }
            else {
                channel.sendMessage("No match found");
            }
        }
    }

    private EmbedBuilder buildCategoryHelp(String prefix) {
        StringBuilder builder = new StringBuilder();
        builder.append("```yml");
        List<String> categories = new ArrayList<>();

        for (Command cmd : guthix.listCommands()) {
            String category = cmd.category().getCategory();
            if (!categories.contains(category)) {
                categories.add(category);

                builder.append("\n -").append(category);
            }
        }

        builder.append("```");

        return new EmbedBuilder()
                .setTitle("Command Categories")
                .setColor(Color.GREEN)
                .addField("Prefix", String.format("```%s```", prefix))
                .addField("Categories", builder.toString())
                .setFooter(String.format("Use `%shelp <category> to see commands in the category", prefix));
    }

    private EmbedBuilder buildTargetCategory(String prefix, String category) {
        StringBuilder builder = new StringBuilder().append("```yml");

        for (Command cmd : guthix.listCommands()) {
            String cat = cmd.category().getCategory().toLowerCase();
            if (cat.equals(category.toLowerCase())) {
                builder.append("\n- ").append(cmd.aliases().get(0));
            }
        }

        builder.append("```");

        return new EmbedBuilder()
                .setTitle(String.format("%s Commands", category))
                .setColor(Color.GREEN)
                .addField("Prefix", String.format("```%s```", prefix))
                .addField("Commands:", builder.toString())
                .setFooter(String.format("Use `%shelp <command> to see command info", prefix));
    }

    private EmbedBuilder buildCommandHelp(TextChannel channel, String prefix, String command) {

        String targetedCommand = null;
        Collection<String> targetedAliases = null;
        String targetedDescription = null;
        String targetedUsage = null;
        CommandPermission targetedPermissions = null;

        for (Command cmd : guthix.listCommands()) {
            for (String alias : cmd.aliases()) {
                if (alias.equals(command)) {
                    targetedCommand = cmd.name();
                    targetedAliases = cmd.aliases();
                    targetedDescription = cmd.description();
                    targetedUsage = cmd.usage();
                    targetedPermissions = cmd.permission();
                }
            }
        }

        if (targetedCommand == null) {
            return new EmbedBuilder()
                    .setColor(Color.RED)
                    .addField("ERROR:", "That command does not exist");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("```yml\n");

        if (targetedAliases.size() != 0) {
            builder.append(String.format("- Aliases: %s\n", String.join(", ", targetedAliases)));
        }
        if (!targetedDescription.equals("none")) {
            builder.append(String.format("- Description: %s\n", targetedDescription));
        }
        if (!targetedUsage.equals("")) {
            builder.append(String.format("- Usage: %s\n", targetedUsage));
        }
        builder.append(String.format("- Permissions: %s\n", targetedPermissions.toString()));

        builder.append("```");

        return new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle(String.format("%s%s", prefix, targetedCommand))
                .setDescription(builder.toString());
    }

    public boolean isCommand(String input) {
        for (Command cmd : guthix.listCommands()) {
            Collection<String> aliases = cmd.aliases();
            for (String alias : aliases) {
                if (alias.toLowerCase().equals(input.toLowerCase())) {
                    return  true;
                }
            }
        }

        return false;
    }

    public boolean isCategory(String input) {
        for (Command cmd : guthix.listCommands()) {
            String category = cmd.category().getCategory().toLowerCase();
            if (category.toLowerCase().equals(input.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
