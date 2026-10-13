package dev.xyat.kineticcore.api.itemcreator;

import java.util.List;

/**
 * Immutable spawn recipe; duplicate attribute or effect IDs are rejected.
 * @param entityId exact entity-type registry ID
 * @param attributes attributes applied after synchronous spawning
 * @param effects effects applied after synchronous spawning
 * @param name optional plain-text name for the item and spawned entity
 */
public record SpawnEggTemplate(String entityId, List<AttributeSpec> attributes, List<EffectSpec> effects, String name) {
    public SpawnEggTemplate(String entityId, List<AttributeSpec> attributes, List<EffectSpec> effects) { this(entityId, attributes, effects, ""); }
    public SpawnEggTemplate {
        name = ItemCreatorValidation.validateName(name);
        ItemCreatorValidation.validateId(entityId);
        attributes = List.copyOf(attributes);
        effects = List.copyOf(effects);
        ItemCreatorValidation.validateAttributes(attributes);
        ItemCreatorValidation.validateEffects(effects);
    }
}
