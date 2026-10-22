package dev.salmonllama.fsbot.discord.commands.staff;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.entities.BlockedServerEntity;
import dev.salmonllama.fsbot.database.entities.BlockedUserEntity;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import dev.salmonllama.fsbot.discord.commands.SlashCommand;
import dev.salmonllama.fsbot.services.BlocklistService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

@Component
public class BlockSlashCommand implements SlashCommand {

    private static final Logger logger = LoggerFactory.getLogger(BlockSlashCommand.class);
    private static final Color EMBED_COLOR = new Color(0xED4245);

    private final BlocklistService blocklistService;
    private final BotProperties properties;
    private final StaticPermissionRepository staticPermissionRepository;

    public BlockSlashCommand(
            BlocklistService blocklistService,
            BotProperties properties,
            StaticPermissionRepository staticPermissionRepository
    ) {
        this.blocklistService = blocklistService;
        this.properties = properties;
        this.staticPermissionRepository = staticPermissionRepository;
    }

    @Override
    public String getName() {
        return "block";
    }

    @Override
    public String getDescription() {
        return "Manage user and server blocklists (Staff only)";
    }

    @Override
    public SlashCommandData buildCommandData() {
        OptionData userActionOption = new OptionData(OptionType.STRING, "action", "Action to perform", true)
                .addChoice("block", "block")
                .addChoice("unblock", "unblock")
                .addChoice("check", "check");

        SubcommandData userSub = new SubcommandData("user", "Block, unblock, or check a user")
                .addOptions(userActionOption)
                .addOption(OptionType.USER, "user", "Target Discord user", true)
                .addOption(OptionType.STRING, "reason", "Reason for blocking", false);

        OptionData serverActionOption = new OptionData(OptionType.STRING, "action", "Action to perform", true)
                .addChoice("block", "block")
                .addChoice("unblock", "unblock")
                .addChoice("check", "check");

        SubcommandData serverSub = new SubcommandData("server", "Block, unblock, or check a server")
                .addOptions(serverActionOption)
                .addOption(OptionType.STRING, "server_id", "Target server / guild ID", true)
                .addOption(OptionType.STRING, "reason", "Reason for blocking", false);

        OptionData listTypeOption = new OptionData(OptionType.STRING, "type", "Blocklist to view", true)
                .addChoice("users", "users")
                .addChoice("servers", "servers");

        SubcommandData listSub = new SubcommandData("list", "List currently blocked users or servers")
                .addOptions(listTypeOption);

        return Commands.slash(getName(), getDescription())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(userSub, serverSub, listSub);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        Member member = event.getMember();

        if (!isAdmin(userId, member)) {
            event.reply("You do not have permission to manage the blocklist.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("Please specify a subcommand: `user`, `server`, or `list`.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        switch (subcommand) {
            case "user" -> handleUserBlock(event);
            case "server" -> handleServerBlock(event);
            case "list" -> handleList(event);
            default -> event.reply("Unknown subcommand: " + subcommand).setEphemeral(true).queue();
        }
    }

    private void handleUserBlock(SlashCommandInteractionEvent event) {
        String action = event.getOption("action", OptionMapping::getAsString);
        User targetUser = event.getOption("user", OptionMapping::getAsUser);
        String reason = Optional.ofNullable(event.getOption("reason")).map(OptionMapping::getAsString).orElse("No reason provided");

        if (targetUser == null || action == null) {
            event.reply("Invalid user block arguments.").setEphemeral(true).queue();
            return;
        }

        String targetId = targetUser.getId();

        switch (action.toLowerCase()) {
            case "block" -> {
                if (blocklistService.isUserBlocked(targetId)) {
                    event.reply("User " + targetUser.getAsMention() + " (`" + targetId + "`) is already blocked.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                BlockedUserEntity blocked = blocklistService.blockUser(targetId, reason);
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("User Blocked")
                        .setColor(EMBED_COLOR)
                        .addField("User", targetUser.getAsMention() + " (" + targetUser.getAsTag() + ")", true)
                        .addField("User ID", "`" + targetId + "`", true)
                        .addField("Reason", reason, false)
                        .setFooter("Blocked by " + event.getUser().getAsTag());

                event.replyEmbeds(embed.build()).queue();
            }
            case "unblock" -> {
                if (!blocklistService.isUserBlocked(targetId)) {
                    event.reply("User " + targetUser.getAsMention() + " (`" + targetId + "`) is not currently blocked.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                blocklistService.unblockUser(targetId);
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("User Unblocked")
                        .setColor(Color.GREEN)
                        .setDescription("User " + targetUser.getAsMention() + " (`" + targetId + "`) has been removed from the blocklist.")
                        .setFooter("Unblocked by " + event.getUser().getAsTag());

                event.replyEmbeds(embed.build()).queue();
            }
            case "check" -> {
                Optional<BlockedUserEntity> entityOpt = blocklistService.getBlockedUser(targetId);
                if (entityOpt.isEmpty()) {
                    event.reply("User " + targetUser.getAsMention() + " (`" + targetId + "`) is **not** blocked.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                BlockedUserEntity entity = entityOpt.get();
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Block Status: Blocked")
                        .setColor(EMBED_COLOR)
                        .addField("User", targetUser.getAsMention() + " (" + targetUser.getAsTag() + ")", true)
                        .addField("User ID", "`" + entity.getId() + "`", true)
                        .addField("Reason", entity.getReason() != null ? entity.getReason() : "None", false)
                        .addField("Added", entity.getAdded() != null ? entity.getAdded().toString() : "Unknown", true);

                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
            default -> event.reply("Invalid action: " + action).setEphemeral(true).queue();
        }
    }

    private void handleServerBlock(SlashCommandInteractionEvent event) {
        String action = event.getOption("action", OptionMapping::getAsString);
        String serverId = event.getOption("server_id", OptionMapping::getAsString);
        String reason = Optional.ofNullable(event.getOption("reason")).map(OptionMapping::getAsString).orElse("No reason provided");

        if (serverId == null || serverId.isBlank() || action == null) {
            event.reply("Invalid server block arguments.").setEphemeral(true).queue();
            return;
        }

        switch (action.toLowerCase()) {
            case "block" -> {
                if (blocklistService.isServerBlocked(serverId)) {
                    event.reply("Server `" + serverId + "` is already blocked.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                BlockedServerEntity blocked = blocklistService.blockServer(serverId, null, null, reason);
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Server Blocked")
                        .setColor(EMBED_COLOR)
                        .addField("Server ID", "`" + serverId + "`", true)
                        .addField("Reason", reason, false)
                        .setFooter("Blocked by " + event.getUser().getAsTag());

                event.replyEmbeds(embed.build()).queue();
            }
            case "unblock" -> {
                if (!blocklistService.isServerBlocked(serverId)) {
                    event.reply("Server `" + serverId + "` is not currently blocked.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                blocklistService.unblockServer(serverId);
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Server Unblocked")
                        .setColor(Color.GREEN)
                        .setDescription("Server `" + serverId + "` has been removed from the blocklist.")
                        .setFooter("Unblocked by " + event.getUser().getAsTag());

                event.replyEmbeds(embed.build()).queue();
            }
            case "check" -> {
                Optional<BlockedServerEntity> entityOpt = blocklistService.getBlockedServer(serverId);
                if (entityOpt.isEmpty()) {
                    event.reply("Server `" + serverId + "` is **not** blocked.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                BlockedServerEntity entity = entityOpt.get();
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Block Status: Blocked")
                        .setColor(EMBED_COLOR)
                        .addField("Server ID", "`" + entity.getId() + "`", true)
                        .addField("Server Name", entity.getName() != null ? entity.getName() : "Unknown", true)
                        .addField("Reason", entity.getReason() != null ? entity.getReason() : "None", false)
                        .addField("Added", entity.getAdded() != null ? entity.getAdded().toString() : "Unknown", true);

                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
            default -> event.reply("Invalid action: " + action).setEphemeral(true).queue();
        }
    }

    private void handleList(SlashCommandInteractionEvent event) {
        String type = event.getOption("type", OptionMapping::getAsString);
        if ("servers".equalsIgnoreCase(type)) {
            List<BlockedServerEntity> servers = blocklistService.getAllBlockedServers();
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Blocked Servers (" + servers.size() + ")")
                    .setColor(EMBED_COLOR);

            if (servers.isEmpty()) {
                embed.setDescription("No servers are currently blocked.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (BlockedServerEntity s : servers) {
                    sb.append("• `").append(s.getId()).append("`");
                    if (s.getName() != null) {
                        sb.append(" (").append(s.getName()).append(")");
                    }
                    if (s.getReason() != null && !s.getReason().isBlank()) {
                        sb.append(" - ").append(s.getReason());
                    }
                    sb.append("\n");
                }
                embed.setDescription(sb.toString());
            }

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        } else {
            List<BlockedUserEntity> users = blocklistService.getAllBlockedUsers();
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Blocked Users (" + users.size() + ")")
                    .setColor(EMBED_COLOR);

            if (users.isEmpty()) {
                embed.setDescription("No users are currently blocked.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (BlockedUserEntity u : users) {
                    sb.append("• <@").append(u.getId()).append("> (`").append(u.getId()).append("`)");
                    if (u.getReason() != null && !u.getReason().isBlank()) {
                        sb.append(" - ").append(u.getReason());
                    }
                    sb.append("\n");
                }
                embed.setDescription(sb.toString());
            }

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
    }

    private boolean isAdmin(String userId, Member member) {
        if (properties.discord() != null && properties.discord().ownerId() > 0) {
            if (String.valueOf(properties.discord().ownerId()).equals(userId)) {
                return true;
            }
        }

        if (member != null && member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }

        return staticPermissionRepository.existsByUserIdAndPermission(userId, "admin");
    }
}
