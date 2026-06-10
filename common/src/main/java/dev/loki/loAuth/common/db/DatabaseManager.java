package dev.loki.loAuth.common.db;

import dev.loki.loAuth.common.cfg.DbConfig;
import dev.loki.loAuth.common.exception.DatabaseException;
import dev.loki.loAuth.common.exception.DuplicateUserException;
import dev.loki.loAuth.common.model.User;
import dev.loki.loAuth.common.util.CodeGenerator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DatabaseManager implements AutoCloseable {

    private final HikariDataSource ds;
    private final DbConfig cfg;
    private final boolean sqlite;

    public DatabaseManager(final DbConfig cfg, final String appName, final int maxPoolSize) {
        this.cfg = cfg;
        this.sqlite = "sqlite".equals(cfg.getDbType());

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(cfg.getJdbcUrl());

        if (sqlite) {
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setMaximumPoolSize(1);
            hikari.setMinimumIdle(1);
            hikari.setConnectionTimeout(30_000);
            hikari.addDataSourceProperty("journal_mode", "WAL");
            hikari.addDataSourceProperty("busy_timeout", "30000");
        } else {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("PostgreSQL driver not found", e);
            }
            hikari.setUsername(cfg.getDbUser());
            hikari.setPassword(cfg.getDbPassword());
            hikari.setMaximumPoolSize(maxPoolSize);
            hikari.setMinimumIdle(Math.min(2, maxPoolSize));
            hikari.setConnectionTimeout(30_000);
            hikari.setIdleTimeout(600_000);
            hikari.setMaxLifetime(1_800_000);
            hikari.addDataSourceProperty("sslmode", "prefer");
            hikari.addDataSourceProperty("ApplicationName", appName);
            hikari.setDriverClassName("org.postgresql.Driver");
        }

        this.ds = new HikariDataSource(hikari);
        initSchema();
    }

    // ─── Schema ────────────────────────────────────────────────────────────────

    private void initSchema() {
        String usersTable = sqlite ? """
            CREATE TABLE IF NOT EXISTS users (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                minecraft_nick  TEXT    NOT NULL,
                discord_id      TEXT    NOT NULL,
                created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
                updated_at      TEXT    NOT NULL DEFAULT (datetime('now')),
                UNIQUE (minecraft_nick),
                UNIQUE (discord_id)
            )
            """ : """
            CREATE TABLE IF NOT EXISTS users (
                id              BIGSERIAL    PRIMARY KEY,
                minecraft_nick  VARCHAR(16)  NOT NULL,
                discord_id      VARCHAR(20)  NOT NULL,
                created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                CONSTRAINT uq_minecraft_nick UNIQUE (minecraft_nick),
                CONSTRAINT uq_discord_id     UNIQUE (discord_id),
                CONSTRAINT ck_minecraft_nick CHECK (minecraft_nick ~ '^[a-zA-Z0-9_]{3,16}$'),
                CONSTRAINT ck_discord_id     CHECK (discord_id ~ '^[0-9]{17,20}$')
            )
            """;

        String verificationsTable = sqlite ? """
            CREATE TABLE IF NOT EXISTS pending_verifications (
                code            TEXT    PRIMARY KEY,
                minecraft_nick  TEXT    NOT NULL UNIQUE,
                expires_at      INTEGER NOT NULL
            )
            """ : """
            CREATE TABLE IF NOT EXISTS pending_verifications (
                code            VARCHAR(16)  PRIMARY KEY,
                minecraft_nick  VARCHAR(16)  NOT NULL,
                expires_at      BIGINT       NOT NULL,
                CONSTRAINT uq_pv_minecraft_nick UNIQUE (minecraft_nick)
            )
            """;

        String loginsTable = sqlite ? """
            CREATE TABLE IF NOT EXISTS pending_logins (
                token           TEXT    PRIMARY KEY,
                discord_id      TEXT    NOT NULL UNIQUE,
                minecraft_nick  TEXT    NOT NULL UNIQUE,
                message_id      TEXT,
                expires_at      INTEGER NOT NULL
            )
            """ : """
            CREATE TABLE IF NOT EXISTS pending_logins (
                token           VARCHAR(16)  PRIMARY KEY,
                discord_id      VARCHAR(20)  NOT NULL,
                minecraft_nick  VARCHAR(16)  NOT NULL,
                message_id      VARCHAR(20),
                expires_at      BIGINT       NOT NULL,
                CONSTRAINT uq_pl_discord_id     UNIQUE (discord_id),
                CONSTRAINT uq_pl_minecraft_nick UNIQUE (minecraft_nick)
            )
            """;

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(verificationsTable);
            stmt.execute(loginsTable);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize DB schema", e);
        }
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    public User addUser(final String minecraftNick, final String discordId) {
        validateNick(minecraftNick);
        validateDiscordId(discordId);

        String sql = sqlite ? """
            INSERT INTO users (minecraft_nick, discord_id)
            VALUES (?, ?)
            """ : """
            INSERT INTO users (minecraft_nick, discord_id)
            VALUES (?, ?)
            RETURNING id, minecraft_nick, discord_id, created_at
            """;

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, minecraftNick);
            ps.setString(2, discordId);

            if (sqlite) {
                ps.executeUpdate();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        return new User(rs.getLong(1), minecraftNick, discordId, Instant.now());
                    }
                }
                throw new SQLException("INSERT returned no rows");
            } else {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapUser(rs);
                    throw new SQLException("INSERT returned no rows");
                }
            }
        } catch (SQLException e) {
            throw translateSqlException(e);
        }
    }

    public boolean isRegistered(final String minecraftNick) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT 1 FROM users WHERE LOWER(minecraft_nick) = LOWER(?) LIMIT 1")) {
            ps.setString(1, minecraftNick);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("DB error in isRegistered", e);
        }
    }

    public Optional<User> findByDiscordId(final String discordId) {
        validateDiscordId(discordId);
        return queryOneUser(
            "SELECT id, minecraft_nick, discord_id, created_at FROM users WHERE discord_id = ?",
            discordId
        );
    }

    public Optional<User> findByMinecraftNick(final String nick) {
        validateNick(nick);
        return queryOneUser(
            "SELECT id, minecraft_nick, discord_id, created_at FROM users WHERE LOWER(minecraft_nick) = LOWER(?)",
            nick
        );
    }

    public boolean removeUser(final String discordId) {
        validateDiscordId(discordId);
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE discord_id = ?")) {
            ps.setString(1, discordId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to remove user", e);
        }
    }

    // ─── Pending Verifications ────────────────────────────────────────────────

    public String createVerificationCode(final String minecraftNick) {
        validateNick(minecraftNick);
        String code = CodeGenerator.generate(cfg.getVerificationCodeLength());
        long expiresAt = Instant.now().getEpochSecond() + cfg.getAuthTimeoutSeconds();

        String sql = sqlite ? """
            INSERT INTO pending_verifications (code, minecraft_nick, expires_at)
            VALUES (?, ?, ?)
            ON CONFLICT(minecraft_nick) DO UPDATE
                SET code = excluded.code, expires_at = excluded.expires_at
            """ : """
            INSERT INTO pending_verifications (code, minecraft_nick, expires_at)
            VALUES (?, ?, ?)
            ON CONFLICT (minecraft_nick) DO UPDATE
                SET code = EXCLUDED.code, expires_at = EXCLUDED.expires_at
            """;

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, minecraftNick);
            ps.setLong(3, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create verification code", e);
        }
        return code;
    }

    public Optional<String> consumeVerificationCode(final String code) {
        if (sqlite) return consumeDeleteSelect("pending_verifications", "code", code);
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                DELETE FROM pending_verifications
                WHERE code = ? AND expires_at > ?
                RETURNING minecraft_nick
                """)) {
            ps.setString(1, code);
            ps.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("minecraft_nick")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to consume verification code", e);
        }
    }

    // ─── Pending Logins ───────────────────────────────────────────────────────

    public String createLoginToken(final String minecraftNick, final String discordId) {
        validateNick(minecraftNick);
        validateDiscordId(discordId);
        String token = CodeGenerator.generate(16);
        long expiresAt = Instant.now().getEpochSecond() + cfg.getAuthTimeoutSeconds();

        String sql = sqlite ? """
            INSERT INTO pending_logins (token, discord_id, minecraft_nick, expires_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(discord_id) DO UPDATE
                SET token = excluded.token,
                    minecraft_nick = excluded.minecraft_nick,
                    message_id = NULL,
                    expires_at = excluded.expires_at
            """ : """
            INSERT INTO pending_logins (token, discord_id, minecraft_nick, expires_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (discord_id) DO UPDATE
                SET token = EXCLUDED.token,
                    minecraft_nick = EXCLUDED.minecraft_nick,
                    message_id = NULL,
                    expires_at = EXCLUDED.expires_at
            """;

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, discordId);
            ps.setString(3, minecraftNick);
            ps.setLong(4, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create login token", e);
        }
        return token;
    }

    public void updateLoginMessageId(final String token, final String messageId) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE pending_logins SET message_id = ? WHERE token = ?")) {
            ps.setString(1, messageId);
            ps.setString(2, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update message id", e);
        }
    }

    public Optional<String> consumeLoginToken(final String token) {
        if (sqlite) return consumeDeleteSelect("pending_logins", "token", token);
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                DELETE FROM pending_logins
                WHERE token = ? AND expires_at > ?
                RETURNING minecraft_nick
                """)) {
            ps.setString(1, token);
            ps.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("minecraft_nick")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to consume login token", e);
        }
    }

    public Optional<String> consumeLoginTokenByMessageId(final String messageId) {
        if (sqlite) return consumeDeleteSelect("pending_logins", "message_id", messageId);
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                DELETE FROM pending_logins
                WHERE message_id = ? AND expires_at > ?
                RETURNING minecraft_nick
                """)) {
            ps.setString(1, messageId);
            ps.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("minecraft_nick")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to consume login token by message id", e);
        }
    }

    public boolean isTokenConsumed(final String token) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT 1 FROM pending_logins WHERE token = ? AND expires_at > ? LIMIT 1")) {
            ps.setString(1, token);
            ps.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("DB error in isTokenConsumed", e);
        }
    }

    public void deleteLoginToken(final String token) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM pending_logins WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete login token", e);
        }
    }

    public List<String> popExpiredLogins() {
        long now = Instant.now().getEpochSecond();
        List<String> nicks = new ArrayList<>();

        if (sqlite) {
            try (Connection conn = ds.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT minecraft_nick FROM pending_logins WHERE expires_at <= ?")) {
                    ps.setLong(1, now);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) nicks.add(rs.getString("minecraft_nick"));
                    }
                }
                if (!nicks.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM pending_logins WHERE expires_at <= ?")) {
                        ps.setLong(1, now);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to pop expired logins", e);
            }
            return nicks;
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM pending_logins WHERE expires_at <= ? RETURNING minecraft_nick")) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) nicks.add(rs.getString("minecraft_nick"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to pop expired logins", e);
        }
        return nicks;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Optional<String> consumeDeleteSelect(final String table, final String column, final String value) {
        long now = Instant.now().getEpochSecond();
        try (Connection conn = ds.getConnection()) {
            Optional<String> result;
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT minecraft_nick FROM " + table + " WHERE " + column + " = ? AND expires_at > ?")) {
                ps.setString(1, value);
                ps.setLong(2, now);
                try (ResultSet rs = ps.executeQuery()) {
                    result = rs.next() ? Optional.of(rs.getString("minecraft_nick")) : Optional.empty();
                }
            }
            if (result.isPresent()) {
                try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + table + " WHERE " + column + " = ?")) {
                    ps.setString(1, value);
                    ps.executeUpdate();
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to consume from " + table, e);
        }
    }

    private Optional<User> queryOneUser(final String sql, final String param) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Query failed", e);
        }
    }

    private static User mapUser(final ResultSet rs) throws SQLException {
        return new User(
            rs.getLong("id"),
            rs.getString("minecraft_nick"),
            rs.getString("discord_id"),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    private static void validateNick(final String nick) {
        if (nick == null || !nick.matches("^[a-zA-Z0-9_]{3,16}$"))
            throw new IllegalArgumentException("Invalid Minecraft nick: " + nick);
    }

    private static void validateDiscordId(final String id) {
        if (id == null || !id.matches("^[0-9]{17,20}$"))
            throw new IllegalArgumentException("Invalid Discord ID: " + id);
    }

    private static RuntimeException translateSqlException(final SQLException e) {
        String state = e.getSQLState();
        if ("23505".equals(state) || "23000".equals(state)) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("minecraft_nick") || msg.contains("UNIQUE constraint failed: users.minecraft_nick")))
                return new DuplicateUserException("Minecraft nick already registered");
            if (msg != null && (msg.contains("discord_id") || msg.contains("UNIQUE constraint failed: users.discord_id")))
                return new DuplicateUserException("Discord ID already registered");
            return new DuplicateUserException("Duplicate entry");
        }
        return new DatabaseException("Database error", e);
    }

    @Override
    public void close() {
        ds.close();
    }
}
