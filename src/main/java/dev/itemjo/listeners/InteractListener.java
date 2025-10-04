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

        switch (id) {
            case "REPAIRER":
                handleRepairer(event, player, item);
                break;
            case "POTION_STRENGTH_2":
            case "POTION_SPEED_2":
            case "POTION_FIRE_RES_2":
            case "POTION_REGEN_2":
            case "POTION_HEAL_2":
                handleReusablePotion(event, player, item, id);
                break;
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
            plugin.getCooldownService().applyCooldown(player, actionKey, plugin.getCooldownService().getRepairerSeconds());
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

        // Apply effect based on id
        switch (id) {
            case "POTION_STRENGTH_2":
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 90 * 20, 1, false, true, true));
                break;
            case "POTION_SPEED_2":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 90 * 20, 1, false, true, true));
                break;
            case "POTION_FIRE_RES_2":
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 180 * 20, 0, false, true, true));
                break;
            case "POTION_REGEN_2":
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 22 * 20, 1, false, true, true));
                break;
            case "POTION_HEAL_2":
                player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1, false, true, true));
                break;
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
}
