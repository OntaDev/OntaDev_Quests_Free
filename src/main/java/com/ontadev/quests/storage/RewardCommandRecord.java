// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.storage;

import com.ontadev.libs.orm.annotation.entity.Column;
import com.ontadev.libs.orm.annotation.entity.Entity;
import com.ontadev.libs.orm.annotation.entity.GeneratedValue;
import com.ontadev.libs.orm.annotation.entity.Id;
import com.ontadev.libs.orm.annotation.entity.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table("quest_reward_commands")
@Getter
@Setter
@NoArgsConstructor
public class RewardCommandRecord {

    @Id
    @GeneratedValue
    private Long id;

    @Column(value = "reward_id", indexed = true)
    private Integer rewardId;

    @Column(type = "TEXT")
    private String command;

    public RewardCommandRecord(Integer rewardId, String command) {
        this.rewardId = rewardId;
        this.command = command;
    }
}
