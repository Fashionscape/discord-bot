/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.GalleryChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GalleryChannelRepository extends JpaRepository<GalleryChannelEntity, String> {

    List<GalleryChannelEntity> findByServerId(String serverId);

    List<GalleryChannelEntity> findByServerIdAndEnabledTrue(String serverId);

    Optional<GalleryChannelEntity> findByChannelIdAndEnabledTrue(String channelId);

    boolean existsByChannelIdAndEnabledTrue(String channelId);
}
