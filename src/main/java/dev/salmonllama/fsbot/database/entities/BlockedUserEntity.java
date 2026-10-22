/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.database.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "blocked_users")
public class BlockedUserEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "added", nullable = false)
    private Instant added;

    public BlockedUserEntity() {
    }

    public BlockedUserEntity(String id, String reason, Instant added) {
        this.id = id;
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
        BlockedUserEntity that = (BlockedUserEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BlockedUserEntity{" +
                "id='" + id + '\'' +
                ", reason='" + reason + '\'' +
                ", added=" + added +
                '}';
    }
}
