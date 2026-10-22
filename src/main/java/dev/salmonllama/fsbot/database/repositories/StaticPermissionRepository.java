/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.StaticPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaticPermissionRepository extends JpaRepository<StaticPermissionEntity, Long> {

    List<StaticPermissionEntity> findByUserId(String userId);

    boolean existsByUserIdAndPermission(String userId, String permission);

    void deleteByUserIdAndPermission(String userId, String permission);
}
