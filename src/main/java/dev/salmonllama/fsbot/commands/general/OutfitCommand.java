package dev.salmonllama.fsbot.commands.general;

import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class OutfitCommand extends Command {
    private final int MAX_OUTFITS = 5;
    private final Collection<String> NON_TAG_ALIASES = new ArrayList<>(Arrays.asList("outfit", "o"));

    @Override public String name() { return "Outfit"; }
    @Override public String description() { return "Generates a random image with the given tag. Use ~tags to see valid tags."; }
    @Override public String usage() { return "outfit <String tag>"; }
    @Override public CommandCategory category() { return CommandCategory.GENERAL; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return initAliases(); }

    @Override
    public void onCommand(CommandContext ctx) {
        // Parse command used for logic path
        // if "outfit" -> look for tags. if no tags -> show random
        // if not "outfit" -> match the command to existing tags -> look for numbers
        String command = ctx.getUsedAlias();
        String[] args = ctx.getArgs();

        OutfitController.getDistinctTags().thenAccept(tags -> {
            if (tags.contains(command)) {
                // args parsing and command logic with tag as caller
                handleTagCommand(command, args, ctx);
            } else if (NON_TAG_ALIASES.contains(command)) {
                // args parsing and command logic with name as caller
                handleNameCommand(command, args, ctx);
            }
        });
    }

    private void handleTagCommand(String command, String[] args, CommandContext ctx) {
        switch (args.length) {
            case 0:
                // Send one single random outfit of the given tag
                OutfitController.findRandomByTag(command).thenAccept(possibleOutfit -> {
                    possibleOutfit.ifPresent(outfit -> {
                        ctx.getApi().getUserById(outfit.getSubmitter()).thenAcceptAsync(user -> {
                            EmbedBuilder response = new EmbedBuilder()
                                    .setTitle(user.getDiscriminatedName())
                                    .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                                    .setImage(outfit.getLink())
                                    .setUrl(outfit.getLink());

                            if (!outfit.getMeta().equals("")) {
                                response.setDescription(outfit.getMeta());
                            }

                            ctx.reply(response);
                        });
                    });
                }).exceptionally(ExceptionLogger.get());
            case 1:
                // Send the given number of outfits, not to exceed 5 for ratelimit reasons
                if (isNumeric(args[0])) {
                    int num = Math.min(Integer.parseInt(args[0]), MAX_OUTFITS);

                    OutfitController.findRandomByTag(command, num).thenAccept(possibleOutfits -> {
                       possibleOutfits.ifPresent(outfits -> {
                               outfits.forEach(outfit -> {
                                   ctx.getApi().getUserById(outfit.getSubmitter()).thenAcceptAsync(user -> {
                                       EmbedBuilder response = new EmbedBuilder()
                                               .setTitle(user.getDiscriminatedName())
                                               .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                                               .setImage(outfit.getLink())
                                               .setUrl(outfit.getLink());

                                       if (!outfit.getMeta().equals("")) {
                                           response.setDescription(outfit.getMeta());
                                       }

                                       ctx.reply(response);
                                   });
                               });
                       });
                    });
                } else {
                    ctx.reply("Improper command usage");
                }
        }
    }

    private void handleNameCommand(String command, String[] args, CommandContext ctx) {
        switch (args.length) {
            case 0:
                // Send one truly random outfit
                OutfitController.findRandom().thenAccept(possibleOutfit -> {
                    possibleOutfit.ifPresent(outfit -> {
                        ctx.getApi().getUserById(outfit.getSubmitter()).thenAcceptAsync(user -> {
                            EmbedBuilder response = new EmbedBuilder()
                                    .setTitle(user.getDiscriminatedName())
                                    .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                                    .setImage(outfit.getLink())
                                    .setUrl(outfit.getLink());

                            if (!outfit.getMeta().equals("")) {
                                response.setDescription(outfit.getMeta());
                            }

                            ctx.reply(response);
                        });
                    });
                });
            case 1:
                // Send the given number of outfits, not to exceed 5 for ratelimit reasons
                if (isNumeric(args[0])) {
                    int num = Math.min(Integer.parseInt(args[0]), MAX_OUTFITS);

                    OutfitController.findRandom(num).thenAccept(possibleOutfits -> {
                        possibleOutfits.ifPresent(outfits -> {
                            outfits.forEach(outfit -> {
                                ctx.getApi().getUserById(outfit.getSubmitter()).thenAcceptAsync(user -> {
                                    EmbedBuilder response = new EmbedBuilder()
                                            .setTitle(user.getDiscriminatedName())
                                            .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                                            .setImage(outfit.getLink())
                                            .setUrl(outfit.getLink());

                                    if (!outfit.getMeta().equals("")) {
                                        response.setDescription(outfit.getMeta());
                                    }

                                    ctx.reply(response);
                                });
                            });
                        });
                    });
                } else {
                    // First arg is not a number, send an outfit of the given tag
                    String tag = args[0];
                    OutfitController.findRandomByTag(tag).thenAccept(possibleOutfit -> {
                        possibleOutfit.ifPresent(outfit -> {
                            ctx.getApi().getUserById(outfit.getSubmitter()).thenAcceptAsync(user -> {
                                EmbedBuilder response = new EmbedBuilder()
                                        .setTitle(user.getDiscriminatedName())
                                        .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                                        .setImage(outfit.getLink())
                                        .setUrl(outfit.getLink());

                                if (!outfit.getMeta().equals("")) {
                                    response.setDescription(outfit.getMeta());
                                }

                                ctx.reply(response);
                            });
                        });
                    });
                }
            case 2:
                // Send the given number of outfits of the given tag
                if (isNumeric(args[1])) {
                    int num = Math.min(Integer.parseInt(args[1]), MAX_OUTFITS);

                    OutfitController.findRandomByTag(args[0], num).thenAccept(possibleOutfits -> {
                        possibleOutfits.ifPresent(outfits -> {
                            outfits.forEach(outfit -> {
                                ctx.getApi().getUserById(outfit.getSubmitter()).thenAcceptAsync(user -> {
                                    EmbedBuilder response = new EmbedBuilder()
                                            .setTitle(user.getDiscriminatedName())
                                            .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                                            .setImage(outfit.getLink())
                                            .setUrl(outfit.getLink());

                                    if (!outfit.getMeta().equals("")) {
                                        response.setDescription(outfit.getMeta());
                                    }

                                    ctx.reply(response);
                                });
                            });
                        });
                    });
                }
        }
    }

    private List<String> initAliases() {
        List<String> aliases = OutfitController.getDistinctTags().join();
        aliases.addAll(NON_TAG_ALIASES);
        return aliases;
    }

    private boolean isNumeric(String input) { // If this is needed elsewhere, it will be moved to a public utility
        if (input == null) {
            return false;
        }

        try {
            Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
