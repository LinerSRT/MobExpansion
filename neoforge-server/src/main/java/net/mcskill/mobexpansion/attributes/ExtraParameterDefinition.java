package net.mcskill.mobexpansion.attributes;

public record ExtraParameterDefinition(String key, double defaultValue, double min, double max) {
    public double sanitize(double value) {
        return Math.max(min, Math.min(max, value));
    }

    public String langKey() {
        return "gui.mobexpansion.mob_drop_config.extra." + key;
    }

    public String tooltipLangKey() {
        return "gui.mobexpansion.mob_drop_config.extra." + key + ".tooltip";
    }
}
