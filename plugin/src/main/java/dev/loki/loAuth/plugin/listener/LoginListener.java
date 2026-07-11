package dev.loki.loAuth.plugin.listener;

import dev.loki.loAuth.plugin.cfg.MessageConfig;
import dev.loki.loAuth.plugin.cfg.PluginCfg;
import dev.loki.loAuth.common.db.DatabaseManager;
import dev.loki.loAuth.common.model.User;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class LoginListener {

    private final ProxyServer server;
    private final DatabaseManager db;
    private final PluginCfg cfg;
    private final Logger log;
    private final MessageConfig msg;

    private final Map<String, ScheduledFuture<?>> pendingKicks = new ConcurrentHashMap<>();
    private final Map<String, String> pendingTokens = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "foxauth-scheduler");
        t.setDaemon(true);
        return t;
    });

    public LoginListener(final ProxyServer server, final DatabaseManager db,
                         final PluginCfg cfg, final Logger log) {
        this.server = server;
        this.db = db;
        this.cfg = cfg;
        this.log = log;
        this.msg = cfg.getMessages();
    }

    @Subscribe
    public void onLogin(final LoginEvent event) {
        String nick = event.getPlayer().getUsername();

        if (!db.isRegistered(nick)) {
            handleFirstLogin(event, nick);
        } else {
            handleReturningLogin(event, nick);
        }
    }

    private void handleFirstLogin(final LoginEvent event, final String nick) {
        String code = db.createVerificationCode(nick);
        int ttl = cfg.getAuthTimeoutSeconds();
        String discordLink = cfg.getDiscordLink();

        event.getPlayer().sendMessage(Component.text()
                .append(msg.header()).appendNewline()
                .append(msg.code(code)).appendNewline()
                .append(msg.discordLink(discordLink)).appendNewline()
                .append(msg.expires(ttl))
                .build()
        );

        log.info("First login for {}, verification code issued.", nick);
    }

    private void handleReturningLogin(final LoginEvent event, final String nick) {
        Optional<String> discordId = db.findByMinecraftNick(nick).map(User::discordId);
        if (discordId.isEmpty()) {
            event.getPlayer().sendMessage(Component.text("Ошибка авторизации. Обратись к администратору.", NamedTextColor.RED));
            return;
        }

        String token = db.createLoginToken(nick, discordId.get());
        pendingTokens.put(nick, token);
        pollApproval(nick, token);

        log.info("Returning login for {}, awaiting Discord approval.", nick);
    }

    private void pollApproval(final String nick, final String token) {
        int ttl = cfg.getAuthTimeoutSeconds();

        Component awaitMessage = Component.text()
                .append(msg.returningHeader()).appendNewline()
                .append(msg.returningBody()).appendNewline()
                .append(msg.react(ttl)).appendNewline()
                .append(msg.returningTimeout())
                .build();

        ScheduledFuture<?>[] pollRef = new ScheduledFuture<?>[1];
        pollRef[0] = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (db.isTokenConsumed(token)) {
                    pollRef[0].cancel(false);
                    cancelKick(nick);
                    pendingTokens.remove(nick);
                    server.getPlayer(nick).ifPresent(p ->
                        p.sendMessage(msg.success())
                    );
                    log.info("Login approved for {}.", nick);
                }
            } catch (Exception e) {
                log.error("Poll error for {}: {}", nick, e.getMessage());
            }
        }, 2, 2, TimeUnit.SECONDS);

        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            pollRef[0].cancel(false);
            pendingTokens.remove(nick);
            db.deleteLoginToken(token);
            server.getPlayer(nick).ifPresent(p ->
                p.sendMessage(Component.text()
                    .append(msg.expiredHeader()).appendNewline()
                    .append(msg.expiredBody())
                    .build())
            );
            log.info("Login timeout for {}.", nick);
        }, ttl, TimeUnit.SECONDS);

        pendingKicks.put(nick, timeoutFuture);

        server.getPlayer(nick).ifPresent(p -> p.sendMessage(awaitMessage));
    }

    private void cancelKick(final String nick) {
        ScheduledFuture<?> f = pendingKicks.remove(nick);
        if (f != null) {
            f.cancel(false);
        }
    }
}
