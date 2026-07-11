package dev.loki.loAuth.plugin.cfg;

import dev.loki.loAuth.common.cfg.DbConfig;
import dev.loki.loAuth.common.cfg.YamlConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class PluginCfg implements DbConfig {

    private final YamlConfig cfg;

    public PluginCfg(final Path configPath) throws IOException {
        cfg = new YamlConfig(configPath);
    }

    public String getMode() {
        return cfg.getRequired("mode", String.class);
    }

    public String getToken() {
        return cfg.get("token", "");
    }

    public String getDiscordLink() {
        return cfg.get("discord_link", "discord.gg/ЗАМЕНИ_НА_СВОЙ");
    }

    @Override
    public String getDbType() {
        return cfg.getRequired("db_type", String.class);
    }

    @Override
    public String getDbHost() {
        return cfg.getRequired("db_host", String.class);
    }

    @Override
    public int getDbPort() {
        return cfg.getRequired("db_port", Integer.class);
    }

    @Override
    public String getDbName() {
        return cfg.getRequired("db_name", String.class);
    }

    @Override
    public String getDbUser() {
        return cfg.getRequired("db_user", String.class);
    }

    @Override
    public String getDbPassword() {
        return cfg.getRequired("db_password", String.class);
    }

    @Override
    public int getAuthTimeoutSeconds() {
        return cfg.getRequired("auth_timeout_seconds", Integer.class);
    }

    @Override
    public int getVerificationCodeLength() {
        return cfg.getRequired("verification_code_length", Integer.class);
    }

    public MessageConfig getMessages() {
        Object msgs = cfg.get("messages", null);
        if (msgs instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) msgs;
            return new MessageConfig(map);
        }
        return new MessageConfig(Map.of());
    }
}
