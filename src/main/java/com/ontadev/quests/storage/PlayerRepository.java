// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.storage;

import com.ontadev.libs.orm.OrmRepository;
import com.ontadev.libs.orm.annotation.Query;

import java.util.Optional;

public interface PlayerRepository extends OrmRepository<PlayerRecord, String> {
    @Query("SELECT * FROM quest_players WHERE uuid = ?")
    Optional<PlayerRecord> findByUuid(String uuid);
}
