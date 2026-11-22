// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.storage;

import com.ontadev.libs.orm.OrmRepository;
import com.ontadev.libs.orm.annotation.Modifying;
import com.ontadev.libs.orm.annotation.Query;

import java.util.List;

public interface PlayerRewardRepository extends OrmRepository<PlayerRewardRecord, Long> {

    @Query("SELECT * FROM quest_player_rewards WHERE player_uuid = ?")
    List<PlayerRewardRecord> findByPlayer(String playerUuid);

    @Modifying("DELETE FROM quest_player_rewards WHERE player_uuid = ?")
    void deleteByPlayer(String playerUuid);
}
