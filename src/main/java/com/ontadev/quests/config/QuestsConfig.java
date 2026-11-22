// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.config;

import com.ontadev.libs.config.YamlConfig;
import com.ontadev.libs.config.Comment;
import com.ontadev.libs.message.Message;
import lombok.Getter;
import lombok.Setter;

@com.ontadev.libs.ioc.annotation.stereotype.Config
@Getter
@Setter
public class QuestsConfig extends YamlConfig {

    @Comment("Пропуски нового игрока. Изменение не затрагивает существующих игроков.\nДля изменения у всех: /quests reset skips <число> confirm.")
    private int skipsPerPlayer = 3;

    @Comment("Интервал сохранения накопленного прогресса в секундах, минимум 1.\nЗавершение квеста сохраняется сразу. Изменения применяются через /quests reload.")
    private int saveIntervalSeconds = 10;

    @Comment("Тексты поддерживают MiniMessage. <quest_title> - название квеста.")
    private Message questTitle = new Message("<gold><quest_title>");

    @Comment("Номер квеста для игроков с правом quests.quest.id. Плейсхолдер: <quest_id>.")
    private Message questIdLore = new Message("", "<dark_gray>ID квеста: <white><quest_id>");

    @Comment("Только подпись сочетания клавиш, подставляется вместо <bind> в подсказках.\nСамо действие всегда выполняется через Shift + ЛКМ.")
    private String skipBind = "Shift + ЛКМ";

    @Comment("Подсказки пропуска: доступ определяется правом quests.skip.\n<bind> - подпись из skipBind.")
    private Message skipAvailableHint = new Message("<green>Пропустить квест: <white><bind>");

    private Message skipLockedHint = new Message("<red>Пропустить квест: <white><bind> <red>(только с VIP)");

    private Message questClosedTitle = new Message("<italic><red>Квест недоступен</italic>");

    @Comment("Описания квестов: <quest_type> - действие, <quest_target> - цель,\n<quest_requirement> - нужное количество, <enchantments> - список зачарований.")
    private Message completedQuest = new Message(
            "",
            "<italic><gray>Для выполнения данного квеста</italic>",
            "<italic><gray>Вам нужно <quest_type>:</italic>",
            "<italic><yellow><quest_target></italic>",
            "<italic><yellow><enchantments></italic>",
            "<italic><gray>Прогресс: <quest_requirement>/<quest_requirement></italic>",
            "<italic><gray>Статус: <green>Выполнено</italic>",
            "",
            "<italic>Вы уже прошли квест</italic>"
    );

    @Comment("Дополнительные плейсхолдеры текущего квеста: <quest_progress> - прогресс,\n<quest_skips> - оставшиеся пропуски.")
    private Message currentQuest = new Message(
            "",
            "<gray>Для выполнения данного квеста",
            "<gray>Вам нужно <quest_type>:",
            "<italic><yellow><quest_target></italic>",
            "<italic><yellow><enchantments></italic>",
            "<gray>Прогресс: <quest_progress>/<quest_requirement>",
            "<italic><gray>Статус: <red>Не выполнено</italic>",
            "",
            "<italic><gray>Пропуски квестов: </gray><yellow><quest_skips></yellow></italic>"
    );

    private Message closedQuest = new Message(
            "<italic><red>Для начала выполните</italic>",
            "<italic><red>предыдущие задания</italic>"
    );

    @Comment("<reward_items> раскрывается в список предметов награды по шаблонам ниже.")
    private Message rewardContents = new Message("<gray>Вы получите:", "<reward_items>", "");

    @Comment("Строка предмета: <name> - название, <amount> - количество.")
    private String rewardItemLine = "<gray> • <white><name> <gray>×<amount>";

    @Comment("Строка для каждого зачарования, включая зачарования в книгах. Плейсхолдер: <enchantment>.")
    private String rewardEnchantLine = "<dark_gray>    ◦ <aqua><enchantment>";

    private String rewardNoItemsLine = "<gray> • <white>Особая награда";

    @Comment("Описание закрытого квеста для игроков с правом quests.quest.id.\nПраво позволяет просматривать квест, но не выполнять его раньше очереди.")
    private Message adminClosedQuest = new Message(
            "",
            "<gray>Для выполнения данного квеста",
            "<gray>Вам нужно <quest_type>:",
            "<italic><yellow><quest_target></italic>",
            "<italic><yellow><enchantments></italic>",
            "<gray>Цель: <quest_requirement>",
            "<italic><gray>Статус: <red>Закрыт</red> (вы видите его как админ)</italic>"
    );

    private Message rewardTitle = new Message("<italic><gold>НАГРАДА</italic>");

    private Message rewardClaimed = new Message("<yellow>Вы уже забрали награду");

    private Message rewardAvailableLore = new Message("<italic><green>Нажмите для получения награды</italic>");

    private Message rewardNotAvailableLore = new Message("<italic><red>Пройдите этап, чтобы забрать награду</italic>");

    @Comment("Стандартный заголовок использует глифы ресурспака.\nБез ресурспака замените, например, на <gold>Квесты.")
    private Message menuTitle = new Message("§fꈁꀀꈂꍁꈂꀁ§0ꈃꄒ");

    @Override
    public String getFileName() {
        return "config";
    }
}
