package dev.salmonllama.fsbot.database.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "blocked_servers")
public class BlockedServerEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "owner_id", length = 64)
    private String ownerId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "added", nullable = false)
    private Instant added;

    public BlockedServerEntity() {
    }

    public BlockedServerEntity(String id, String name, String ownerId, String reason, Instant added) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.reason = reason;
        this.added = added != null ? added : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (added == null) {
            added = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getAdded() {
        return added;
    }

    public void setAdded(Instant added) {
        this.added = added;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockedServerEntity that = (BlockedServerEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BlockedServerEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", reason='" + reason + '\'' +
                ", added=" + added +
                '}';
    }
}
