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
public class MessagesConfig extends YamlConfig {

    @Comment("Тексты поддерживают MiniMessage. Для применения изменений: /quests reload.\nПлейсхолдеры в угловых скобках заменяются данными команды.")
    private Message noPermission = new Message("<red>Недостаточно прав");
    private Message onlyPlayers = new Message("<red>Эту команду можно выполнить только в игре");
    private Message pageNotAvailable = new Message("<red>Страница с квестами недоступна");
    private Message questSkipped = new Message("<green>Квест пропущен");
    private Message questCompleted = new Message("<green>Квест завершён");
    private Message questCreated = new Message("<green>Квест создан");
    private Message playerOffline = new Message("<red>Игрок не найден");
    private Message rewardNotAvailable = new Message("<red>Награда не доступна");
    private Message rewardAlreadyGot = new Message("<red>Вы уже забрали награду");
    private Message rewardCollected = new Message("<green>Награда получена");
    private Message notEnoughSpace = new Message("<red>Освободите инвентарь");
    private Message notNumber = new Message("<red>Вы ввели не число");
    private Message questSkipsEnded = new Message("<red>Пропуски квестов закончились");
    private Message questSkipNotAvailable = new Message("<red>Пропускать квесты могут только игроки с VIP");
    private Message questsEnded = new Message("<red>Квесты закончились");

    @Comment("<count> - количество изменённых квестов, <from> и <to> - границы диапазона.")
    private Message questIconChanged = new Message("<green>Иконка изменена у квестов: <count> (<from>-<to>)");
    private Message questIconUsage = new Message(
            "<yellow>/quests quest icon {номер или от-до} - <gold>поставить иконкой предмет из правой руки");
    private Message questDeleted = new Message("<green>Квест успешно удалён");
    private Message playerNotFound = new Message("<red>Игрок не в сети");
    private Message questNotFound = new Message("<red>Квест не найден");
    private Message questTypeNotFound = new Message("<red>Такой тип квеста не найден");
    private Message noQuestToConfirm = new Message("<red>Для начала создайте квест");
    private Message questCancelled = new Message("<green>Квест снят с добавления");
    private Message noQuestIcon = new Message("<red>Нет предмета для иконки квеста в правой руке");
    private Message noQuestTarget = new Message("<yellow>В левой руке пусто: для квеста подойдёт любой предмет");
    private Message badArguments = new Message("<red>Неправильные аргументы");
    private Message entityNotFound = new Message("<yellow>Такой моб не найден: для квеста подойдёт любой моб");
    private Message error = new Message("<red>Возникла ошибка");
    private Message reloadDone = new Message("<green>Конфиги и переводы квестов перезагружены");
    private Message reloadFailed = new Message("<red>Не удалось перезагрузить конфиги. Прежние настройки сохранены; подробности в консоли");
    @Comment("Предпросмотр: <name>, <type>, <goal>, <target>, <entity>, <icon>\n- название, тип, количество, предмет цели, моб и иконка квеста.")
    private Message questPreview = new Message(
            "<yellow>Квест:",
            "<yellow>Название: <name>",
            "<yellow>Тип: <type>",
            "<yellow>Кол-во для выполнения: <goal>",
            "<yellow>Цель предмет: <target>",
            "<yellow>Цель моб: <entity>",
            "<yellow>Иконка: <icon>",
            "<green>Для подтверждения создания квеста пропишите /quests quest confirm",
            "<red>Для отмены /quests quest cancel"
    );
    private Message questCreateUsage = new Message(
            "<yellow>/quests quest create {Тип квеста} {Цель кол-во} {Моб (для KILL_MOBS)} {Название}",
            "<yellow>Правая рука - иконка, левая - цель",
            "<yellow>/quests quest delete {id необязательно} - <gold>удаление квеста",
            "<yellow>/quests quest confirm - <gold>подтверждение создания квеста",
            "<yellow>/quests quest cancel - <gold>отмена создания квеста"
    );
    private Message rewardUsage = new Message(
            "<yellow>/quests reward create {id необязательно} - <gold>правая рука иконка, левая награда",
            "<yellow>/quests reward delete {id необязательно}"
    );
    private Message commandsUsage = new Message(
            "<yellow>/quests commands {clear|list} {id награды}",
            "<yellow>/quests commands add {id награды} {команда...} - <gold>%player% заменится ником игрока"
    );
    private Message pageUsage = new Message(
            "<yellow>/quests page list - <gold>страницы и сколько на них заполнено",
            "<yellow>/quests page info {страница} - <gold>чего не хватает на странице"
    );
    @Comment("Список страниц: <pages> - число страниц. В строках ниже <page> - номер,\n<quests>/<max_quests> и <rewards>/<max_rewards> - заполненность.\nВ подробностях: <missing_quests> и <missing_rewards> - недостающие номера.")
    private Message pageListHeader = new Message("<yellow>Страниц: <white><pages>");
    private Message pageListLine = new Message("<gray>Страница <page>: <white>квестов <quests>/<max_quests>, наград <rewards>/<max_rewards>");
    private Message pageInfo = new Message(
            "<yellow>Страница <page>: <white>квестов <quests>/<max_quests>, наград <rewards>/<max_rewards>",
            "<gray>Нет квестов: <white><missing_quests>",
            "<gray>Нет наград: <white><missing_rewards>"
    );
    private Message userUsage = new Message(
            "<yellow>/quests user {ник / UUID} {skips|quests|progress} {число}",
            "<yellow>/quests user {ник / UUID} rewards list",
            "<yellow>/quests user {ник / UUID} rewards {add|remove} {id}"
    );

