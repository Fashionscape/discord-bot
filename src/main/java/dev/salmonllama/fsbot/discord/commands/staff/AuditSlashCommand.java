package dev.salmonllama.fsbot.discord.commands.staff;

import dev.salmonllama.fsbot.discord.commands.SlashCommand;
import dev.salmonllama.fsbot.services.AuditService;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

/**
 * Home-server command for configuring where audit entries are posted.
 */
@Component
public class AuditSlashCommand implements SlashCommand {

    private final AuditService auditService;
    private final PermissionService permissionService;

    public AuditSlashCommand(AuditService auditService, PermissionService permissionService) {
        this.auditService = auditService;
        this.permissionService = permissionService;
    }

    @Override
    public String getName() {
        return "audit";
    }

    @Override
    public String getDescription() {
        return "Configure the audit log channel (Owner and bot admins only)";
    }

    @Override
    public boolean isHomeGuildOnly() {
        return true;
    }

    @Override
    public SlashCommandData buildCommandData() {
        SubcommandData configSub = new SubcommandData("config", "Set the channel that audit entries are posted to")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Audit channel in this server", true)
                        .setChannelTypes(ChannelType.TEXT));

        return Commands.slash(getName(), getDescription())
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(configSub);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        if (!permissionService.isBotAdmin(userId)) {
            event.reply("Only the bot owner and bot admins can configure the audit log.").setEphemeral(true).queue();
            return;
        }

        if (!"config".equals(event.getSubcommandName())) {
            event.reply("Please specify a subcommand: `config`.").setEphemeral(true).queue();
            return;
        }

        GuildChannelUnion selected = event.getOption("channel", OptionMapping::getAsChannel);
        if (selected == null || !selected.getType().isMessage()) {
            event.reply("Please choose a text channel.").setEphemeral(true).queue();
            return;
        }

        GuildMessageChannel channel = selected.asGuildMessageChannel();
        String guildId = channel.getGuild().getId();
        if (!permissionService.isHomeGuild(guildId)) {
            event.reply("The audit channel must be in the home server.").setEphemeral(true).queue();
            return;
        }

        if (!channel.canTalk() || !channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            event.reply("I can't post embeds in " + channel.getAsMention() + ". Give me Send Messages and Embed Links there first.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        auditService.setAuditChannelId(channel.getId());
        auditService.record(event.getJDA(), AuditService.Action.AUDIT_CHANNEL_SET, userId, guildId,
                AuditService.TARGET_CHANNEL, channel.getId(), "Audit channel set to " + channel.getAsMention());

        event.reply("Audit entries will now be posted to " + channel.getAsMention() + ".").setEphemeral(true).queue();
    }
}
