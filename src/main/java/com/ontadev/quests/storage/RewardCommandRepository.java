// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.storage;

import com.ontadev.libs.orm.OrmRepository;
import com.ontadev.libs.orm.annotation.Modifying;
import com.ontadev.libs.orm.annotation.Query;

import java.util.List;

public interface RewardCommandRepository extends OrmRepository<RewardCommandRecord, Long> {

    @Query("SELECT * FROM quest_reward_commands ORDER BY id")
    List<RewardCommandRecord> findAllOrdered();

    @Modifying("DELETE FROM quest_reward_commands WHERE reward_id = ?")
    void deleteByReward(Integer rewardId);
}
