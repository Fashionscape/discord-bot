/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.database.entities;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "server_configs")
public class ServerConfigEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "prefix", length = 32)
    private String prefix = "~";

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(name = "welcome_channel", length = 64)
    private String welcomeChannel;

    public ServerConfigEntity() {
    }

    public ServerConfigEntity(String id, String prefix, String welcomeMessage, String welcomeChannel) {
        this.id = id;
        this.prefix = prefix != null ? prefix : "~";
        this.welcomeMessage = welcomeMessage;
        this.welcomeChannel = welcomeChannel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getWelcomeChannel() {
        return welcomeChannel;
    }

    public void setWelcomeChannel(String welcomeChannel) {
        this.welcomeChannel = welcomeChannel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerConfigEntity that = (ServerConfigEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ServerConfigEntity{" +
                "id='" + id + '\'' +
                ", prefix='" + prefix + '\'' +
                ", welcomeChannel='" + welcomeChannel + '\'' +
                '}';
    }
}
