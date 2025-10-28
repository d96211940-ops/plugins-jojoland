package dev.itemjo.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownService {
    private final Plugin plugin;
    private final Map<String, Long> playerActionToReadyAtMs = new HashMap<>();

    private int snowballSeconds;
    private int repairerSeconds;

    public CooldownService(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.snowballSeconds = plugin.getConfig().getInt("cooldowns.snowball", 15);
        this.repairerSeconds = plugin.getConfig().getInt("cooldowns.repairer", 600);
    }

    public boolean isOnCooldown(Player player, String actionKey) {
        String key = buildKey(player.getUniqueId(), actionKey);
        long now = System.currentTimeMillis();
        Long readyAt = playerActionToReadyAtMs.get(key);
        return readyAt != null && readyAt > now;
    }

    public long getRemainingSeconds(Player player, String actionKey) {
        String key = buildKey(player.getUniqueId(), actionKey);
        long now = System.currentTimeMillis();
        Long readyAt = playerActionToReadyAtMs.get(key);
        if (readyAt == null || readyAt <= now) return 0;
        return Math.max(0, (readyAt - now + 999) / 1000);
    }

    public void applyCooldown(Player player, String actionKey, int seconds) {
        String key = buildKey(player.getUniqueId(), actionKey);
        long readyAt = System.currentTimeMillis() + seconds * 1000L;
        playerActionToReadyAtMs.put(key, readyAt);
    }

    public int getSnowballSeconds() {
        return snowballSeconds;
    }

    public int getRepairerSeconds() {
        return repairerSeconds;
    }

    private String buildKey(UUID uuid, String actionKey) {
        return uuid.toString() + ":" + actionKey;
    }
}
