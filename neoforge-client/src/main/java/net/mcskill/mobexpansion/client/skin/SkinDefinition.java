package net.mcskill.mobexpansion.client.skin;

import net.mcskill.mobexpansion.Core;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.List;

/**
 * Описание скина игрока: ресурсы GeckoLib и какие кости повторяют позу ванильной модели игрока.
 * <p>
 * Ресурсы: {@code geo/skin/<id>.geo.json}, {@code animations/skin/<id>.animation.json},
 * {@code textures/entity/skin/<id>.png} (+ {@code <id>_emissive.png}, если {@code emissive}).
 * <p>
 * Кости тела/головы/рук/ног в модели должны стоять «как у игрока» при нулевых поворотах (руки и ноги вниз):
 * на них каждый кадр переносятся повороты ванильных частей (ходьба, удар, лук, щит, присед, элитры и т.д.),
 * поэтому анимации в json на эти кости не вешать — только на остальные (крылья, хвост, волосы, украшения).
 * В json обязательны анимации {@code idle, walk, run, flap, glide, flare} (см. {@link PlayerSkinAnimatable}).
 *
 * @param scale     масштаб модели относительно её пикселей
 * @param lift      подъём модели (в пикселях модели), если в нулевой позе ступни уходят ниже нуля
 * @param emissive  есть ли светящаяся текстура {@code <id>_emissive.png}
 * @param body      кость тела, родитель головы/рук/ног (повороты детей пересчитываются относительно неё)
 * @param rightHand кость, к которой крепится предмет в правой руке (кисть или предплечье с кистью)
 * @param rightGrip точка хвата относительно pivot кости {@code rightHand}, в пикселях модели и осях GeckoLib
 *                  (X зеркален относительно Blockbench), лицевая сторона кисти — по -Z
 * @param firstPersonScale масштаб руки от первого лица
 * @param firstPersonHand  центр кончиков пальцев правой руки относительно pivot плеча (пиксели модели, оси GeckoLib):
 *                         по нему рука от первого лица ставится так, чтобы кисть была там же, где ванильная
 * @param loops     анимации, которые играют всегда, поверх основных (каждая — своим контроллером)
 */
public record SkinDefinition(String id, float scale, float lift, boolean emissive,
                             String body, String head, String rightArm, String leftArm, String rightLeg, String leftLeg,
                             String rightHand, String leftHand, Vector3f rightGrip, Vector3f leftGrip,
                             float firstPersonScale, Vector3f firstPersonHand,
                             List<String> loops, SkinAttackStyle attack) {

    /** Козлиный демон Capra (Littleroom): рост ~81 px, корона-нимб, огненный взмах. */
    public static final SkinDefinition CAPRA = new SkinDefinition("capra", 0.45F, 2.3F, true,
            "capra", "h_head", "rightbicep", "leftbicep", "rightthigh", "leftthigh",
            "righthand", "lefthand", new Vector3f(-1.9F, -5.0F, -1.8F), new Vector3f(1.9F, -5.0F, -1.8F),
            0.42F, new Vector3f(4.1F, -40.2F, -2.3F),
            List.of("crown_spin"), SkinAttackStyle.CAPRA);

    /** Суккуба Lillith (Littleroom): рост ~40 px, хвост, крылья, волосы, розовый вихрь и пентаграмма. */
    public static final SkinDefinition LILLITH = new SkinDefinition("lillith", 0.8F, 2.0F, false,
            "body", "h_head", "right_arm", "left_arm", "right_thigh", "left_thigh",
            "forearm2", "forearm", new Vector3f(0.0F, -10.5F, -3.0F), new Vector3f(0.0F, -10.5F, -3.0F),
            0.6F, new Vector3f(1.6F, -22.8F, 0.5F),
            List.of(), SkinAttackStyle.LILLITH);

    public ResourceLocation model() {
        return Core.loc("geo/skin/" + id + ".geo.json");
    }

    public ResourceLocation animations() {
        return Core.loc("animations/skin/" + id + ".animation.json");
    }

    public ResourceLocation texture() {
        return Core.loc("textures/entity/skin/" + id + ".png");
    }

    public ResourceLocation emissiveTexture() {
        return Core.loc("textures/entity/skin/" + id + "_emissive.png");
    }
}
