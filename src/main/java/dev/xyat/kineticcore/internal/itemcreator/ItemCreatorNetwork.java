package dev.xyat.kineticcore.internal.itemcreator;

import dev.xyat.kineticcore.api.itemcreator.*;
import dev.xyat.kineticcore.api.network.*;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemCreatorNetwork {
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(KineticResourceIds.of("kineticcore", "item_creator"), "2", NetworkVersionPolicy.EXACT);
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();
    private static final AtomicInteger NEXT = new AtomicInteger();
    private static final ConcurrentHashMap<Integer, Consumer<KineticItemCreator.Result>> PENDING = new ConcurrentHashMap<>();
    private static ServerboundSender<ItemCreatorProtocol.Request> requests;
    private static ClientboundSender<KineticItemCreator.Result> results;
    private ItemCreatorNetwork() { }
    public static void register() {
        REGISTRATION.run(
            () -> results = CHANNEL.registerClientboundLazy(1, KineticItemCreator.Result.class,
                    NetworkCodec.of((b,r) -> { b.writeVarInt(r.requestId()); b.writeBoolean(r.success()); b.writeUtf(r.translationKey(), 256); },
                            b -> new KineticItemCreator.Result(b.readVarInt(), b.readBoolean(), b.readUtf(256))),
                    () -> ItemCreatorNetwork::receive),
            () -> requests = CHANNEL.registerServerbound(0, ItemCreatorProtocol.Request.class,
                    NetworkCodec.of(ItemCreatorProtocol::writeRequest, ItemCreatorProtocol::readRequest),
                    (request, context) -> results.send(context.sender(), ItemCreatorService.give(context.sender(), request)))
        );
    }
    public static int requestEgg(SpawnEggTemplate template, Consumer<KineticItemCreator.Result> result) {
        int id = next(); return send(new ItemCreatorProtocol.Request(id, template, null, List.of()), result);
    }
    public static int requestPotion(KineticItemCreator.PotionKind kind, List<EffectSpec> effects, Consumer<KineticItemCreator.Result> result) {
        return requestPotion(kind, effects, "", result);
    }
    public static int requestPotion(KineticItemCreator.PotionKind kind, List<EffectSpec> effects, String name, Consumer<KineticItemCreator.Result> result) {
        int id = next(); return send(new ItemCreatorProtocol.Request(id, null, kind, effects, name), result);
    }
    private static int next() { return NEXT.updateAndGet(i -> i == Integer.MAX_VALUE ? 1 : i + 1); }
    private static int send(ItemCreatorProtocol.Request request, Consumer<KineticItemCreator.Result> result) {
        if (requests == null) throw new IllegalStateException("Editor network is unavailable");
        PENDING.put(request.requestId(), result);
        try { requests.send(request); } catch (RuntimeException e) { PENDING.remove(request.requestId()); throw e; }
        return request.requestId();
    }
    private static void receive(KineticItemCreator.Result result) {
        var callback = PENDING.remove(result.requestId()); if (callback != null) callback.accept(result);
    }
    public static void disconnected() {
        PENDING.forEach((id, callback) -> {
            if (PENDING.remove(id, callback)) {
                try { callback.accept(new KineticItemCreator.Result(id, false, "gui.kineticcore.item_creator.disconnected")); }
                catch (RuntimeException error) { dev.xyat.kineticcore.api.runtime.KineticRuntime.logger().warn("Item creator disconnect callback failed", error); }
            }
        });
    }
}
