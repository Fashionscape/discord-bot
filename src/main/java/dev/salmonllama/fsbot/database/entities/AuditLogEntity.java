package dev.salmonllama.fsbot.database.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_created", columnList = "created"),
        @Index(name = "idx_audit_log_target", columnList = "target_type, target_id")
})
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "action", length = 64, nullable = false)
    private String action;

    @Column(name = "actor_id", length = 64, nullable = false)
    private String actorId;

    @Column(name = "guild_id", length = 64)
    private String guildId;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created", nullable = false)
    private Instant created;

    public AuditLogEntity() {
    }

    public AuditLogEntity(String action, String actorId, String guildId, String targetType, String targetId,
                          String details, Instant created) {
        this.action = action;
        this.actorId = actorId;
        this.guildId = guildId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.created = created != null ? created : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (created == null) {
            created = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getActorId() {
        return actorId;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreated() {
        return created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLogEntity that = (AuditLogEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditLogEntity{" +
                "id=" + id +
                ", action='" + action + '\'' +
                ", actorId='" + actorId + '\'' +
                ", guildId='" + guildId + '\'' +
                ", targetType='" + targetType + '\'' +
                ", targetId='" + targetId + '\'' +
                ", created=" + created +
                '}';
    }
}
