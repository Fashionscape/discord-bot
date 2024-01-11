/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.commands.developer;

import dev.salmonllama.fsbot.database.controllers.StaticPermissionController;
import dev.salmonllama.fsbot.database.models.StaticPermission;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PermissionCommand extends Command {
    @Override public String name() { return "Permission"; }
    @Override public String description() { return "Manages a user's static permissions"; }
    @Override public String usage() { return "permission <list|add|remove> <@user-mention> <keyword>"; }
    @Override public CommandCategory category() { return CommandCategory.DEVELOPER; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.OWNER); }
    @Override public List<String> aliases() { return Arrays.asList("permission", "permissions", "perm", "perms"); }

    @Override
    public void onCommand(CommandContext ctx) {
        String[] args = ctx.getArgs();

        switch (args[0]) {
            case "list" ->
                // List all the static permissions
                    list(ctx);
            case "add" ->
                // Add a static permission to the mentioned user, if any
                    add(ctx);
            case "remove" ->
                // Remove a static permission from the mentioned user, if any
                    remove(ctx);
            default -> ctx.reply("Incorrect usage, check ~help permission");
        }
    }

    private void list(CommandContext ctx) {
        if (!ctx.getMessage().getMentionedUsers().isEmpty()) {
            User mentionedUser = ctx.getMessage().getMentionedUsers().get(0);

            StaticPermissionController
                    .getByUser(mentionedUser.getIdAsString())
                    .thenAcceptAsync(possiblePerms -> possiblePerms.ifPresentOrElse(perms -> {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(String.format("Permissions for %s", mentionedUser.getName()))
                        .setFooter(String.format("User has %s total static permissions", perms.size()))
                        .setColor(Color.GREEN);

                perms.forEach(perm -> embed.addField(perm.getUserId(), perm.getPermission()));

                ctx.reply(embed);
            }, () -> {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("No Permissions Found")
                        .setDescription(String.format("User %s has no static permissions", mentionedUser.getName()));

                ctx.reply(embed);
            }));
        } else { // Fetch and display all user permissions
            StaticPermissionController
                    .getAll()
                    .thenAcceptAsync(possiblePerms -> possiblePerms.ifPresentOrElse(perms -> {
                List<EmbedBuilder> embeds = new ArrayList<>();

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("All static permissions")
                        .setFooter(String.format("Found %s total static permissions", perms.size()))
                        .setColor(Color.green);
                embeds.add(embed);


                List<String> permKeywords = new ArrayList<>();
                perms.forEach(perm -> {
                    if (!permKeywords.contains(perm.getPermission())) {
                        permKeywords.add(perm.getPermission());
                    }
                });

                permKeywords.forEach(keyword -> {
                    EmbedBuilder e = new EmbedBuilder().setTitle(keyword);
                    StringBuilder text = new StringBuilder().append("```md").append(System.lineSeparator());

                    StaticPermissionController.getByPermission(keyword).thenAcceptAsync(
                                    (optSp) -> optSp.ifPresent(
                                            keywordPerms -> keywordPerms.forEach(
                                                    perm -> ctx.getApi().getUserById(perm.getUserId()).thenAcceptAsync(
                                                            (discordUser) -> text.append(perm.getUserId())
                                                                    .append(" - ")
                                                                    .append(discordUser.getName())
                                                                    .append(System.lineSeparator())
                                                    ).join()
                                            )
                                    )
                    ).join();
                    text.append("```");
                    e.setDescription(text.toString());
                    embeds.add(e);
                });
                EmbedBuilder[] embedArray = embeds.toArray(new EmbedBuilder[0]);
                ctx.reply(embedArray);
            }, () -> {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("No Permissions Found")
                        .setDescription("No permissions have been added.");

                ctx.reply(embed);
            }));
        }
    }

    private void add(CommandContext ctx) {
        String userId;
        if (ctx.getMessage().getMentionedUsers().isEmpty()) {
            // If no mentioned users, then the user ID = args[1]
            userId = ctx.getArgs()[1];
        } else {
            userId = ctx.getMessage().getMentionedUsers().get(0).getIdAsString();
        }

        StaticPermission perm = new StaticPermission.StaticPermissionBuilder(userId).setPermission(ctx.getArgs()[2]).build();

        StaticPermissionController.insert(perm).thenAcceptAsync((Void) -> {
            EmbedBuilder response = new EmbedBuilder()
                    .setTitle("Permissions Added")
                    .addField("User:", userId)
                    .addField("Permission:", ctx.getArgs()[2]);
            ctx.reply(response);
        });
    }

    private void remove(CommandContext ctx) { // TODO: Remove is not functional
        String userId = ctx.getArgs()[1];

        StaticPermissionController.delete(userId, ctx.getArgs()[2]).thenAcceptAsync((Void) -> {
            EmbedBuilder response = new EmbedBuilder()
                    .setTitle("Permissions Removed")
                    .addField("User:", userId)
                    .addField("Permission:", ctx.getArgs()[2]);
            ctx.reply(response);
        }).exceptionally(ExceptionLogger.get());
    }
}
