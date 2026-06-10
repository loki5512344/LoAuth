package dev.loki.loAuth.discord.cfg;

import dev.loki.loAuth.common.cfg.DbConfig;

public interface BotConfig extends DbConfig {
    String getToken();
}
