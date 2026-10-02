package com.darkspirit69.jadebridge;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.bukkit.configuration.ConfigurationSection;

public final class Settings {

    private boolean enabled = true;
    private boolean debug;
    private int blockRequestLimit = 20;
    private int entityRequestLimit = 20;
    private int positionDeviation = 21;
    private Map<Identifier, Object> configOverrides = Map.of();

    public void load(JadeBridge plugin) {
        plugin.reloadConfig();
        var config = plugin.getConfig();
        enabled = config.getBoolean("enabled", true);
        debug = config.getBoolean("debug", false);
        blockRequestLimit = clamp(config.getInt("rate-limit.block-requests", 20));
        entityRequestLimit = clamp(config.getInt("rate-limit.entity-requests", 20));
        positionDeviation = Math.max(0, config.getInt("max-position-deviation", 21));
        configOverrides = readOverrides(config.getConfigurationSection("config-overrides"), plugin.getSLF4JLogger());
    }

    private static int clamp(int value) {
        return Math.max(1, value);
    }

    private static Map<Identifier, Object> readOverrides(ConfigurationSection section, org.slf4j.Logger logger) {
        if (section == null) {
            return Map.of();
        }
        Map<Identifier, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            try {
                Identifier id = Identifier.parse(key);
                Object value = section.get(key);
                if (value instanceof Boolean || value instanceof Number || value instanceof String) {
                    values.put(id, value);
                } else {
                    throw new IllegalArgumentException("not a boolean, number or string");
                }
            } catch (Exception e) {
                logger.warn("Ignoring invalid config override '{}': {}", key, e.getMessage());
            }
        }
        return values;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int blockRequestLimit() {
        return blockRequestLimit;
    }

    public int entityRequestLimit() {
        return entityRequestLimit;
    }

    public int positionDeviation() {
        return positionDeviation;
    }

    public Map<Identifier, Object> configOverrides() {
        return configOverrides;
    }
}
