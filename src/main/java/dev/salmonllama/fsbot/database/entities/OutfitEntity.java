package dev.salmonllama.fsbot.database.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "outfits", indexes = {
        @Index(name = "idx_outfits_submitter", columnList = "submitter"),
        @Index(name = "idx_outfits_tag", columnList = "tag"),
        @Index(name = "idx_outfits_deleted", columnList = "deleted"),
        @Index(name = "idx_outfits_featured", columnList = "featured"),
        @Index(name = "idx_outfits_server_id", columnList = "server_id")
})
public class OutfitEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "link", nullable = false, columnDefinition = "TEXT")
    private String link;

    @Column(name = "submitter", length = 64, nullable = false)
    private String submitter;

    @Column(name = "tag", length = 255)
    private String tag;

    @Column(name = "meta", columnDefinition = "TEXT")
    private String meta;

    @Column(name = "discord_name", length = 255)
    private String discordName;

    @Column(name = "created", nullable = false)
    private Instant created;

    @Column(name = "updated", nullable = false)
    private Instant updated;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    // Times shown by /outfit random. Changed only by OutfitRepository.incrementDisplayCount,
    // so entity saves never overwrite a concurrent increment.
    @Column(name = "display_count", nullable = false, updatable = false)
    private int displayCount = 0;

    // Changed only by the vote queries in OutfitRepository, alongside outfit_votes.
    @Column(name = "vote_count", nullable = false, insertable = false, updatable = false)
    private int voteCount = 0;

    @Column(name = "delete_hash", length = 255)
    private String deleteHash;

    @Column(name = "server_id", length = 64, nullable = false)
    private String serverId;

    @Column(name = "channel_id", length = 64, nullable = false)
    private String channelId;

    public OutfitEntity() {
    }

    public OutfitEntity(String id, String link, String submitter, String tag, String meta,
                        String discordName, Instant created, Instant updated, boolean deleted,
                        boolean featured, int displayCount, String deleteHash,
                        String serverId, String channelId) {
        this.id = id;
        this.link = link;
        this.submitter = submitter;
        this.tag = tag;
        this.meta = meta;
        this.discordName = discordName;
        this.created = created != null ? created : Instant.now();
        this.updated = updated != null ? updated : Instant.now();
        this.deleted = deleted;
        this.featured = featured;
        this.displayCount = displayCount;
        this.deleteHash = deleteHash;
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.channelId = Objects.requireNonNull(channelId, "channelId");
    }

    @PrePersist
    protected void onCreate() {
        if (created == null) {
            created = Instant.now();
        }
        if (updated == null) {
            updated = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updated = Instant.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getDiscordName() {
        return discordName;
    }

    public void setDiscordName(String discordName) {
        this.discordName = discordName;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public int getDisplayCount() {
        return displayCount;
    }

    public void setDisplayCount(int displayCount) {
        this.displayCount = displayCount;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public String getDeleteHash() {
        return deleteHash;
    }

    public void setDeleteHash(String deleteHash) {
        this.deleteHash = deleteHash;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutfitEntity that = (OutfitEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OutfitEntity{" +
                "id='" + id + '\'' +
                ", link='" + link + '\'' +
                ", submitter='" + submitter + '\'' +
                ", tag='" + tag + '\'' +
                ", discordName='" + discordName + '\'' +
                ", serverId='" + serverId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", created=" + created +
                ", deleted=" + deleted +
                '}';
    }
}
