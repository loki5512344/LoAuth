package dev.loki.loAuth.plugin;

import dev.loki.loAuth.plugin.cfg.PluginCfg;
import dev.loki.loAuth.plugin.listener.LoginListener;
import dev.loki.loAuth.common.db.DatabaseManager;
import dev.loki.loAuth.discord.Bot;
import dev.loki.loAuth.discord.cfg.BotConfig;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "loauth", name = "loAuth", version = "0.0.1",
        description = "Discord 2FA auth for Velocity", authors = {"loki"})
public final class FoxAuth {

    private final ProxyServer server;
    private final Logger log;
    private final Path dataDir;

    private DatabaseManager db;
    private Bot discordBot;

    @Inject
    public FoxAuth(final ProxyServer server, final Logger log, @DataDirectory final Path dataDir) {
        this.server = server;
        this.log = log;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onInit(final ProxyInitializeEvent event) {
        PluginCfg cfg;
        try {
            cfg = new PluginCfg(getOrCreateConfig("config.yml"));
        } catch (IOException e) {
            log.error("Failed to load config: {}", e.getMessage());
            return;
        }

        db = new DatabaseManager(cfg, "loAuthPlugin", 5);
        server.getEventManager().register(this, new LoginListener(server, db, cfg, log));

        if ("embedded".equals(cfg.getMode())) {
            startEmbeddedBot(cfg);
        } else {
            log.info("Mode: standalone — external Discord bot required.");
        }

        log.info("loAuth initialized.");
    }

    private void startEmbeddedBot(final PluginCfg cfg) {
        String token = cfg.getToken();
        if (token.isEmpty()) {
            log.warn("Mode is 'embedded' but token is empty! Discord bot not started.");
            return;
        }

        try {
            discordBot = new Bot(new BotConfig() {
                @Override public String getDbType() { return cfg.getDbType(); }
                @Override public String getToken() { return token; }
                @Override public String getDbHost() { return cfg.getDbHost(); }
                @Override public int getDbPort() { return cfg.getDbPort(); }
                @Override public String getDbName() { return cfg.getDbName(); }
                @Override public String getDbUser() { return cfg.getDbUser(); }
                @Override public String getDbPassword() { return cfg.getDbPassword(); }
                @Override public int getAuthTimeoutSeconds() { return cfg.getAuthTimeoutSeconds(); }
                @Override public int getVerificationCodeLength() { return cfg.getVerificationCodeLength(); }
            });
            log.info("Discord bot started in embedded mode.");
        } catch (Exception e) {
            log.error("Failed to start embedded Discord bot: {}", e.getMessage());
        }
    }

    @Subscribe
    public void onShutdown(final ProxyShutdownEvent event) {
        if (discordBot != null) discordBot.shutdown();
        if (db != null) db.close();
    }

    private Path getOrCreateConfig(final String fileName) throws IOException {
        Files.createDirectories(dataDir);
        Path target = dataDir.resolve(fileName);
        if (Files.notExists(target)) {
            try (InputStream in = getClass().getResourceAsStream("/" + fileName)) {
                if (in == null) throw new IOException("Resource not found: " + fileName);
                Files.copy(in, target);
            }
        }
        return target;
    }
}
