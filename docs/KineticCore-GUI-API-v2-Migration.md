# KineticCore v2 — Changes and Addon Migration

Audience: an engineer or coding model who ports a KineticCore addon (ContentStudio, AdventureSystems, CombatSystems, EntityControl, ItemControl, KineticArmory, MobAscension, ModRefinery, RealmControl, TACZWorkshop, TextStudio, EnchantWorks, …) to the rebuilt client GUI API.

Target: Minecraft 1.20.1, Forge 47.4.2, Java 17. The new API is shaped so that the later move to MC 26.1 (Java 25, `GuiGraphics` → `GuiGraphicsExtractor`, `ResourceLocation` → `Identifier`, new input events) happens inside KineticCore, and addon GUI code should need little or no change.

This is a **breaking** release. Every old client-GUI type under `dev.xyat.kineticcore.api.client.{screen, widget, theme, text, layout, overlay, selector, editor, command}` is gone. There are no deprecated bridges. An addon compiles again only once each of its GUI files has been ported.

This file is the only migration reference. Older KineticCore notes and patches (the scrollbar migration note, the scrollbar fix patches and the old ContentStudio example patch) describe removed APIs and must not be used.

---

## What changed in this release

**Client GUI (breaking)**

- Screens are replaced by pages: `KineticPage` (canvas or native layout), `KineticContainerPage<M>`, `KineticHudEditorPage`, opened with `KineticGui` or `openChild`.
- All `add*` methods and `KineticWidgets.create*` factories are replaced by `KineticUi` builders. Controls are public interfaces (`KineticButton`, `KineticTextField`, `KineticSelectionList`, …). `.layer(n)` replaces the high-z variants.
- `GuiGraphics`, `Font`, texture `ResourceLocation`s and raw input callbacks are replaced by `KineticGraphics`, `KineticText`, `KineticTexture` and the input records (`MouseInput`, `KeyInput`, …).
- `GuiTheme` → `KineticTheme`, `GuiLayout` → `KineticLayout`, `GridScrollController` → `KineticScrollController`, `SmoothSelectionList` → `KineticRowList`, `KineticCommandSuggestions` → `KineticCommandAssist`, `HudPositionEditor` → `KineticHudEditorPage`.
- Selectors and editors no longer take a parent screen. `KTConfigApi` opens pages instead of creating screens. `KineticClientMenus` registers container pages. GUI-related events and hooks pass `KineticGraphics`.
- `ScreenInitContext` (vanilla/third-party screen injection) now adds `KineticButton`s and `KineticCustomControl`s. They are overlay controls: drawn above the host screen, receive mouse input before it, and never take the screen's keyboard focus (safe on the chat screen).
- New general capabilities used by the migrated addons:
  - `KineticI18n.hasTranslation(key)`
  - `KineticText.scrollOffset(contentWidth, viewportWidth)` for self-drawn mixed text and icon scrolling
  - `KineticGui.captureNavigationParent()` + `KineticGui.openChild(page, parent)` to open a page after a server round-trip with the back target captured when the request was sent
  - `KineticGraphics.item(stack, x, y, alpha)` for translucent item icons
  - `KineticScrollController.scrollTo(offset)` (animated)
  - `KineticRowList.rowTop(index)`, `mouseX()` and `mouseY()` for inline hit areas in rows
  - `KineticTheme.button(...)` to paint a standard button inside a self-drawn row
  - `KineticGraphicsInterop.wrap(GuiGraphics)` for mixins, and `unwrap(KineticGraphics)` for third-party APIs that need a `GuiGraphics`
- Draft configuration (`configureDraft`, `configureStandaloneDraft`, `reserveStandaloneDraft`) may be called from a page constructor; it takes effect when the page is hosted.

**Stable API names and final-JAR checks**

