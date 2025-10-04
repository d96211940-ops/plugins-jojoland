package dev.itemjo.items;

import dev.itemjo.ItemJoPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemRegistry {
    private final ItemJoPlugin plugin;

    public static final String NBT_KEY_ID = "itemjo:id";
    public static final String NBT_KEY_USES = "itemjo:uses";

    public ItemRegistry(ItemJoPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // nothing to cache yet; parsing happens on demand
    }

    public ItemStack create(ItemId predefinedId, int amount) {
        // Backward compatibility to predefined keys in config
        String path = "items." + predefinedId.name();
        if (!plugin.getConfig().isConfigurationSection(path)) {
            // Fallback to hard-coded minimal defaults
            return createFallback(predefinedId, amount);
        }
        return createFromSection(predefinedId.name(), plugin.getConfig().getConfigurationSection(path), amount);
    }

    public ItemStack createByStringId(String id, int amount) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("items." + id);
        if (section == null) return null;
        return createFromSection(id, section, amount);
    }

    private ItemStack createFromSection(String id, ConfigurationSection section, int amount) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().set(Keys.ID, PersistentDataType.STRING, id);
            String display = section.getString("display-name");
            if (display != null) meta.setDisplayName(color(display));
            if (section.getBoolean("unbreakable", false)) meta.setUnbreakable(true);
            for (String flag : section.getStringList("flags")) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flag));
                } catch (IllegalArgumentException ignored) {}
            }
        });

        // Potion data
        if (stack.getItemMeta() instanceof PotionMeta && section.isConfigurationSection("potion")) {
            PotionMeta pm = (PotionMeta) stack.getItemMeta();
            ConfigurationSection ps = section.getConfigurationSection("potion");
            String base = ps.getString("base", "WATER");
            PotionType type = parsePotionType(base);
            boolean upgraded = ps.getBoolean("upgraded", false);
            pm.setBasePotionData(new PotionData(type, false, upgraded));
            stack.setItemMeta(pm);
        }

        // Attributes
        if (section.isList("attributes")) {
            for (Object o : section.getList("attributes")) {
                if (!(o instanceof java.util.Map)) continue;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) o;
                try {
                    Attribute attribute = Attribute.valueOf(String.valueOf(map.get("attribute")));
                    EquipmentSlot slot = EquipmentSlot.valueOf(String.valueOf(map.get("slot")));
                    double amountVal = Double.parseDouble(String.valueOf(map.get("amount")));
                    AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(String.valueOf(map.get("operation")));
                    AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "cfg", amountVal, op, slot);
                    ItemMeta meta = stack.getItemMeta();
                    meta.addAttributeModifier(attribute, modifier);
                    stack.setItemMeta(meta);
                } catch (Exception ignored) {}
            }
        }

        // Behaviors
        if (section.isList("behaviors")) {
            for (Object o : section.getList("behaviors")) {
                if (!(o instanceof java.util.Map)) continue;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> b = (java.util.Map<String, Object>) o;
                String type = String.valueOf(b.get("type"));
                if ("reusable_potion".equalsIgnoreCase(type)) {
                    int uses = parseTimeOrInt(b.get("uses"), 15);
                    stack.editMeta(meta -> meta.getPersistentDataContainer().set(Keys.USES, PersistentDataType.INTEGER, uses));
                    // Update lore
                    ItemMeta meta = stack.getItemMeta();
                    java.util.List<String> lore = meta.getLore();
                    if (lore == null) lore = new java.util.ArrayList<>();
                    if (lore.isEmpty()) lore.add(ChatColor.GRAY + "Использований: " + uses);
                    else lore.set(0, ChatColor.GRAY + "Использований: " + uses);
                    meta.setLore(lore);
                    stack.setItemMeta(meta);
                }
            }
        }

        return stack;
    }

    private ItemStack createFallback(ItemId id, int amount) {
        switch (id) {
            case DRAGON_HELMET:
                return createDragonHelmet(amount);
            case KING_ELYTRA:
                return createKingElytra(amount);
            case UNBREAKABLE_ELYTRA:
                return createUnbreakableElytra(amount);
            case SLOW_SNOWBALL:
                return tagBasic(new ItemStack(Material.SNOWBALL, amount), id, meta -> meta.setDisplayName("Снежок"));
            case REPAIRER:
                return tagBasic(new ItemStack(Material.BLAZE_ROD, amount), id, meta -> meta.setDisplayName("Ремонтник"));
            case POTION_STRENGTH_2:
                return createReusablePotion(id, PotionType.STRENGTH, true, amount);
            case POTION_SPEED_2:
                return createReusablePotion(id, PotionType.SWIFTNESS, true, amount);
            case POTION_FIRE_RES_2:
                return createReusablePotion(id, PotionType.FIRE_RESISTANCE, true, amount);
            case POTION_REGEN_2:
                return createReusablePotion(id, PotionType.REGENERATION, true, amount);
            case POTION_HEAL_2:
                return createReusablePotion(id, PotionType.HEALING, true, amount);
        }
        return null;
    }

    private ItemStack createDragonHelmet(int amount) {
        ItemStack stack = new ItemStack(Material.GOLDEN_HELMET, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        meta.setDisplayName(color(plugin.getConfig().getString("items.DRAGON_HELMET.display-name", "Шлем Дракона")));
        // Simulate netherite helmet attributes: armor and toughness
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(UUID.randomUUID(), "armor", 3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(UUID.randomUUID(), "toughness", 3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(UUID.randomUUID(), "kb", 0.1, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        stack.setItemMeta(meta);
        return tagBasic(stack, ItemId.DRAGON_HELMET, null);
    }

    private ItemStack createKingElytra(int amount) {
        ItemStack stack = new ItemStack(Material.ELYTRA, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(color(plugin.getConfig().getString("items.KING_ELYTRA.display-name", "Элитры Короля")));
        // Add netherite chestplate-like attributes
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(UUID.randomUUID(), "armor", 8.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(UUID.randomUUID(), "toughness", 3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(UUID.randomUUID(), "kb", 0.1, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return tagBasic(stack, ItemId.KING_ELYTRA, null);
    }

    private ItemStack createUnbreakableElytra(int amount) {
        ItemStack stack = new ItemStack(Material.ELYTRA, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.setDisplayName(color(plugin.getConfig().getString("items.UNBREAKABLE_ELYTRA.display-name", "Улучшенные Элитры")));
        stack.setItemMeta(meta);
        return tagBasic(stack, ItemId.UNBREAKABLE_ELYTRA, null);
    }

    private ItemStack createReusablePotion(ItemId id, PotionType type, boolean upgraded, int amount) {
        ItemStack stack = new ItemStack(Material.POTION, Math.max(1, amount));
        ItemMeta baseMeta = stack.getItemMeta();
        if (!(baseMeta instanceof PotionMeta)) return stack;
        PotionMeta meta = (PotionMeta) baseMeta;
        meta.setBasePotionData(new PotionData(type, false, upgraded));
        meta.setDisplayName(color(plugin.getConfig().getString("items." + id.name() + ".display-name", id.name())));
        List<String> lore = new ArrayList<>();
        int uses = plugin.getConfig().getInt("reusable-potions.uses", 15);
        lore.add(ChatColor.GRAY + "Использований: " + uses);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        ItemStack tagged = tagBasic(stack, id, null);
        tagged.editMeta(m -> m.getPersistentDataContainer().set(Keys.USES, org.bukkit.persistence.PersistentDataType.INTEGER, uses));
        return tagged;
    }

    private ItemStack tagBasic(ItemStack itemStack, ItemId id, java.util.function.Consumer<ItemMeta> metaEditor) {
        itemStack.editMeta(meta -> {
            meta.getPersistentDataContainer().set(Keys.ID, org.bukkit.persistence.PersistentDataType.STRING, id.name());
            if (metaEditor != null) metaEditor.accept(meta);
        });
        return itemStack;
    }

    private String color(String input) {
        if (input == null) return null;
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private PotionType parsePotionType(String name) {
        try {
            return PotionType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PotionType.WATER;
        }
    }

    private int parseTimeOrInt(Object value, int def) {
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

    public static class Keys {
        public static final org.bukkit.NamespacedKey ID = new org.bukkit.NamespacedKey(ItemJoPlugin.getInstance(), NBT_KEY_ID);
        public static final org.bukkit.NamespacedKey USES = new org.bukkit.NamespacedKey(ItemJoPlugin.getInstance(), NBT_KEY_USES);
        public static final org.bukkit.NamespacedKey SLOW_SNOWBALL_PROJECTILE = new org.bukkit.NamespacedKey(ItemJoPlugin.getInstance(), "itemjo:slow_snowball_projectile");
        public static final org.bukkit.NamespacedKey SLOW_SNOWBALL_DURATION_TICKS = new org.bukkit.NamespacedKey(ItemJoPlugin.getInstance(), "itemjo:slow_duration");
        public static final org.bukkit.NamespacedKey SLOW_SNOWBALL_AMPLIFIER = new org.bukkit.NamespacedKey(ItemJoPlugin.getInstance(), "itemjo:slow_amplifier");
    }
}
