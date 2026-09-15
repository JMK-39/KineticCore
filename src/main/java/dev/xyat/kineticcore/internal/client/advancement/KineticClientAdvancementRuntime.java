package dev.xyat.kineticcore.internal.client.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KineticClientAdvancementRuntime {
    private static Field progressField;
    private static ClientAdvancements failedManager;

    private KineticClientAdvancementRuntime() {
    }

    public static Set<ResourceLocation> completedIds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return Set.of();

        ClientAdvancements manager = minecraft.getConnection().getAdvancements();
        if (manager == failedManager) return Set.of();

        try {
            if (progressField == null) {
                progressField = ObfuscationReflectionHelper.findField(ClientAdvancements.class, "f_104390_");
                progressField.setAccessible(true);
            }

            Object value = progressField.get(manager);
            if (!(value instanceof Map<?, ?> progressMap)) return Set.of();

            Set<ResourceLocation> completed = new LinkedHashSet<>();
            for (Map.Entry<?, ?> entry : progressMap.entrySet()) {
                if (entry.getKey() instanceof Advancement advancement
                        && entry.getValue() instanceof AdvancementProgress progress
                        && progress.isDone()) {
                    completed.add(advancement.getId());
                }
            }
            return Set.copyOf(completed);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            failedManager = manager;
            return Set.of();
        }
    }
    public static List<Advancement> all() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return List.of();
        return new ArrayList<>(minecraft.getConnection().getAdvancements().getAdvancements().getAllAdvancements());
    }

}
