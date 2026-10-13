package dev.xyat.kineticcore.internal.itemcreator;

import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.network.NetworkBuffer;
import java.util.ArrayList;
import java.util.List;

public final class ItemCreatorProtocol {
    public record Request(int requestId, SpawnEggTemplate template, KineticItemCreator.PotionKind kind, List<EffectSpec> effects, String name) {
        public Request(int requestId, SpawnEggTemplate template, KineticItemCreator.PotionKind kind, List<EffectSpec> effects) { this(requestId, template, kind, effects, template == null ? "" : template.name()); }
        public Request {
            name = ItemCreatorValidation.validateName(name);
            if (requestId <= 0) throw new IllegalArgumentException("Invalid request ID");
            effects = List.copyOf(effects);
            ItemCreatorValidation.validateEffects(effects);
            if (template == null && kind == null) throw new IllegalArgumentException("Missing potion kind");
        }
    }
    private ItemCreatorProtocol() { }
    public static void writeRequest(NetworkBuffer b, Request request) {
        b.writeVarInt(request.requestId()); b.writeBoolean(request.template() != null);
        if (request.template() != null) {
            var template = request.template();
            b.writeUtf(template.entityId(), 256); b.writeVarInt(template.attributes().size());
            for (var spec : template.attributes()) { b.writeUtf(spec.id(), 256); b.writeDouble(spec.amount()); b.writeEnum(spec.operation()); }
            writeEffects(b, template.effects());
        } else { b.writeEnum(request.kind()); writeEffects(b, request.effects()); }
        b.writeUtf(request.template() != null ? request.template().name() : request.name(), ItemCreatorValidation.MAX_NAME_LENGTH);
    }
    public static Request readRequest(NetworkBuffer b) {
        int id = b.readVarInt();
        if (b.readBoolean()) {
            String entity = b.readUtf(256);
            int count = count(b);
            List<AttributeSpec> attributes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) attributes.add(new AttributeSpec(b.readUtf(256), b.readDouble(), b.readEnum(AttributeSpec.Operation.class)));
            var effects = readEffects(b);
            return new Request(id, new SpawnEggTemplate(entity, attributes, effects, b.readUtf(ItemCreatorValidation.MAX_NAME_LENGTH)), null, List.of());
        }
        var kind = b.readEnum(KineticItemCreator.PotionKind.class);
        var effects = readEffects(b);
        return new Request(id, null, kind, effects, b.readUtf(ItemCreatorValidation.MAX_NAME_LENGTH));
    }
    private static int count(NetworkBuffer b) {
        int count = b.readVarInt();
        if (count < 0 || count > ItemCreatorValidation.MAX_ENTRIES) throw new IllegalArgumentException("Too many template entries");
        return count;
    }
    private static List<EffectSpec> readEffects(NetworkBuffer b) {
        int count = count(b); List<EffectSpec> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(new EffectSpec(b.readUtf(256), b.readVarInt(), b.readInt(), b.readBoolean(), b.readBoolean()));
        ItemCreatorValidation.validateEffects(entries); return entries;
    }
    private static void writeEffects(NetworkBuffer b, List<EffectSpec> entries) {
        b.writeVarInt(entries.size());
        for (var spec : entries) { b.writeUtf(spec.id(),256); b.writeVarInt(spec.amplifier()); b.writeInt(spec.durationTicks()); b.writeBoolean(spec.particles()); b.writeBoolean(spec.icon()); }
    }
}
