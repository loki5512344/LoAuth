package dev.loki.loAuth.discord;

import dev.loki.loAuth.discord.cfg.BotCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BotLauncher {

    private BotLauncher() {}

    private static final Logger log = LoggerFactory.getLogger(BotLauncher.class);

    public static void main(final String[] args) {
        BotCfg cfg;
        try {
            cfg = new BotCfg(getOrCreateConfig("config.yml"));
        } catch (IOException e) {
            log.error("Failed to load config: {}", e.getMessage());
            return;
        }

        Bot bot = new Bot(cfg);

        Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdown, "shutdown"));

        log.info("loAuth bot started.");
    }

    private static Path getOrCreateConfig(final String fileName) throws IOException {
        Path path = Path.of(System.getProperty("user.dir")).resolve(fileName);
        if (Files.notExists(path)) {
            try (InputStream in = BotLauncher.class.getResourceAsStream("/" + fileName)) {
                if (in == null) {
                    throw new IOException("Resource not found: " + fileName);
                }
                Files.copy(in, path);
            }
        }
        return path;
    }
}
