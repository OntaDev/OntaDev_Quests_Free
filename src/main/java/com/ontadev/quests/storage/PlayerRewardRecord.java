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
@Table("quest_player_rewards")
@Getter
@Setter
@NoArgsConstructor
public class PlayerRewardRecord {

    @Id
    @GeneratedValue
    private Long id;

    @Column(value = "player_uuid", length = 36, indexed = true)
    private String playerUuid;

    @Column(value = "reward_id")
    private Integer rewardId;

    public PlayerRewardRecord(String playerUuid, Integer rewardId) {
        this.playerUuid = playerUuid;
        this.rewardId = rewardId;
    }
}
