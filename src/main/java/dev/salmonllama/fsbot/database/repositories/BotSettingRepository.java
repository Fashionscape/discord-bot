package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.BotSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotSettingRepository extends JpaRepository<BotSettingEntity, String> {
}
