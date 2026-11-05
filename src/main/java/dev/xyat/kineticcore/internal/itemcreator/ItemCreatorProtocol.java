package dev.xyat.kineticcore.internal.itemcreator;

import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.network.NetworkBuffer;
import java.util.ArrayList;
import java.util.List;

public final class ItemCreatorProtocol {
    /** Stays below vanilla's 32 KiB serverbound limit even when the optional network-limit feature is disabled. */
    public static final int MAX_REQUEST_BYTES = 30_000;
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
    public static void validateRequestSize(Request request) {
        if (dev.xyat.kineticcore.api.network.NetworkBuffers.encode(b -> writeRequest(b, request)).length > MAX_REQUEST_BYTES)
            throw new IllegalArgumentException("Item creator request is too large");
    }
    public static void writeRequest(NetworkBuffer b, Request request) {
        b.writeVarInt(request.requestId()); b.writeBoolean(request.template() != null);
        if (request.template() != null) {
            var template = request.template();
            b.writeUtf(template.entityId(), 256); b.writeVarInt(template.attributes().size());
            for (var spec : template.attributes()) { b.writeUtf(spec.id(), 256); b.writeDouble(spec.amount()); b.writeEnum(spec.operation()); }
            writeEffects(b, template.effects());
        } else { b.writeEnum(request.kind()); writeEffects(b, request.effects()); }
        b.writeUtf(request.template() != null ? request.template().name() : request.name(), ItemCreatorValidation.MAX_NAME_LENGTH);
        if (request.template() != null) {
            b.writeVarInt(request.template().equipment().size());
            for (var spec : request.template().equipment()) { b.writeEnum(spec.slot());b.writeUtf(spec.item(), EquipmentSpec.MAX_ITEM_LENGTH); }
        }
    }
    public static Request readRequest(NetworkBuffer b) {
        int id = b.readVarInt();
        if (b.readBoolean()) {
            String entity = b.readUtf(256);
            int count = count(b);
            List<AttributeSpec> attributes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) attributes.add(new AttributeSpec(b.readUtf(256), b.readDouble(), b.readEnum(AttributeSpec.Operation.class)));
            var effects = readEffects(b);
            String name = b.readUtf(ItemCreatorValidation.MAX_NAME_LENGTH);
            int size = b.readVarInt();
            if (size < 0 || size > EquipmentSpec.Slot.values().length) throw new IllegalArgumentException("Too many equipment slots");
            List<EquipmentSpec> equipment = new ArrayList<>();
            for (int i = 0; i < size; i++) equipment.add(new EquipmentSpec(b.readEnum(EquipmentSpec.Slot.class), b.readUtf(EquipmentSpec.MAX_ITEM_LENGTH)));
            return new Request(id, new SpawnEggTemplate(entity, attributes, effects, name, equipment), null, List.of());
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
