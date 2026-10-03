package net.mcskill.mobexpansion.drops;

public enum DropDestination {
    KILLER_INVENTORY,
    WORLD;

    public DropDestination next() {
        return this == WORLD ? KILLER_INVENTORY : WORLD;
    }

    public boolean givesToKiller() {
        return this != WORLD;
    }

    public String langSuffix() {
        return this == WORLD ? "world" : "killer";
    }

    public static DropDestination byName(String name) {
        if (name != null && "WORLD".equalsIgnoreCase(name.trim()))
            return WORLD;
        return KILLER_INVENTORY;
    }
}
