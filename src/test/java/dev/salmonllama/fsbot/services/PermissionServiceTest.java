package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.entities.StaticPermissionEntity;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionServiceTest {

    private static final String OWNER = "100";
    private static final String BOT_ADMIN = "200";
    private static final String BOT_STAFF = "300";
    private static final String SUBMITTER = "400";
    private static final String STRANGER = "500";

    @Mock
    private StaticPermissionRepository staticPermissionRepository;

    @Mock
    private Member member;

    @Mock
    private Guild guild;

    private PermissionService permissionService;
    private OutfitEntity outfit;

    @BeforeEach
    void setUp() {
        BotProperties properties = new BotProperties(
                new BotProperties.DiscordProperties("token", Long.parseLong(OWNER), "", "home-guild"),
                new BotProperties.MinioProperties("http://localhost:9000", "access", "secret", "bucket", "https://media.fashionscape.org")
        );
        permissionService = new PermissionService(staticPermissionRepository, properties);

        when(staticPermissionRepository.existsByUserIdAndPermission(BOT_ADMIN, PermissionService.ADMIN)).thenReturn(true);
        when(staticPermissionRepository.existsByUserIdAndPermission(BOT_STAFF, PermissionService.STAFF)).thenReturn(true);

        outfit = new OutfitEntity("outfit-1", "https://media.fashionscape.org/outfits/outfit-1.png", SUBMITTER,
                "osrs", null, "Player", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");

        when(member.getGuild()).thenReturn(guild);
    }

    private void memberIsModeratorOf(String guildId) {
        when(guild.getId()).thenReturn(guildId);
        when(member.hasPermission(any(Permission.class))).thenReturn(true);
    }

    // /block user|server|list: owner, bot admin

    @Test
    void blockIsAllowedForOwnerAndBotAdminOnly() {
        assertTrue(permissionService.isBotAdmin(OWNER));
        assertTrue(permissionService.isBotAdmin(BOT_ADMIN));
        assertFalse(permissionService.isBotAdmin(BOT_STAFF));
        assertFalse(permissionService.isBotAdmin(STRANGER));
        assertFalse(permissionService.isBotAdmin(null));
    }

    // /outfit restore: owner, bot admin, bot staff

    @Test
    void restoreIsAllowedForOwnerBotAdminAndBotStaff() {
        assertTrue(permissionService.isBotStaff(OWNER));
        assertTrue(permissionService.isBotStaff(BOT_ADMIN));
        assertTrue(permissionService.isBotStaff(BOT_STAFF));
        assertFalse(permissionService.isBotStaff(STRANGER));
        assertFalse(permissionService.isBotStaff(null));
    }

    // /outfit delete and gallery Delete button: submitter, bot staff, mods of the outfit's server

    @Test
    void submitterCanModerateOwnOutfit() {
        assertTrue(permissionService.canModerateOutfit(SUBMITTER, null, outfit));
    }

    @Test
    void botStaffCanModerateAnyOutfit() {
        assertTrue(permissionService.canModerateOutfit(OWNER, null, outfit));
        assertTrue(permissionService.canModerateOutfit(BOT_ADMIN, null, outfit));
        assertTrue(permissionService.canModerateOutfit(BOT_STAFF, null, outfit));
    }

    @Test
    void moderatorOfOutfitServerCanModerate() {
        memberIsModeratorOf("server-1");
        assertTrue(permissionService.canModerateOutfit(STRANGER, member, outfit));
    }

    @Test
    void moderatorOfAnotherServerCannotModerate() {
        memberIsModeratorOf("server-2");
        assertFalse(permissionService.canModerateOutfit(STRANGER, member, outfit));
    }

    @Test
    void regularMemberOfOutfitServerCannotModerate() {
        when(guild.getId()).thenReturn("server-1");
        when(member.hasPermission(any(Permission.class))).thenReturn(false);
        assertFalse(permissionService.canModerateOutfit(STRANGER, member, outfit));
    }

    @Test
    void strangerWithoutMemberCannotModerate() {
        assertFalse(permissionService.canModerateOutfit(STRANGER, null, outfit));
    }

    // /permissions: owner only

    @Test
    void onlyTheOwnerIsOwner() {
        assertTrue(permissionService.isOwner(OWNER));
        assertFalse(permissionService.isOwner(BOT_ADMIN));
        assertFalse(permissionService.isOwner(null));
    }

    @Test
    void homeGuildMatchesConfiguredId() {
        assertTrue(permissionService.isHomeGuild("home-guild"));
        assertFalse(permissionService.isHomeGuild("server-1"));
        assertFalse(permissionService.isHomeGuild(null));
    }

    @Test
    void grantingTwiceSavesOneRow() {
        when(staticPermissionRepository.existsByUserIdAndPermission(STRANGER, PermissionService.STAFF))
                .thenReturn(false, true);

        assertTrue(permissionService.grant(STRANGER, PermissionService.STAFF));
        assertFalse(permissionService.grant(STRANGER, PermissionService.STAFF));

        verify(staticPermissionRepository, times(1)).save(any(StaticPermissionEntity.class));
    }

    @Test
    void revokingMissingPermissionDeletesNothing() {
        assertFalse(permissionService.revoke(STRANGER, PermissionService.ADMIN));
        verify(staticPermissionRepository, never()).deleteByUserIdAndPermission(any(), any());
    }

    @Test
    void permissionTypesAreNormalized() {
        assertEquals("admin", PermissionService.normalizePermissionType(" ADMIN "));
        assertEquals("staff", PermissionService.normalizePermissionType("staff"));
        assertNull(PermissionService.normalizePermissionType("owner"));
        assertNull(PermissionService.normalizePermissionType(null));
    }
}
