// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.storage;

import com.ontadev.libs.orm.OrmRepository;
import com.ontadev.libs.orm.annotation.Modifying;
import com.ontadev.libs.orm.annotation.Query;

import java.util.List;

public interface QuestRepository extends OrmRepository<QuestRecord, Integer> {

    @Query("SELECT * FROM quests ORDER BY id")
    List<QuestRecord> findAllOrdered();

    @Modifying("DELETE FROM quests WHERE id = ?")
    void deleteById(Integer id);
}
