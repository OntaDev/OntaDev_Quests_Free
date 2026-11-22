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
@Table("quest_rewards")
@Getter
@Setter
@NoArgsConstructor
public class RewardRecord {

    @Id
    private Integer id;

    @Column(type = "TEXT")
    private String icon;

    @Column(value = "reward_item", type = "TEXT")
    private String rewardItem;
}
