/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.database.entities;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "gallery_channels", indexes = {
        @Index(name = "idx_gallery_channels_server_id", columnList = "server_id")
})
public class GalleryChannelEntity {

    @Id
    @Column(name = "channel_id", length = 64, nullable = false)
    private String channelId;

    @Column(name = "server_id", length = 64, nullable = false)
    private String serverId;

    @Column(name = "emoji", length = 255)
    private String emoji;

    @Column(name = "tag", length = 255)
    private String tag;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public GalleryChannelEntity() {
    }

    public GalleryChannelEntity(String channelId, String serverId, String emoji, String tag, boolean enabled) {
        this.channelId = channelId;
        this.serverId = serverId;
        this.emoji = emoji;
        this.tag = tag;
        this.enabled = enabled;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GalleryChannelEntity that = (GalleryChannelEntity) o;
        return Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    @Override
    public String toString() {
        return "GalleryChannelEntity{" +
                "channelId='" + channelId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", emoji='" + emoji + '\'' +
                ", tag='" + tag + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
