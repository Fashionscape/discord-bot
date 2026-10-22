/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

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
        BotProperties.MinioProperties minioProps = properties.minio();
        logger.info("Configuring MinIO client connecting to endpoint: {}", minioProps.endpoint());

        return MinioClient.builder()
                .endpoint(minioProps.endpoint())
                .credentials(minioProps.accessKey(), minioProps.secretKey())
                .build();
    }
}
