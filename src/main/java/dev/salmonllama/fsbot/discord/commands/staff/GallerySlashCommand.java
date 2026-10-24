package dev.salmonllama.fsbot.discord.commands.staff;

import dev.salmonllama.fsbot.database.entities.GalleryChannelEntity;
import dev.salmonllama.fsbot.database.repositories.GalleryChannelRepository;
import dev.salmonllama.fsbot.discord.commands.SlashCommand;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

@Component
public class GallerySlashCommand implements SlashCommand {

    private static final Logger logger = LoggerFactory.getLogger(GallerySlashCommand.class);
    private static final Color EMBED_COLOR = new Color(0x3B88C3);

    private final GalleryChannelRepository galleryChannelRepository;
    private final PermissionService permissionService;

    public GallerySlashCommand(GalleryChannelRepository galleryChannelRepository, PermissionService permissionService) {
        this.galleryChannelRepository = galleryChannelRepository;
        this.permissionService = permissionService;
    }

    @Override
    public String getName() {
        return "gallery";
    }

    @Override
    public String getDescription() {
        return "Configure and manage gallery channels in this server";
    }

    @Override
    public SlashCommandData buildCommandData() {
        SubcommandData listSub = new SubcommandData("list", "List all active gallery channels in this server");

        SubcommandData addSub = new SubcommandData("add", "Register a channel as an outfit gallery")
                .addOption(OptionType.CHANNEL, "channel", "The channel to register", true)
                .addOption(OptionType.STRING, "tag", "Default outfit tag for this channel (e.g. general, osrs, rs3)", false)
                .addOption(OptionType.STRING, "emoji", "Default reaction emoji", false);

        SubcommandData removeSub = new SubcommandData("remove", "Unregister a gallery channel")
                .addOption(OptionType.CHANNEL, "channel", "The gallery channel to remove", true);

        return Commands.slash(getName(), getDescription())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(listSub, addSub, removeSub);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command can only be used within a server.").setEphemeral(true).queue();
            return;
        }

        String userId = event.getUser().getId();
        Member member = event.getMember();

        if (!hasManagePermission(userId, member)) {
            event.reply("You do not have permission to manage server galleries.").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("Please specify a subcommand: `list`, `add`, or `remove`.").setEphemeral(true).queue();
            return;
        }

        switch (subcommand) {
            case "list" -> handleList(event, guild);
            case "add" -> handleAdd(event, guild);
            case "remove" -> handleRemove(event, guild);
            default -> event.reply("Unknown subcommand: " + subcommand).setEphemeral(true).queue();
        }
    }

    private void handleList(SlashCommandInteractionEvent event, Guild guild) {
        List<GalleryChannelEntity> galleries = galleryChannelRepository.findByServerIdAndEnabledTrue(guild.getId());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Registered Gallery Channels")
                .setColor(EMBED_COLOR)
                .setFooter("Guild: " + guild.getName());

        if (galleries.isEmpty()) {
            embed.setDescription("No active gallery channels configured in this server. Use `/gallery add` to register one!");
        } else {
            StringBuilder sb = new StringBuilder();
            for (GalleryChannelEntity g : galleries) {
                sb.append("• <#").append(g.getChannelId()).append(">");
                if (g.getTag() != null && !g.getTag().isBlank()) {
                    sb.append(" (Tag: `").append(g.getTag()).append("`)");
                }
                sb.append("\n");
            }
            embed.setDescription(sb.toString());
        }

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleAdd(SlashCommandInteractionEvent event, Guild guild) {
        GuildChannel channel = event.getOption("channel", OptionMapping::getAsChannel);
        String tag = Optional.ofNullable(event.getOption("tag")).map(OptionMapping::getAsString).orElse("general");
        String emoji = Optional.ofNullable(event.getOption("emoji")).map(OptionMapping::getAsString).orElse(":heartpulse:");

        if (channel == null || channel.getType() != ChannelType.TEXT) {
            event.reply("You must select a valid text channel.").setEphemeral(true).queue();
            return;
        }

        String channelId = channel.getId();
        Optional<GalleryChannelEntity> existing = galleryChannelRepository.findById(channelId);

        if (existing.isPresent() && existing.get().isEnabled()) {
            event.reply("Channel <#" + channelId + "> is already registered as an active gallery.").setEphemeral(true).queue();
            return;
        }

        GalleryChannelEntity entity = existing.orElse(new GalleryChannelEntity());
        entity.setChannelId(channelId);
        entity.setServerId(guild.getId());
        entity.setTag(tag);
        entity.setEmoji(emoji);
        entity.setEnabled(true);

        galleryChannelRepository.save(entity);
        logger.info("Staff action: configured gallery channel {} (actor={}, guild={})", channelId, event.getUser().getId(), guild.getId());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Gallery Channel Registered")
                .setColor(Color.GREEN)
                .setDescription("Channel <#" + channelId + "> is now actively tracking outfit uploads!")
                .addField("Tag", tag, true)
                .addField("Emoji", emoji, true);

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleRemove(SlashCommandInteractionEvent event, Guild guild) {
        GuildChannel channel = event.getOption("channel", OptionMapping::getAsChannel);
        if (channel == null) {
            event.reply("You must select a channel to remove.").setEphemeral(true).queue();
            return;
        }

        String channelId = channel.getId();
        Optional<GalleryChannelEntity> existing = galleryChannelRepository.findById(channelId);

        if (existing.isEmpty() || !existing.get().isEnabled()) {
            event.reply("Channel <#" + channelId + "> is not registered as an active gallery.").setEphemeral(true).queue();
            return;
        }

        GalleryChannelEntity entity = existing.get();
        entity.setEnabled(false);
        galleryChannelRepository.save(entity);
        logger.info("Staff action: disabled gallery channel {} (actor={}, guild={})", channelId, event.getUser().getId(), guild.getId());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Gallery Channel Removed")
                .setColor(Color.ORANGE)
                .setDescription("Channel <#" + channelId + "> is no longer being tracked as an outfit gallery.");

        event.replyEmbeds(embed.build()).queue();
    }

    private boolean hasManagePermission(String userId, Member member) {
        if (member != null && (member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER))) {
            return true;
        }
        return permissionService.isBotStaff(userId);
    }
}
