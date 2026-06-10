package dev.loki.loAuth.discord;

import dev.loki.loAuth.common.db.DatabaseManager;
import dev.loki.loAuth.discord.auth.AuthService;
import dev.loki.loAuth.discord.cfg.BotConfig;
import dev.loki.loAuth.discord.listener.LoginReactionListener;
import dev.loki.loAuth.discord.listener.VerifyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Bot {

    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    private final BotConfig cfg;
    private final DatabaseManager db;
    private final JDA jda;
    private final AuthService auth;

    public Bot(final BotConfig cfg) {
        this.cfg = cfg;
        this.db = new DatabaseManager(cfg, "loAuthBot", 10);
        this.jda = JDABuilder
            .createDefault(cfg.getToken(),
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.MESSAGE_CONTENT)
            .setStatus(OnlineStatus.ONLINE)
            .build();
        this.auth = new AuthService(db, cfg, jda);

        jda.addEventListener(
            new VerifyListener(auth),
            new LoginReactionListener(db)
        );
    }

    public void shutdown() {
        auth.close();
        db.close();
        jda.shutdown();
        log.info("loAuth bot shut down.");
    }
}
