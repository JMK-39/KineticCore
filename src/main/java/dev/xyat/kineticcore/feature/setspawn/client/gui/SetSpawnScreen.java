package dev.xyat.kineticcore.feature.setspawn.client.gui;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.Suggestion;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableActionList;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.feature.setspawn.network.SetSpawnNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SetSpawnScreen extends KineticScreen {
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

    private AutoCompleteBox activeInput;
    private ScrollableActionList activeListWidget;

    public SetSpawnScreen(SetSpawnNetwork.OpenSetSpawnGuiPacket packet) {
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
    protected void buildUi() {
        int panelW = canvasWidth() - 40;
        int startX = 20;
        int topY = 16;

        addTabBar(
                startX,
                topY,
                250,
                List.of(
                        KineticI18n.translatable("gui.kineticcore.setspawn.dim"),
                        KineticI18n.translatable("gui.kineticcore.setspawn.biome"),
                        KineticI18n.translatable("gui.kineticcore.setspawn.struct")
                ),
                List.of(
                        KineticI18n.translatable("gui.kineticcore.setspawn.tooltip.dim"),
                        KineticI18n.translatable("gui.kineticcore.setspawn.tooltip.biome"),
                        KineticI18n.translatable("gui.kineticcore.setspawn.tooltip.struct")
                ),
                currentTab,
                this::switchTab
        );

        addButton(
                startX + panelW - 145, topY, 80,
                KineticI18n.translatable("gui.kineticcore.hud_editor.save"),
                null,
                () -> SetSpawnNetwork.saveToServer(new SetSpawnNetwork.SaveSetSpawnPacket(
                        globalEnable, dimEnable, dims, biomeEnable, biomes, structEnable, structs
                ))
        );
        addButton(
                startX + panelW - 60, topY, 60,
                KineticI18n.translatable("gui.kineticcore.config.back"),
                null,
                this::onClose
        );

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
        activeInput = addAutoCompleteField(
                startX, searchY, inputW,
                Component.empty(),
                inputPlaceholder,
                this::getActiveDict,
                null
        );
        activeInput.setSelectionResponder(value -> {
            addToList(value);
            activeInput.setValue("");
        });

        Component enabled = KineticI18n.translatable("gui.kineticcore.setspawn.enable");
        Component disabled = KineticI18n.translatable("gui.kineticcore.setspawn.disable");
        addToggleButton(
                startX + inputW + 5, searchY, 85,
                globalEnable,
                KineticText.translatable("gui.kineticcore.setspawn.global_btn", enabled),
                KineticText.translatable("gui.kineticcore.setspawn.global_btn", disabled),
                KineticI18n.translatable("gui.kineticcore.setspawn.tooltip.global"),
                ignored -> true,
                value -> globalEnable = value
        );

        boolean currentEnable = currentTab == 0 ? dimEnable : (currentTab == 1 ? biomeEnable : structEnable);
        String tabPrefix = currentTab == 0
                ? "gui.kineticcore.setspawn.dim_btn"
                : (currentTab == 1 ? "gui.kineticcore.setspawn.biome_btn" : "gui.kineticcore.setspawn.struct_btn");
        Component tabTooltip = KineticI18n.translatable(
                currentTab == 0
                        ? "gui.kineticcore.setspawn.tooltip.dim"
                        : (currentTab == 1 ? "gui.kineticcore.setspawn.tooltip.biome" : "gui.kineticcore.setspawn.tooltip.struct")
        );
        addToggleButton(
                startX + inputW + 95, searchY, 85,
                currentEnable,
                KineticText.translatable(tabPrefix, enabled),
                KineticText.translatable(tabPrefix, disabled),
                tabTooltip,
                ignored -> true,
                value -> {
                    if (currentTab == 0) dimEnable = value;
                    else if (currentTab == 1) biomeEnable = value;
                    else structEnable = value;
                }
        );

        String currentEnv = currentTab == 0 ? playerDim : (currentTab == 1 ? playerBiome : playerStruct);
        boolean isOverworldDim = currentTab == 0 && "minecraft:overworld".equals(currentEnv);
        StateButton envBtn = addButton(
                startX + panelW - 85, searchY, 85,
                KineticI18n.translatable("gui.kineticcore.setspawn.add_current_single"),
                Component.literal(toDisplayEntry(currentEnv, getCurrentPrefix())),
                () -> {
                    if (currentEnv.equals("none") || currentEnv.isEmpty() || isOverworldDim) return;
                    addToList(currentEnv);
                }
        );
        envBtn.setEnabled(!(currentEnv.equals("none") || currentEnv.isEmpty() || isOverworldDim));

        int listY = 76;
        int listH = canvasHeight() - listY - 16;
        List<String> activeData = currentTab == 0 ? dims : (currentTab == 1 ? biomes : structs);

        activeListWidget = addScrollableActionList(
                startX,
                listY,
                panelW,
                listH,
                activeListItems(activeData),
                -1,
                0,
                38,
                index -> {
                    if (activeListWidget != null) activeListWidget.setSelectedIndex(-1);
                },
                this::removeActiveEntry
        );
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
        rebuildUi();
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

    private Suggestion toSuggestion(String id, String prefix) {
        ResourceLocation loc = KineticResourceIds.tryParse(id);
        if (loc == null) return new Suggestion(id, Component.empty());
        String translated = getCurrentLanguageTranslation(prefix, loc);
        return new Suggestion(id, translated.isEmpty() ? Component.empty() : Component.literal(translated));
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

    private List<Suggestion> getActiveDict() {
        if (currentTab == 0) return getDimDict();
        if (currentTab == 1) return getBiomeDict();
        return getStructDict();
    }

    private List<Suggestion> getDimDict() {
        return this.serverDictDims.stream()
                .filter(id -> !id.equals("minecraft:overworld"))
                .map(id -> toSuggestion(id, "dimension"))
                .toList();
    }

    private List<Suggestion> getBiomeDict() {
        return this.serverDictBiomes.stream()
                .map(id -> toSuggestion(id, "biome"))
                .toList();
    }

    private List<Suggestion> getStructDict() {
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
    protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        GuiTheme.shadow(g, this.canvasWidth(), this.canvasHeight());

        int panelW = this.canvasWidth() - 40;
        int startX = 20;
        int listY = 76;
        int listH = this.canvasHeight() - listY - 16;

        GuiTheme.panel(g, 10, 8, this.canvasWidth() - 20, this.canvasHeight() - 16);

        GuiTheme.separator(g, 16, 40, this.canvasWidth() - 32);
        GuiTheme.separator(g, 16, 71, this.canvasWidth() - 32);

        GuiTheme.panelAlt(g, startX - 2, listY - 2, panelW + 4, listH + 4);

    }

    @Override
    protected boolean canvasKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            if (isControlFocused(activeInput) && !activeInput.getValue().isEmpty()) {
                addToList(activeInput.getValue());
                activeInput.setValue("");
                return true;
            }
        }
        return false;
    }


}
