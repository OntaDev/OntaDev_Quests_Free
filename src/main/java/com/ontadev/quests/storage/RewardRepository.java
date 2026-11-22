// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.storage;

import com.ontadev.libs.orm.OrmRepository;
import com.ontadev.libs.orm.annotation.Modifying;
import com.ontadev.libs.orm.annotation.Query;

import java.util.List;

public interface RewardRepository extends OrmRepository<RewardRecord, Integer> {

    @Query("SELECT * FROM quest_rewards ORDER BY id")
    List<RewardRecord> findAllOrdered();

    @Modifying("DELETE FROM quest_rewards WHERE id = ?")
    void deleteById(Integer id);
}
