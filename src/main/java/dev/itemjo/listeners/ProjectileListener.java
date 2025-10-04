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

        // Apply slowness 10 for 3 seconds (amplifier 9)
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 9, false, true, true));
    }

    @EventHandler
    public void onSnowballThrow(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != org.bukkit.Material.SNOWBALL) return;
        String id = item.getItemMeta() == null ? null : item.getItemMeta().getPersistentDataContainer().get(ItemRegistry.Keys.ID, org.bukkit.persistence.PersistentDataType.STRING);
        if (id == null || !id.equals("SLOW_SNOWBALL")) return;

        Player player = event.getPlayer();
        String actionKey = "slow_snowball";
        if (plugin.getCooldownService().isOnCooldown(player, actionKey)) {
            long seconds = plugin.getCooldownService().getRemainingSeconds(player, actionKey);
            player.sendMessage(ChatColor.RED + "КД: " + seconds + " сек.");
            event.setCancelled(true);
            return;
        }
        plugin.getCooldownService().applyCooldown(player, actionKey, plugin.getCooldownService().getSnowballSeconds());
    }
}
