/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.database.entities;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "color_roles", indexes = {
        @Index(name = "idx_color_roles_server", columnList = "server_id")
})
public class ColorRoleEntity {

    @Id
    @Column(name = "role_id", length = 64, nullable = false)
    private String roleId;

    @Column(name = "server_id", length = 64, nullable = false)
    private String serverId;

    @Column(name = "color", length = 32)
    private String color;

    @Column(name = "group_id", length = 64)
    private String groupId;

    public ColorRoleEntity() {
    }

    public ColorRoleEntity(String roleId, String serverId, String color, String groupId) {
        this.roleId = roleId;
        this.serverId = serverId;
        this.color = color;
        this.groupId = groupId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorRoleEntity that = (ColorRoleEntity) o;
        return Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId);
    }

    @Override
    public String toString() {
        return "ColorRoleEntity{" +
                "roleId='" + roleId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", color='" + color + '\'' +
                ", groupId='" + groupId + '\'' +
                '}';
    }
}
