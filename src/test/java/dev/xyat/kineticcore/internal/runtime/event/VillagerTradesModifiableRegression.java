package dev.xyat.kineticcore.internal.runtime.event;

// 26.1 defines villager trades in data packs and has no trade events.
//? if <26.1 {

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.ArrayList;
import java.util.List;

/** Trade lists handed to Kinetic handlers stay changeable when another mod left List.of(...) for a level. */
public final class VillagerTradesModifiableRegression {
    private static int checks;
    private static void check(boolean result, String description) {
        checks++;
        if (!result) throw new AssertionError(description);
    }
    public static void main(String[] args) {
        VillagerTrades.ItemListing first = (trader, random) -> null;
        VillagerTrades.ItemListing second = (trader, random) -> null;
        VillagerTrades.ItemListing added = (trader, random) -> null;
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = new Int2ObjectOpenHashMap<>();
        trades.put(1, List.of(first, second));
        List<VillagerTrades.ItemListing> vanilla = new ArrayList<>(List.of(first));
        trades.put(2, vanilla);

        List<VillagerTrades.ItemListing> level1 = KineticVillagerEventRuntime.modifiableTrades(trades, 1);
        check(level1.equals(List.of(first, second)), "copy keeps the listings in order");
        level1.clear();
        level1.add(added);
        check(trades.get(1) == level1, "the copy replaces the unmodifiable list in the pool");
        check(trades.get(1).equals(List.of(added)), "changes reach the pool Forge reads after the event");
        check(KineticVillagerEventRuntime.modifiableTrades(trades, 1) == level1, "a second call returns the same list");

        check(KineticVillagerEventRuntime.modifiableTrades(trades, 2) == vanilla, "a modifiable list is handed out as is");
        check(KineticVillagerEventRuntime.modifiableTrades(trades, 5) == null, "a missing level stays missing");
        check(!trades.containsKey(5), "a missing level is not added");
        System.out.println("PASS: " + checks + " villager trade list checks");
    }
}
//?}
