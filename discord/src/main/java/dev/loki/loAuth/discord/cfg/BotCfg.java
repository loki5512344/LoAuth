package dev.loki.loAuth.discord.cfg;

import dev.loki.loAuth.common.cfg.YamlConfig;

import java.io.IOException;
import java.nio.file.Path;

public final class BotCfg implements BotConfig {

    private final YamlConfig cfg;

    public BotCfg(final Path configPath) throws IOException {
        cfg = new YamlConfig(configPath);
    }

    @Override public String getToken()                   { return cfg.getRequired("token",                   String.class); }
    @Override public String getDbType()                  { return cfg.getRequired("db_type",                 String.class); }
    @Override public String getDbHost()                  { return cfg.getRequired("db_host",                 String.class); }
    @Override public int    getDbPort()                  { return cfg.getRequired("db_port",                 Integer.class); }
    @Override public String getDbName()                  { return cfg.getRequired("db_name",                 String.class); }
    @Override public String getDbUser()                  { return cfg.getRequired("db_user",                 String.class); }
    @Override public String getDbPassword()              { return cfg.getRequired("db_password",             String.class); }
    @Override public int    getAuthTimeoutSeconds()      { return cfg.getRequired("auth_timeout_seconds",    Integer.class); }
    @Override public int    getVerificationCodeLength()  { return cfg.getRequired("verification_code_length",Integer.class); }
}
