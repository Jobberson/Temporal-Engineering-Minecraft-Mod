package com.snog.temporalengineering.api;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for temporal adapters.
 * Safe by design: no crashes allowed.
 */
public final class TemporalAdapterRegistry {

    private static final Map<Class<?>, TemporalAdapter> ADAPTERS = new ConcurrentHashMap<>();

    private TemporalAdapterRegistry() {}

    /**
     * Register an adapter for a BlockEntity class.
     */
    public static <T extends BlockEntity> void register(
            Class<T> blockEntityClass,
            TemporalAdapter adapter
    ) {
        ADAPTERS.put(blockEntityClass, adapter);
    }

    /**
     * Attempt to apply temporal acceleration.
     * This method MUST NEVER crash.
     */
    public static boolean tryApply(
            BlockEntity be,
            float multiplier,
            int durationTicks
    ) {
        if (be == null) return false;

        // 1️⃣ Native support
        if (be instanceof ITemporalAffectable affectable) {
            affectable.applyTimeMultiplier(multiplier, durationTicks);
            return true;
        }

        // 2️⃣ Adapter lookup
        Class<?> beClass = be.getClass();

        for (Map.Entry<Class<?>, TemporalAdapter> entry : ADAPTERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(beClass)) {
                try {
                    return entry.getValue().apply(be, multiplier, durationTicks);
                } catch (Throwable t) {
                    // SAFETY RULE: adapters are allowed to fail silently
                    return false;
                }
            }
        }

        return false;
    }
}
