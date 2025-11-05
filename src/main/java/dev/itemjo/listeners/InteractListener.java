package dev.itemjo.listeners;

import dev.itemjo.ItemJoPlugin;
import dev.itemjo.items.ItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InteractListener implements Listener {
    private final ItemJoPlugin plugin;

    public InteractListener(ItemJoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String id = meta.getPersistentDataContainer().get(ItemRegistry.Keys.ID, org.bukkit.persistence.PersistentDataType.STRING);
        if (id == null) return;
        Player player = event.getPlayer();

        if (isRepairer(id)) {
            handleRepairer(event, player, item);
            return;
        }
        if (isReusablePotion(id)) {
            handleReusablePotion(event, player, item, id);
            return;
        }
    }

    private void handleRepairer(PlayerInteractEvent event, Player player, ItemStack item) {
        String actionKey = "repairer";
        if (plugin.getCooldownService().isOnCooldown(player, actionKey)) {
            player.sendMessage(ChatColor.RED + "КД: " + plugin.getCooldownService().getRemainingSeconds(player, actionKey) + " сек.");
            event.setCancelled(true);
            return;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack helmet = inv.getHelmet();
        ItemStack chest = inv.getChestplate();
        ItemStack legs = inv.getLeggings();
        ItemStack boots = inv.getBoots();

        int repaired = 0;
        repaired += repairItem(helmet);
        repaired += repairItem(chest);
        repaired += repairItem(legs);
        repaired += repairItem(boots);

        if (repaired > 0) {
            int seconds = resolveRepairerCooldown(idFromItem(item));
            plugin.getCooldownService().applyCooldown(player, actionKey, seconds);
            player.sendMessage(ChatColor.GREEN + "Починка: " + repaired + " предметов на 50%.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Нечего чинить.");
        }
    }

    private int repairItem(ItemStack item) {
        if (item == null) return 0;
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable)) return 0;
        org.bukkit.inventory.meta.Damageable dmg = (org.bukkit.inventory.meta.Damageable) item.getItemMeta();
        int currentDamage = dmg.getDamage();
        if (currentDamage <= 0) return 0;
        int newDamage = Math.max(0, currentDamage - Math.max(1, currentDamage / 2));
        dmg.setDamage(newDamage);
        item.setItemMeta((ItemMeta) dmg);
        return 1;
    }

    private void handleReusablePotion(PlayerInteractEvent event, Player player, ItemStack item, String id) {
        event.setCancelled(true); // prevent default drink behavior to control uses
        if (item.getType() != Material.POTION) return;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta)) return;
        PotionMeta potionMeta = (PotionMeta) meta;

        Integer uses = meta.getPersistentDataContainer().get(ItemRegistry.Keys.USES, org.bukkit.persistence.PersistentDataType.INTEGER);
        if (uses == null || uses <= 0) uses = plugin.getConfig().getInt("reusable-potions.uses", 15);

        // Apply effects from config
        org.bukkit.configuration.ConfigurationSection s = plugin.getConfig().getConfigurationSection("items." + id);
        if (s != null && s.isList("behaviors")) {
            for (Object o : s.getList("behaviors")) {
                if (!(o instanceof java.util.Map)) continue;
                java.util.Map<?, ?> b = (java.util.Map<?, ?>) o;
                if (!"reusable_potion".equalsIgnoreCase(String.valueOf(b.get("type")))) continue;
                Object effectsObj = b.get("effects");
                if (effectsObj instanceof java.util.List) {
                    for (Object eo : (java.util.List<?>) effectsObj) {
                        if (!(eo instanceof java.util.Map)) continue;
                        java.util.Map<?, ?> em = (java.util.Map<?, ?>) eo;
                        String effectName = String.valueOf(em.get("effect"));
                        int duration = parseToTicks(em.get("duration"), 0);
                        int amplifier = parseToInt(em.get("amplifier"), 0);
                        org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(effectName);
                        if (type != null) {
                            player.addPotionEffect(new PotionEffect(type, duration, amplifier, false, true, true));
                        }
                    }
                }
            }
        }

        int remaining = Math.max(0, uses - 1);
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        if (lore.isEmpty()) lore.add(ChatColor.GRAY + "Использований: " + remaining);
        else lore.set(0, ChatColor.GRAY + "Использований: " + remaining);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(ItemRegistry.Keys.USES, org.bukkit.persistence.PersistentDataType.INTEGER, remaining);
        item.setItemMeta(meta);

        if (remaining == 0) {
            // Consume the item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }
            player.sendMessage(ChatColor.RED + "Зелье израсходовано.");
        }
    }

    private boolean isReusablePotion(String id) {
        org.bukkit.configuration.ConfigurationSection s = plugin.getConfig().getConfigurationSection("items." + id);
        if (s == null || !s.isList("behaviors")) return false;
        for (Object o : s.getList("behaviors")) {
            if (!(o instanceof java.util.Map)) continue;
            java.util.Map<?, ?> b = (java.util.Map<?, ?>) o;
            if ("reusable_potion".equalsIgnoreCase(String.valueOf(b.get("type")))) return true;
        }
        return false;
    }

    private String idFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(ItemRegistry.Keys.ID, org.bukkit.persistence.PersistentDataType.STRING);
    }

    private boolean isRepairer(String id) {
        org.bukkit.configuration.ConfigurationSection s = plugin.getConfig().getConfigurationSection("items." + id);
        if (s == null || !s.isList("behaviors")) return false;
        for (Object o : s.getList("behaviors")) {
            if (!(o instanceof java.util.Map)) continue;
            java.util.Map<?, ?> b = (java.util.Map<?, ?>) o;
            if ("repairer".equalsIgnoreCase(String.valueOf(b.get("type")))) return true;
        }
        return false;
    }

    private int resolveRepairerCooldown(String id) {
        org.bukkit.configuration.ConfigurationSection s = plugin.getConfig().getConfigurationSection("items." + id);
        if (s != null && s.isList("behaviors")) {
            for (Object o : s.getList("behaviors")) {
                if (!(o instanceof java.util.Map)) continue;
                java.util.Map<?, ?> b = (java.util.Map<?, ?>) o;
                if ("repairer".equalsIgnoreCase(String.valueOf(b.get("type")))) {
                    Object v = b.get("cooldown");
                    return parseToSeconds(v, plugin.getCooldownService().getRepairerSeconds());
                }
            }
        }
        return plugin.getCooldownService().getRepairerSeconds();
    }

    private int parseToTicks(Object value, int defTicks) {
        int seconds = parseToSeconds(value, 0);
        if (seconds > 0) return seconds * 20;
        if (value instanceof Number) return ((Number) value).intValue();
        return defTicks;
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

    private int parseToInt(Object value, int def) {
        if (value == null) return def;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return def; }
    }
}
