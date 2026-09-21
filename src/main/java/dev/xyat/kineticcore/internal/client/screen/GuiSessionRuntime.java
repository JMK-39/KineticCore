package dev.xyat.kineticcore.internal.client.screen;

import dev.xyat.kineticcore.api.client.screen.KineticContainerScreen;
import dev.xyat.kineticcore.api.client.screen.KineticNativeScreen;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Internal owner for Kinetic Screen parent navigation and shared draft sessions. */
public final class GuiSessionRuntime {
    private static final Map<Screen, Screen> PARENTS = new WeakHashMap<>();
    private static final Map<Screen, ParentBinding> EXPLICIT_PARENTS = new WeakHashMap<>();
    private static final Map<Screen, DraftSession> DRAFTS = new WeakHashMap<>();

    private record ParentBinding(Screen parent) {
    }

    private GuiSessionRuntime() {
    }

    public static boolean routeSelectionListWheel(
            List<? extends GuiEventListener> children,
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (children == null || !Double.isFinite(mouseX) || !Double.isFinite(mouseY)
                || !Double.isFinite(delta) || delta == 0D) return false;
        // The last registered hovered child is visually on top. Buttons and text
        // fields also own their regions: their rejected wheel events must not leak
        // through to an underlying scrolling list.
        for (int index = children.size() - 1; index >= 0; index--) {
            GuiEventListener child = children.get(index);
            if (child != null && child.isMouseOver(mouseX, mouseY)) {
                child.mouseScrolled(mouseX, mouseY, delta);
                return true;
            }
        }
        return false;
    }

    public static void handleScreenOpening(Screen current, Screen next) {
        if (current == next) return;

        ParentBinding explicitBinding = takeExplicitParent(next);
        Screen navigationParent = explicitBinding == null ? current : explicitBinding.parent();

        // Opening directly from a null screen starts a new navigation root unless
        // the child explicitly declared its parent before being opened.
        if (current == null && next != null && explicitBinding == null) {
            synchronized (PARENTS) {
                PARENTS.remove(next);
            }
        }

        if (isKineticScreen(next)) {
            if (explicitBinding != null) {
                synchronized (PARENTS) {
                    if (navigationParent == null) {
                        PARENTS.remove(next);
                    } else {
                        PARENTS.put(next, navigationParent);
                    }
                }
            } else if (current != null && isNotReturningToAncestor(current, next)) {
                synchronized (PARENTS) {
                    PARENTS.put(next, current);
                }
            }
            if (navigationParent != null) {
                shareDraftSessionWithChild(navigationParent, next);
            }
        }

        if (current != null && differentDraftSession(current, next)) {
            try {
                discardDraft(current);
            } finally {
                releaseDraft(current);
            }
        }
    }

    public static void setExplicitParent(Screen screen, Screen parent) {
        if (screen == null) return;
        synchronized (EXPLICIT_PARENTS) {
            EXPLICIT_PARENTS.put(screen, new ParentBinding(parent));
        }
    }

    private static ParentBinding takeExplicitParent(Screen screen) {
        if (screen == null) return null;
        synchronized (EXPLICIT_PARENTS) {
            return EXPLICIT_PARENTS.remove(screen);
        }
    }