    private Message rewardCreated = new Message("<green>Награда успешно создана");
    @Comment("<id> - номер награды, <player> - имя получателя.")
    private Message rewardGiven = new Message("<green>Награда <id> выдана игроку <player>");
    private Message rewardGiveUsage = new Message("<yellow>/quests reward give {id} {игрок} - <gold>выдать предметы награды для проверки (без команд и отметки о получении)");
    private Message rewardDeleted = new Message("<green>Награда успешно удалена");
    private Message rewardNotFound = new Message("<red>Награда не существует");
    private Message noRewardIcon = new Message("<red>Предмет для иконки не обнаружен в правой руке");
    private Message noRewardItem = new Message("<red>Предмет для награды не обнаружен в левой руке");
    private Message rewardIdNeeded = new Message("<red>Укажите id награды");

    private Message commandAdded = new Message("<green>Команда добавлена");
    private Message commandsCleared = new Message("<green>Команды награды удалены");
    private Message commandsList = new Message("<green>Команды награды <id>:");
    private Message commandsEmpty = new Message("<yellow>У награды нет команд");

    private Message playerDataUpdated = new Message("<green>Данные игрока обновлены");
    @Comment("<rewards> - номера уже полученных наград игрока.")
    private Message playerRewardsList = new Message("<green>Полученные награды: <rewards>");
    private Message playerRewardAdded = new Message("<green>Награда добавлена к списку полученных");
    private Message playerRewardRemoved = new Message("<green>Награда удалена из полученных");
    private Message playerHasReward = new Message("<red>Эта награда уже есть у игрока");
    private Message playerHasNoReward = new Message("<red>Игрок ещё не получал такую награду");
    private Message unknownAction = new Message("<red>Неизвестное действие: <action>");

    private Message resetUsage = new Message(
            "<yellow>/quests reset progress [confirm] - <gold>сбросить прохождение и полученные награды у всех, сохранить пропуски",
            "<yellow>/quests reset skips [число] [confirm] - <gold>установить пропуски всем (без числа - skipsPerPlayer из конфига)",
            "<yellow>/quests reset all [confirm] - <red>удалить все квесты, награды и их команды, сбросить прохождение и пропуски"
    );
    private Message resetProgressWarning = new Message("<yellow>У ВСЕХ игроков, включая офлайн: текущий квест станет 1, прогресс 0, отметки о полученных наградах будут очищены. Пропуски и сами квесты/награды сохранятся.");
    @Comment("В предупреждениях о сбросе <skips> - новое количество пропусков у каждого игрока.")
    private Message resetSkipsWarning = new Message("<yellow>Количество пропусков у ВСЕХ существующих игроков, включая офлайн, станет <skips>. Прохождение и награды сохранятся.");
    private Message resetAllWarning = new Message("<red>Будут удалены ВСЕ квесты, награды и команды наград. У ВСЕХ игроков сбросится прохождение и список полученных наград, пропусков станет <skips>. Уже выданные предметы не отзываются.");
    @Comment("<command> - готовая команда подтверждения сброса.")
    private Message resetConfirmation = new Message("<yellow>Для подтверждения выполните: <white><command>");
    private Message resetStarted = new Message("<yellow>Сброс начат. Дождитесь завершения.");
    private Message resetInProgress = new Message("<yellow>Данные квестов сбрасываются или уже были сброшены. Повторите команду после завершения.");
    @Comment("<mode> - режим сброса: progress, skips или all.")
    private Message resetDone = new Message("<green>Массовый сброс <mode> завершён для всех игроков.");
    private Message resetFailed = new Message("<red>Сброс не выполнен. Подробности в консоли сервера.");

    @Override
    public String getFileName() {
        return "messages";
    }
}
