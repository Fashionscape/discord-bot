package dev.salmonllama.fsbot.discord.commands.rs3;

import dev.salmonllama.fsbot.discord.commands.SlashCommand;
import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionClient;
import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionItem;
import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionResult;
import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionSlotRs3;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class Rs3SearchSlashCommand implements SlashCommand {

    private static final Logger logger = LoggerFactory.getLogger(Rs3SearchSlashCommand.class);
    private static final Color DEFAULT_COLOR = new Color(0x3B88C3);

    private final ScapeFashionClient scapeFashionClient;

    public Rs3SearchSlashCommand(ScapeFashionClient scapeFashionClient) {
        this.scapeFashionClient = scapeFashionClient;
    }

    @Override
    public String getName() {
        return "rs3search";
    }

    @Override
    public String getDescription() {
        return "Search RuneScape 3 items by color or name on rune.scape.fashion";
    }

    @Override
    public SlashCommandData buildCommandData() {
        OptionData queryOption = new OptionData(OptionType.STRING, "query", "Item name or hex color code (e.g. #FF0000)", true);

        OptionData slotOption = new OptionData(OptionType.STRING, "slot", "Optional equipment slot filter", false);
        for (ScapeFashionSlotRs3 slot : ScapeFashionSlotRs3.values()) {
            slotOption.addChoice(slot.getValue(), slot.getValue());
        }

        return Commands.slash(getName(), getDescription())
                .addOptions(queryOption, slotOption);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String query = event.getOption("query", OptionMapping::getAsString);
        if (query == null || query.isBlank()) {
            event.reply("Please specify a search query (item name or hex color).").setEphemeral(true).queue();
            return;
        }

        String slotStr = Optional.ofNullable(event.getOption("slot")).map(OptionMapping::getAsString).orElse(null);
        Optional<ScapeFashionSlotRs3> slotOpt = slotStr != null ? parseSlot(slotStr) : Optional.empty();

        event.deferReply().queue(hook -> {
            CompletableFuture<ScapeFashionResult> future;
            if (query.trim().startsWith("#")) {
                future = scapeFashionClient.searchRs3Color(query.trim(), slotOpt);
            } else {
                future = scapeFashionClient.searchRs3Item(query.trim(), slotOpt);
            }

            future.thenAccept(result -> {
                if (!result.hasItems()) {
                    hook.sendMessage("No RuneScape 3 items found matching `" + query + "` on rune.scape.fashion.")
                            .queue();
                    return;
                }

                EmbedBuilder embed = buildResultEmbed(result, query);
                hook.sendMessageEmbeds(embed.build()).queue();
            }).exceptionally(ex -> {
                logger.error("Failed to query ScapeFashion RS3 search", ex);
                hook.sendMessage("An error occurred while querying rune.scape.fashion: " + ex.getMessage()).queue();
                return null;
            });
        });
    }

    private Optional<ScapeFashionSlotRs3> parseSlot(String s) {
        for (ScapeFashionSlotRs3 slot : ScapeFashionSlotRs3.values()) {
            if (slot.getValue().equalsIgnoreCase(s) || slot.name().equalsIgnoreCase(s)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    private EmbedBuilder buildResultEmbed(ScapeFashionResult result, String query) {
        ScapeFashionItem bestMatch = result.getBestMatch().orElseThrow();

        Color embedColor = DEFAULT_COLOR;
        if (bestMatch.getColors() != null && bestMatch.getColors().length > 0) {
            try {
                embedColor = Color.decode(bestMatch.getColors()[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("RS3 Best Match: " + bestMatch.getName(), result.getLink())
                .setColor(embedColor)
                .addField("Slot", bestMatch.getSlot() != null ? bestMatch.getSlot() : "Unknown", true)
                .addField("Match", String.format("%.1f%%", bestMatch.getMatch() * 100), true);

        if (bestMatch.getColors() != null && bestMatch.getColors().length > 0) {
            embed.addField("Colors", String.join(", ", bestMatch.getColors()), true);
        }

        if (bestMatch.getImages() != null && bestMatch.getImages().getDetail() != null) {
            embed.setThumbnail(bestMatch.getImages().getDetail());
        }

        if (bestMatch.getWiki() != null && bestMatch.getWiki().getLink() != null) {
            embed.setAuthor("RS3 Wiki: " + bestMatch.getName(), bestMatch.getWiki().getLink());
        }

        List<ScapeFashionItem> allItems = result.getItems();
        if (allItems.size() > 1) {
            StringBuilder others = new StringBuilder();
            int count = Math.min(allItems.size(), 6);
            for (int i = 1; i < count; i++) {
                ScapeFashionItem item = allItems.get(i);
                others.append("• ").append(item.getName()).append(" (").append(String.format("%.1f%%", item.getMatch() * 100)).append(")\n");
            }
            if (allItems.size() > 6) {
                others.append("... and ").append(allItems.size() - 6).append(" more [view on web](").append(result.getLink()).append(")");
            }
            embed.addField("Other Matches", others.toString(), false);
        }

        embed.setFooter("FashionScape Bot • rune.scape.fashion query: " + query);
        return embed;
    }
}
