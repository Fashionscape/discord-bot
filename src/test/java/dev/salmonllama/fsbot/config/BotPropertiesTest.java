package dev.salmonllama.fsbot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BotPropertiesTest {

    @Test
    void testDefaultValues() {
        BotProperties.DiscordProperties discord = new BotProperties.DiscordProperties("", 0L, "", "");
        BotProperties.SeaweedProperties seaweed = new BotProperties.SeaweedProperties(
                "http://localhost:8333",
                "seaweedadmin",
                "seaweedadmin",
                "fashionscape-outfits",
                "https://media.fashionscape.org"
        );
        BotProperties properties = new BotProperties(discord, seaweed);

        assertEquals("", properties.discord().token());
        assertEquals(0L, properties.discord().ownerId());
        assertEquals("http://localhost:8333", properties.seaweed().endpoint());
        assertEquals("fashionscape-outfits", properties.seaweed().bucket());
    }

    @Test
    void testBindingFromPropertySource() {
        Map<String, Object> map = new HashMap<>();
        map.put("fsbot.discord.token", "sample-test-token");
        map.put("fsbot.discord.owner-id", "123456789012345678");
        map.put("fsbot.discord.dev-guild-id", "987654321098765432");
        map.put("fsbot.seaweed.endpoint", "https://s3.example.com");
        map.put("fsbot.seaweed.bucket", "custom-bucket");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
        Binder binder = new Binder(source);

        BotProperties properties = binder.bind("fsbot", BotProperties.class).get();

        assertNotNull(properties);
        assertEquals("sample-test-token", properties.discord().token());
        assertEquals(123456789012345678L, properties.discord().ownerId());
        assertEquals("987654321098765432", properties.discord().devGuildId());
        assertEquals("https://s3.example.com", properties.seaweed().endpoint());
        assertEquals("custom-bucket", properties.seaweed().bucket());
    }
}
