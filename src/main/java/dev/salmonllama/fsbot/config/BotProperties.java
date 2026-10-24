package dev.salmonllama.fsbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "fsbot")
public record BotProperties(
        @DefaultValue DiscordProperties discord,
        @DefaultValue SeaweedProperties seaweed
) {
    public record DiscordProperties(
            @DefaultValue("") String token,
            @DefaultValue("0") long ownerId,
            @DefaultValue("") String devGuildId,
            @DefaultValue("") String homeGuildId
    ) {}

    public record SeaweedProperties(
            @DefaultValue("http://localhost:8333") String endpoint,
            @DefaultValue("seaweedadmin") String accessKey,
            @DefaultValue("seaweedadmin") String secretKey,
            @DefaultValue("fashionscape-outfits") String bucket,
            @DefaultValue("https://media.fashionscape.org") String publicUrl
    ) {}
}
