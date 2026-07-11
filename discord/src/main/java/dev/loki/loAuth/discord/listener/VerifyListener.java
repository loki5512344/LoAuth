package dev.loki.loAuth.discord.listener;

import dev.loki.loAuth.common.exception.DuplicateUserException;
import dev.loki.loAuth.discord.auth.AuthService;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class VerifyListener extends ListenerAdapter {

    private static final String COMMAND = "!verify";

    private final AuthService auth;

    public VerifyListener(final AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.isFromType(ChannelType.PRIVATE)) {
            return;
        }

        String content = event.getMessage().getContentRaw().trim();
        if (!content.toUpperCase().startsWith(COMMAND)) {
            return;
        }

        String[] parts = content.split("\\s+", 2);
        if (parts.length < 2) {
            event.getChannel().sendMessage("Использование: `!verify КОД`").queue();
            return;
        }

        String code = parts[1].trim().toUpperCase();
        String discordId = event.getAuthor().getId();

        try {
            auth.verifyNewUser(code, discordId);
            event.getChannel().sendMessage("✅ Ты успешно зарегистрирован! Можешь заходить на сервер.").queue();
        } catch (IllegalArgumentException e) {
            event.getChannel().sendMessage("❌ Неверный или просроченный код. Зайди на сервер снова чтобы получить новый.").queue();
        } catch (DuplicateUserException e) {
            event.getChannel().sendMessage("⚠️ Этот Discord аккаунт уже привязан к другому игроку.").queue();
        }
    }
}
