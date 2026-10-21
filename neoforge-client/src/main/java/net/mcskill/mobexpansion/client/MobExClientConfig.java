package net.mcskill.mobexpansion.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Клиентские настройки (экран «Mods → MobExpansion → Config»). */
public final class MobExClientConfig {
    public static final ModConfigSpec SPEC;
    /** Не показывать эффект удара скина (взмах, печать, частицы) на самом себе при виде от первого лица. */
    public static final ModConfigSpec.BooleanValue HIDE_SKIN_ATTACK_FX_FIRST_PERSON;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        HIDE_SKIN_ATTACK_FX_FIRST_PERSON = builder
                .translation("config.mobexpansion.hide_skin_attack_fx_first_person")
                .define("hideSkinAttackFxFirstPerson", false);
        SPEC = builder.build();
    }

    private MobExClientConfig() {
    }
}
