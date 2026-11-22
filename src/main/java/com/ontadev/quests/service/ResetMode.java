// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.service;

public enum ResetMode {
    PROGRESS, SKIPS, ALL;

    public boolean resetsProgress() {
        return this != SKIPS;
    }

    public boolean resetsSkips() {
        return this != PROGRESS;
    }
}