- Public control methods never share a name and descriptor with vanilla widget methods, because ForgeGradle would rename those to `m_*` in the production JAR and addon call sites would fail with `NoSuchMethodError`. Controls use `controlX()`, `controlWidth()`, `moveControlX()`, `setControlVisible()`, `controlFocused()`, `textValue()`, `onTextChange()`, … (§1.1).
- Gradle tasks check the final JAR, not the compiler output: `checkKineticReobfApi` in the core and `checkKineticReobfRefs` for addons (§12).
- `KineticGraphicsInterop.unwrap(KineticGraphics)` hands a vanilla `GuiGraphics` to third-party APIs that require one (for example JEI's `IRecipeLayoutDrawable.draw`).

**Input text colors and selectors**

- Single-line inputs follow one color rule: placeholder pure white (only while empty and unfocused), value equal to the default cyan, modified value green, and invalid value red. Input text has no shadow. `valueColor` overrides the valid-value color for special cases. New API: `KineticTextField.setDefaultText/defaultText/setValueColor`, builder `.defaultText(...)` / `.defaultValue(...)` / `.valueColor(...)`, and `KineticTheme.fieldPlaceholderText/fieldDefaultText/fieldModifiedText/fieldErrorText` (§5).
- Text areas draw their placeholder in the same pure white.
- The entity selector marks selected entities with a green outline.

**Scrollbars**

- The thumb hint takes priority over the tooltip of the control that contains the scrollbar, so it also appears inside `KineticRowList` and other custom controls.
- Hint and middle-click jump need a jump target: they appear when the controller is bound (`bindSelection`) and the bound index is `>= 0`. `KineticRowList` binds automatically to the selected row, or to the last clicked row when nothing is selected. Hand-written `KineticScrollController` areas must bind themselves (§8.1).
- The jump flash is on the border: a selected entry has an orange border, and during the two flash phases the border switches to the inverse color (blue). The cell itself does not change.

**Third-party integrations removed from the core (breaking)**

- Curios and JEI are no longer compile dependencies or optional `mods.toml` dependencies of KineticCore.
- Removed `api.compat.curios.KineticCuriosEvents` and its internal runtime.
- Removed the built-in Curios slots in the item selector's inventory tab, the JEI hovered-item lookup used by copy-item, and the JEI exclusion-area mixin used by mini effects.
- New neutral extension points replace them: `KineticSelectors.registerInventorySource(...)` and `KineticHoveredItems.register(...)`. The `EffectAreaProvider` interface is unchanged. See §8.12 for where each integration should live now.
- API cleanup after v2: duplicate entry points such as `KineticUiState`, `KineticEnvironment` and `KineticFeatures` were removed, and a few members were renamed. The full old → new table and a scan command are in §13.

---

## 0. How to use this document

1. Read §1–§3 once. They cover the new model, which is different from the old one: you now build pages instead of subclassing screens.
2. Port one file at a time. Take its old imports and find each one in §4 (package/type map), then port its `add*` calls with §5, its overrides with §6, and its drawing code with §7.
3. For special screen kinds, use the recipes in §8: lists, container menus, HUD editors, vanilla-screen injection, command input, config GUIs, menus and events.
4. Before touching any file, run the whole-project scan in §11 and port **every** hit, not just the files that fail to compile first.
5. Finish each file with the checklist in §11 and the verification in §12.

**Hard rules for the port.** These are non-negotiable. KineticCore's own build enforces the same rules, and an addon build enforces them with `gradle/kinetic-addon-architecture.gradle` (§12):

- Never import `dev.xyat.kineticcore.internal.*`. Internal classes are not part of the contract and will change without notice.
- GUI classes do not extend `Screen`, `AbstractContainerScreen` or any vanilla widget. They extend `KineticPage`, `KineticContainerPage`, `KineticHudEditorPage`, `KineticCustomControl` or `KineticRowList`.
- Page code does not take `GuiGraphics`, `Font`, `PoseStack` or raw GLFW key and button ints, and never uses a `ResourceLocation` as a texture handle. Use `KineticGraphics`, `KineticText`, `KineticTexture` and the input records instead. `ResourceLocation` as a business or registry ID (recipe types, item IDs, config keys) is still fine.
- Translations go through `KineticI18n.translatable(...)`. Never use `Component.translatable(...)` or the old `KineticText.translatable(...)`.
- Mixins are the only exception: code inside `mixin` packages may still use vanilla `GuiGraphics`, because it runs inside vanilla methods. Mixins must still not import `internal`. Use `KineticTheme.current()` for colours.

---

## 1. The new model in one page

| Old | New |
|---|---|
| `class X extends KineticScreen` | `class X extends KineticPage` (a plain object, not a `Screen`) |
| `class X extends KineticNativeScreen` | `class X extends KineticPage` + `super(title, PageLayout.NATIVE)` |
| `class X extends KineticContainerScreen<M>` | `class X extends KineticContainerPage<M>` |
| `buildUi()` + `addButton(...)` etc. | `build(KineticUi ui)` + `ui.button(x, y, w)....build()` |
| `renderCanvasBackground(GuiGraphics g, …)` | `renderBackground(KineticGraphics g, …)` |
| `canvasMouseClicked(double, double, int)` | `onMouseClick(MouseInput in)` (after controls) or `onMouseClickCapture` (before) |
| `new XScreen(parent)` + `setScreen` / `setParentScreen` | `openChild(new XPage())`, or `KineticGui.open(page)` from game code |
| `GuiTheme.panel(graphics, …)` | `KineticTheme.panel(graphics, …)` (same names; takes `KineticGraphics`) |
| `GridScrollController` | `KineticScrollController` (same behaviour, takes `KineticGraphics`/`MouseButton`) |
| `SmoothSelectionList`/`SmoothEntry` | `KineticRowList<T>` |
| concrete widget classes (`StateButton`, `KineticEditBox`, …) | public interfaces (`KineticButton`, `KineticTextField`, …) |

Pages are hosted by internal screens. The host owns canvas scaling, scissor transforms, tooltips, focus, context menus, dialogs, drafts, layers and scrollbar hover/middle-click dispatch. The page only describes its content.

Minimal page:

```java
public final class MyPage extends KineticPage {
    private boolean enabled;

    public MyPage() {
        super(KineticI18n.translatable("gui.mymod.my_page.title"));
        setPausesGame(false);                       // replaces isPauseScreen() { return false; }
    }

    @Override
    protected void build(KineticUi ui) {            // called on open, resize, return-from-child and rebuild()
        ui.toggle(20, 40, 120)
                .value(enabled)
                .labels(KineticI18n.translatable("gui.mymod.on"), KineticI18n.translatable("gui.mymod.off"))
                .onChange(v -> enabled = v)
                .build();
        ui.button(20, 300, 80).text(KineticI18n.translatable("gui.mymod.back")).onClick(this::close).build();
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mouseX, int mouseY, float partialTick) {
        KineticTheme.panel(g, 10, 10, width() - 20, height() - 20);
        g.centeredText(title(), width() / 2, 18, KineticTheme.current().text(), true);
    }
}
// open it:  KineticGui.open(new MyPage());   or, from another page:  openChild(new MyPage());
```

**`build` must be idempotent.** It runs again whenever the host re-initialises: on resize, on returning from a child page, and after `rebuild()`. Keep all state in fields, and have `build` read that state to create controls. Do not keep references to controls across builds except through fields you reassign inside `build`.

---

### 1.1 Stable API names (no vanilla name collisions)

ForgeGradle's reobf renames every method whose name and descriptor match a mapped Minecraft method in the implementing class's hierarchy. The core's controls are backed by vanilla widgets, so any public API method named like a vanilla widget method (`getX()`, `isFocused()`, `EditBox.getValue()`, …) would be renamed in the core JAR and break every addon call site. The public API therefore uses these names:

| Removed from the API | Use |
|---|---|
| `getX()` / `getY()` / `getWidth()` / `getHeight()` | `controlX()` / `controlY()` / `controlWidth()` / `controlHeight()` |
| `setX(int)` / `setY(int)` / `setWidth(int)` | `moveControlX(int)` / `moveControlY(int)` / `resizeControlWidth(int)` |
| `setHeight(int)` (custom controls) | `resizeControlHeight(int)` |
| `isVisible()` / `setVisible(boolean)` | `controlVisible()` / `setControlVisible(boolean)` |
| `isHovered()` / `isFocused()` | `controlHovered()` / `controlFocused()` |
| `KineticTextField.getValue()` / `setValue(String)` | `textValue()` / `setTextValue(String)` |
| `setResponder` / `setMaxLength` / `setFilter` / `setFormatter` | `onTextChange` / `limitTextLength` / `filterText` / `formatText` |
| `getCursorPosition()` / `setCursorPosition(int)` | `cursorIndex()` / `setCursorIndex(int)` |
| `KineticTextArea.getValue/setValue/setCharacterLimit/setValueListener` | `textValue/setTextValue/limitTextLength/onTextChange` |
| `KineticSlider.setValue(double)` | `setSliderValue(double)` |

Unchanged (no collision): `isEnabled/setEnabled/setActive`, `setTooltip(...)`, `contains`, `moveTo`, `KineticToggle.value/setValue(boolean)`, `KineticButton.text/setText`. Vanilla objects that are not Kinetic controls (a vanilla `Button` in a mixin, `Screen.width`, …) keep their vanilla names; this only concerns calls on Kinetic API types.

A successful `compileJava` does not prove anything here, because the rename happens in `reobfJar`. Always run the final-JAR checks in §12.

## 2. Coordinates, layouts and layers

- **`PageLayout.CANVAS`** (the default) replaces `KineticScreen`. It uses a fixed virtual canvas of at most 640×360, scaled to the window. `width()` and `height()` return the canvas size (they replace `canvasWidth()`/`canvasHeight()`). To change the design size or safe margin, call `useCanvas(w, h, margin)` in the constructor. Every hook receives canvas coordinates.
- **`PageLayout.NATIVE`** replaces `KineticNativeScreen`. It uses the GUI-scaled screen coordinates, and `width()`/`height()` return the scaled screen size.
- **`KineticContainerPage<M>`** replaces `KineticContainerScreen`. Hooks receive UI (virtual) coordinates, except `renderScreenOverlay`, which receives screen coordinates.
- `layoutMetrics()`, `isCompactLayout()`, `isPortraitLayout()` and `isUltrawideLayout()` replace `layout()`, `layoutLevel()` and `safeArea()`.
- Constants: `KineticPage.CANVAS_WIDTH` (640), `CANVAS_HEIGHT` (360), `SAFE_MARGIN` (6), `CONTROL_HEIGHT` (16), `ITEM_BUTTON_HEIGHT` (38) and `CARD_BUTTON_HEIGHT` (26).
- Old `toScreenX/toVirtualX/toScreenRight/…` and `canvasX/canvasY/canvasScale` have **no** replacement, because the host converts everything. If you think you need them, you are probably drawing something that should be a custom control or a `renderForeground` overlay.
- **Layers replace "HighZ".** Any builder that supports `.layer(n)` (buttons, toggles, tab bars and every list or grid builder) draws the control above normal controls. `addHighZButton(...)` → `ui.button(...).layer(1)`, and `addCompactHighZButton(...)` → `ui.button(...).compact().layer(1)`. To draw your own content above an item-sized layer, call `g.raise(n)` inside `g.isolated(...)`.

---

## 3. Input model

All input reaches the page as immutable records from `dev.xyat.kineticcore.api.client.gui.input`:

| Record | Fields and helpers |
|---|---|
| `MouseInput` | `x()`, `y()`, `button()` (`MouseButton` enum: LEFT/RIGHT/MIDDLE/…), `rawButton()`, `modifiers()`, `isLeft()`, `isRight()`, `isMiddle()`, `hasShift()`, `hasControl()`, `hasAlt()`, `inside(x, y, w, h)` |
| `MouseDragInput` | `x()`, `y()`, `button()`, `rawButton()`, `deltaX()`, `deltaY()` |
| `ScrollInput` | `x()`, `y()`, `deltaX()`, `deltaY()` (use `deltaY()` where the old code used `delta`), `inside(...)` |
| `KeyInput` | `keyCode()`, `scanCode()`, `modifiers()`, `is(KineticKeyBindings.Key)`, `isEscape()`, `isEnter()` (includes keypad Enter), `hasShift()`, `hasControl()`, `hasAlt()` |
| `CharInput` | `codePoint()`, `modifiers()`, `text()` |

Page hooks. They all return `true` when the event was consumed.

| Hook | When it runs |
|---|---|
| `onMouseClickCapture(MouseInput)` | **Before** controls. Use it for things that must win over controls (drag handles, closing popups, command-suggestion clicks). |
| `onMouseClick(MouseInput)` | **After** no control consumed the click. This is the direct replacement for `canvasMouseClicked` / `nativeMouseClicked` / `containerMouseClicked`. Do **not** call a super method first: the controls have already had their chance. |
| `onMouseRelease(MouseInput)` | Always called, before controls, and the release still reaches the controls (so a drag released over a button is never lost). |
| `onMouseDrag(MouseDragInput)` | Before controls; return `true` only while your own drag is active. Same order as the old `scroll.drag(...) \|\| super.canvasMouseDragged(...)`. Works on container pages too. |
| `onMouseScroll(ScrollInput)` | After controls. |
| `onMouseMove(double x, double y)` | Every move. |
| `onKeyPress(KeyInput)`, `onKeyRelease(KeyInput)`, `onCharTyped(CharInput)` | **Before** the focused control. Return `false` for keys you don't handle. |
| `onTick()` | Every client tick (replaces `tick`/`canvasTick`/`nativeTick`/`containerTick`). |
| `onRemoved()` | When the host screen is removed (replaces `removed()`/`screenRemoved()`). |
| `onCloseRequested()` | Escape or `close()`. Return `true` if you handled it (for example by opening an "unsaved changes" dialog). Replaces `handleCloseRequest()`. |

Conversions:

- `button == 0` → `input.isLeft()`. `KineticMouseButtons.isPrimary(button)` → `input.isLeft()`.
- `keyCode == 257 || keyCode == 335` → `input.isEnter()`. `keyCode == 256` → `input.isEscape()`.
- Other GLFW key constants → `input.is(KineticKeyBindings.Key.X)`.
- `Screen.hasShiftDown()` inside a handler → `input.hasShift()`.

**Raw button exception.** Inside pages and custom controls every button is a `MouseInput`. Outside pages, some public contexts still hand you a raw `int button`: vanilla/third-party screen events (`ScreenMouseButtonContext.button()`), `KineticClientEvents.MouseButtonContext`, and mixin injections into vanilla methods. That is allowed. In those places:

- test the button with `KineticMouseButtons.isPrimary(button)`, `isSecondary(button)` or `isMiddle(button)`, or convert it once with `MouseButton.of(button)`;
- never compare against `0`, `1`, `2` or GLFW constants directly.

An `int button` in such a context is not a migration error.

---

## 4. Package / type map (old → new)

### 4.1 Screens, text, theme, layout, overlays

| Old | New |
|---|---|
| `api.client.screen.KineticScreen` | `api.client.gui.page.KineticPage` (layout `CANVAS`) |
| `api.client.screen.KineticNativeScreen` | `api.client.gui.page.KineticPage` (layout `NATIVE`) |
| `api.client.screen.KineticContainerScreen<M>` | `api.client.gui.page.KineticContainerPage<M>` |
| `api.client.theme.GuiTheme` (drawing helpers) | `api.client.gui.theme.KineticTheme` |
| `GuiTheme.Surface` / `Indicator` / `Palette` / `DEFAULT` | `KineticTheme.Surface` / `Indicator` / `Palette` / `DEFAULT` |
| `api.client.text.KineticText.translatable(...)` | `api.text.KineticI18n.translatable(...)` |
| `KineticText.get(key, args)` | `KineticI18n.string(key, args)` |
| `KineticText.drawScrollingLeft/Centered/Right(g, font, …)` | `g.scrollingText / scrollingTextCentered / scrollingTextRight(…)` |
| font measuring (`font.width`, `font.lineHeight`, `plainSubstrByWidth`, `split`) | `api.client.gui.text.KineticText.width / lineHeight / trim / ellipsize / wrap / wrappedHeight` |
| `KineticText.hasTranslation(key)` | `KineticI18n.hasTranslation(key)` |
| `KineticText.scrollOffset(contentWidth, viewportWidth)` (manual scrolling of mixed text + icons) | `api.client.gui.text.KineticText.scrollOffset(contentWidth, viewportWidth)` (same timing as `g.scrollingText`) |
| `GuiTheme.trim(font, text, width)` | `KineticText.trim(text, width)` |
| `KineticNumericFields.formatDecimal(value)` | `NumberType.DECIMAL.format(value)` |
| `GuiTheme.runWithoutDepthTest(runnable)` | `g.isolated(() -> { g.raise(1); … })`. Raise more steps if the content must clear several item layers. |
| `GuiTheme.scrollbar(...)`, `KineticScroll.renderScrollbarState / stateThumbHeight / stateOffsetFromPointer / stateOffsetFrom* / renderHorizontalScrollbarState / stateHorizontal*` | a `KineticScrollController` per scroll area: `update(total, visible)` or `updateRange(max, total, visible)`, `render`, `beginDrag`/`drag`/`release`, `scroll(deltaY)`, `scrollTo(offset)`. Pixel-based lists become row-based (`offset × rowHeight`). |
| `KineticWidgets.createCompactButton / createButton / create…(...)` + `renderControl(...)` | see §8.13; controls are only created by the page's `KineticUi` |
| `api.client.layout.GuiLayout` | `api.client.gui.layout.KineticLayout` (same members, renamed) |
| `api.client.overlay.KineticOverlays` | `api.client.gui.overlay.KineticOverlays` (same members) |
| `api.client.selector.KineticSelectors` | `api.client.gui.selector.KineticSelectors` (no `Screen parent` argument any more, see §8.8) |
| `api.client.selector.HudPositionEditor` | `api.client.gui.selector.KineticHudEditorPage` (§8.4) |
| `api.client.editor.KineticCommandListEditor` | `api.client.gui.editor.KineticCommandListEditor` (`open(...)`, `action(...)`) |
| `api.client.command.KineticCommandSuggestions` | `api.client.gui.command.KineticCommandAssist` (§8.6) |
| `api.client.widget.state.*` (`DragStateController`, `EditedEntryTracker`, `LayerState`) | `api.client.gui.state.*` (package move only; `KineticUiState` was removed, see the API cleanup section) |
| `api.client.widget.scroll.KineticScrollSettings` | `api.client.gui.scroll.KineticScrollSettings` |
| `KineticScroll.GridScrollController` | `api.client.gui.scroll.KineticScrollController` |
| `KineticScroll.State` | `api.client.gui.scroll.KineticScrollAnimator` |
| `KineticScroll.SmoothSelectionList<E>` / `SmoothEntry<E>` | `api.client.gui.widget.list.KineticRowList<T>` (§8.1) |
| `KineticAutoComplete.Suggestion` | `api.client.search.KineticSuggestion` |
| `KineticAutoComplete.stringDictionary(supplier)` | `KineticSuggestion.fromStrings(supplier)` |
| `api.client.widget.render.KineticEntityPreview` / `EntityPreviewRenderer` | `api.client.gui.widget.KineticEntityPreview` (`create()`, `render(KineticGraphics, …)`) |
| `KineticWidgets.create*(...)` detached factories | none. Controls are created only through `KineticUi` (§5). Panels and tab modules receive the `KineticUi` from the page's `build`. |

### 4.2 Control types

All of these live in `api.client.gui.widget` and are interfaces. Obtain them only from `KineticUi` builders.

| Old concrete class | New interface | Notable API |
|---|---|---|
| `KineticControl` / `WidgetControl` | `KineticControl` | `controlX/controlY/controlWidth/controlHeight`, `moveControlX/moveControlY/resizeControlWidth`, `moveTo(x, y)` (was `setPosition`), `controlVisible/setControlVisible`, `isEnabled/setEnabled/setActive`, `setTooltip(Component)`, `setTooltip(Supplier<Component>)`, `contains`, `controlHovered`, `controlFocused` (see §1.1: the vanilla names are not part of the API) |
| `StateButton`, `HighZButton`, `MenuButton` | `KineticButton` | `text/setText`, `isSelected/setSelected`, `isError/setError` |
| `ItemButton` | `KineticItemButton` | `icon()` |
| `ToggleButton`, `HighZToggleButton` | `KineticToggle` | `value/setValue` |
| `CycleButton` | `KineticCycleButton` | `index/setIndex`, `options()` |
| `ColorPreviewButton` | `KineticColorButton` | `rgb/setRgb` |
| `ColorSwatchButton` | `KineticColorSwatch` | `rgb/setRgb` |
| `Slider` | `KineticSlider` | `value/setSliderValue`, `isError`, `text/setText` |
| `KineticEditBox` | `KineticTextField` | `textValue/setTextValue`, `setDefaultText/defaultText`, `setValueColor`, `onTextChange`, `limitTextLength`, `filterText`, `formatText`, `setPlaceholder`, `setTextEditable`, `setValidator`, `isValueValid`, `setValidationError`, `flashValidationError`, `cursorIndex/setCursorIndex` |
| `KineticMultiLineEditBox` | `KineticTextArea` | `textValue/setTextValue`, `limitTextLength`, `onTextChange` |
| `NumericEditBox` | `KineticNumberField` (extends `KineticTextField`) | `getIntValue/getLongValue/getDoubleValue`, `setIntValue/…` |
| `AutoCompleteBox` | `KineticAutoCompleteField` | `setSelectionResponder`, `setSuggestionMatcher`, `setMaxVisibleSuggestions`, `setSuggestionPopupMaxWidth`, `isSuggestionPopupOpen` |
| `NumericAutoCompleteBox` | `KineticNumberAutoCompleteField` | both of the above |
| `KineticDropdowns.Dropdown` | `KineticDropdown` | `options()`, `selectedIndex/Value/Option`, `setSelectedValue` |
| `KineticDropdowns.Option` | `KineticDropdown.Option` | same record and validation |
| `KineticTabs.TabBar` | `KineticTabBar` | `tabAt`, `selectedIndex/setSelectedIndex`, `setTabActive` |
| `KineticTabs.ScrollableTabStrip` | `KineticTabStrip` | `setTabs`, `tabs`, `selectedIndex`, `scrollOffset`, `scrollBy`, `ensureSelectedVisible`, `tabAt`, `hoveredTooltip` |
| `KineticTabs.ScrollableTab` | `TabStripItem` | same record |
| `TextureButton` | none. Write a `KineticCustomControl` that draws `g.texture(...)` (§8.2). |
| `AutoCompleteBoxGroup` | none. Each autocomplete field closes its own popup, and the page closes popups on outside clicks. |

### 4.3 List and grid types (`api.client.gui.widget.list`)

The nested records and enums moved out of `KineticTabs` unchanged: `SelectionItem`, `ItemSelectionItem`, `ItemGridDensity`, `ItemGridOutline`, `ItemGridItem`, `ActionItem`, `ItemActionItem`, `RowAction`, `MultiActionItem`, `ActionHit`, `ToggleActionItem`, `RowToggle`, `MultiToggleItem`, `ToggleHit`, `ToggleItem`.

| Old interface | New interface |
|---|---|
| `ScrollableSelectionList` | `KineticSelectionList` |
| `ScrollableItemSelectionList` | `KineticItemSelectionList` |
| `ScrollableItemGrid` | `KineticItemGrid` |
| `ScrollableActionList` | `KineticActionList` |
| `ScrollableItemActionList` | `KineticItemActionList` |
| `ScrollableMultiActionList` | `KineticMultiActionList` |
| `ScrollableToggleActionList` | `KineticToggleActionList` |
| `ScrollableMultiToggleList` | `KineticMultiToggleList` |
| `ScrollableToggleList` | `KineticToggleList` |

Their methods are unchanged (`setItems`, `items`, `selectedIndex`, `setSelectedIndex`, `setBounds`, `scrollOffset`, `setScrollOffset`, `maxScrollOffset`, `ensureSelectedVisible`, `itemAt`, `hoveredTooltip`, …).

---

## 5. Controls: `add*` → `KineticUi` builders

Every builder shares `tooltip(Component)`, `tooltip(Supplier<Component>)` (dynamic), `enabled(boolean)` and `visible(boolean)`, and ends with `.build()`, which returns the control. `null` arguments that meant "none" in the old API are simply omitted.

| Old call | New call |
|---|---|
| `addButton(x, y, w, text, tooltip, action)` | `ui.button(x, y, w).text(text).tooltip(tooltip).onClick(action).build()` |
| `addButtonWithHandler(x, y, w, text, tooltip, btn -> …)` | `ui.button(x, y, w).text(text).onClick(btn -> …).build()` (`Consumer<KineticButton>`) |
| `addCompactButton(...)` | `ui.button(...).compact()...` |
| `addCardButton(x, y, w, narration, tooltip, action)` | `ui.button(x, y, w).text(narration).card().onClick(action).build()` |
| `addHighZButton` / `addCompactHighZButton` | `ui.button(...).layer(1)` / `.compact().layer(1)` |
| `addItemButton(x, y, w, stack, text, tooltip, action)` | `ui.itemButton(x, y, w, stack).text(text).onClick(action).build()` |
| `addToggleButton(x, y, w, value, onText, offText, tooltip, validator, responder)` | `ui.toggle(x, y, w).value(value).labels(onText, offText).tooltip(tooltip).validator(validator).onChange(responder).build()` |
| `addCompactToggleButton` / `addHighZToggleButton` | `ui.toggle(...).compact()` / `.layer(1)` |
| `addCycleButton(x, y, w, options, index, tooltip, validator, responder)` | `ui.cycleButton(x, y, w, options).index(index).validator(validator).onChange(responder).build()` |
| `addColorPreviewButton(x, y, w, rgb, text, tooltip, action)` | `ui.colorButton(x, y, w, rgb).text(text).onClick(action).build()` |
| `addColorSwatchButton(x, y, rgb, tooltip, action)` | `ui.colorSwatch(x, y, rgb).onClick(action).build()` |
| `addSlider(x, y, w, label, min, max, step, value, validator, responder, tooltip)` | `ui.slider(x, y, w).label(label).range(min, max, step).value(value).validator(validator).onChange(responder).build()` |
| `addTextField(x, y, w, label[, placeholder, validator, responder])` | `ui.textField(x, y, w).label(label).placeholder(p).validator(v).value(initial).maxLength(n).onChange(r).build()` |
| `addMultiLineTextField(x, y, w, h, label, …)` | `ui.textArea(x, y, w, h).label(...).placeholder(...).value(...).maxLength(n).onChange(...).build()` |
| `addIntegerField / addLongField / addDecimalField(x, y, w, label, allowNegative, min, max, validator, responder)` | `ui.numberField(x, y, w, NumberType.INT / LONG / DECIMAL).label(label).allowNegative(b).range(min, max).validator(v).value(n).onChange(r).build()` |
| `addAutoCompleteField(x, y, w, label, placeholder, dictSupplier, responder)` | `ui.autoComplete(x, y, w, dictSupplier).label(label).placeholder(p).value(v).onChange(r).onSelect(s).build()` |
| `addIntegerAutoCompleteField / Long / Decimal(...)` | `ui.numberAutoComplete(x, y, w, NumberType.X, dictSupplier).label(...).range(...).onChange(...).onSelect(...).build()` |
| `addDropdown(x, y, w, options, selected, tooltip, validator, responder)` | `ui.dropdown(x, y, w, options).selected(value).validator(v).onChange(r).build()` |
| `addTabBar(x, y, w, labels, tooltips, selected, responder)` | `ui.tabBar(x, y, w, labels).tooltips(tooltips).selected(i).onSelect(r).build()` |
| `addHighZTabBar` / `addVerticalHighZTabBar` | `ui.tabBar(...).layer(1)` / `.vertical().layer(1)` |
| `addScrollableTabStrip(x, y, w, tabs, pinned, selected, offset, prev, next, responder)` | `ui.tabStrip(x, y, w, tabs).pinnedLeading(pinned).selected(i).scrollOffset(px).arrows(prev, next).onSelect(r).build()` |
| `addScrollableSelectionList(x, y, w, h, items, selected, offset, responder)` | `ui.selectionList(x, y, w, h, items).selected(i).scrollOffset(rows).onSelect(r).build()` |
| `addScrollableItemSelectionList(...)` | `ui.itemSelectionList(x, y, w, h, items).selected(i).scrollOffset(rows).onSelect(r).build()` |
| `addScrollableItemGrid(x, y, w, h, density, items, offset, responder)` | `ui.itemGrid(x, y, w, h, density, items).scrollOffset(rows).onClick(r).build()` |
| `addScrollableActionList(x, y, w, h, items, selected, offset, actionW, responder, actionResponder)` | `ui.actionList(x, y, w, h, items).selected(i).scrollOffset(rows).actionWidth(actionW).onSelect(r).onAction(a).build()` |
| `addScrollableItemActionList(...)` | `ui.itemActionList(...)`, same options as `actionList` |
| `addScrollableMultiActionList(..., responder, (row, action) -> …)` | `ui.multiActionList(x, y, w, h, items).selected(i).scrollOffset(rows).onSelect(r).onAction((row, action) -> …).build()` |
| `addScrollableToggleActionList(..., toggleW, actionW, responder, toggleResponder, actionResponder)` | `ui.toggleActionList(x, y, w, h, items).toggleWidth(tw).actionWidth(aw).onSelect(r).onToggle(t).onAction(a).build()` |
| `addScrollableMultiToggleList(..., responder, (hit, value) -> …)` | `ui.multiToggleList(x, y, w, h, items).onSelect(r).onToggle((hit, value) -> …).build()` |
| `addScrollableToggleList(x, y, w, h, items, offset, responder)` | `ui.toggleList(x, y, w, h, items).scrollOffset(rows).onToggle(r).build()` |
| `addHighZScrollable*List(...)` | the same builder + `.layer(1)` |
| `addScrollableButton(..., l, t, r, b, pixelOffsetSupplier)` / `addScrollableToggleButton` / `addScrollableWidget(widget, …)` | `KineticUi rows = ui.scrollViewport(l, t, r, b, pixelOffsetSupplier);` then `rows.button(...)`, `rows.toggle(...)`, `rows.numberField(...)`, `rows.add(customControl)` (canvas pages only) |
| `addSmoothSelectionList(list)` | `ui.add(new MyRowList(...))` (§8.1) |
| `addExternalWidget(vanillaWidget)` | `ui.add(new MyCustomControl(...))` (§8.2) |
| `addControl(widget, tooltip)` | the matching builder |
| `removeKineticControl(c)` / `removeExternalWidget(w)` | `ui().remove(control)` |
| `registerWidgetTooltip(c, t)` / `registerDynamicWidgetTooltip(c, supplier)` | builder `.tooltip(...)`, or later `control.setTooltip(...)` |
| `setExternalWidgetEnabled/Visible(w, b)` | `control.setEnabled(b)` / `control.setControlVisible(b)` |
| `resetScrollableWidgets()` | nothing to do. Every rebuild starts clean. |
| `rebuildUi()` | `rebuild()` |

Behaviour notes:

- `onClick(Runnable)` and `onClick(Consumer<KineticButton>)` are both available.
- A `KineticNumberField` is also a `KineticTextField`, so `limitTextLength`, `onTextChange` and `setTextEditable` all work.
- A responder attached with `field.onTextChange(...)` after creation still works. Prefer `.onChange(...)` on the builder.
- A builder that is created but never `.build()`-ed adds nothing.

**Input text colors (enforced by the core, do not draw your own):**

| State | Color |
|---|---|
| Empty and unfocused: placeholder | pure white (`KineticTheme.fieldPlaceholderText()`); the placeholder's own color styling and `§` codes are ignored |
| Empty and focused | no placeholder |
| Value equal to the default | cyan (`KineticTheme.fieldDefaultText()`) |
| Any other value (content entered / modified) | green (`KineticTheme.fieldModifiedText()`) |
| Invalid value | red (`KineticTheme.fieldErrorText()`) |
| Custom rule | whatever `valueColor` returns (return `null` to fall back to the rows above) |

- Set the default with `.defaultText(String)` (text / auto-complete builders) or `.defaultValue(Number)` (number builders), or later `field.setDefaultText(...)`. Without a default, any entered text counts as modified. Search boxes normally have no default.
- Special cases use `.valueColor(value -> …)` or `field.setValueColor(...)`, for example a shop price field: `.valueColor(v -> canAfford(v) ? 0xFF55FF55 : 0xFFFF5555)`.
- Do not style placeholders (no colored components, no `§` codes, no `KineticI18n` argument styling tricks) expecting them to show; placeholders are always plain white.

---

## 6. Screen methods → page methods

| Old (screen) | New (page) |
|---|---|
| `buildUi()` | `build(KineticUi ui)` |
| `renderCanvasBackground` / `renderNativeBackground` / `renderContainerBackground` (UI part) | `renderBackground(KineticGraphics, mx, my, pt)`. Container pages also get `renderContainerBackground(...)`, drawn behind the slots. |
| `renderCanvasForeground` / `renderNativeOverlayRequests` / `renderUiForeground` | `renderForeground(KineticGraphics, mx, my, pt)` (above controls) |
| `renderScreenOverlay` (container) | `renderScreenOverlay(KineticGraphics, screenMx, screenMy, pt)` |
| `renderTooltips(graphics, scaledMx, scaledMy, mx, my)` / `requestContainerTooltips(...)` | `renderTooltips(int mx, int my)`. Call `showTooltip` / `showItemTooltip` from it. |
| `showTooltipLine(c)` / `showTooltip(list[, maxWidth])` / `showFormattedTooltip(seq)` / `showItemTooltip(stack)` | `showTooltip(c)` / `showTooltip(list[, maxWidth])` / `showFormattedTooltip(seq)` / `showItemTooltip(stack)` |
| `canvasMouseClicked` / `nativeMouseClicked` / `containerMouseClicked` | `onMouseClick(MouseInput)`, or `onMouseClickCapture` if the old code ran **before** `super.canvasMouseClicked(...)` |
| `canvasMouseReleased` / `…Dragged` / `…Scrolled` / `…Moved` | `onMouseRelease` / `onMouseDrag` / `onMouseScroll` / `onMouseMove` |
| `canvasKeyPressed` / `…KeyReleased` / `…CharTyped` | `onKeyPress` / `onKeyRelease` / `onCharTyped` |
| `tick` / `canvasTick` / `nativeTick` / `containerTick` / `containerUiTick` | `onTick()` |
| `removed()` / `screenRemoved()` | `onRemoved()` |
| `handleCloseRequest()` | `onCloseRequested()` (return `true` = handled) |
| `onClose()` | `close()` (runs `onCloseRequested` first) |
| `navigateBack()` | `navigateBack()` (skips the close hook) |
| `setParentScreen(parent)` / `new X(parent)` + `minecraft.setScreen(x)` | parent page: `openChild(new XPage(...))`. Closing the child returns to the parent automatically. |
| `isPauseScreen()` override | `setPausesGame(false)` in the constructor (default `true`) |
| `canvasWidth()` / `canvasHeight()` / `uiWidth()` / `uiHeight()` | `width()` / `height()` |
| `useCanvas(w, h, margin)` | `useCanvas(w, h, margin)` (constructor only) |
| `title` field | `title()` |
| `font` field | none. Use `KineticText.*` for measuring and `g.text(...)` for drawing. |
| `minecraft` field | none. Use `KineticClientRuntime` (player, level, …) or `KineticGui`. |
| `focusControl(c)` / `blurControl(c)` / `clearControlFocus()` | `focus(c)` / `blur(c)` / `clearFocus()` |
| `isControlFocused(c)` | `isFocused(c)` (page helper) |
| `focusedControl()` | `focusedControl()` (returns your custom control for custom controls) |
| `openContextMenu(x, y, items[, width])` / `closeContextMenu()` | same names on the page |
| `openDialog(title, message, yes, no, onYes, onNo)` | same |
| `overlayBlocksInput()` | same |
| `reserveStandaloneDraft` / `configureDraft` / `configureStandaloneDraft` / `commitDraft` / `discardDraft` / `hasUnsavedEdits` | same names |
| `registerPreviewWheelTarget(renderer, key, x, y, w, h)` | `registerPreviewZoomArea(KineticEntityPreview, key, x, y, w, h)` |
| `enableUiScissor(g, l, t, r, b)` … `disableUiScissor(g)` | `g.scissor(l, t, r, b)` … `g.endScissor()`, or better `g.clipped(l, t, r, b, () -> …)`. Coordinates are page coordinates; the host transforms them. |
| `enableCanvasScissor` / `disableCanvasScissor` | same as above |
| `isInsideCanvas(x, y)` / `isInsideUi(x, y)` | `x >= 0 && y >= 0 && x < width() && y < height()` |

Container-only (`KineticContainerPage<M>`): `menu()`, `hoveredSlot()`, `leftPos()`, `topPos()`, `imageWidth()`, `imageHeight()`, `setImageSize(w, h)`, `setTitleLabelPosition(x, y)` and `setInventoryLabelPosition(x, y)`. Call the setters from the constructor. By default, `renderTooltips` shows the hovered slot's item tooltip; call `super.renderTooltips(mx, my)` if you override it and still want that.

---

## 7. Drawing: `GuiGraphics` → `KineticGraphics`

`KineticGraphics` (`api.client.gui.render`) is the only drawing surface pages, custom controls, HUD renderers and render events see. The table covers every drawing call the old addons used. If something is missing, write it as a custom control built from these primitives. Do not unwrap to vanilla.

| Old | New |
|---|---|
| `graphics.fill(x1, y1, x2, y2, argb)` | `g.fill(x1, y1, x2, y2, argb)` |
| `graphics.fillGradient(x1, y1, x2, y2, top, bottom)` | `g.fillGradient(...)` |
| `graphics.renderOutline(x, y, w, h, argb)` | `g.outline(x, y, w, h, argb)` |
| `graphics.hLine / vLine` | `g.hLine(x1, x2, y, argb)` / `g.vLine(x, y1, y2, argb)` |
| `graphics.drawString(font, text, x, y, argb)` | `g.text(text, x, y, argb, true)`. **Vanilla's 5-arg `drawString` has a shadow**, so pass `true`. `g.text(text, x, y, argb)` has **no** shadow. |
| `graphics.drawString(font, text, x, y, argb, shadow)` | `g.text(text, x, y, argb, shadow)` (String, Component or FormattedCharSequence) |
| `graphics.drawCenteredString(font, text, cx, y, argb)` | `g.centeredText(text, cx, y, argb, true)` |
| `graphics.drawWordWrap(font, text, x, y, maxW, argb)` | `g.wrappedText(text, x, y, maxW, argb)` |
| `KineticText.drawScrollingLeft/Centered/Right(g, font, text, …)` | `g.scrollingText / scrollingTextCentered / scrollingTextRight(text, …)` |
| `graphics.renderItem(stack, x, y)` / `renderFakeItem` | `g.item(stack, x, y)` / `g.fakeItem(stack, x, y)` |
| `graphics.renderItemDecorations(font, stack, x, y[, text])` | `g.itemDecorations(stack, x, y[, text])` |
| `graphics.blit(new ResourceLocation(ns, path), x, y, u, v, w, h[, texW, texH])` | `g.texture(TEX, x, y, u, v, w, h)` with `static final KineticTexture TEX = KineticTexture.of(ns, path[, texW, texH]);` (default size 256×256) |
| scaled `blit(loc, x, y, w, h, u, v, rw, rh, tw, th)` | `g.texture(TEX, x, y, w, h, u, v, rw, rh)` |
| `graphics.setColor(r, g, b, a)` + `blit` + reset | `g.texture(TEX, x, y, u, v, w, h, argb)` (tinted) |
| `blit(x, y, 0, w, h, mobEffectTextures.get(effect))` | `g.effectIcon(effect, x, y, size)` |
| `pose().pushPose()` / `popPose()` | `g.push()` / `g.pop()`, or `g.isolated(() -> …)` |
| `pose().translate(x, y, 0)` / `scale(sx, sy, 1)` | `g.translate(x, y)` / `g.scale(sx, sy)` |
| `pose().mulPose(Axis.ZP.rotationDegrees(d))` | `g.rotate(d)` |
| `pose().translate(0, 0, 200..400)` (bring to front) | `g.raise(1)`. One step = 250 depth, enough to clear an item and its decorations. |
| `enableScissor` / `disableScissor` | `g.scissor(l, t, r, b)` / `g.endScissor()`, or `g.clipped(l, t, r, b, runnable)` |
| `font.width(x)` / `font.lineHeight` | `g.textWidth(x)` / `g.lineHeight()`, or `KineticText.width(x)` / `KineticText.lineHeight()` outside rendering |
| `font.plainSubstrByWidth(s, w)` | `KineticText.trim(s, w)`, or `KineticText.ellipsize(s, w)` for a trailing `...` |
| `font.split(component, w)` | `KineticText.wrap(component, w)` (returns `List<FormattedCharSequence>`), plus `KineticText.wrappedHeight` |
| `graphics.renderTooltip(...)` / `renderComponentTooltip(...)` | from `renderTooltips`: `showTooltip(...)` / `showItemTooltip(...)`. From HUD or event code: `KineticOverlays.requestTooltip(...)` |
| `InventoryScreen.renderEntityInInventoryFollowsMouse(...)` | `KineticEntityPreview.create()`, then `preview.render(g, entity or id, key, x, y, w, h, hovered)` |
| `graphics.guiWidth()` / `guiHeight()` | page: `width()` / `height()`; HUD: `KineticClientRuntime.guiScaledWidth()` / `guiScaledHeight()` |
| `RenderSystem.*` | not allowed in addon GUI code. Every primitive above sets up its own state. |

Theme helpers (`KineticTheme`, same names as the old `GuiTheme`, all taking `KineticGraphics`): `current()`, `canvasBackground`, `surface` (×2), `stateSurface`, `selectionFlash`, `selectionFlashText`, `checkerboard`, `colorSwatch`, `separator`, `verticalSeparator`, `gridFrame`, `stateOutline` (×2), `indicatorOutline` (×2), `indicatorFill` (×2), `indicatorColor`, `panel`, `panelAlt`, `shadow`, `alphaText(g, text, x, y, alpha)` (no `Font`), `hovering(mx, my, x, y, w, h)`, `itemSlot` (×5), `itemGrid`, `itemSelectorGrid`, `item(g, stack, x, y, slotSize, scale, decorations)` (no `Font`), `fieldText()` and `fieldMutedText()`.

Palette colours: `KineticTheme.current().text() / mutedText() / danger() / background() / …`.

---

## 8. Recipes for special cases

### 8.1 Custom row lists (old `SmoothSelectionList` / manual `GridScrollController` lists)

Use `KineticRowList<T>`. The core handles smooth scrolling, the themed scrollbar and its hover hint, middle-click jump with selection flash, the wheel, thumb dragging, clipping, zebra row surfaces and up/down key selection. You draw only the row content.

```java
private final class TypeList extends KineticRowList<Row> {
    TypeList() { super(16, 68, 612, 240, 24); }              // x, y, width, height, rowHeight

    @Override
    protected void renderRow(KineticGraphics g, Row row, int index, int x, int y, int w, int h,
                             boolean hovered, boolean selected) {
        g.item(row.icon(), x + 5, y + 4);
        g.text(KineticText.trim(row.name(), 320), x + 28, y + 8, KineticTheme.current().text());
    }

    @Override
    protected boolean onRowClick(Row row, int index, MouseInput input) {   // default: left click selects
        if (!input.isLeft()) return false;
        select(index);                  // fires setOnSelect callback and keeps the row visible
        return true;
    }

    @Override protected Component rowTooltip(Row row, int index) { return row.tooltip(); }  // optional
    @Override protected Component emptyText() { return KineticI18n.translatable("gui.mymod.empty"); } // optional
}
// in build():  list = ui.add(new TypeList());  list.setItems(rows);  list.setOnSelect(i -> …);
```

Other members:

- `items()`, `setItems(...)`
- `selectedIndex()`, `selectedItem()`, `setSelectedIndex(i)` (no callback), `select(i)` (with callback)
- `scrollOffset()`, `setScrollOffset(rows)`, `scrollTo(i)`
- `visibleRows()`, `rowAt(mx, my)`, `rowsWidth()`, `rowTop(index)` (current top Y of a row)
- `mouseX()` / `mouseY()` (last render's pointer) for hover checks inside `renderRow`
- Override `renderRowBackground(...)` to change or remove the zebra background.

**Buttons inside rows.** Old lists often created a real button per row entry and drew it manually with `renderControl`. In v2:

- paint it with `KineticTheme.button(g, x, y, w, h, text, hovered, active, error)` in `renderRow`, computing `hovered` from `mouseX()` / `mouseY()`;
- handle the click in `onRowClick`, where `rowTop(index)` and `input.inside(...)` give the button's hit area.

If every row is "label + toggle + action", use the built-in `ui.toggleActionList(...)` / `actionList(...)` / `multiActionList(...)` instead.

A hand-written list that drew rows in `renderBackground` with a `GridScrollController` field can alternatively keep that structure: replace the field with `KineticScrollController`. The signatures are identical except that `beginDrag(mx, my, input.button(), x, y, w, h, minThumb)` now takes the `MouseButton` (old hit padding: `beginDrag(mx, my, input.button(), x, y, w, h, minThumb, hitPadding)`), `release(input.button())`, and `render(g, …)` takes `KineticGraphics`. Prefer `KineticRowList` for new code.

**Middle-click jump for hand-written scroll areas.** Without a binding the thumb shows no hint and middle-click does nothing. Bind once and draw the flash for each item:

```java
private final KineticScrollController scroll = new KineticScrollController();
private int lastClicked = -1;   // or an existing selection concept (selected entry, editing row, active tab)

public MyPage() {
    // 目标：选中项；没有选中概念时用最近一次点击的项 / target: the selection, or the last clicked item
    scroll.bindSelection(() -> selectedIndex >= 0 ? selectedIndex : lastClicked,
            index -> index / COLUMNS - VISIBLE_ROWS / 2);            // item index → scroll offset (rows here)
}
// render loop, after drawing each item:
scroll.renderSelectionFlash(g, index, cellX, cellY, CELL, CELL); // draws the inverse-color border only during flash phases
```

Keep the target valid when the list changes (reset it to -1 or clamp it). For a plain list the target mapping can be omitted: the default centres the target row.

### 8.2 Custom controls (old external widgets, texture buttons, panels that drew and handled input)

```java
public final class TrashButton extends KineticCustomControl {
    private static final KineticTexture ICON = KineticTexture.of("itemcontrol", "textures/gui/trash.png", 16, 16);
    private final Runnable action;

    public TrashButton(int x, int y, Runnable action) { super(x, y, 18, 18); this.action = action; }

    @Override
    protected void render(KineticGraphics g, int mx, int my, float pt) {
        KineticTheme.stateSurface(g, controlX(), controlY(), controlWidth(), controlHeight(), KineticTheme.Surface.PANEL, false, controlHovered(), false);
        g.texture(ICON, controlX() + 1, controlY() + 1, 0, 0, 16, 16);
    }

    @Override
    protected boolean onMouseClick(MouseInput in) { if (!in.isLeft()) return false; action.run(); return true; }
}
// page:  ui.add(new TrashButton(10, 10, this::clear)).setTooltip(KineticI18n.translatable("…"));
```

Hooks: `render` (required), `onMouseClick` (only when inside the bounds), `onMouseRelease` (also delivered when the pointer left the control after a handled press), `onMouseDrag`, `onMouseScroll`, `onKeyPress`/`onCharTyped` (only while focused; return `true` from `isFocusable()`), `onFocusChanged`, `onTick`, `narration`. Self-drawn buttons call `playClickSound()` in `onMouseClick` to keep the vanilla click sound.

State setters are final and work with the page: `moveControlX/moveControlY/resizeControlWidth/resizeControlHeight/setControlVisible/setEnabled/setTooltip`.

Old "panel"/"tab module" classes that received the screen and called `screen.addXxx(...)` should now receive the `KineticUi` (and the page, for services such as `showTooltip`). Build their controls inside the page's `build`, and forward rendering and input from the page hooks.

### 8.3 Container menus

```java
public final class DummyPage extends KineticContainerPage<DummyMenu> {
    public DummyPage(DummyMenu menu, Component title) { super(menu, title); setImageSize(176, 222); }
    @Override protected void build(KineticUi ui) { … }
    @Override protected void renderContainerBackground(KineticGraphics g, int mx, int my, float pt) { … slots frame … }
}
// client setup (replaces MenuScreens.register / KineticClientMenus.register(type, XScreen::new)):
KineticClientMenus.register(MyMenus.DUMMY, DummyPage::new);          // PageFactory<M>: (menu, title) -> page
```

### 8.4 HUD position editors (old `HudPositionEditor` inside a native screen)

Extend `KineticHudEditorPage`. It is a NATIVE page that does not pause the game and builds the Save/Reset/Cancel buttons itself.

```java
public final class WalletHudEditorPage extends KineticHudEditorPage {
    private final List<Component> preview = Hud.previewLines();
    public WalletHudEditorPage() { super(KineticI18n.translatable("screen.adventuresystems.wallet.hud_editor")); }
    @Override protected int elementWidth()  { return Hud.contentWidth(preview); }
    @Override protected int elementHeight() { return Hud.contentHeight(preview.size()); }
    @Override protected HudLayout initialLayout(int sw, int sh) { return new HudLayout(savedX(sw), savedY(sh), Config.scale()); }
    @Override protected HudLayout defaultLayout(int sw, int sh) { return new HudLayout(sw - elementWidth() - 2, sh - elementHeight() - 2, 1.0D); }
    @Override protected void renderElement(KineticGraphics g, int x, int y, int mx, int my) { Hud.renderLines(g, preview, x, y); }
    @Override protected void save(HudLayout layout) { Config.setLayout(layout.x(), layout.y(), layout.scale()); KTConfigApi.refreshOpenScreens(); }
}
```

Optional overrides: `minimumScale()`, `instruction()` and `positionText(HudLayout)`. When your config stores offsets from a corner, override `positionText` to display those offsets. Helpers: `currentLayout()`, `scaledElementWidth()`, `scaledElementHeight()` and `resetLayout()`.

Editors that place something inside the inventory and need their own layout (own labels, no wheel scaling) extend `KineticPage` with `PageLayout.NATIVE` and draw the old `HudPositionEditor.renderInventoryReference(...)` with `KineticHudEditorPage.renderInventoryReference(g, width(), height(), mx, my)` (inventory background, labels and the mouse-following player model); `getInventoryLeft/Top` and `INVENTORY_WIDTH/HEIGHT` are public on the same class.

In the HUD renderer, `KineticClientRuntime.currentScreen() instanceof XHudEditorScreen` becomes `KineticGui.currentPage() instanceof XHudEditorPage`.

### 8.5 Adding controls to vanilla or third-party screens (inventory buttons and the like)

```java
KineticClientEvents.onScreenInitAfter(ctx -> {
    if (!(ctx.screen() instanceof InventoryScreen)) return;
    ctx.addButton(x, y, 20, KineticI18n.translatable("…"), action);   // returns KineticButton
    ctx.addControl(new TrashButton(x, y, action));                       // any KineticCustomControl
    KineticTextField box = ctx.addTextField(x, y, 80);                  // editable text field (joins the screen as a normal child)
});
```

`ctx.removeControl(control)` undoes any of these calls. Buttons and custom controls are overlays that never take keyboard focus; `addTextField` is the one exception, because typing needs focus. The old `ctx.addControl(<internal widget>)` is gone.

Injected controls are overlays owned by the core:

- They are drawn after the host screen and get mouse click, drag, release and wheel events before it.
- They are not added to the screen's listener list, so they never steal keyboard focus (the chat input stays focused).
- They are cleared whenever the screen re-initialises; your init handler adds them again.
- Custom controls here get no `onTick` or key hooks. To keep state in sync every frame, do it from an `onScreenRenderBefore` handler.

**Example — a draggable scrollbar on the vanilla chat screen (TextStudio).** The old version was a `ChatScreen` mixin that re-implemented scrollbar math. In v2 it is one custom control that delegates everything to `KineticScrollController`:

```java
KineticClientEvents.onScreenInitAfter(ctx -> {
    if (ctx.screen() instanceof ChatScreen screen) scrollbars.put(screen, ctx.addControl(new ChatScrollbar(screen)));
});
KineticClientEvents.onScreenRenderBefore((screen, g, mx, my, pt) -> {
    ChatScrollbar bar = scrollbars.get(screen);
    if (bar != null) bar.sync();                         // keeps bounds/visibility current even while hidden
});

final class ChatScrollbar extends KineticCustomControl {
    private final Screen screen;
    private final KineticScrollController scroll = new KineticScrollController();
    private boolean dragging;

    ChatScrollbar(Screen screen) { super(0, 0, 6, 1); this.screen = screen; }

    int sync() {                                         // vanilla chat position 0 = bottom, Kinetic offset 0 = top
        int visible = MinecraftChat.linesPerPage(), total = MinecraftChat.activeTrimmedMessageCount();
        int max = total - visible, track = (int) (visible * 9 * MinecraftChat.scale());
        setX((int) ((MinecraftChat.width() + 4) * MinecraftChat.scale()) + 2);
        setY(screen.height - 40 - track);
        setHeight(Math.max(1, track));
        setControlVisible(max > 0);
        if (max <= 0) return 0;
        scroll.updateRange(max, total, visible);
        if (!dragging) scroll.setOffset(max - MinecraftChat.activeScrollbarPosition());
        return max;
    }

    private void apply(int max) {
        int delta = (max - scroll.offset()) - MinecraftChat.activeScrollbarPosition();
        if (delta != 0) MinecraftChat.scroll(delta);
    }

    @Override protected void render(KineticGraphics g, int mx, int my, float pt) {
        if (sync() > 0) scroll.render(g, mx, my, controlX(), controlY(), controlWidth(), controlHeight(), 10);
    }
    @Override protected boolean onMouseClick(MouseInput in) {
        int max = sync();
        if (max <= 0 || !scroll.beginDrag(in.x(), in.y(), in.button(), controlX(), controlY(), controlWidth(), controlHeight(), 10)) return false;
        dragging = true; apply(max); return true;
    }
    @Override protected boolean onMouseDrag(MouseDragInput in) {
        if (!dragging) return false;
        int max = sync(); scroll.drag(in.y(), controlY(), controlHeight(), 10); apply(max); return true;
    }
    @Override protected boolean onMouseRelease(MouseInput in) {
        if (!dragging) return false;
        dragging = false; scroll.release(in.button()); return true;
    }
}
```

Mixins remain the tool only for changing vanilla behaviour (for example hiding a vanilla report button via its own fields). New interactive elements go through `ScreenInitContext` + `KineticCustomControl`. If a mixin must draw Kinetic visuals, wrap its `GuiGraphics` with `KineticGraphicsInterop.wrap(graphics)`; never use this wrapper in pages.

`ScreenInitContext` also exposes `findExistingListener(type)`, `removeFirstExistingListener(type)`, `addListener(listener)` and `removeListener(listener)`. These raw-listener calls are a **last-resort exception**: they exist only for modifying a widget that the vanilla or third-party screen already owns and that has no Kinetic control type, for example hiding or re-positioning another mod's special widget. Anything you **add** (buttons, inputs, toggles, menus, custom-drawn elements) must still go through `ctx.addButton` / `ctx.addTextField` / `ctx.addControl` or a Kinetic page.

### 8.6 Command input with suggestions (old `KineticCommandSuggestions`)

```java
private KineticCommandAssist assist;

@Override protected void build(KineticUi ui) {
    KineticTextField input = ui.textField(44, 278, 552).maxLength(2048).value("/").build();
    assist = KineticCommandAssist.attach(input, width(), height(), false, 10, value -> { /* your onChange */ });
    focus(input);
}
@Override protected void renderForeground(KineticGraphics g, int mx, int my, float pt) { assist.render(g, mx, my); }
@Override protected boolean onKeyPress(KeyInput in)             { return assist.keyPress(in); }
@Override protected boolean onMouseClickCapture(MouseInput in)  { return assist.mouseClick(in); }
@Override protected boolean onMouseScroll(ScrollInput in)       { return assist.mouseScroll(in); }
```

`attach` takes over the field's responder, so pass your change handler as the last argument.

### 8.7 Scroll viewports (rows of real controls that scroll with a custom-drawn list)

```java
KineticUi rows = ui.scrollViewport(LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H,
        () -> scroll.smoothOffset() * ROW_H);                 // pixel offset supplier
for (int i = 0; i < entries.size(); i++) {
    int y = LIST_Y + i * ROW_H + 6;                           // unscrolled content coordinates
    rows.button(x, y, 30).text(UP).onClick(() -> move(i, -1)).build();
}
```

Viewports work on CANVAS pages only. Controls outside the viewport are clipped and receive no input. When the number of rows changes, call `rebuild()`.

### 8.8 Selectors and editors

The parent screen argument is gone. The currently open page or screen becomes the parent automatically.

| Old | New |
|---|---|
| `KineticSelectors.openItemSelector(this, preset, cb)` | `KineticSelectors.openItemSelector(cb)` / `openItemSelector(preset, cb)` / `openItemSelectorWithOptions(...)` |
| `openEntitySelector(this, …)` | `openEntitySelector(…)` (two overloads) |
| `openItemListEditor(this, …)` | `openItemListEditor(…)` |
| `openNbtEditor(this, initial, onSave)` | `openNbtEditor(initial, onSave)` |
| `openColorPicker(this, title, rgb, onApply)` | `openColorPicker(title, rgb, onApply)` |
| `openPalette(this, …)` | `openPalette(…)` |
| `KineticCommandListEditor.open(parent, getter, setter, pageId, entryId, text)` / `create(...)` | `KineticCommandListEditor.open(getter, setter, pageId, entryId, text)`; use `KineticCommandListEditor.action(...)` for a config-button `Runnable` |

The callbacks run while the child is closing. When control is back on the parent, `build` runs again, so update state fields in the callback and let `build` recreate the controls.

### 8.9 Config GUI (`KTConfigApi`)

| Old | New |
|---|---|
| `KTConfigApi.createIndexScreen(parent)` / `createScreen(parent)` | `KTConfigApi.openIndex()` |
| `createScreen(parent, pageId)` / `createRegisteredPageScreen(parent, pageId)` | `KTConfigApi.openPage(pageId)` |
| `createScreen(parent, page)` / `createPageScreen(parent, page)` | `KTConfigApi.openPage(page)` |
| `createScreenForOwner(parent, modId)` | `KTConfigApi.openOwner(modId)` |
| `refreshScreenFromSource(screen)` | `KTConfigApi.refreshOpenScreens()` |
| `screenAction(parent -> new XScreen(parent))` (button action in a `KTConfigPage`) | `KTConfigApi.pageAction(XPage::new)`; use `KTConfigApi.configPageAction(() -> someKTConfigPage)` for config pages |
| `installConfigScreen(modId, parent -> …)` (Forge mod-list button) | `KTConfigApi.installConfigHub(modId)` or `KTConfigApi.installConfigPage(modId, XPage::new)` |

### 8.10 Events and hooks that carried `GuiGraphics`

| Old | New |
|---|---|
| `KineticClientEvents.onHudRender(stage, (GuiGraphics g, pt) -> …)` | `(KineticGraphics g, float pt) -> …` |
| `onScreenRenderBefore/After((screen, GuiGraphics g, mx, my, pt) -> …)` | `(screen, KineticGraphics g, mx, my, pt) -> …` |
| `ClientHooks.ResourceReloadUi.render(GuiGraphics, w, h)` | `render(KineticGraphics, w, h)` |
| `ScreenInitContext.addControl(widget)` | `ctx.addButton(...)` / `ctx.addTextField(...)` / `ctx.addControl(KineticCustomControl)` (§8.5) |

`Screen` stays in event signatures for identifying vanilla screens. To test for your own GUI, use `KineticGui.currentPage() instanceof MyPage` or `KineticGui.currentPage(MyPage.class)`.

### 8.11 Opening and closing from game code

| Old | New |
|---|---|
| `KineticClientRuntime.openScreen(new XScreen(...))` | `KineticGui.open(new XPage(...))` (root page; Escape returns to the game) |
| opening on top of the current GUI | `KineticGui.openChild(page)` |
| `openScreen(null)` / `minecraft.setScreen(null)` | `KineticGui.closeScreen()` |
| `currentScreen() instanceof XScreen s ? s : null` | `KineticGui.currentPage(XPage.class)` (nullable) |

Network handlers that forward a packet to an open GUI keep a public method on the page and call it through `KineticGui.currentPage(XPage.class)`.

### 8.12 Third-party integrations (Curios, JEI, …) now live in compat addons

The core ships no third-party integration. Put each one in the addon that already depends on that mod, or in a small optional compat addon, and plug it into the neutral extension points:

| Removed from the core | Where it goes now |
|---|---|
| `KineticCuriosEvents.onChange(handler)` | The addon that needs it subscribes to Curios' own `CurioChangeEvent` (it depends on Curios anyway). |
| Curios slots in the item selector's inventory tab | The compat addon registers `KineticSelectors.registerInventorySource((player, sink) -> CuriosApi.getCuriosInventory(player).ifPresent(inv -> inv.getCurios().values().forEach(h -> { var s = h.getStacks(); for (int i = 0; i < s.getSlots(); i++) sink.accept(s.getStackInSlot(i)); })))` once on the client. |
| JEI hovered item for copy-item (Ctrl+C on a JEI entry) | The compat addon's `@JeiPlugin` keeps the `IJeiRuntime` and registers `KineticHoveredItems.register(() -> runtime == null ? ItemStack.EMPTY : …getIngredientUnderMouse(VanillaTypes.ITEM_STACK)…)`. Also return the bookmark overlay's `getItemStackUnderMouse()`. |
| JEI exclusion areas for mini effects (the `InventoryEffectRendererGuiHandler` mixin) | The compat addon carries that mixin itself (`remap = false`, `require = 0`) and returns `((EffectAreaProvider) screen).kineticcore$effectAreas()` when the screen implements `EffectAreaProvider`. The core still implements `EffectAreaProvider` on effect-rendering inventory screens. |

Both registration calls return a `KineticEventSubscription`. Close it if the integration is ever disabled at runtime.

### 8.13 Old detached controls, panels, helpers and tab modules

Old addons often built controls outside the screen:

- `KineticWidgets.createCompactButton(...)` and the other `create*` factories
- controls kept in fields of a panel, helper or row entry and drawn manually with `KineticWidgets.renderControl(...)`
- clicks forwarded by hand

None of this exists in v2. The rules are fixed:

1. **Every control is created by the owning page's `KineticUi`**, inside `build(KineticUi ui)` or methods called from it. Panels, tab modules, helpers and editors receive the `KineticUi` (and the page, for services such as `showTooltip`, `openChild`, `rebuild`) as parameters. "It is not a screen" is not a reason to construct a widget any other way.
2. **No control instance lives across a rebuild.** Keep the *state* (values, selection, scroll offset, open tab) in fields and recreate controls in `build`. A field that stores a control must be reassigned in every `build`. Do not reuse the old instance after `rebuild()`, a resize, or returning from a child page.
3. **No manual rendering or event forwarding of controls.** The host draws and routes every control. A panel that needs custom visuals is a `KineticCustomControl` (§8.2). A row with inline buttons is a `KineticRowList` painting `KineticTheme.button(...)` (§8.1).
4. **Dynamic sub-sections** (for example parameter fields that change with a selected type) may add controls through `ui()` and remove them with `ui().remove(control)` between rebuilds, as long as `build` recreates the same state from fields. Use this only when a full `rebuild()` would disturb focus while the user is typing. Otherwise prefer `rebuild()`.
5. **Scrolling groups of real controls** use `ui.scrollViewport(...)` (§8.7), not per-frame `setX`/`setY` repositioning.

---

## 9. Worked example — ContentStudio `RecipeTypeFilterScreen`

Before (abridged):

```java
final class RecipeTypeFilterScreen extends KineticScreen {
    private final GridScrollController scroll = new GridScrollController();
    private AutoCompleteBox search;
    RecipeTypeFilterScreen(RecipeRemovalScreen parent, Map<ResourceLocation, Long> counts, ResourceLocation selected) {
        super(RecipeRemovalScreen.tr("filter_type")); setParentScreen(parent); …
    }
    @Override protected void buildUi() {
        search = addAutoCompleteField(16, 36, 608, tr("filter_type_search"), tr("filter_type_search"),
                KineticAutoComplete.stringDictionary(() -> …), null);
        search.onTextChange(value -> { query = value; refresh(); scroll.setOffset(0); });
        addButton(564, 328, 60, tr("back"), null, this::onClose);
    }
    @Override protected void renderCanvasBackground(GuiGraphics graphics, int mx, int my, float pt) {
        GuiTheme.panel(graphics, 0, 0, 640, 360);
        … enableUiScissor … rows with graphics.renderItem / drawString / font.plainSubstrByWidth … scroll.render(…)
    }
    @Override protected boolean canvasMouseClicked(double x, double y, int button) { … scroll.beginDrag … row hit … }
    @Override protected boolean canvasMouseScrolled(…) { … }  canvasMouseDragged(…)  canvasMouseReleased(…)
    @Override public boolean isPauseScreen() { return false; }
}
```

After. This version compiles against the new core, with the parent also ported to a page (`RecipeRemovalPage`):

```java
final class RecipeTypeFilterPage extends KineticPage {
    private record Row(ResourceLocation type, long count) { }

    private final RecipeRemovalPage parent;
    private final List<Row> all = new ArrayList<>();
    private final ResourceLocation selected;
    private String query = "";
    private TypeList list;

    RecipeTypeFilterPage(RecipeRemovalPage parent, Map<ResourceLocation, Long> counts, ResourceLocation selected) {
        super(RecipeRemovalPage.tr("filter_type"));
        setPausesGame(false);
        this.parent = parent;
        this.selected = selected;
        counts.forEach((type, count) -> all.add(new Row(type, count)));
    }

    @Override
    protected void build(KineticUi ui) {
        ui.autoComplete(16, 36, 608, KineticSuggestion.fromStrings(() -> all.stream()
                        .map(row -> row.type().toString()).toList()))
                .label(RecipeRemovalPage.tr("filter_type_search"))
                .placeholder(RecipeRemovalPage.tr("filter_type_search"))
                .value(query)
                .onChange(value -> { query = value; refresh(); list.setScrollOffset(0); })
                .build();
        ui.button(564, 328, 60).text(RecipeRemovalPage.tr("back")).onClick(this::close).build();
        list = ui.add(new TypeList());
        refresh();
    }

    private void refresh() {
        List<Row> visible = new ArrayList<>();
        visible.add(new Row(null, all.stream().mapToLong(Row::count).sum()));
        for (Row row : all) {
            if (KineticSearch.match(row.type() + " " + RecipeRemovalPage.typeName(row.type()).getString(), query)) {
                visible.add(row);
            }
        }
        list.setItems(visible);
        for (int i = 0; i < visible.size(); i++) {
            ResourceLocation type = visible.get(i).type();
            if (type == null ? selected == null : type.equals(selected)) list.setSelectedIndex(i);
        }
    }

    @Override
    protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KineticTheme.panel(graphics, 0, 0, 640, 360);
        graphics.text(title(), 16, 12, KineticTheme.current().text());
    }

    private final class TypeList extends KineticRowList<Row> {
        TypeList() { super(16, 68, 612, 240, 24); }

        @Override
        protected void renderRow(KineticGraphics g, Row row, int index, int x, int y, int width, int height,
                                 boolean hovered, boolean selectedRow) {
            if (row.type() != null) {
                ItemStack icon = RecipeRemovalPage.typeIcon(row.type());
                if (!icon.isEmpty()) g.item(icon, x + 5, y + 4);
            }
            String name = row.type() == null ? RecipeRemovalPage.tr("all_types").getString()
                    : RecipeRemovalPage.typeName(row.type()).getString();
            g.text(KineticText.trim(name + " · " + row.count(), 320), x + 28, y + 8, KineticTheme.current().text());
            if (row.type() != null) {
                g.text(KineticText.trim(row.type().toString(), 240), x + 354, y + 8, KineticTheme.current().mutedText());
            }
        }

        @Override
        protected boolean onRowClick(Row row, int index, MouseInput input) {
            if (!input.isLeft()) return false;
            parent.setTypeFilter(row.type());
            close();
            return true;
        }
    }
}
// parent page:  openChild(new RecipeTypeFilterPage(this, counts, currentType));
```

What changed:

- About 40 lines of scroll, drag, scissor and hit-test code disappeared into `KineticRowList`.
- The list now gets the scrollbar hover hint, middle-click jump, flash and keyboard selection for free.
- The parent keeps its own state. After the child closes, the parent's `build` runs again and shows the new filter.

---

## 10. Pitfalls seen while porting KineticCore's own features

1. **Shadow flag.** Vanilla `drawString(font, text, x, y, color)` and `drawCenteredString` draw a shadow. The matching new calls are `g.text(text, x, y, color, true)` and `g.centeredText(text, cx, y, color, true)`. The 4-argument `g.text(...)` has no shadow.
2. **Click order.** The old pattern `if (super.canvasMouseClicked(...)) return true; …` maps to `onMouseClick`, with no super call. Code that ran *before* `super` (for example `blurControl(search)` or starting a drag) belongs in `onMouseClickCapture` when it must not be swallowed by a control.
3. **Rebuild after structural changes.** Adding or removing rows that each own real controls requires `rebuild()`. Old screens often relied on `init()` re-running when returning from a selector. The page still rebuilds on return, but call `rebuild()` explicitly for in-page changes.
4. **Focus checks.** `isControlFocused(c)` becomes `isFocused(c)` (page helper). `c.controlFocused()` also works on controls.
5. **`isPauseScreen()`.** Pages pause by default. Editors opened in-game usually want `setPausesGame(false)`.
6. **Escape handling.** "Unsaved changes" dialogs go in `onCloseRequested()` (return `true`). A Back button that should also ask calls `close()`, not `navigateBack()`.
7. **Canvas numbers.** Old code used `canvasWidth()` for layout; use `width()`. Old code hard-coding 640×360 still works on CANVAS pages.
8. **Scrolling delta.** `canvasMouseScrolled(x, y, delta)` → `onMouseScroll(in)` with `in.deltaY()`.
9. **HUD offsets.** HUD editors that store "distance from the bottom-right corner" must convert in `save` and `positionText` (see `TpsHudEditorPage` in KineticCore for the exact math).
10. **Suggestions type.** `List<KineticAutoComplete.Suggestion>` → `List<KineticSuggestion>`. `KineticSearch.*Dictionary()` already returns the new type.

---

## 11. Whole-project scan, then per-file checklist

Before editing, scan the **entire** addon, not only the files that fail to compile first. That covers screens, tabs, panels, editors, helpers, HUD renderers, mixin screen injections and network GUI callbacks. Port every hit. For example, from the addon root:

```bash
rg -n --type java -e 'dev\.xyat\.kineticcore\.internal' \
  -e 'dev\.xyat\.kineticcore\.api\.client\.(screen|widget|theme|text|layout|overlay|selector|editor|command)\b' \
  -e '\b(KineticScreen|KineticNativeScreen|KineticContainerScreen|GuiTheme|GuiLayout|GridScrollController|SmoothSelectionList|SmoothEntry|HudPositionEditor|KineticWidgets|KineticTabs|KineticCommandSuggestions|KineticCuriosEvents)\b' \
  -e '\b(StateButton|ItemButton|ToggleButton|CycleButton|TextureButton|KineticEditBox|NumericEditBox|AutoCompleteBox|KineticMultiLineEditBox|Dropdown|Slider)\b' \
  -e '\bGuiGraphics\b|\bFont\b|PoseStack|RenderSystem' \
  -e 'Button\.builder|new EditBox|new MultiLineEditBox|Tooltip\.create|addRenderableWidget|clearWidgets\(|rebuildWidgets\(|this\.init\(\)' \
  -e 'Minecraft\.getInstance\(\)\.setScreen|\.setScreen\(|MenuScreens\.register|openScreen\(' \
  -e 'KineticSelectors\.open\w+\(this|KineticText\.translatable|Component\.translatable\(' \
  -e 'KTConfigApi\.(create\w*Screen\w*|screenAction|installConfigScreen|refreshScreenFromSource)' \
  -e 'new ResourceLocation\(.*textures/' \
  src
```

- Hits inside `mixin` packages that use vanilla `GuiGraphics` are allowed (see the hard rules). Everything else is migration work.
- The scan is wider than the build check on purpose: `checkKineticAddonArchitecture` (§12) fails the build on the hard-rule hits, while this scan also lists old API names that only the compiler would report.
- `\bFont\b` and `\bSlider\b` can also match unrelated names; confirm each hit.

Then, per file:

- [ ] No `import dev.xyat.kineticcore.internal`.
- [ ] No `extends KineticScreen / KineticNativeScreen / KineticContainerScreen / Screen / AbstractContainerScreen`.
- [ ] No `GuiGraphics`, `Font`, `PoseStack`, `RenderSystem` or `Minecraft.getInstance()` in GUI code (mixins excepted for `GuiGraphics`). No `ResourceLocation` used as a texture handle; `ResourceLocation` as a business or registry ID is fine.
- [ ] No `Component.translatable(` and no old `KineticText.translatable(`; use `KineticI18n`.
- [ ] Each `add*` call became a `ui.*` builder ending in `.build()`.
- [ ] `build` is idempotent and reads state from fields.
- [ ] Input hooks use the records; no raw GLFW numbers.
- [ ] Every `new XScreen(parent)` + `setScreen` became `openChild(new XPage(...))` or `KineticGui.open(...)`.
- [ ] Config buttons use `KTConfigApi.pageAction / configPageAction / KineticCommandListEditor.action`.
- [ ] Menu screens are registered with `KineticClientMenus.register(type, XPage::new)`.
- [ ] HUD renderers and `onHudRender`/`onScreenRender*` lambdas take `KineticGraphics`.
- [ ] Network protocols, packet formats, translation keys and lang files are unchanged. Client-side packet handlers that referenced old Screen types or looked up the open GUI are migrated to `KineticGui.open(...)` / `KineticGui.currentPage(XPage.class)`.
- [ ] No Curios/JEI code relied on KineticCore; integrations are wired through §8.12.

**Business behaviour must not change.** GUI v2 migration may change *how things are drawn and operated*, never the business rules. For every migrated page, compare against the old code:

- [ ] Default values of every field, toggle, dropdown and cycle button.
- [ ] Number rules: integer vs decimal, negative allowed or not, min/max, formatting of displayed values.
- [ ] Validators and the exact moment values are written (on change vs on save).
- [ ] Save / apply / cancel / back semantics, including which data is written and toasts shown.
- [ ] Unsaved-changes confirmation and draft behaviour (`configureDraft`, `commitDraft`, `discardDraft`).
- [ ] Escape / back navigation targets and `onCloseRequested` handling.
- [ ] Pause behaviour (`setPausesGame(false)` where the old screen returned `false` from `isPauseScreen`).
- [ ] Search and filter rules (matching, pinyin, case), sort order.
- [ ] Selection state after actions (add, delete, duplicate, reorder), and scroll position persistence (static "last scroll" fields included).
- [ ] Network packets sent, their order and payloads; data and config formats.
- [ ] Permission checks and server round-trips (request → open) unchanged.

---

## 12. Verification

Division of work:

1. **The migrator (you, the coding model)** does the static work only:
   - run the whole-project scan (§11);
   - audit every call against the public API;
   - migrate every hit completely;
   - self-review against both checklists.

   **Do not run Gradle, `build` or `compileJava`.**
2. **The project owner** compiles locally and sends the compiler errors back. The migrator fixes them and returns updated sources. Repeat until the build is clean.
   - **Architecture check (mandatory).** Copy `gradle/kinetic-addon-architecture.gradle` from the core and add `apply from: 'gradle/kinetic-addon-architecture.gradle'` to the addon's `build.gradle`. `compileJava` then first runs `checkKineticAddonArchitecture`, which fails on every hard-rule violation outside mixins: `internal`/`feature` imports, `extends Screen`/`AbstractContainerScreen`/vanilla widgets, `Button.builder`, `new EditBox`, `new ConfirmScreen`, `Tooltip.create`, `addRenderableWidget`/`clearWidgets`/`this.init()`, `setScreen`/`MenuScreens.register`/`NetworkHooks.openScreen`, imports of vanilla GUI types that have a Kinetic replacement (`GuiGraphics`, `Font`, vanilla widgets, `ConfirmScreen`, `ClientTooltipComponent`), `Screen.hasShiftDown()`-style modifier checks, `Component.translatable` and texture `ResourceLocation`s. Each failure names the file, line, rule id and the API to use instead; treat them like compiler errors. Mixins (`@Mixin` classes and `mixin` packages) may still use vanilla GUI types but not `internal` or `Component.translatable`.
   - A genuine exception, such as a JEI category that has to draw with `GuiGraphics`, is listed per file and rule in `build.gradle`, after the `apply from` line: `ext.kineticArchitectureAllow = ['src/main/java/.../ExampleCategory.java': ['vanilla-gui-type']]`. An entry that no longer matches anything fails the check, so the list cannot go stale. Rule of thumb: when the API already provides the capability, use it; only where it does not may an addon build its own, and that is exactly what an allow entry declares. Vanilla screen types such as `Screen`, `PauseScreen` or `InventoryScreen` stay importable for `instanceof` checks on screens the API hands out.
3. **Final-JAR check (mandatory).** `compileJava` passing is not enough: ForgeGradle renames vanilla-colliding methods during `reobfJar`.
   - Core: `gradlew build` runs `checkKineticReobfApi`, which compares every public/protected method of `dev.xyat.kineticcore.api` in the compiled classes with the reobf JAR and fails if any was renamed (for example to `m_252754_`).
   - Addons: copy `gradle/KineticReobfCheck.java` and `gradle/kinetic-addon-reobf-verification.gradle` from the core, add `apply from: 'gradle/kinetic-addon-reobf-verification.gradle'` **before** the `dependencies` block and `kineticCoreReobf "dev.xyat.kineticcore:kineticcore:${kineticCoreVersion}"` next to the `fg.deobf(...)` core line. `gradlew build` then runs `checkKineticReobfRefs`: every method reference from the addon JAR into `dev/xyat/kineticcore` must resolve by name and descriptor in the released core JAR (`-Pkineticcore_jar=<path>` tests against a local core build). It also reports references to `internal` packages.
   - Manual run: `java gradle/KineticReobfCheck.java addon <kineticcore.jar> <addon.jar>`.
4. **The project owner** runs the in-game smoke test for each page:
   - open and close with Escape and with Back
   - resize the window (tests `build` idempotence)
   - open a child page or selector and return
   - scroll lists with the wheel and by dragging the thumb
   - hover the scrollbar thumb for 0.5 s (hint)
   - middle-click the thumb (jump + flash)
   - tooltips, context menus and dialogs
   - GUI scale 1–4 and a 4K window
   - the business-equivalence items from §11


---

## 13. API cleanup after v2: removed duplicates and renamed members

Several public entry points did exactly what another API already does. They were removed without bridges, so
an addon that uses one of them no longer compiles against this core and must be updated. Run this scan from the
addon root and replace **every** hit using the table below:

```bash
rg -n --type java \
  -e '\bKineticUiState\b' -e '\bKineticEnvironment\b' -e '\bKineticFeatures\b' \
  -e 'KineticPaths\.(configDirectory|gameDirectory)\(' \
  -e 'CommandText\.createSuggestCommand\(' -e 'onCrawlPose\(|\bCrawlPoseHandler\b' \
  -e 'KineticSuperFlight\.maxSpeed\(' -e 'superFlightRenderPitch\(' \
  -e 'KineticCreativeTabs\.refreshSearch\(' -e '\.getStats\([^,)]*,\s*[0-9]' \
  -e 'KineticFlight\.(sources|addSource|removeSource|isFlightAllowed|refresh|isDebouncing|setDebouncing|lastKnownFlying|setLastKnownFlying|serverNoclipEnabled|isInternalUpdate|isProcessingExplicitCancel|isGamemodeSwitching)\b' \
  src
```

The `getStats` pattern only matches the old `ServerTickTracker` call with a numeric mode; vanilla
`player.getStats()` and the new `Stat` form do not match.

| Removed | Replacement |
|---|---|
| `KineticUiState.Drag<T>` | `api.client.gui.state.DragStateController<T>` (same methods) |
| `KineticUiState.EditedEntries<T>` | `api.client.gui.state.EditedEntryTracker<T>` (same methods) |
| `KineticUiState.Layer<L>` | `api.client.gui.state.LayerState<L>` (same methods) |
| `KineticEnvironment.isClient / isDedicatedServer / runOnClient / callOnClient / runOnDedicatedServer` | `KineticPlatform.` + the same method name and arguments |
| `KineticFeatures.isEnabled(id)` | `KineticFeatureSwitches.isEnabled(id)` |
| `KineticPaths.configDirectory()` / `gameDirectory()` | `KineticPlatform.configDirectory()` / `gameDirectory()` |
| `CommandText.createSuggestCommand(display, prefix, key)` | `CommandText.suggest(display, prefix, key)`; a translated label uses `CommandText.suggest(KineticI18n.translatable(labelKey), prefix, key)` |
| `CommonHooks.onCrawlPose(handler)` / `CommonHooks.CrawlPoseHandler` | `CommonHooks.onPlayerPoseUpdate(handler)` / `CommonHooks.PlayerPoseUpdateHandler` |
| `KineticSuperFlight.maxSpeed(entity)` | `KineticFlightAttributes.flightSpeed(entity)` |
| `KineticFlightClient.superFlightRenderPitch()` | removed without replacement; it was only kept for old addons |
| `KineticCreativeTabs.refreshSearch(items)` (common class, crashed on servers) | `api.client.search.KineticItemSearch.refreshCreativeSearch(items)` (client only) |
| `ServerTickTracker.getStats(seconds, 0)` / `getStats(seconds, 1)` | `getStats(seconds, ServerTickTracker.Stat.AVERAGE)` / `getStats(seconds, ServerTickTracker.Stat.MAXIMUM)` |
| `KineticFlight.sources / addSource / removeSource / isFlightAllowed / refresh` | `KineticFlightSources.sources / addSource / removeSource / allowsFlight / refresh` |
| `KineticFlight.serverNoclipEnabled(player)` | `KineticFlight.noclipEnabled(player)` |
| `KineticFlight.isDebouncing / setDebouncing / lastKnownFlying / setLastKnownFlying` and the public fields `isInternalUpdate / isProcessingExplicitCancel / isGamemodeSwitching` | removed; they were KineticCore flight-feature internals that nothing outside the feature read |
| `KineticItemTooltips.registerComponentFactory(type, data -> new X())` where `X implements ClientTooltipComponent` (`getHeight`, `getWidth(Font)`, `renderImage(Font, x, y, GuiGraphics)`) | the same call where `X implements KineticTooltipComponent`: `height()`, `width()` (measure text with `KineticText.width`), `render(KineticGraphics, x, y)`. Draw text with `g.text(..., true)` and icons with `g.item(...)`; `g.push()` / `g.translate` / `g.scale` / `g.pop()` replace `pose()` |
| The `marked` component of `SelectionItem`, `ItemSelectionItem`, `ActionItem`, `ItemActionItem`, `ToggleActionItem`, `MultiActionItem` and `MultiToggleItem` (the small green dot at the row end) | removed without replacement; delete the argument right after `active` in full constructor calls and drop `item.marked()` reads. The short convenience constructors keep their parameters |

`KineticFlight` keeps `installNoclipSyncSender`, `noclipEnabled`, `applyServerNoclip`, `syncServerNoclip` and
`copyPersistentState`.

**Behaviour changes that need no addon code change:**

- `MinecraftContainers`, `MinecraftAttributes` and `MinecraftChat` now always work. Their mixins used to live in
  feature packages, so turning off the "Container Item Access" or "Attribute Range Extension" startup switch made
  every call throw `ClassCastException`. The switches now only turn off the matching KineticCore features.
- A menu type registered with `KineticMenuTypes.register` receives an empty `NetworkBuffer` when the server opened
  it without an opening payload, instead of crashing the client.
- Single-line input text sits exactly where vanilla `EditBox` puts it (the earlier extra 3 px downward offset was
  removed). Multi-line text areas now scroll their text with the scrollbar and clip it to the field, and font mods
  that draw input text themselves render it inside the scaled canvas.
- Leaving creative mode now tells the client that noclip was switched off.

**Known hits in the addons checked with this release:**

- RealmControl `teleport/TpdCommand.java` lines 63–69: four `CommandText.createSuggestCommand(` →
  `CommandText.suggest(`.

## 14. Addon-local helpers that the core now covers

The addons checked with this release each carried private copies of helpers that do what a core API does. Delete
the helper and use the core entry point; the visible result stays the same.

| Addon helper or pattern | Core replacement |
|---|---|
| `InputColors.apply(ui.textField(...)...build())` (cyan until edited, white placeholder) | `ui.textField(...)...firstShownTextAsDefault().build()`; the same on `numberField`, `autoComplete` and `numberAutoComplete`. An already built field: `field.useFirstShownTextAsDefault()` |
| `ColorText.translatable(key, args)` with a per-key `ChatFormatting` table | `KineticI18n.translatable(key, args)`; put a `§` code right before each placeholder in the language files |
| `.withStyle(style -> style.withClickEvent(new ClickEvent(...)).withHoverEvent(...))` | `CommandText.clickToRun / clickToSuggest / clickToCopy / clickToOpenUrl(text, value, hover)` |
| `CommandUtils` help lines | `CommandText.header / executable / suggest` |
| `MinecraftForge.EVENT_BUS.post(event)` / `@SubscribeEvent` for the addon's own or a third-party event | `KineticExternalEvents.post(event)` / `KineticExternalEvents.subscribe(Type.class, listener)` |
| World-to-screen labels drawn with `RenderSystem`, `PoseStack` and `Font.drawInBatch` | `KineticWorldRender.beginScreenOverlay(context)`: `project(worldPos)` and `graphics()` |
| Hand-written scrollbars (thumb maths, grab offset, `KineticScrollAnimator` wheel handling) | `KineticScrollController`: `update` / `updateRange`, `render`, `beginDrag` / `drag` / `release`, `scroll` |
| A custom HUD position editor screen | `KineticHudEditorPage` (`renderBackdrop`, `scalable()` for fixed-scale elements) |
| `ClientTooltipComponent` implementations | `KineticTooltipComponent` |
| `mx >= c.controlX() && mx < c.controlX() + c.controlWidth() && ...` | `c.contains(mx, my)` |
| Gray `ChatFormatting.GRAY` / `§7` on client text | `KineticTheme.muted(text)` |
