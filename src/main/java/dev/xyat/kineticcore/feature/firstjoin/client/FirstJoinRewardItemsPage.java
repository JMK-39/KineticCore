package dev.xyat.kineticcore.feature.firstjoin.client;

import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.render.KineticTexture;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.ui.NumberType;
import dev.xyat.kineticcore.api.client.gui.widget.KineticEntityPreview;
import dev.xyat.kineticcore.api.client.gui.widget.KineticNumberField;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.config.client.KTServerConfigClient;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfigGui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One inventory-shaped editor for all first-join rewards. */
public final class FirstJoinRewardItemsPage extends KineticPage {
    private static final int SLOT = 18;
    private static final int BASE_X = 232;
    private static final int BASE_Y = 56;
    private static final KineticTexture INVENTORY_TEXTURE = KineticTexture.of("minecraft", "textures/gui/container/inventory.png");
    private static final int INVENTORY_X = BASE_X + 8;
    private static final int INVENTORY_Y = BASE_Y + 84;
    private static final int HOTBAR_Y = BASE_Y + 142;
    private static final int ARMOR_X = BASE_X + 8;
    private static final int ARMOR_Y = BASE_Y + 8;
    private static final int OFFHAND_X = BASE_X + 77;
    private static final int OFFHAND_Y = BASE_Y + 62;
    private static final List<String> ARMOR = List.of("helmet", "chestplate", "leggings", "boots", "offhand");

    private final Map<Integer, ItemStack> inventory = new LinkedHashMap<>();
    private final List<String> legacyExtras = new ArrayList<>();
    private final Map<String, ItemStack> equipment = new LinkedHashMap<>();
    private final KineticEntityPreview preview = KineticEntityPreview.create();
    private KineticNumberField countField;
    private SlotRef editingCountSlot;
    private Map<String, Object> savedSnapshot;

