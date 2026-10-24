package dev.salmonllama.fsbot.storage;

import dev.salmonllama.fsbot.config.BotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class SeaweedConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SeaweedConfiguration.class);

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public S3Client s3Client(BotProperties properties) {
        BotProperties.SeaweedProperties seaweedProps = properties.seaweed();
        logger.info("Configuring S3 client for SeaweedFS endpoint: {}", seaweedProps.endpoint());

        return S3Client.builder()
                .endpointOverride(URI.create(seaweedProps.endpoint()))
                // SeaweedFS ignores the region, but the SDK requires one to sign requests
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(seaweedProps.accessKey(), seaweedProps.secretKey())))
                // Buckets are addressed as http://host:8333/bucket, not bucket.host
                .forcePathStyle(true)
                // Only send and check checksums when an operation needs them; newer SDK
                // defaults add checksum headers that third-party S3 servers may not accept
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
