package dev.salmonllama.fsbot.database.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "static_permissions", indexes = {
        @Index(name = "idx_static_permissions_user", columnList = "user_id")
})
public class StaticPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "permission", length = 64, nullable = false)
    private String permission;

    @Column(name = "added", nullable = false)
    private Instant added;

    public StaticPermissionEntity() {
    }

    public StaticPermissionEntity(String userId, String permission, Instant added) {
        this.userId = userId;
        this.permission = permission;
        this.added = added != null ? added : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (added == null) {
            added = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
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
        StaticPermissionEntity that = (StaticPermissionEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StaticPermissionEntity{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", permission='" + permission + '\'' +
                ", added=" + added +
                '}';
    }
}
