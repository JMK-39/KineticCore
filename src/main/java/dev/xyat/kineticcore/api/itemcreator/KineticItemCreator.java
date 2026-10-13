package dev.xyat.kineticcore.api.itemcreator;

import dev.xyat.kineticcore.internal.itemcreator.ItemCreatorNetwork;
import dev.xyat.kineticcore.internal.itemcreator.ItemCreatorService;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Validated item construction and server-authoritative editor requests. */
public final class KineticItemCreator {
    /** Vanilla potion forms. */
    public enum PotionKind { NORMAL, SPLASH, LINGERING }
    /** Server result; errors keep the editor draft intact. */
    public record Result(int requestId, boolean success, String translationKey) { }
    private KineticItemCreator() { }
    /** Registers the editor protocol during core bootstrap. */
    public static void register() { ItemCreatorNetwork.register(); }
    /** Builds a validated egg without giving it to any player. */
    public static ItemStack buildEgg(Level level, SpawnEggTemplate template) { return ItemCreatorService.buildEgg(level, template); }
    /** Builds a custom potion without giving it to any player. */
    public static ItemStack buildPotion(PotionKind kind, List<EffectSpec> effects) { return ItemCreatorService.buildPotion(kind, effects); }
    /** Builds a custom potion with a bounded plain-text name. */
    public static ItemStack buildPotion(PotionKind kind, List<EffectSpec> effects, String name) { return ItemCreatorService.buildPotion(kind, effects, name); }
    /** Requests one egg; the server checks permissions and registry IDs before delivery. */
    public static int requestEgg(SpawnEggTemplate template, Consumer<Result> result) { return ItemCreatorNetwork.requestEgg(template, result); }
    /** Requests one potion; the server checks permissions and effects before delivery. */
    public static int requestPotion(PotionKind kind, List<EffectSpec> effects, Consumer<Result> result) { return ItemCreatorNetwork.requestPotion(kind, effects, result); }
    /** Requests one named custom potion. */
    public static int requestPotion(PotionKind kind, List<EffectSpec> effects, String name, Consumer<Result> result) { return ItemCreatorNetwork.requestPotion(kind, effects, name, result); }
}
