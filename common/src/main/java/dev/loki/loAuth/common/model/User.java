package dev.loki.loAuth.common.model;

public record User(long id, String minecraftNick, String discordId, java.time.Instant createdAt) {}