package net.mcskill.mobexpansion.spawner;

public enum SpawnMode {
    STANDARD,
    RANDOM;

    public static SpawnMode byName(String name) {
        if (name == null || name.isEmpty())
            return STANDARD;
        try {
            return SpawnMode.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return STANDARD;
        }
    }

    public SpawnMode next() {
        final SpawnMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
