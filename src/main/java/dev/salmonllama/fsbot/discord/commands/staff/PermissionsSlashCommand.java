package dev.salmonllama.fsbot.discord.commands.staff;

import dev.salmonllama.fsbot.database.entities.StaticPermissionEntity;
import dev.salmonllama.fsbot.discord.commands.SlashCommand;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
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

/**
 * Owner-only management of bot-level static permissions (admin, staff).
 */
@Component
public class PermissionsSlashCommand implements SlashCommand {

    private static final Logger logger = LoggerFactory.getLogger(PermissionsSlashCommand.class);
    private static final Color EMBED_COLOR = new Color(0x5865F2);

    private final PermissionService permissionService;

    public PermissionsSlashCommand(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public String getName() {
        return "permissions";
    }

    @Override
    public String getDescription() {
        return "Grant, revoke, or list bot admin and staff permissions (Owner only)";
    }

    @Override
    public boolean isHomeGuildOnly() {
        return true;
    }

    @Override
    public SlashCommandData buildCommandData() {
        SubcommandData grantSub = new SubcommandData("grant", "Grant a bot permission to a user")
                .addOption(OptionType.USER, "user", "Target Discord user", true)
                .addOptions(permissionTypeOption(true));

        SubcommandData revokeSub = new SubcommandData("revoke", "Revoke a bot permission from a user")
                .addOption(OptionType.USER, "user", "Target Discord user", true)
                .addOptions(permissionTypeOption(true));

        SubcommandData listSub = new SubcommandData("list", "List bot permissions for one user, or for everyone")
                .addOption(OptionType.USER, "user", "Only show this user's permissions", false);

        return Commands.slash(getName(), getDescription())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(grantSub, revokeSub, listSub);
    }

    private static OptionData permissionTypeOption(boolean required) {
        return new OptionData(OptionType.STRING, "type", "Permission type", required)
                .addChoice(PermissionService.ADMIN, PermissionService.ADMIN)
                .addChoice(PermissionService.STAFF, PermissionService.STAFF);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!permissionService.isOwner(event.getUser().getId())) {
            event.reply("Only the bot owner can manage bot permissions.").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("Please specify a subcommand: `grant`, `revoke`, or `list`.").setEphemeral(true).queue();
            return;
        }

        switch (subcommand) {
            case "grant" -> handleGrant(event);
            case "revoke" -> handleRevoke(event);
            case "list" -> handleList(event);
            default -> event.reply("Unknown subcommand: " + subcommand).setEphemeral(true).queue();
        }
    }

    private void handleGrant(SlashCommandInteractionEvent event) {
        User target = event.getOption("user", OptionMapping::getAsUser);
        String type = PermissionService.normalizePermissionType(event.getOption("type", OptionMapping::getAsString));
        if (target == null || type == null) {
            event.reply("Please provide a user and a permission type (`admin` or `staff`).").setEphemeral(true).queue();
            return;
        }

        if (!permissionService.grant(target.getId(), type)) {
            event.reply(target.getAsMention() + " already has `" + type + "`.").setEphemeral(true).queue();
            return;
        }

        logPrivilegedAction(event, "granted " + type + " to user " + target.getId());
        event.reply("Granted `" + type + "` to " + target.getAsMention() + " (`" + target.getId() + "`).")
                .setEphemeral(true)
                .queue();
    }

    private void handleRevoke(SlashCommandInteractionEvent event) {
        User target = event.getOption("user", OptionMapping::getAsUser);
        String type = PermissionService.normalizePermissionType(event.getOption("type", OptionMapping::getAsString));
        if (target == null || type == null) {
            event.reply("Please provide a user and a permission type (`admin` or `staff`).").setEphemeral(true).queue();
            return;
        }

        if (!permissionService.revoke(target.getId(), type)) {
            event.reply(target.getAsMention() + " doesn't have `" + type + "`.").setEphemeral(true).queue();
            return;
        }

        logPrivilegedAction(event, "revoked " + type + " from user " + target.getId());
        event.reply("Revoked `" + type + "` from " + target.getAsMention() + " (`" + target.getId() + "`).")
                .setEphemeral(true)
                .queue();
    }

    private void handleList(SlashCommandInteractionEvent event) {
        User target = Optional.ofNullable(event.getOption("user")).map(OptionMapping::getAsUser).orElse(null);
        List<StaticPermissionEntity> permissions = permissionService.list(target != null ? target.getId() : null);

        if (permissions.isEmpty()) {
            String message = target != null
                    ? target.getAsMention() + " has no bot permissions."
                    : "No bot permissions are set. Only the owner has staff access.";
            event.reply(message).setEphemeral(true).queue();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (StaticPermissionEntity permission : permissions) {
            sb.append("• <@").append(permission.getUserId()).append("> (`").append(permission.getUserId())
                    .append("`) - `").append(permission.getPermission()).append("`\n");
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Bot Permissions (" + permissions.size() + ")")
                .setColor(EMBED_COLOR)
                .setDescription(sb.toString());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void logPrivilegedAction(SlashCommandInteractionEvent event, String action) {
        String guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        logger.info("Staff action: {} (actor={}, guild={})", action, event.getUser().getId(), guildId);
    }
}
