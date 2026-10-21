/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.BlockedServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedServerRepository extends JpaRepository<BlockedServerEntity, String> {

    boolean existsById(String id);
}
