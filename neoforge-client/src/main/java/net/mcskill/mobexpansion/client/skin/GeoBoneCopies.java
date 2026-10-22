package net.mcskill.mobexpansion.client.skin;

import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.List;

/**
 * Копии костей модели в позе покоя. Кости BakedGeoModel общие для всех, кто рисует модель, а рендерер игрока в скине
 * пропускает расчёт анимации, если уже считал его в этом тике, — доверяя, что позу никто не трогал. Всё, что рисует
 * модель скина не как игрока (предмет, рука от первого лица), рисует свою копию, иначе анимация персонажей дёргается.
 */
final class GeoBoneCopies {
    private GeoBoneCopies() {
    }

    static BakedGeoModel copyOf(BakedGeoModel source) {
        final List<GeoBone> top = new ArrayList<>();
        for (GeoBone bone : source.topLevelBones())
            top.add(copyOf(bone, null));
        return new BakedGeoModel(top, source.properties());
    }

    static GeoBone copyOf(GeoBone source, @Nullable GeoBone parent) {
        final GeoBone bone = new GeoBone(parent, source.getName(), source.getMirror(), source.getInflate(), source.shouldNeverRender(), source.getReset());
        bone.updatePivot(source.getPivotX(), source.getPivotY(), source.getPivotZ());
        final BoneSnapshot rest = source.getInitialSnapshot();
        if (rest != null)
            bone.updateRotation(rest.getRotX(), rest.getRotY(), rest.getRotZ());
        else
            bone.updateRotation(source.getRotX(), source.getRotY(), source.getRotZ());
        bone.getCubes().addAll(source.getCubes());
        for (GeoBone child : source.getChildBones())
            bone.getChildBones().add(copyOf(child, bone));
        return bone;
    }
}
