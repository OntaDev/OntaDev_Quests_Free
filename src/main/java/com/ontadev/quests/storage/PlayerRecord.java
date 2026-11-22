// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.storage;

import com.ontadev.libs.orm.annotation.entity.Column;
import com.ontadev.libs.orm.annotation.entity.Entity;
import com.ontadev.libs.orm.annotation.entity.Id;
import com.ontadev.libs.orm.annotation.entity.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table("quest_players")
@Getter
@Setter
@NoArgsConstructor
public class PlayerRecord {

    @Id
    @Column(length = 36)
    private String uuid;

    @Column
    private Integer skips;

    @Column
    private Integer progress;

    @Column("quest_id")
    private Integer questId;
}
