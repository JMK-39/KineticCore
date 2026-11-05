package dev.xyat.kineticcore.internal.client.selector;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors.ItemSelectorOptions;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors.ItemSelectorPreset;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors.ItemListRuleAdapter;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.client.search.KineticItemSearch;
import dev.xyat.kineticcore.internal.client.gui.text.KineticText;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** The native item selector with a persistent multiselection draft, including tag and mod rules. */
public final class ItemListEditorScreen extends ItemSelectorScreen {
    public enum SelectionMode { ITEMS_ONLY, ITEMS_TAGS_MODS }
    private final LinkedHashSet<String> rules = new LinkedHashSet<>();
    private final Consumer<List<String>> onSave;
    private final SelectionMode selectionMode;
    private final ItemListRuleAdapter adapter;
    private String previousRuleQuery;
    private List<String> queryRules = List.of();

    public ItemListEditorScreen(Component title, List<String> initialRules, SelectionMode selectionMode,
                                Consumer<List<String>> onSave) {
        this(title, initialRules, selectionMode, null, onSave);
    }

    public ItemListEditorScreen(Component title, List<String> initialRules, SelectionMode selectionMode,
                                ItemListRuleAdapter adapter, Consumer<List<String>> onSave) {
        super(Objects.requireNonNull(title), KineticClientRuntime.currentScreen(), null, selectionMode == SelectionMode.ITEMS_ONLY
                ? ItemSelectorOptions.itemsOnly(ItemSelectorPreset.defaults(), List.of(), stack -> adapter == null || adapter.acceptsItem(stack))
                : ItemSelectorOptions.filtered(ItemSelectorPreset.defaults(), List.of(), stack -> adapter == null || adapter.acceptsItem(stack)));
        this.selectionMode = Objects.requireNonNull(selectionMode);
        this.adapter = adapter;
        this.onSave = Objects.requireNonNull(onSave);
        if (initialRules != null) for (String value : initialRules) {
            if (value != null && !value.isBlank()) rules.add(value.trim());
        }
    }

    @Override protected int extraToolbarWidth() { return 88; }

    @Override protected void buildExtraToolbar(int right, int top) {
        addButton(right - 84, top, 40, KineticText.translatable("gui.kineticcore.items.list_editor.rules"),
                title, this::showRules);
        addButton(right - 40, top, 40, KineticText.translatable("gui.kineticcore.hud_editor.save"),
                title, () -> {
                    onSave.accept(List.copyOf(rules));
                    if (KineticClientRuntime.currentScreen() == this) navigateBack();
                });
    }

    @Override protected void choose(Selection selection, int button) {
        String rule;
        if (selection.isItem()) {
            var id = KineticRegistries.items().id(selection.stack().getItem());
            if (id == null) return;
            ItemStack stack = selection.stack();
            if (adapter != null) {
                String existing = rules.stream().filter(stored -> matches(stored, stack)).findFirst().orElse(null);
                if (adapter.editItem(stack, existing, button, KineticClientRuntime.shiftModifierDown(), replacement -> {
                    rules.removeIf(stored -> matches(stored, stack));
                    if (replacement != null && !replacement.isBlank() && acceptsRule(replacement)) rules.add(replacement);
                })) return;
            }
            rule = adapter == null ? id.toString() : adapter.itemRule(selection.stack());
            if (selected(selection.stack())) {
                rules.removeIf(existing -> matches(existing, selection.stack()));
                return;
            }
        } else if (selectionMode == SelectionMode.ITEMS_TAGS_MODS && selection.isTag()) {
            rule = adapter == null ? "#" + selection.value() : adapter.groupRule("#" + selection.value());
        } else if (selectionMode == SelectionMode.ITEMS_TAGS_MODS && selection.isMod()) {
            rule = adapter == null ? "@" + selection.value() : adapter.groupRule("@" + selection.value());
        } else return;
        if (rule == null || rule.isBlank() || !acceptsRule(rule)) return;
        if (!rules.remove(rule) && button == 0) rules.add(rule);
    }

    @Override protected boolean acceptsRule(String rule) {
        return adapter == null || adapter.acceptsRule(rule);
    }

    @Override protected List<String> additionalRuleCandidates() {
        return adapter == null ? List.of() : List.copyOf(adapter.ruleCandidates());
    }

    @Override protected boolean matchesRuleFilter(KineticItemSearch.CachedItem item, String rule) {
        return adapter == null ? super.matchesRuleFilter(item, rule) : adapter.matches(rule, item.stack());
    }

    @Override protected boolean matchesRuleQuery(KineticItemSearch.CachedItem item, String query) {
        if (adapter == null) return super.matchesRuleQuery(item, query);
        if (!query.equals(previousRuleQuery)) {
            previousRuleQuery = query;
            var candidates = new LinkedHashSet<String>();
            if (adapter.acceptsRule(query)) candidates.add(query);
            adapter.ruleCandidates().stream().filter(rule -> rule.startsWith(query.substring(0, 1))
                    && rule.contains(query.substring(1))).forEach(candidates::add);
            queryRules = List.copyOf(candidates);
        }
        var stack = item.stack();
        return queryRules.stream().anyMatch(rule -> adapter.matches(rule, stack));
    }

    @Override protected boolean selected(ItemStack stack) {
        return rules.stream().anyMatch(rule -> matches(rule, stack));
    }

    private boolean matches(String rule, ItemStack stack) {
        if (adapter != null) return adapter.matches(rule, stack);
        var id = KineticRegistries.items().id(stack.getItem());
        if (id == null) return false;
        if (rule.startsWith("@")) return id.getNamespace().equals(rule.substring(1));
        if (rule.startsWith("#")) {
            var tag = KineticResourceIds.tryParse(rule.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        return id.toString().equals(rule);
    }

    private void showRules() {
        List<KineticOverlays.MenuItem> entries = new ArrayList<>();
        for (String rule : List.copyOf(rules)) entries.add(KineticOverlays.MenuItem.action(
                adapter == null ? Component.literal(rule) : adapter.ruleLabel(rule), () -> { rules.remove(rule); showRules(); }));
        if (entries.isEmpty()) entries.add(KineticOverlays.MenuItem.action(
                KineticText.translatable("gui.kineticcore.items.list_editor.empty"), () -> { }));
        openContextMenu(460, 24, entries, 172);
    }
}
