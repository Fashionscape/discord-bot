package dev.salmonllama.fsbot.discord.commands.staff;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.entities.GalleryChannelEntity;
import dev.salmonllama.fsbot.database.repositories.GalleryChannelRepository;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GallerySlashCommandTest {

    @Mock
    private GalleryChannelRepository galleryChannelRepository;

    @Mock
    private StaticPermissionRepository staticPermissionRepository;

    @Mock
    private SlashCommandInteractionEvent slashEvent;

    @Mock
    private ReplyCallbackAction replyCallbackAction;

    @Mock
    private Guild mockGuild;

    @Mock
    private Member mockMember;

    @Mock
    private User mockUser;

    @Mock
    private GuildChannel mockChannel;

    private BotProperties properties;
    private GallerySlashCommand command;

    @BeforeEach
    void setUp() {
        properties = new BotProperties(
                new BotProperties.DiscordProperties("token", 123456789L, "dev-guild", ""),
                new BotProperties.MinioProperties("http://localhost:9000", "access", "secret", "bucket", "https://media.fashionscape.org")
        );
        command = new GallerySlashCommand(galleryChannelRepository, new PermissionService(staticPermissionRepository, properties));

        when(slashEvent.getGuild()).thenReturn(mockGuild);
        when(mockGuild.getId()).thenReturn("guild-1");
        when(mockGuild.getName()).thenReturn("Fashion Guild");

        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("123456789"); // bot owner

        when(slashEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(slashEvent.replyEmbeds(any(MessageEmbed.class))).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(anyBoolean())).thenReturn(replyCallbackAction);
    }

    @Test
    void testCommandData() {
        assertEquals("gallery", command.getName());
        assertNotNull(command.getDescription());

        SlashCommandData data = command.buildCommandData();
        assertEquals("gallery", data.getName());
        assertEquals(3, data.getSubcommands().size());
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("list")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("add")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("remove")));
    }

    @Test
    void testListGalleries() {
        when(slashEvent.getSubcommandName()).thenReturn("list");
        GalleryChannelEntity g = new GalleryChannelEntity("chan-1", "guild-1", ":heart:", "fashionscape", true);
        when(galleryChannelRepository.findByServerIdAndEnabledTrue("guild-1")).thenReturn(List.of(g));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> captor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(slashEvent).replyEmbeds(captor.capture());
        MessageEmbed embed = captor.getValue();
        assertEquals("Registered Gallery Channels", embed.getTitle());
    }

    @Test
    void testAddGallery() {
        when(slashEvent.getSubcommandName()).thenReturn("add");
        when(slashEvent.getOption(eq("channel"), any())).thenReturn(mockChannel);
        when(mockChannel.getType()).thenReturn(ChannelType.TEXT);
        when(mockChannel.getId()).thenReturn("new-chan");
        when(galleryChannelRepository.findById("new-chan")).thenReturn(Optional.empty());

        command.execute(slashEvent);

        verify(galleryChannelRepository).save(any(GalleryChannelEntity.class));
        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testRemoveGallery() {
        when(slashEvent.getSubcommandName()).thenReturn("remove");
        when(slashEvent.getOption(eq("channel"), any())).thenReturn(mockChannel);
        when(mockChannel.getId()).thenReturn("old-chan");

        GalleryChannelEntity existing = new GalleryChannelEntity("old-chan", "guild-1", ":heart:", "osrs", true);
        when(galleryChannelRepository.findById("old-chan")).thenReturn(Optional.of(existing));

        command.execute(slashEvent);

        assertFalse(existing.isEnabled());
        verify(galleryChannelRepository).save(existing);
        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));
    }
}
