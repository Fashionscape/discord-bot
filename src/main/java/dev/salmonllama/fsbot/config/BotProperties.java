package dev.salmonllama.fsbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "fsbot")
public record BotProperties(
        @DefaultValue DiscordProperties discord,
        @DefaultValue MinioProperties minio
) {
    public record DiscordProperties(
            @DefaultValue("") String token,
            @DefaultValue("0") long ownerId,
            @DefaultValue("") String devGuildId
    ) {}

    public record MinioProperties(
            @DefaultValue("http://localhost:9000") String endpoint,
            @DefaultValue("minioadmin") String accessKey,
            @DefaultValue("minioadmin") String secretKey,
            @DefaultValue("fashionscape-outfits") String bucket,
            @DefaultValue("https://media.fashionscape.org") String publicUrl
    ) {}
}
