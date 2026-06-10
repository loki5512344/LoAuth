package dev.loki.loAuth.common.cfg;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class YamlConfig {

    private final Map<String, Object> data;

    @SuppressWarnings("unchecked")
    public YamlConfig(final Path path) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(path)) {
            Object loaded = yaml.load(in);
            if (loaded instanceof Map) {
                this.data = (Map<String, Object>) loaded;
            } else {
                throw new IOException("Invalid config format: expected a map at root");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getRequired(final String key, final Class<T> type) {
        Object val = data.get(key);
        if (val == null) {
            throw new IllegalStateException("Missing config key: '" + key + "'");
        }
        if (!type.isInstance(val)) {
            throw new IllegalStateException("Config key '" + key + "' has wrong type: expected " + type.getSimpleName());
        }
        return (T) val;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final String key, final T defaultValue) {
        Object val = data.get(key);
        if (val == null) return defaultValue;
        try {
            return (T) val;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}
