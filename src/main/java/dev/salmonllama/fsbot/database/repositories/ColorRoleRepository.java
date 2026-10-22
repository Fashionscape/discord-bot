/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.ColorRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColorRoleRepository extends JpaRepository<ColorRoleEntity, String> {

    List<ColorRoleEntity> findByServerId(String serverId);

    Optional<ColorRoleEntity> findByRoleId(String roleId);

    void deleteByRoleId(String roleId);
}
