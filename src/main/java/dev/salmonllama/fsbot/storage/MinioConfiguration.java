package dev.salmonllama.fsbot.storage;

import dev.salmonllama.fsbot.config.BotProperties;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MinioConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(BotProperties properties) {
        BotProperties.SeaweedProperties seaweedProps = properties.seaweed();
        logger.info("Configuring MinIO client for SeaweedFS S3 endpoint: {}", seaweedProps.endpoint());

        return MinioClient.builder()
                .endpoint(seaweedProps.endpoint())
                .credentials(seaweedProps.accessKey(), seaweedProps.secretKey())
                .build();
    }
}
