package net.mcskill.mobexpansion.registry;

import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Supplier;

public class DeferredRegistryReflect {
    private static final Field entriesField;

    static {
        entriesField = ObfuscationReflectionHelper.findField(DeferredRegister.class, "entries");
    }

    public static <T> Map<DeferredHolder<T, ?>, Supplier<? extends T>> getEntries(DeferredRegister<T> register) {
        try {
            //noinspection unchecked
            return (Map<DeferredHolder<T, ?>, Supplier<? extends T>>) entriesField.get(register);
        } catch (Exception ex) {
            return null;
        }
    }
}