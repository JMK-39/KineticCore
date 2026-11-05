package dev.xyat.kineticcoreitemcreatorvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTextField;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.gui.LayoutCheck;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Real GUI checks with isolated sample settings; never resets the user's installed configuration. */
public final class ConfigGuiValidation {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ConfigGuiValidation.class);
    private static int phase, ticks, captures, ownerRows, singleRows;
    private static volatile boolean captureReady;
    private static Screen caller, owner;
    private static KTConfigPage page;
    private static String query;
    private static java.util.Map<?, ?> pendingBefore;
    private static int resetReturn;
    private static int originalGuiScale = -1;
    private static final int[] samples = new int[20];
    private static int saves, savesBefore;
    private static final java.util.List<java.util.List<?>> listSaves = new java.util.ArrayList<>();
    private static final java.util.List<String> listSamples = java.util.stream.IntStream.range(0, 40).mapToObj(i -> "entry " + i).toList();
    private static int colorSaves;
    private static boolean styleBefore;
    private static String nbtSaved;

    private ConfigGuiValidation() {}

    public static void install() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, ConfigGuiValidation::tick);
        LOG.info("CONFIG_GUI_INSTALL game={}", dev.xyat.kineticcore.api.runtime.KineticPlatform.gameDirectory());
    }

    private static void tick() {
        if (phase == 99) return;
        try {
            var client = Minecraft.getInstance();
            require(++ticks < (phase == 0 ? 12000 : 2400), "configuration GUI phase timed out: " + phase);
            if (client.getOverlay() != null || ticks < 12) return;
            switch (phase) {
                case 0 -> {
                    if (Boolean.getBoolean("kineticcore.configGuiValidation.inWorld")) {
                        if (client.player == null || client.level == null || client.screen != null) return;
                    } else if (client.screen == null) return;
                    require(client.getWindow().getWidth() == 1920 && client.getWindow().getHeight() == 1080,
                            "configuration GUI validation requires a 1920x1080 client");
                    originalGuiScale = client.options.guiScale().get();
                    client.options.guiScale().set(0);
                    resizeClient();
                    java.util.Arrays.fill(samples, 7);
                    var builder = KTConfigPage.builder("kineticcorevalidation:layout", net.minecraft.network.chat.Component.literal("Layout validation"))
                            .scope(dev.xyat.kineticcore.api.config.client.KTConfigScope.CLIENT_LOCAL).onSave(() -> saves++);
                    for (int i = 0; i < samples.length; i++) {
                        final int index = i;
                        builder.intValueUnbounded("sample_" + i, net.minecraft.network.chat.Component.literal("Sample " + i),
                                () -> samples[index], value -> samples[index] = value, i + 1, null);
                    }
                    page = builder.build();
                    KTConfigApi.register(page);
                    query = "sample_0";
                    caller = client.screen;
                    KTConfigApi.openOwner("kineticcorevalidation");
                    owner = client.screen;
                    require(owner.getClass().getSimpleName().equals("KTModuleConfigScreen"), "owner is not the generic module page");
                    ownerRows = rows(owner, "rows");
                    next(1);
                }
                case 1 -> { capture("owner"); resetReturn=2; next(11); }
                // The last two keys are typed one by one. The page rebuilds its search box after each key; the new
                // box once selected everything typed so far, so each key replaced the query instead of adding to it.
                case 2 -> { if (captureReady) { search().setTextValue(query.substring(0, query.length() - 2)); next(40); } }
                case 40 -> { type(query.charAt(query.length() - 2)); next(41); }
                case 41 -> { type(query.charAt(query.length() - 1)); next(3); }
                case 3 -> {
                    require(query.equals(search().textValue()), "owner search lost its query");
                    require(rows(owner, "rows") > 0 && rows(owner, "rows") < ownerRows, "owner search did not filter its rows");
                    capture("owner-search"); next(4);
                }
                case 4 -> { if (captureReady) { java.util.Arrays.fill(samples, 7); KTConfigApi.openPage(page); next(5); } }
                case 5 -> {
                    require(client.screen.getClass().getSimpleName().equals("KTConfigScreen"), "single page is not the generic config page");
                    singleRows = rows(client.screen, "layoutEntries");
                    capture("single"); resetReturn=6; next(11);
                }
                case 6 -> { if (captureReady) { search().setTextValue(query.substring(0, query.length() - 2)); next(42); } }
                case 42 -> { type(query.charAt(query.length() - 2)); next(43); }
                case 43 -> { type(query.charAt(query.length() - 1)); next(7); }
                case 7 -> {
                    require(query.equals(search().textValue()), "single search lost its query");
                    require(rows(client.screen, "layoutEntries") > 0 && rows(client.screen, "layoutEntries") < singleRows,
                            "single search did not filter its rows");
                    capture("single-search"); next(8);
                }
                case 8 -> { if (captureReady) { click(24, 12); next(9); } }
                case 9 -> {
                    require(client.screen == owner, "single Back did not return to its owner");
                    require(query.equals(search().textValue()), "Back lost the owner's search state");
                    click(24, 12); next(10);
                }
                case 10 -> {
                    require(client.screen == caller, "owner Back did not return to its caller");
                    KTConfigApi.openIndex(); next(15);
                }
                case 11 -> {
                    if (!captureReady) return;
                    pendingBefore = new java.util.HashMap<>((java.util.Map<?, ?>)value(client.screen, "pendingValues"));
                    savesBefore = saves;
                    click(560,12); next(12);
                }
                case 12 -> {
                    Object dialog = value(((KineticScreenHost)client.screen).kineticRuntime().overlays(), "dialog");
                    require(dialog != null, "Reset All must require a confirmation dialog");
                    require(pendingBefore.equals(value(client.screen, "pendingValues")), "opening confirmation changed the draft");
                    capture(resetReturn == 2 ? "owner-reset-confirm" : "single-reset-confirm"); next(13);
                }
                case 13 -> {
                    if (!captureReady) return;
                    Object dialog = value(((KineticScreenHost)client.screen).kineticRuntime().overlays(), "dialog");
                    clickOverlay((dev.xyat.kineticcore.api.client.gui.widget.KineticButton)value(dialog, "cancelButton"));
                    require(pendingBefore.equals(value(client.screen, "pendingValues")), "cancelling reset changed the draft");
                    click(560,12); next(14);
                }
                case 14 -> {
                    Object dialog = value(((KineticScreenHost)client.screen).kineticRuntime().overlays(), "dialog");
                    clickOverlay((dev.xyat.kineticcore.api.client.gui.widget.KineticButton)value(dialog, "confirmButton"));
                    require(saves > savesBefore, "confirmed reset must persist immediately without Save");
                    for (int i = 0; i < samples.length; i++) require(samples[i] == i + 1, "reset omitted sample " + i);
                    invoke(client.screen,"refreshFromSource"); invoke(client.screen,"rebuildUi");
                    next(resetReturn);
                }
                case 15 -> { capture("index"); next(16); }
                case 16 -> {
                    if (!captureReady) return;
                    require(client.screen.getClass().getSimpleName().equals("KTConfigIndexScreen"), "installed addon index missing");
                    // The style switch at the top right of the index flips the GUI style at once.
                    styleBefore = dev.xyat.kineticcore.internal.client.gui.theme.GuiStyle.modern();
                    click(588,12);
                    require(dev.xyat.kineticcore.internal.client.gui.theme.GuiStyle.modern() != styleBefore, "index style switch did not change the GUI style");
                    next(18);
                }
                case 18 -> { capture("index-style-switched"); next(19); }
                case 19 -> {
                    if (!captureReady) return;
                    click(588,12);
                    require(dev.xyat.kineticcore.internal.client.gui.theme.GuiStyle.modern() == styleBefore, "index style switch did not switch back");
                    LOG.info("CONFIG_GUI_STYLE_SWITCH_PASS modernBefore={}", styleBefore);
                    click(24,12); next(17);
                }
                case 17 -> {
                    require(client.screen == caller, "index Back did not return to its caller");
                    openSampleList(); next(20);
                }
                case 20 -> {
                    capture("compact-list");
                    // Rows start right under the header and the description line (LIST_Y 44): row 0's Down button.
                    click(570, 52);
                    require(((List<?>) value(client.screen, "values")).get(0).equals("entry 1"), "list reorder did not move the selected entry");
                    next(21);
                }
                case 21 -> {
                    if (!captureReady) return;
                    var done=client.screen.children().stream().filter(control -> control instanceof dev.xyat.kineticcore.api.client.gui.widget.KineticButton button
                            && button.controlX()==596 && button.controlY()==4).findFirst().orElseThrow();
                    client.screen.setFocused(done); pressEnter();
                    require(listSaves.isEmpty(),"Enter activated a focused Save action outside confirmation");
                    click(616, 12);
                    require(listSaves.size() == 1 && listSaves.get(0).size() == 40
                            && listSaves.get(0).get(1).equals("entry 0"), "list Done lost or reordered other entries");
                    openSampleList(); click(560,12); click(24,12); next(22);
                }
                case 22 -> {
                    Object dialog = value(((KineticScreenHost)client.screen).kineticRuntime().overlays(), "dialog");
                    require(dialog != null && listSaves.size() == 1, "Back must confirm an unsaved list without writing it");
                    clickOverlay((dev.xyat.kineticcore.api.client.gui.widget.KineticButton)value(dialog, "cancelButton"));
                    require(listSaves.size() == 1, "discarding a list invoked its save callback");
                    KineticClientRuntime.openScreen(dev.xyat.kineticcore.internal.client.selector.ColorPickerScreen.single(
                            net.minecraft.network.chat.Component.literal("A long color picker title which must stay clear of its toolbar actions"),
                            0x123456, color -> colorSaves++)); next(23);
                }
                case 23 -> {
                    int y = ((Number)value(client.screen,"panelY")).intValue();
                    require(Math.abs(2*y + 215 - 360) <= 1, "single-color dialog is not centered vertically");
                    capture("single-color-centered"); next(24);
                }
                case 24 -> {
                    if (!captureReady) return;
                    int y = ((Number)value(client.screen,"panelY")).intValue();
                    click(490,y+18);
                    require(colorSaves == 1, "color Apply did not invoke its callback once");
                    String text = java.util.stream.IntStream.range(0,100).mapToObj(i -> "line " + i + " x".repeat(70)).collect(java.util.stream.Collectors.joining("\n"));
                    KineticClientRuntime.openScreen(new dev.xyat.kineticcore.internal.client.selector.NbtEditorScreen(text, raw -> null, raw -> nbtSaved=raw, client.screen));
                    next(25);
                }
                case 25 -> {
                    capture("expanded-nbt");
                    click(80,70);
                    Object widget=value(client.screen,"nbtEditor");
                    double before=((Number)value(widget,"scrollOffset")).doubleValue();
                    var wheel=client.screen.getClass().getDeclaredMethod("canvasMouseScrolled",double.class,double.class,double.class);
                    wheel.setAccessible(true);
                    wheel.invoke(client.screen,80D,12D,-4D);
                    require(((Number)value(widget,"scrollOffset")).doubleValue()==before,"NBT toolbar wheel scrolled the editor");
                    wheel.invoke(client.screen,80D,70D,before>0 ? 4D : -4D);
                    require(((Number)value(widget,"scrollOffset")).doubleValue()!=before,"NBT content wheel did not scroll");
                    checkNativeTooltipSnapshot(); next(26);
                }
                case 26 -> {
                    if (!captureReady) return;
                    click(616,12);
                    require(nbtSaved != null && nbtSaved.length()>14000 && nbtSaved.contains("line 99"),"NBT Save truncated the text");
                    KineticClientRuntime.openScreen(caller);
                    var message=net.minecraft.network.chat.Component.literal("Command fixture");
                    var commands=new java.util.concurrent.atomic.AtomicReference<List<String>>(java.util.stream.IntStream.range(0,40).mapToObj(i -> "say isolated fixture " + i).toList());
                    dev.xyat.kineticcore.api.client.gui.editor.KineticCommandListEditor.open(commands::get,commands::set,"kineticcorevalidation:commands","commands",
                            new dev.xyat.kineticcore.api.client.gui.editor.KineticCommandListEditor.Text(message,message,message,message,message,message,message,null));
                    next(27);
                }
                case 27 -> {
                    capture("compact-commands");
                    Screen list=client.screen; click(524,38);
                    require(client.screen==list,"disabled command action opened its row editor");
                    click(80,38); next(28);
                }
                case 28 -> {
                    require(client.screen.getClass().getSimpleName().equals("CommandEditorScreen"),"command text did not open its editor");
                    capture("command-editor"); next(29);
                }
                case 29 -> {
                    if (!captureReady) return;
                    click(24,12);
                    require(client.screen.getClass().getSimpleName().equals("CommandListEditorScreen"),"command Back did not return to the list");
                    click(24,12);
                    KineticClientRuntime.openScreen(owner);
                    prepareToasts(false); next(30);
                }
                case 30 -> {
                    assertToastSpacing(false); capture("notification-short-long"); next(31);
                }
                case 31 -> {
                    if(!captureReady)return;
                    prepareToasts(true); next(32);
                }
                case 32 -> {
                    assertToastSpacing(true); capture("notification-long-short"); next(33);
                }
                case 33 -> {
                    if(!captureReady)return;
                    dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.clearToasts(); next(90);
                }
                case 90 -> {
                    phase = 99;
                    LOG.info("CONFIG_GUI_PASS captures={} page={} query={} size=1920x1080", captures, page.id(), query);
                    writeResult("CONFIG_GUI_PASS captures=" + captures + " page=" + page.id()
                            + " query=" + query + " size=1920x1080");
                    String nextValidation = System.getProperty("kineticcore.configGuiValidation.thenValidation", "");
                    if (!nextValidation.isBlank()) {
                        Class.forName(nextValidation).getMethod("install").invoke(null);
                        return;
                    }
                    restoreGuiScale();
                    if (Boolean.getBoolean("kineticcore.configGuiValidation.stopAfter")) KineticClientRuntime.stopClient();
                }
                default -> throw new AssertionError("unknown configuration GUI phase " + phase);
            }
        } catch (Throwable failure) {
            phase = 99;
            LOG.error("CONFIG_GUI_FAIL", failure);
            writeResult("CONFIG_GUI_FAIL " + failure);
            if (caller != null) KineticClientRuntime.openScreen(caller);
            restoreGuiScale();
            if (Boolean.getBoolean("kineticcore.configGuiValidation.stopAfter")) KineticClientRuntime.stopClient();
        }
    }

    private static void next(int value) { phase = value; ticks = 0; }

    private static void prepareToasts(boolean wrappedFirst) {
        dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.clearToasts();
        for(var position:dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.Position.values()) {
            var shortText=net.minecraft.network.chat.Component.literal("One-line notice");
            var wrapped=net.minecraft.network.chat.Component.literal("Wrapped notification\nSecond line");
            dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.toast("spacing-"+position+"-first",wrappedFirst?wrapped:shortText,position,30000,0,0);
            dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.toast("spacing-"+position+"-second",wrappedFirst?shortText:wrapped,position,30000,0,0);
        }
    }

    private static void assertToastSpacing(boolean wrappedFirst) throws ReflectiveOperationException {
        var field=dev.xyat.kineticcore.internal.client.overlay.GuiOverlayRuntime.class.getDeclaredField("ACTIVE_TOASTS");
        field.setAccessible(true);var notices=(java.util.List<?>)field.get(null);
        require(notices.size()==10,"notification spacing fixture lost an anchor or message");
        for(int i=0;i<notices.size();i+=2) {
            var first=notices.get(i);var second=notices.get(i+1);
            int firstY=Math.round(((Number)value(first,"currentY")).floatValue());
            int secondY=Math.round(((Number)value(second,"currentY")).floatValue());
            int firstHeight=wrappedFirst?28:18,secondHeight=wrappedFirst?18:28;
            boolean bottom=value(first,"position")==dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.Position.BOTTOM_CENTER;
            int gap=bottom?firstY-secondY-secondHeight:secondY-firstY-firstHeight;
            require(gap==2,"notification borders must have two points between short and wrapped notices: "+value(first,"position")+" gap="+gap);
        }
    }

    private static void openSampleList() throws ReflectiveOperationException {
        var type=Class.forName("dev.xyat.kineticcore.internal.client.config.KTConfigListScreen");
        var constructor=type.getDeclaredConstructor(net.minecraft.network.chat.Component.class,net.minecraft.network.chat.Component.class,
                boolean.class,List.class,java.util.function.Consumer.class);
        constructor.setAccessible(true);
        KineticClientRuntime.openScreen((Screen)constructor.newInstance(net.minecraft.network.chat.Component.literal("Compact list"),
                net.minecraft.network.chat.Component.literal("Forty isolated entries; reorder and discard must preserve the complete list."),false,
                listSamples,(java.util.function.Consumer<List<?>>) result -> listSaves.add(new java.util.ArrayList<>(result))));
    }

    private static void checkNativeTooltipSnapshot() throws ReflectiveOperationException {
        var overlays=((KineticScreenHost)Minecraft.getInstance().screen).kineticRuntime().overlays();
        var scope=new java.util.concurrent.atomic.AtomicBoolean(true);
        var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        var extra=net.minecraft.network.chat.Component.literal("Fixture action instruction");
        try(var subscription=dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips.onBuild((item,lines)->{
            if(scope.get()) lines.add(net.minecraft.network.chat.Component.literal("Fixture unsaved draft"));
        })) {
            overlays.itemTooltip(stack,List.of(extra)); scope.set(false); stack.setCount(7);
        }
        Object request=value(overlays,"tooltip");
        var lines=(List<?>)value(request,"lines");
        require(lines.stream().anyMatch(line -> ((net.minecraft.network.chat.Component)line).getString().contains("Fixture unsaved draft")),
                "native tooltip was built after its draft scope closed");
        require(lines.stream().anyMatch(line -> ((net.minecraft.network.chat.Component)line).getString().contains("Fixture action instruction")),
                "native tooltip lost page instructions");
        require(((net.minecraft.world.item.ItemStack)value(request,"stack")).getCount()==1,"queued native tooltip shares its caller's mutable stack");
    }

    private static void restoreGuiScale() {
        if (originalGuiScale < 0) return;
        var client = Minecraft.getInstance();
        client.options.guiScale().set(originalGuiScale);
        originalGuiScale = -1;
        resizeClient();
    }

    private static void resizeClient() {
        //? if >=26.1 {
        /*Minecraft.getInstance().resizeGui();
        *///?} else {
        Minecraft.getInstance().resizeDisplay();
        //?}
    }

    private static void writeResult(String result) {
        try {
            String configured = System.getProperty("kineticcore.configGuiValidation.output");
            Path directory = configured == null
                    ? dev.xyat.kineticcore.api.runtime.KineticPlatform.gameDirectory().resolve("config-gui-validation")
                    : Path.of(configured);
            require(directory.isAbsolute(), "configuration GUI output directory must be absolute");
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("result.txt"), result + System.lineSeparator());
        } catch (Exception failure) {
            LOG.error("CONFIG_GUI_RESULT_FAIL {}", failure.toString());
        }
    }

    private static KineticTextField search() {
        return Minecraft.getInstance().screen.children().stream()
                .filter(control -> control instanceof KineticTextField field && field.controlY() == 4)
                .map(control -> (KineticTextField) control).findFirst().orElseThrow();
    }

    private static int rows(Screen screen, String name) throws ReflectiveOperationException {
        Field field = screen.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return ((List<?>) field.get(screen)).size();
    }

    private static Object value(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> type=object.getClass();type!=null;type=type.getSuperclass()) try {
            Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);
        } catch (NoSuchFieldException ignored) {}
        throw new NoSuchFieldException(name);
    }
    private static void invoke(Object object,String name) throws ReflectiveOperationException {
        for (Class<?> type=object.getClass();type!=null;type=type.getSuperclass()) try {
            var method=type.getDeclaredMethod(name);method.setAccessible(true);method.invoke(object);return;
        } catch (NoSuchMethodException ignored) {}
        throw new NoSuchMethodException(name);
    }
    private static void clickOverlay(dev.xyat.kineticcore.api.client.gui.widget.KineticButton button) {
        clickScreen(button.controlX()+button.controlWidth()/2.0,button.controlY()+button.controlHeight()/2.0);
    }

    /** Types one character through the screen, as the keyboard does. */
    private static void type(char c) {
        var screen = Minecraft.getInstance().screen;
        //? if >=26.1 {
        /*screen.charTyped(new net.minecraft.client.input.CharacterEvent(c));
        *///?} else {
        screen.charTyped(c, 0);
        //?}
    }

    private static void click(double x, double y) {
        var screen = Minecraft.getInstance().screen;
        var host = (KineticScreenHost) screen;
        double sx = host.kineticRuntime().canvas().toScreenX(x), sy = host.kineticRuntime().canvas().toScreenY(y);
        clickScreen(sx,sy);
    }
    private static void clickScreen(double sx,double sy) {
        var screen=Minecraft.getInstance().screen;
        //? if >=26.1 {
        /*var input = new net.minecraft.client.input.MouseButtonEvent(sx, sy, new net.minecraft.client.input.MouseButtonInfo(0, 0));
        screen.mouseClicked(input, false); screen.mouseReleased(input);
        *///?} else {
        screen.mouseClicked(sx, sy, 0); screen.mouseReleased(sx, sy, 0);
        //?}
    }

    private static void pressEnter() {
        var screen=Minecraft.getInstance().screen;
        //? if >=26.1 {
        /*screen.keyPressed(new net.minecraft.client.input.KeyEvent(257,0,0));
        *///?} else {
        screen.keyPressed(257,0,0);
        //?}
    }

    private static void capture(String name) {
        var problems = LayoutCheck.currentScreenProblems();
        require(problems.isEmpty(), "configuration GUI layout: " + problems);
        require(LayoutCheck.lastCheckedControls > 0, "layout check saw no configuration controls");
        var client = Minecraft.getInstance();
        captureReady = false;
        String file = "core-config-1080-" + name + ".png";
        //? if >=26.1 {
        /*net.minecraft.client.Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), 1, message -> {
        *///?} else {
        net.minecraft.client.Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), message -> {
        //?}
            captures++; captureReady = true; LOG.info("CONFIG_GUI_CAPTURE {}", file);
        });
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
