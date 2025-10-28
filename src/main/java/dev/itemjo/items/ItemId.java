package dev.itemjo.items;

public enum ItemId {
    DRAGON_HELMET,
    KING_ELYTRA,
    UNBREAKABLE_ELYTRA,
    SLOW_SNOWBALL,
    REPAIRER,
    POTION_STRENGTH_2,
    POTION_SPEED_2,
    POTION_FIRE_RES_2,
    POTION_REGEN_2,
    POTION_HEAL_2;

    public static ItemId fromString(String value) {
        for (ItemId id : values()) {
            if (id.name().equalsIgnoreCase(value) || value.replace('-', '_').equalsIgnoreCase(id.name())) {
                return id;
            }
        }
        return null;
    }
}
