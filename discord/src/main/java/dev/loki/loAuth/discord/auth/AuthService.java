package dev.loki.loAuth.discord.auth;

import dev.loki.loAuth.common.db.DatabaseManager;
import dev.loki.loAuth.discord.cfg.BotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AuthService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final DatabaseManager db;
    private final BotConfig cfg;
    private final JDA jda;
    private final ScheduledExecutorService scheduler;

    public AuthService(final DatabaseManager db, final BotConfig cfg, final JDA jda) {
        this.db = db;
        this.cfg = cfg;
        this.jda = jda;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auth-expiry");
            t.setDaemon(true);
            return t;
        });

        int ttl = cfg.getAuthTimeoutSeconds();
        scheduler.scheduleAtFixedRate(this::kickExpiredLogins, ttl, ttl, TimeUnit.SECONDS);
    }

    public void verifyNewUser(final String code, final String discordId) {
        Optional<String> nick = db.consumeVerificationCode(code.toUpperCase());
        if (nick.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired code: " + code);
        }
        db.addUser(nick.get(), discordId);
        log.info("New user registered: {} -> {}", nick.get(), discordId);
    }

    public void requestLoginApproval(final String minecraftNick, final String discordId) {
        String token = db.createLoginToken(minecraftNick, discordId);

        User discordUser = jda.retrieveUserById(discordId).complete();
        discordUser.openPrivateChannel().queue(channel ->
            channel.sendMessage(
                "🔐 **Запрос на вход**\n" +
                "Игрок **" + minecraftNick + "** пытается зайти на сервер.\n" +
                "Поставь реакцию ✅ на это сообщение чтобы разрешить вход.\n" +
                "Сообщение истекает через " + cfg.getAuthTimeoutSeconds() + " секунд."
            ).queue(message -> {
                message.addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✅")).queue();
                db.updateLoginMessageId(token, message.getId());
            }),
            error -> log.warn("Cannot send DM to {}: {}", discordId, error.getMessage())
        );
    }

    public void approveLogin(final String discordId) {
        db.findByDiscordId(discordId).ifPresent(user -> {
            Optional<String> nick = db.consumeLoginToken(findTokenByDiscordId(discordId));
            nick.ifPresent(n -> log.info("Login approved for {} ({})", n, discordId));
        });
    }

    private String findTokenByDiscordId(final String discordId) {
        // Ищем токен через БД — запрос по discord_id в pending_logins
        // consumeLoginToken вызывается из LoginReactionListener напрямую по token из message
        return discordId; // placeholder — см. LoginReactionListener
    }

    private void kickExpiredLogins() {
        try {
            List<String> expired = db.popExpiredLogins();
            if (!expired.isEmpty()) {
                log.info("Expired logins cleaned up: {}", expired);
            }
        } catch (Exception e) {
            log.error("Error during expired login cleanup", e);
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }
}
