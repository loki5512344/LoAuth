package dev.loki.loAuth.discord.listener;

import dev.loki.loAuth.common.db.DatabaseManager;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoginReactionListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoginReactionListener.class);
    private static final String CHECKMARK = "✅";

    private final DatabaseManager db;

    public LoginReactionListener(final DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void onMessageReactionAdd(final MessageReactionAddEvent event) {
        if (event.getUser() == null || event.getUser().isBot()) return;
        if (!CHECKMARK.equals(event.getReaction().getEmoji().getName())) return;

        // Ищем токен по message_id — бот сохранил его при отправке
        db.consumeLoginTokenByMessageId(event.getMessageId()).ifPresent(nick ->
            log.info("Login approved for {} via reaction.", nick)
        );
    }
}