    public FirstJoinRewardItemsPage() {
        super(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.title"));
        setPausesGame(false);
        int fallback = 0;
        for (String raw : KTServerConfigClient.getStringList(PlayerConfigGui.PAGE_ID, "items", PlayerConfig.firstJoinItemsRaw)) {
            if (raw == null) continue;
            int slot = fallback;
            String text = raw.trim();
            if (text.startsWith("[")) {
                int close = text.indexOf(']');
                if (close > 1) {
                    try {
                        slot = Integer.parseInt(text.substring(1, close));
                        text = text.substring(close + 1).trim();
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            ItemStack stack = PlayerConfig.parseItemStack(text);
            if (slot >= 0 && slot < 36 && !stack.isEmpty()) inventory.put(slot, stack);
            else legacyExtras.add(raw);
            fallback++;
        }
        for (String key : ARMOR) {
            equipment.put(key, PlayerConfig.parseItemStack(KTServerConfigClient.getString(
                    PlayerConfigGui.PAGE_ID, key, defaultEquipment(key))));
        }
        savedSnapshot = payload();
    }

    private static String defaultEquipment(String key) {
        return switch (key) {
            case "helmet" -> PlayerConfig.helmetId;
            case "chestplate" -> PlayerConfig.chestplateId;
            case "leggings" -> PlayerConfig.leggingsId;
            case "boots" -> PlayerConfig.bootsId;
            default -> PlayerConfig.offhandId;
        };
    }

    @Override
    protected void build(KineticUi ui) {
        countField = ui.numberField(462, 150, 52, NumberType.INT)
                .label(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.count"))
                .allowNegative(false).range(1, 999).onChange(this::updateCount).build();
        countField.limitTextLength(3);
        countField.setActive(false);
        ui.button(118, 317, 136).text(KineticI18n.translatable("gui.kineticcore.firstjoin.import_inventory"))
                .tooltip(KineticI18n.translatable("gui.kineticcore.firstjoin.import_inventory.tooltip"))
                .onClick(this::importInventory).build();
        ui.button(269, 317, 100).text(KineticI18n.translatable("gui.kineticcore.config.back"))
                .tooltip(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.back.tooltip"))
                .onClick(this::requestClose).build();
        ui.button(384, 317, 136).text(KineticI18n.translatable("gui.kineticcore.config.save"))
                .tooltip(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.save.tooltip"))
                .onClick(this::save).build();
    }

    private void importInventory() {
        hideCountEditor();
        LocalPlayer player = KineticClientRuntime.localPlayer();
        if (player == null) return;
        inventory.clear();
        legacyExtras.clear();
        for (int i = 0; i < Math.min(36, player.getInventory().items.size()); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) inventory.put(i, stack.copy());
        }
        equipment.put("helmet", player.getInventory().armor.get(3).copy());
        equipment.put("chestplate", player.getInventory().armor.get(2).copy());
        equipment.put("leggings", player.getInventory().armor.get(1).copy());
        equipment.put("boots", player.getInventory().armor.get(0).copy());
        equipment.put("offhand", player.getInventory().offhand.get(0).copy());
        save();
    }

    private ItemStack stackAt(SlotRef ref) {
        if (ref == null) return ItemStack.EMPTY;
        if (ref.type().equals("inventory")) return inventory.getOrDefault(ref.index(), ItemStack.EMPTY);
        if (ref.type().equals("equipment")) return equipment.getOrDefault(ARMOR.get(ref.index()), ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    private void setStack(SlotRef ref, ItemStack stack) {
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        if (ref.type().equals("inventory")) {
            if (copy.isEmpty()) inventory.remove(ref.index());
            else inventory.put(ref.index(), copy);
        } else if (ref.type().equals("equipment")) {
            equipment.put(ARMOR.get(ref.index()), copy);
        }
    }

    private SlotRef slotAt(double x, double y) {
        for (int slot = 0; slot < 36; slot++) {
            int sx = INVENTORY_X + slot % 9 * SLOT;
            int sy = slot < 9 ? HOTBAR_Y : INVENTORY_Y + (slot - 9) / 9 * SLOT;
            if (KineticTheme.hovering(x, y, sx, sy, SLOT, SLOT)) return new SlotRef("inventory", slot);
        }
        for (int i = 0; i < 4; i++) {
            if (KineticTheme.hovering(x, y, ARMOR_X, ARMOR_Y + i * SLOT, SLOT, SLOT))
                return new SlotRef("equipment", i);
        }
        if (KineticTheme.hovering(x, y, OFFHAND_X, OFFHAND_Y, SLOT, SLOT))
            return new SlotRef("equipment", 4);
        return null;
    }

    private void drawSlot(KineticGraphics graphics, ItemStack stack, int x, int y, boolean hovered) {
        if (hovered) KineticTheme.itemSlot(graphics, x, y, SLOT, true);
        if (stack.isEmpty()) return;
        KineticTheme.item(graphics, stack, x, y, SLOT, 1.0F, false);
        graphics.itemDecorations(stack, x + 1, y + 1, "");
        if (stack.getCount() > 1) {
            String count = Integer.toString(stack.getCount());
            graphics.push();
            graphics.raise(1);
            graphics.text(count, x + 18 - graphics.textWidth(count), y + 10, 0xFF55FF55, true);
            graphics.pop();
        }
    }

    @Override
    protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KineticTheme.panel(graphics, 100, 16, 440, 330);
        graphics.centeredText(title(), width() / 2, 31, KineticTheme.current().text(), true);
        graphics.texture(INVENTORY_TEXTURE, BASE_X, BASE_Y, 0, 0, 176, 166);
        graphics.fill(BASE_X + 87, BASE_Y + 15, BASE_X + 169, BASE_Y + 59, 0xFFC6C6C6);
        graphics.centeredText(KineticI18n.translatable("gui.kineticcore.firstjoin.inventory"),
                BASE_X + 128, BASE_Y + 41, 0xFF404040, false);
        LocalPlayer player = KineticClientRuntime.localPlayer();
        if (player != null) preview.render(graphics, player, "first-join-player",
                BASE_X + 25, BASE_Y + 10, 49, 62, false);
        for (int i = 0; i < 36; i++) {
            int x = INVENTORY_X + i % 9 * SLOT;
            int y = i < 9 ? HOTBAR_Y : INVENTORY_Y + (i - 9) / 9 * SLOT;
            drawSlot(graphics, inventory.getOrDefault(i, ItemStack.EMPTY), x, y,
                    KineticTheme.hovering(mouseX, mouseY, x, y, SLOT, SLOT));
        }
        for (int i = 0; i < 4; i++) {
            int y = ARMOR_Y + i * SLOT;
            drawSlot(graphics, equipment.getOrDefault(ARMOR.get(i), ItemStack.EMPTY), ARMOR_X, y,
                    KineticTheme.hovering(mouseX, mouseY, ARMOR_X, y, SLOT, SLOT));
        }
        drawSlot(graphics, equipment.getOrDefault("offhand", ItemStack.EMPTY), OFFHAND_X, OFFHAND_Y,
                KineticTheme.hovering(mouseX, mouseY, OFFHAND_X, OFFHAND_Y, SLOT, SLOT));
        if (editingCountSlot != null) {
            graphics.text(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.count"),
                    421, 154, KineticTheme.current().text(), false);
        }
        graphics.centeredText(KineticI18n.translatable("gui.kineticcore.firstjoin.inventory.hint"),
                width() / 2, 282, KineticTheme.current().text(), true);
    }

    @Override
    protected void renderTooltips(int mouseX, int mouseY) {
        ItemStack stack = stackAt(slotAt(mouseX, mouseY));
        if (!stack.isEmpty()) showItemTooltip(stack);
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        SlotRef ref = slotAt(input.x(), input.y());
        if (ref == null) return false;
        hideCountEditor();
        ItemStack stack = stackAt(ref);
        if (stack.isEmpty()) {
            KineticSelectors.openItemSelector(selection -> {
                if (selection != null && selection.isItem()) setStack(ref, selection.stack());
            });
        } else {
            openContextMenu(input.x(), input.y(), List.of(
                    KineticOverlays.MenuItem.action(
                            KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.edit_count"),
                            KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.edit_count.tooltip"),
                            () -> editCount(ref)),
                    KineticOverlays.MenuItem.action(
                            KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.replace"),
                            KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.replace.tooltip"),
                            () -> KineticSelectors.openItemSelector(selection -> {
                                if (selection != null && selection.isItem()) {
                                    ItemStack replacement = selection.stack().copy();
                                    replacement.setCount(stackAt(ref).getCount());
                                    setStack(ref, replacement);
                                }
                            })),
                    KineticOverlays.MenuItem.danger(
                            KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.delete"),
                            KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.delete.tooltip"),
                            () -> setStack(ref, ItemStack.EMPTY))));
        }
        return true;
    }

    private void editCount(SlotRef ref) {
        ItemStack stack = stackAt(ref);
        if (stack.isEmpty() || countField == null) return;
        editingCountSlot = ref;
        countField.setTextValue(Integer.toString(stack.getCount()));
        countField.setActive(true);
        focus(countField);
    }

    private void updateCount(String value) {
        if (editingCountSlot == null) return;
        try {
            int count = Integer.parseInt(value);
            if (count < 1 || count > 999) return;
            ItemStack stack = stackAt(editingCountSlot).copy();
            if (stack.isEmpty()) return;
            stack.setCount(count);
            setStack(editingCountSlot, stack);
        } catch (NumberFormatException ignored) {
        }
    }

    private void hideCountEditor() {
        editingCountSlot = null;
        if (countField != null) {
            blur(countField);
            countField.setActive(false);
        }
    }

    private Map<String, Object> payload() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> items = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack != null && !stack.isEmpty()) items.add("[" + slot + "] " + PlayerConfig.serializeItemStack(stack));
        }
        items.addAll(legacyExtras);
        result.put("items", items);
        for (String key : ARMOR) result.put(key, PlayerConfig.serializeItemStack(equipment.get(key)));
        return result;
    }

    private void save() {
        Map<String, Object> values = payload();
        if (KTServerConfigClient.savePartial(PlayerConfigGui.PAGE_ID, values)) {
            savedSnapshot = values;
        } else {
            KineticOverlays.toast(null, KineticI18n.translatable("gui.kineticcore.config.server.save_failed"),
                    KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        }
    }

    private void requestClose() {
        if (payload().equals(savedSnapshot)) {
            navigateBack();
            return;
        }
        openDialog(KineticI18n.translatable("gui.kineticcore.config.unsaved_action.title"),
                KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.unsaved"),
                KineticI18n.translatable("gui.yes"), KineticI18n.translatable("gui.no"),
                () -> {
                    Map<String, Object> before = savedSnapshot;
                    save();
                    if (savedSnapshot != before) navigateBack();
                }, this::navigateBack);
    }

    @Override
    protected boolean onCloseRequested() {
        requestClose();
        return true;
    }

    private record SlotRef(String type, int index) {
    }
}
