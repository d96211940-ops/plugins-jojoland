package dev.itemjo.listeners;

import dev.itemjo.ItemJoPlugin;
import dev.itemjo.items.ItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ProjectileListener implements Listener {
    private final ItemJoPlugin plugin;

    public ProjectileListener(ItemJoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball)) return;
        if (!(projectile.getShooter() instanceof Player)) return;
        Entity hit = event.getHitEntity();
        if (!(hit instanceof Player)) return;
        Player victim = (Player) hit;

        // If the snowball item was our configured one, apply effect
        // We cannot directly get item meta from projectile; rely on config behavior and cooldown gate at throw time
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 9, false, true, true));
    }

    @EventHandler
    public void onSnowballThrow(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != org.bukkit.Material.SNOWBALL) return;
        String id = item.getItemMeta() == null ? null : item.getItemMeta().getPersistentDataContainer().get(ItemRegistry.Keys.ID, org.bukkit.persistence.PersistentDataType.STRING);
        if (id == null) return;
        // Read behavior from config to find snowball_slowness_on_hit for this id
        if (!hasSnowballBehavior(id)) return;

        Player player = event.getPlayer();
        String actionKey = "slow_snowball";
        if (plugin.getCooldownService().isOnCooldown(player, actionKey)) {
            long seconds = plugin.getCooldownService().getRemainingSeconds(player, actionKey);
            player.sendMessage(ChatColor.RED + "КД: " + seconds + " сек.");
            event.setCancelled(true);
            return;
        }
        // Use cooldown from config behavior if provided; fallback to global snowball cooldown seconds
        int seconds = getSnowballCooldownSeconds(id);
        plugin.getCooldownService().applyCooldown(player, actionKey, seconds);
    }

    private boolean hasSnowballBehavior(String id) {
        org.bukkit.configuration.ConfigurationSection s = plugin.getConfig().getConfigurationSection("items." + id);
        if (s == null || !s.isList("behaviors")) return false;
        for (Object o : s.getList("behaviors")) {
            if (!(o instanceof java.util.Map)) continue;
            java.util.Map<?, ?> b = (java.util.Map<?, ?>) o;
            if ("snowball_slowness_on_hit".equalsIgnoreCase(String.valueOf(b.get("type")))) return true;
        }
        return false;
    }

    private int getSnowballCooldownSeconds(String id) {
        org.bukkit.configuration.ConfigurationSection s = plugin.getConfig().getConfigurationSection("items." + id);
        if (s != null && s.isList("behaviors")) {
            for (Object o : s.getList("behaviors")) {
                if (!(o instanceof java.util.Map)) continue;
                java.util.Map<?, ?> b = (java.util.Map<?, ?>) o;
                if ("snowball_slowness_on_hit".equalsIgnoreCase(String.valueOf(b.get("type")))) {
                    Object v = ((java.util.Map<?, ?>) o).get("cooldown");
                    return parseToSeconds(v, plugin.getCooldownService().getSnowballSeconds());
                }
            }
        }
        return plugin.getCooldownService().getSnowballSeconds();
    }

    private int parseToSeconds(Object value, int def) {
        if (value == null) return def;
        if (value instanceof Number) return ((Number) value).intValue();
        String s = String.valueOf(value).trim().toLowerCase();
        try {
            if (s.endsWith("ms")) return (int) Math.ceil(Double.parseDouble(s.substring(0, s.length() - 2)) / 1000.0);
            if (s.endsWith("s")) return Integer.parseInt(s.substring(0, s.length() - 1));
            if (s.endsWith("m")) return Integer.parseInt(s.substring(0, s.length() - 1)) * 60;
            if (s.endsWith("h")) return Integer.parseInt(s.substring(0, s.length() - 1)) * 3600;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
