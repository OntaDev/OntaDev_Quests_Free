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
@Table("quests")
@Getter
@Setter
@NoArgsConstructor
public class QuestRecord {

    @Id
    private Integer id;

    @Column()
    private String title;

    @Column(value = "quest_type", length = 32)
    private String questType;

    @Column
    private Integer goal;

    @Column(type = "TEXT")
    private String target;

    @Column(value = "target_entity", length = 64)
    private String targetEntity;

    @Column(type = "TEXT")
    private String icon;
}
