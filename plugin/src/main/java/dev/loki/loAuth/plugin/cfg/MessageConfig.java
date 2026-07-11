package dev.loki.loAuth.plugin.cfg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageConfig {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");

    private final Map<String, Object> raw;

    @SuppressWarnings("unchecked")
    public MessageConfig(final Map<String, Object> raw) {
        this.raw = raw;
    }

    public Component header() {
        return text("first_login.header");
    }

    public Component returningHeader() {
        return text("returning_login.header");
    }

    public Component returningBody() {
        return text("returning_login.body");
    }

    public Component returningTimeout() {
        return text("returning_login.timeout");
    }

    public Component expiredHeader() {
        return text("expired.header");
    }

    public Component expiredBody() {
        return text("expired.body");
    }

    public Component success() {
        return text("success");
    }

    public Component code(final String code) {
        return parse("first_login.code", Map.of("code", code))
            .clickEvent(ClickEvent.copyToClipboard(code))
            .hoverEvent(HoverEvent.showText(text("first_login.code_hover")));
    }

    public Component discordLink(final String link) {
        String fullLink = link.startsWith("http") ? link : "https://" + link;
        return parse("first_login.discord", Map.of("link", link))
            .clickEvent(ClickEvent.openUrl(fullLink))
            .hoverEvent(HoverEvent.showText(text("first_login.discord_hover")));
    }

    public Component expires(final int seconds) {
        return text("first_login.expires", Map.of("time", String.valueOf(seconds)));
    }

    public Component react(final int seconds) {
        return text("returning_login.react", Map.of("time", String.valueOf(seconds)));
    }

    // ─── Parser ───────────────────────────────────────────────────────────────

    private Component text(final String path) {
        return text(path, Map.of());
    }

    private Component text(final String path, final Map<String, String> placeholders) {
        String rawStr = resolveRaw(path);
        if (rawStr == null) {
            return Component.text('[' + path + ']');
        }
        return parse(rawStr, placeholders);
    }

    private Component parse(final String rawStr, final Map<String, String> placeholders) {
        String str = applyPlaceholders(rawStr, placeholders);

        List<int[]> hexes = new ArrayList<>();
        Matcher m = HEX_PATTERN.matcher(str);
        while (m.find()) {
            hexes.add(new int[]{m.start(), m.end(), Integer.parseInt(m.group(1), 16)});
        }

        TextComponent.Builder builder = Component.text();
        int last = 0;

        for (int i = 0; i < hexes.size(); i++) {
            int[] h = hexes.get(i);
            if (last < h[0]) {
                builder.append(Component.text(str.substring(last, h[0])));
            }
            TextColor color = TextColor.color(h[2]);
            int nextStart = (i + 1 < hexes.size()) ? hexes.get(i + 1)[0] : str.length();
            builder.append(Component.text(str.substring(h[1], nextStart), color));
            last = nextStart;
        }

        if (last < str.length()) {
            builder.append(Component.text(str.substring(last)));
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private String resolveRaw(final String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = raw;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return null;
            }
        }
        Object val = current.get(parts[parts.length - 1]);
        return val instanceof String ? (String) val : null;
    }

    private static String applyPlaceholders(final String str, final Map<String, String> placeholders) {
        String result = str;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