    public static boolean handlePlainScreenEscape(Screen screen, int keyCode) {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE || !isKineticScreen(screen)) return false;
        if (screen instanceof KineticScreen
                || screen instanceof KineticContainerScreen<?>
                || screen instanceof KineticNativeScreen) {
            return false;
        }
        back(screen);
        return true;
    }

    public static void reserveStandaloneOwner(Screen screen) {
        if (screen == null) return;
        synchronized (DRAFTS) {
            DraftSession session = DRAFTS.computeIfAbsent(screen, ignored -> new DraftSession());
            session.reserveStandaloneOwner(screen);
        }
    }

    public static <T> void configureDraft(
            Screen screen,
            Supplier<T> capture,
            Consumer<T> restore,
            boolean standaloneOwner
    ) {
        if (screen == null) return;
        Objects.requireNonNull(capture, "capture");
        Objects.requireNonNull(restore, "restore");
        synchronized (DRAFTS) {
            DraftSession session = DRAFTS.get(screen);
            boolean created = session == null;
            if (created) {
                session = new DraftSession();
                DRAFTS.put(screen, session);
            }
            try {
                session.configureDraft(screen, capture, restore, standaloneOwner);
            } catch (RuntimeException | Error failure) {
                if (created) {
                    DRAFTS.remove(screen);
                    session.release();
                }
                throw failure;
            }
        }
    }

    public static void commitDraft(Screen screen) {
        DraftSession session = draftSessionOf(screen);
        if (session != null) {
            session.commitBaseline(screen);
        }
    }

    public static void discardDraft(Screen screen) {
        DraftSession session = draftSessionOf(screen);
        if (session != null) {
            session.discardToBaseline();
        }
    }

    public static boolean hasUnsavedEdits(Screen screen) {
        DraftSession session = draftSessionOf(screen);
        return session != null && session.isDirty();
    }

    public static void back(Screen screen) {
        Screen parent = parentOf(screen);
        if (screen instanceof AbstractContainerScreen<?> && KineticClientRuntime.localPlayer() != null) {
            KineticClientRuntime.localPlayer().closeContainer();
        }
        KineticClientRuntime.openScreen(parent);
    }

    private static void shareDraftSessionWithChild(Screen current, Screen next) {
        DraftSession draft = draftSessionOf(current);
        if (draft == null || !draft.enabled()) return;
        synchronized (DRAFTS) {
            DraftSession nextDraft = DRAFTS.get(next);
            if (nextDraft != null && nextDraft.isStandaloneOwner(next)) return;
            DRAFTS.put(next, draft);
        }
    }

    private static boolean differentDraftSession(Screen first, Screen second) {
        if (first == null || second == null) return true;
        DraftSession a = draftSessionOf(first);
        DraftSession b = draftSessionOf(second);
        return a == null || !a.enabled() || a != b;
    }

    /** Release the entire shared session once navigation leaves its editing flow. */
    private static void releaseDraft(Screen screen) {
        if (screen == null) return;
        synchronized (DRAFTS) {
            DraftSession abandoned = DRAFTS.get(screen);
            if (abandoned == null) return;
            // Child screens share the same DraftSession. Removing just the current
            // key leaves the owner and its callbacks retained in the static map.
            DRAFTS.entrySet().removeIf(entry -> entry.getValue() == abandoned);
            abandoned.release();
        }
    }

    private static DraftSession draftSessionOf(Screen screen) {
        if (screen == null) return null;
        synchronized (DRAFTS) {
            return DRAFTS.get(screen);
        }
    }

    /** Keep an ancestor's original parent when jumping back multiple levels. */
    private static boolean isNotReturningToAncestor(Screen current, Screen next) {
        if (current == null || next == null) return true;
        synchronized (PARENTS) {
            Set<Screen> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Screen ancestor = PARENTS.get(current);
                 ancestor != null && visited.add(ancestor);
                 ancestor = PARENTS.get(ancestor)) {
                if (ancestor == next) return false;
            }
        }
        return true;
    }

    private static Screen parentOf(Screen screen) {
        if (screen == null) return null;
        synchronized (PARENTS) {
            return PARENTS.get(screen);
        }
    }

    private static boolean isKineticScreen(Screen screen) {
        if (screen == null) return false;
        if (screen instanceof KineticScreen
                || screen instanceof KineticContainerScreen<?>
                || screen instanceof KineticNativeScreen) {
            return true;
        }
        return screen.getClass().getName().startsWith("dev.xyat.kinetic");
    }

    private static final class DraftSession {
        private Supplier<Object> capture;
        private Consumer<Object> restore;
        private Object baseline;
        private Screen owner;
        private boolean standaloneOwner;
        private boolean enabled;
        private boolean restoring;

        void reserveStandaloneOwner(Screen owner) {
            this.owner = owner;
            this.standaloneOwner = true;
        }

        <T> void configureDraft(Screen owner, Supplier<T> capture, Consumer<T> restore, boolean standaloneOwner) {
            // A failed baseline capture must not replace a previously valid draft session.
            T snapshot = capture.get();
            this.owner = owner;
            this.standaloneOwner = standaloneOwner;
            this.capture = capture::get;
            this.restore = value -> {
                @SuppressWarnings("unchecked") T typed = (T) value;
                restore.accept(typed);
            };
            this.baseline = snapshot;
            this.enabled = true;
        }

        boolean isStandaloneOwner(Screen screen) {
            return standaloneOwner && owner == screen;
        }

        boolean enabled() {
            return enabled;
        }

        boolean isDirty() {
            return enabled && capture != null && !Objects.equals(baseline, capture.get());
        }

        void commitBaseline(Screen caller) {
            if (owner != null && owner != caller) return;
            if (owner == caller && enabled && capture != null) {
                baseline = capture.get();
            }
        }

        void discardToBaseline() {
            if (!enabled || restore == null || capture == null) return;
            if (!Objects.equals(baseline, capture.get())) {
                restoreSnapshot(baseline);
            }
        }

        void release() {
            capture = null;
            restore = null;
            baseline = null;
            owner = null;
            standaloneOwner = false;
            enabled = false;
        }

        private void restoreSnapshot(Object value) {
            if (restore == null || restoring) return;
            restoring = true;
            try {
                restore.accept(value);
            } finally {
                restoring = false;
            }
        }
    }
}
