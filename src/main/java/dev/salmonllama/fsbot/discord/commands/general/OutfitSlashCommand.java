package dev.salmonllama.fsbot.discord.commands.general;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.discord.commands.SlashCommand;
import dev.salmonllama.fsbot.discord.components.ButtonHandler;
import dev.salmonllama.fsbot.services.AuditService;
import dev.salmonllama.fsbot.services.OutfitService;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.Instant;
import java.util.Optional;

@Component
public class OutfitSlashCommand implements SlashCommand, ButtonHandler {

    private static final Logger logger = LoggerFactory.getLogger(OutfitSlashCommand.class);

    private static final String BUTTON_PREFIX = "outfit:page:";
    private static final int PAGE_SIZE = 5;
    private static final Color EMBED_COLOR = new Color(0x3B88C3);

    private final OutfitService outfitService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public OutfitSlashCommand(OutfitService outfitService, PermissionService permissionService, AuditService auditService) {
        this.outfitService = outfitService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Override
    public String getName() {
        return "outfit";
    }

    @Override
    public String getDescription() {
        return "Search, view, and manage FashionScape outfits";
    }

    @Override
    public SlashCommandData buildCommandData() {
        SubcommandData randomSub = new SubcommandData("random", "Show a random outfit")
                .addOption(OptionType.STRING, "tag", "Filter by tag", false)
                .addOption(OptionType.USER, "user", "Filter by submitter", false);

        SubcommandData infoSub = new SubcommandData("info", "View details about an outfit")
                .addOption(OptionType.STRING, "id", "Outfit ID", true);

        SubcommandData searchSub = new SubcommandData("search", "Search outfits")
                .addOption(OptionType.STRING, "tag", "Filter by tag", false)
                .addOption(OptionType.USER, "user", "Filter by submitter", false)
                .addOption(OptionType.INTEGER, "page", "Page number (defaults to 1)", false);

        SubcommandData deleteSub = new SubcommandData("delete", "Delete an outfit (Submitter or Staff only)")
                .addOption(OptionType.STRING, "id", "Outfit ID to delete", true);

        SubcommandData restoreSub = new SubcommandData("restore", "Restore a deleted outfit (Staff only)")
                .addOption(OptionType.STRING, "id", "Outfit ID to restore", true);

        return Commands.slash(getName(), getDescription())
                .addSubcommands(randomSub, infoSub, searchSub, deleteSub, restoreSub);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("Please specify a subcommand: `random`, `info`, `search`, `delete`, or `restore`.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        switch (subcommand) {
            case "random" -> handleRandom(event);
            case "info" -> handleInfo(event);
            case "search" -> handleSearch(event);
            case "delete" -> handleDelete(event);
            case "restore" -> handleRestore(event);
            default -> event.reply("Unknown subcommand: " + subcommand).setEphemeral(true).queue();
        }
    }

    private void handleRandom(SlashCommandInteractionEvent event) {
        String tag = Optional.ofNullable(event.getOption("tag")).map(OptionMapping::getAsString).orElse(null);
        User user = Optional.ofNullable(event.getOption("user")).map(OptionMapping::getAsUser).orElse(null);
        String submitterId = user != null ? user.getId() : null;

        Optional<OutfitEntity> outfitOpt = outfitService.getRandomOutfit(tag, submitterId);

        if (outfitOpt.isEmpty()) {
            StringBuilder msg = new StringBuilder("No outfits found");
            if (tag != null) {
                msg.append(" with tag `").append(tag).append("`");
            }
            if (user != null) {
                msg.append(" submitted by ").append(user.getAsMention());
            }
            msg.append(".");
            event.reply(msg.toString()).setEphemeral(true).queue();
            return;
        }

        OutfitEntity outfit = outfitOpt.get();
        EmbedBuilder embed = buildOutfitEmbed(outfit);

        Button upvoteBtn = Button.primary("gallery:upvote:" + outfit.getId(), "Upvote (" + outfit.getDisplayCount() + ")");
        event.replyEmbeds(embed.build())
                .setActionRow(upvoteBtn)
                .queue();
    }

    /**
     * Reads the "id" option. Outfit IDs are full UUIDs that people copy out of embeds, so this drops
     * surrounding whitespace and the backticks the embeds show them in.
     */
    private static String outfitIdOption(SlashCommandInteractionEvent event) {
        String id = event.getOption("id", OptionMapping::getAsString);
        return id == null ? null : id.strip().replace("`", "");
    }

    private void handleInfo(SlashCommandInteractionEvent event) {
        String id = outfitIdOption(event);
        if (id == null || id.isBlank()) {
            event.reply("You must supply an outfit ID.").setEphemeral(true).queue();
            return;
        }

        Optional<OutfitEntity> outfitOpt = outfitService.findById(id);
        if (outfitOpt.isEmpty()) {
            event.reply("Outfit `" + id + "` not found.").setEphemeral(true).queue();
            return;
        }

        OutfitEntity outfit = outfitOpt.get();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Outfit Details: " + outfit.getId())
                .setImage(outfit.getLink())
                .setColor(EMBED_COLOR)
                .addField("Submitter", "<@" + outfit.getSubmitter() + "> (" + outfit.getDiscordName() + ")", true)
                .addField("Tag", outfit.getTag() != null ? outfit.getTag() : "None", true)
                .addField("Upvotes", String.valueOf(outfit.getDisplayCount()), true)
                .addField("Status", outfit.isDeleted() ? "🗑️ Deleted" : "✅ Active", true)
                .addField("Featured", outfit.isFeatured() ? "⭐ Yes" : "No", true)
                .addField("Created", outfit.getCreated() != null ? outfit.getCreated().toString() : "Unknown", true)
                .setFooter("Outfit ID: " + outfit.getId());

        if (outfit.getMeta() != null && !outfit.getMeta().isBlank()) {
            embed.setDescription(outfit.getMeta());
        }

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSearch(SlashCommandInteractionEvent event) {
        String tag = Optional.ofNullable(event.getOption("tag")).map(OptionMapping::getAsString).orElse(null);
        User user = Optional.ofNullable(event.getOption("user")).map(OptionMapping::getAsUser).orElse(null);
        String submitterId = user != null ? user.getId() : null;

        int pageNum = Optional.ofNullable(event.getOption("page"))
                .map(OptionMapping::getAsInt)
                .filter(p -> p >= 1)
                .orElse(1);

        PageRequest pageRequest = PageRequest.of(pageNum - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "created"));
        Page<OutfitEntity> results = outfitService.search(tag, submitterId, pageRequest);

        if (results.isEmpty()) {
            event.reply("No outfits matched your search criteria.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = buildSearchEmbed(results, pageNum, tag, submitterId);
        ActionRow actionRow = buildSearchActionRow(results, pageNum, tag, submitterId);

        event.replyEmbeds(embed.build())
                .setComponents(actionRow)
                .queue();
    }

    private void handleDelete(SlashCommandInteractionEvent event) {
        String id = outfitIdOption(event);
        if (id == null || id.isBlank()) {
            event.reply("You must provide an outfit ID.").setEphemeral(true).queue();
            return;
        }

        Optional<OutfitEntity> outfitOpt = outfitService.findById(id);
        if (outfitOpt.isEmpty()) {
            event.reply("Outfit `" + id + "` not found.").setEphemeral(true).queue();
            return;
        }

        OutfitEntity outfit = outfitOpt.get();
        if (outfit.isDeleted()) {
            event.reply("Outfit `" + id + "` is already deleted.").setEphemeral(true).queue();
            return;
        }

        String userId = event.getUser().getId();
        Member member = event.getMember();

        if (!permissionService.canModerateOutfit(userId, member, outfit)) {
            event.reply("You do not have permission to delete this outfit. Only the submitter, bot staff, or moderators of the server it was submitted in can delete it.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        outfitService.delete(id);
        audit(event, AuditService.Action.OUTFIT_DELETED, id,
                userId.equals(outfit.getSubmitter()) ? "Deleted by its submitter" : "Deleted by staff (submitter <@" + outfit.getSubmitter() + ">)");
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Outfit Deleted")
                .setDescription("Outfit `" + id + "` has been marked as deleted.")
                .setColor(Color.RED)
                .setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleRestore(SlashCommandInteractionEvent event) {
        String id = outfitIdOption(event);
        if (id == null || id.isBlank()) {
            event.reply("You must provide an outfit ID.").setEphemeral(true).queue();
            return;
        }

        String userId = event.getUser().getId();

        if (!permissionService.isBotStaff(userId)) {
            event.reply("You must be a staff member to restore outfits.").setEphemeral(true).queue();
            return;
        }

        Optional<OutfitEntity> outfitOpt = outfitService.findById(id);
        if (outfitOpt.isEmpty()) {
            event.reply("Outfit `" + id + "` not found.").setEphemeral(true).queue();
            return;
        }

        OutfitEntity outfit = outfitOpt.get();
        if (!outfit.isDeleted()) {
            event.reply("Outfit `" + id + "` is already active.").setEphemeral(true).queue();
            return;
        }

        outfitService.restore(id);
        audit(event, AuditService.Action.OUTFIT_RESTORED, id, null);
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Outfit Restored")
                .setDescription("Outfit `" + id + "` has been restored and is now active.")
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void audit(SlashCommandInteractionEvent event, AuditService.Action action, String outfitId, String details) {
        String guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        auditService.record(event.getJDA(), action, event.getUser().getId(), guildId, AuditService.TARGET_OUTFIT, outfitId, details);
    }

    private EmbedBuilder buildOutfitEmbed(OutfitEntity outfit) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Outfit by " + (outfit.getDiscordName() != null ? outfit.getDiscordName() : "Unknown"))
                .setImage(outfit.getLink())
                .setColor(EMBED_COLOR)
                .addField("Tag", outfit.getTag() != null ? outfit.getTag() : "None", true)
                .addField("Outfit ID", "`" + outfit.getId() + "`", true)
                .addField("Submitter", "<@" + outfit.getSubmitter() + ">", true)
                .setFooter("FashionScape Bot • Vote with the button below!");

        if (outfit.getMeta() != null && !outfit.getMeta().isBlank()) {
            embed.setDescription(outfit.getMeta());
        }

        return embed;
    }

    private EmbedBuilder buildSearchEmbed(Page<OutfitEntity> results, int currentPage, String tag, String submitterId) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Outfit Search Results (Page " + currentPage + " of " + results.getTotalPages() + ")")
                .setColor(EMBED_COLOR)
                .setFooter("Total outfits found: " + results.getTotalElements());

        StringBuilder desc = new StringBuilder();
        if (tag != null) {
            desc.append("Filtered by tag: `").append(tag).append("`\n");
        }
        if (submitterId != null) {
            desc.append("Filtered by submitter: <@").append(submitterId).append(">\n");
        }
        if (!desc.isEmpty()) {
            desc.append("\n");
        }

        for (OutfitEntity outfit : results.getContent()) {
            desc.append("• **[`")
                    .append(outfit.getId())
                    .append("`](")
                    .append(outfit.getLink())
                    .append(")** | Tag: `")
                    .append(outfit.getTag() != null ? outfit.getTag() : "None")
                    .append("` | Submitter: <@")
                    .append(outfit.getSubmitter())
                    .append("> | ❤️ ")
                    .append(outfit.getDisplayCount())
                    .append("\n");
        }

        embed.setDescription(desc.toString());
        return embed;
    }

    private ActionRow buildSearchActionRow(Page<OutfitEntity> results, int currentPage, String tag, String submitterId) {
        String tagPayload = tag != null ? tag : "";
        String userPayload = submitterId != null ? submitterId : "";

        Button prevBtn = Button.secondary(
                BUTTON_PREFIX + "prev:" + (currentPage - 1) + ":" + tagPayload + ":" + userPayload,
                "◀ Previous"
        ).withDisabled(!results.hasPrevious());

        Button nextBtn = Button.secondary(
                BUTTON_PREFIX + "next:" + (currentPage + 1) + ":" + tagPayload + ":" + userPayload,
                "Next ▶"
        ).withDisabled(!results.hasNext());

        return ActionRow.of(prevBtn, nextBtn);
    }

    @Override
    public boolean canHandle(String buttonId) {
        return buttonId != null && buttonId.startsWith(BUTTON_PREFIX);
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        // format: outfit:page:<action>:<targetPage>:<tag>:<userId>
        String payload = buttonId.substring(BUTTON_PREFIX.length());
        String[] parts = payload.split(":", 4);

        if (parts.length < 2) {
            event.reply("Invalid pagination interaction.").setEphemeral(true).queue();
            return;
        }

        int targetPage;
        try {
            targetPage = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            event.reply("Invalid page number.").setEphemeral(true).queue();
            return;
        }

        String tag = parts.length > 2 && !parts[2].isBlank() ? parts[2] : null;
        String submitterId = parts.length > 3 && !parts[3].isBlank() ? parts[3] : null;

        PageRequest pageRequest = PageRequest.of(Math.max(0, targetPage - 1), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "created"));
        Page<OutfitEntity> results = outfitService.search(tag, submitterId, pageRequest);

        if (results.isEmpty()) {
            event.reply("No outfits found for this page.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = buildSearchEmbed(results, targetPage, tag, submitterId);
        ActionRow actionRow = buildSearchActionRow(results, targetPage, tag, submitterId);

        event.editMessageEmbeds(embed.build())
                .setComponents(actionRow)
                .queue();
    }
}
