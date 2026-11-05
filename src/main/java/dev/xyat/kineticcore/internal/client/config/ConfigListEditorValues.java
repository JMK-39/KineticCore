package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigEntry;

import java.util.ArrayList;
import java.util.List;

/** Converts visual-editor drafts while leaving final acceptance and persistence to the config entry. */
final class ConfigListEditorValues {
    private ConfigListEditorValues() { }

    static List<?> resultFor(KTConfigEntry<?> entry, List<String> values) {
        if (entry.type() != KTConfigEntry.Type.INTEGER_LIST) return new ArrayList<>(values);
        List<Integer> integers = new ArrayList<>(values.size());
        for (String value : values) {
            try {
                if (value == null) return new ArrayList<>(values);
                integers.add(Integer.parseInt(value.trim()));
            } catch (NumberFormatException invalid) {
                // Keep an invalid draft invalid; never silently drop an item or save a partial list.
                return new ArrayList<>(values);
            }
        }
        return integers;
    }
}
