// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests;

import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.plugin.OntaDev_Template;

public final class OntaDevQuestsPlugin extends OntaDev_Template {

    @Override
    public void onPluginEnable(PluginIoC pluginIoC) {
        getLogger().info("OntaDev_Quests Free enabled");
    }

    @Override
    protected int getBStatsId() {
        return 35437;
    }

}
