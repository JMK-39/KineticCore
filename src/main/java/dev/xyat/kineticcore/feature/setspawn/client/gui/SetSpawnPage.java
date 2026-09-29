package dev.xyat.kineticcore.feature.setspawn.client.gui;

import dev.xyat.kineticcore.api.client.gui.widget.list.ActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticActionList;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticAutoCompleteField;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.feature.setspawn.network.SetSpawnNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SetSpawnPage extends KineticPage {
    private final String playerDim;
    private final String playerBiome;
    private final String playerStruct;

    private boolean globalEnable;
    private boolean dimEnable;
    private boolean biomeEnable;
    private boolean structEnable;

    private final List<String> dims;
    private final List<String> biomes;
    private final List<String> structs;

    private final List<String> serverDictDims;
    private final List<String> serverDictBiomes;
    private final List<String> serverDictStructs;

    private int currentTab = 0;

    private KineticAutoCompleteField activeInput;
    private KineticActionList activeListWidget;

    public SetSpawnPage(SetSpawnNetwork.OpenSetSpawnGuiPacket packet) {
        super(KineticI18n.translatable("gui.kineticcore.setspawn.title"));

        this.globalEnable = packet.globalEnable();
        this.dimEnable = packet.dimEnable();
        this.biomeEnable = packet.biomeEnable();
        this.structEnable = packet.structEnable();

        this.dims = new ArrayList<>(packet.dims());
        this.biomes = new ArrayList<>(packet.biomes());
        this.structs = new ArrayList<>(packet.structs());

        this.playerDim = packet.playerDim();
        this.playerBiome = packet.playerBiome();
        this.playerStruct = packet.playerStruct();

        this.serverDictDims = packet.allDims() != null ? packet.allDims() : new ArrayList<>();
        this.serverDictBiomes = packet.allBiomes() != null ? packet.allBiomes() : new ArrayList<>();
        this.serverDictStructs = packet.allStructs() != null ? packet.allStructs() : new ArrayList<>();
    }

    @Override
    protected void build(KineticUi ui) {
        int panelW = width() - 40;
        int startX = 20;
        int topY = 16;

        ui.tabBar(startX, topY, 250, List.of(
                        KineticI18n.translatable("gui.kineticcore.setspawn.dim"),
                        KineticI18n.translatable("gui.kineticcore.setspawn.biome"),
                        KineticI18n.translatable("gui.kineticcore.setspawn.struct")))
                .tooltips(List.of(
                        KineticI18n.translatable("gui.kineticcore.setspawn.tooltip.dim"),
                        KineticI18n.translatable("gui.kineticcore.setspawn.tooltip.biome"),
                        KineticI18n.translatable("gui.kineticcore.setspawn.tooltip.struct")))
                .selected(currentTab)
                .onSelect(this::switchTab)
                .build();

        ui.button(startX + panelW - 145, topY, 80)
                .text(KineticI18n.translatable("gui.kineticcore.hud_editor.save"))
                .onClick(() -> SetSpawnNetwork.saveToServer(new SetSpawnNetwork.SaveSetSpawnPacket(
                        globalEnable, dimEnable, dims, biomeEnable, biomes, structEnable, structs
                )))
                .build();
        ui.button(startX + panelW - 60, topY, 60)
                .text(KineticI18n.translatable("gui.kineticcore.config.back"))
                .onClick(this::close)
                .build();

        int searchY = 46;
        int switchesW = 270;
        int inputW = panelW - switchesW - 5;

        Component inputPlaceholder = KineticI18n.translatable(
                currentTab == 0
                        ? "gui.kineticcore.setspawn.hint_dim"
                        : (currentTab == 1
                                ? "gui.kineticcore.setspawn.hint_biome"
                                : "gui.kineticcore.setspawn.hint_struct")
        );
        activeInput = ui.autoComplete(startX, searchY, inputW, this::getActiveDict)
                .placeholder(inputPlaceholder)
                .onSelect(value -> {
                    addToList(value);
                    activeInput.setTextValue("");
                })
                .build();

        Component enabled = KineticI18n.translatable("gui.kineticcore.setspawn.enable");
        Component disabled = KineticI18n.translatable("gui.kineticcore.setspawn.disable");
        ui.toggle(startX + inputW + 5, searchY, 85)
                .value(globalEnable)
                .labels(
                        KineticI18n.translatable("gui.kineticcore.setspawn.global_btn", enabled),
                        KineticI18n.translatable("gui.kineticcore.setspawn.global_btn", disabled))
                .tooltip(KineticI18n.translatable("gui.kineticcore.setspawn.tooltip.global"))
                .onChange(value -> globalEnable = value)
                .build();

        boolean currentEnable = currentTab == 0 ? dimEnable : (currentTab == 1 ? biomeEnable : structEnable);
        String tabPrefix = currentTab == 0
                ? "gui.kineticcore.setspawn.dim_btn"
                : (currentTab == 1 ? "gui.kineticcore.setspawn.biome_btn" : "gui.kineticcore.setspawn.struct_btn");
        Component tabTooltip = KineticI18n.translatable(
                currentTab == 0
                        ? "gui.kineticcore.setspawn.tooltip.dim"
                        : (currentTab == 1 ? "gui.kineticcore.setspawn.tooltip.biome" : "gui.kineticcore.setspawn.tooltip.struct")
        );
        ui.toggle(startX + inputW + 95, searchY, 85)
                .value(currentEnable)
                .labels(
                        KineticI18n.translatable(tabPrefix, enabled),
                        KineticI18n.translatable(tabPrefix, disabled))
                .tooltip(tabTooltip)
                .onChange(value -> {
                    if (currentTab == 0) dimEnable = value;
                    else if (currentTab == 1) biomeEnable = value;
                    else structEnable = value;
                })
                .build();

        String currentEnv = currentTab == 0 ? playerDim : (currentTab == 1 ? playerBiome : playerStruct);
        boolean isOverworldDim = currentTab == 0 && "minecraft:overworld".equals(currentEnv);
        ui.button(startX + panelW - 85, searchY, 85)
                .text(KineticI18n.translatable("gui.kineticcore.setspawn.add_current_single"))
                .tooltip(Component.literal(toDisplayEntry(currentEnv, getCurrentPrefix())))
                .enabled(!(currentEnv.equals("none") || currentEnv.isEmpty() || isOverworldDim))
                .onClick(() -> {
                    if (currentEnv.equals("none") || currentEnv.isEmpty() || isOverworldDim) return;
                    addToList(currentEnv);
                })
                .build();

        int listY = 76;
        int listH = height() - listY - 16;
        List<String> activeData = currentTab == 0 ? dims : (currentTab == 1 ? biomes : structs);

        activeListWidget = ui.actionList(startX, listY, panelW, listH, activeListItems(activeData))
                .selected(-1)
                .actionWidth(38)
                .onSelect(index -> {
                    if (activeListWidget != null) activeListWidget.setSelectedIndex(-1);
                })
                .onAction(this::removeActiveEntry)
                .build();
    }

    public void handleSaveResult(boolean success) {
        if (success) {
            KineticOverlays.toast(null, KineticI18n.translatable("gui.kineticcore.setspawn.saved_toast"), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        } else {
            KineticOverlays.toast(null, KineticI18n.translatable("gui.kineticcore.setspawn.save_invalid_toast"), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        }
    }

    private void switchTab(int tab) {
        this.currentTab = tab;
        rebuild();
    }

    private void addToList(String rawValue) {
        String val = rawValue == null ? "" : rawValue.trim();
        if (!val.isEmpty()) {
            if (currentTab == 0 && val.equals("minecraft:overworld")) return;

            List<String> targetList = currentTab == 0 ? dims : (currentTab == 1 ? biomes : structs);
            if (!targetList.contains(val)) {
                targetList.add(val);
                refreshActiveList();
            }
        }
    }

    private String getCurrentPrefix() {
        if (currentTab == 0) return "dimension";
        if (currentTab == 1) return "biome";
        return "structure";
    }

    private String toDisplayEntry(String id, String prefix) {
        ResourceLocation loc = KineticResourceIds.tryParse(id);
        if (loc == null) return id;
        String translated = getCurrentLanguageTranslation(prefix, loc);
        return translated.isEmpty() ? id : id + " - " + translated;
    }

    private KineticSuggestion toSuggestion(String id, String prefix) {
        ResourceLocation loc = KineticResourceIds.tryParse(id);
        if (loc == null) return new KineticSuggestion(id, Component.empty());
        String translated = getCurrentLanguageTranslation(prefix, loc);
        return new KineticSuggestion(id, translated.isEmpty() ? Component.empty() : Component.literal(translated));
    }

    private String getCurrentLanguageTranslation(String prefix, ResourceLocation loc) {
        List<String> keys = new ArrayList<>();
        keys.add(prefix + "." + loc.getNamespace() + "." + loc.getPath());
        if ("structure".equals(prefix)) keys.addAll(getStructureFallbackKeys(loc));
        String translated = KineticSearch.resolveTranslation(keys.toArray(String[]::new));
        return translated == null ? "" : translated;
    }

    private List<String> getStructureFallbackKeys(ResourceLocation loc) {
        List<String> keys = new ArrayList<>();
        String namespace = loc.getNamespace();
        String path = loc.getPath();

        if (path.startsWith("village_")) {
            keys.add("structure." + namespace + ".village");
        }
        if (path.startsWith("ruined_portal_")) {
            keys.add("structure." + namespace + ".ruined_portal");
        }
        if (path.startsWith("ocean_ruin_")) {
            keys.add("structure." + namespace + ".ocean_ruin");
        }
        if (path.startsWith("mineshaft_")) {
            keys.add("structure." + namespace + ".mineshaft");
        }
        if (path.startsWith("shipwreck_")) {
            keys.add("structure." + namespace + ".shipwreck");
        }

        return keys;
    }

    private List<KineticSuggestion> getActiveDict() {
        if (currentTab == 0) return getDimDict();
        if (currentTab == 1) return getBiomeDict();
        return getStructDict();
    }

    private List<KineticSuggestion> getDimDict() {
        return this.serverDictDims.stream()
                .filter(id -> !id.equals("minecraft:overworld"))
                .map(id -> toSuggestion(id, "dimension"))
                .toList();
    }

    private List<KineticSuggestion> getBiomeDict() {
        return this.serverDictBiomes.stream()
                .map(id -> toSuggestion(id, "biome"))
                .toList();
    }

    private List<KineticSuggestion> getStructDict() {
        return this.serverDictStructs.stream()
                .map(id -> toSuggestion(id, "structure"))
                .toList();
    }

    private List<ActionItem> activeListItems(List<String> values) {
        List<ActionItem> items = new ArrayList<>(values.size());
        String prefix = getCurrentPrefix();
        for (String value : values) {
            ResourceLocation id = KineticResourceIds.tryParse(value);
            String translated = id == null ? "" : getCurrentLanguageTranslation(prefix, id);
            items.add(new ActionItem(
                Component.literal(value),
                translated.isEmpty() ? null : Component.literal(translated),
                null,
                true,
                false,
                KineticI18n.translatable("gui.kineticcore.setspawn.remove_short"),
                null,
                true,
                false));
        }
        return items;
    }

    private void refreshActiveList() {
        if (activeListWidget == null) return;
        List<String> activeData = currentTab == 0 ? dims : (currentTab == 1 ? biomes : structs);
        activeListWidget.setItems(activeListItems(activeData));
        activeListWidget.setSelectedIndex(-1);
    }

    private void removeActiveEntry(int index) {
        List<String> activeData = currentTab == 0 ? dims : (currentTab == 1 ? biomes : structs);
        if (index < 0 || index >= activeData.size()) return;
        activeData.remove(index);
        refreshActiveList();
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        KineticTheme.shadow(g, width(), height());

        int panelW = width() - 40;
        int startX = 20;
        int listY = 76;
        int listH = height() - listY - 16;

        KineticTheme.panel(g, 10, 8, width() - 20, height() - 16);

        KineticTheme.separator(g, 16, 40, width() - 32);
        KineticTheme.separator(g, 16, 71, width() - 32);

        KineticTheme.panelAlt(g, startX - 2, listY - 2, panelW + 4, listH + 4);

    }

    @Override
    protected boolean onKeyPress(KeyInput input) {
        if (input.isEnter()) {
            if (isFocused(activeInput) && !activeInput.textValue().isEmpty()) {
                addToList(activeInput.textValue());
                activeInput.setTextValue("");
                return true;
            }
        }
        return false;
    }


}
