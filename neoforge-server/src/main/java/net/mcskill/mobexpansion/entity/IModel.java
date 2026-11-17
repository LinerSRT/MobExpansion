package net.mcskill.mobexpansion.entity;

public interface IModel {
    String modelName();

    String textureName();

    String animationName();

    /**
     * Кости, видимость которых задаёт сам моб, а не анимация (вспышки ударов, стрела в руке и т.п.).
     * Клиентская модель каждый кадр прячет из них те, для которых {@link #isBoneVisible} вернул false.
     */
    default java.util.List<String> toggledBones() {
        return java.util.List.of();
    }

    /** Вызывается на клиенте при рендере для костей из {@link #toggledBones()}. */
    default boolean isBoneVisible(String bone) {
        return true;
    }
}
