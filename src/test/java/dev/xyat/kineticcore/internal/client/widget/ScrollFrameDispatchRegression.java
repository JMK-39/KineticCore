package dev.xyat.kineticcore.internal.client.widget;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 滚动条帧收集：只派发给上一完整帧、后渲染者优先、嵌套 Screen 互不串帧。 */
public final class ScrollFrameDispatchRegression {
    private static int checks;

    private ScrollFrameDispatchRegression() {
    }

    public static void main(String[] args) {
        List<String> hits = new ArrayList<>();
        Fake back = new Fake("back", true, hits);
        Fake front = new Fake("front", true, hits);
        Fake idle = new Fake("idle", false, hits);

        KineticScrollFrameRuntime.Frame frame = new KineticScrollFrameRuntime.Frame();
        check(!frame.middleClick(2), "empty frame ignores middle click");

        KineticScrollFrameRuntime.enter(frame);
        KineticScrollFrameRuntime.register(back);
        KineticScrollFrameRuntime.register(front);
        KineticScrollFrameRuntime.register(front);
        KineticScrollFrameRuntime.register(idle);
        check(!frame.middleClick(2), "click during an unfinished frame uses the previous (empty) frame");
        KineticScrollFrameRuntime.exit(frame);

        check(!frame.middleClick(0), "primary button is never treated as middle click");
        check(frame.middleClick(2), "middle click dispatched after frame completes");
        check(hits.equals(List.of("front")), "top-most (last rendered) hovered participant wins: " + hits);

        KineticScrollFrameRuntime.Frame parent = new KineticScrollFrameRuntime.Frame();
        KineticScrollFrameRuntime.Frame child = new KineticScrollFrameRuntime.Frame();
        KineticScrollFrameRuntime.enter(child);
        KineticScrollFrameRuntime.enter(parent);
        KineticScrollFrameRuntime.register(back);
        KineticScrollFrameRuntime.exit(parent);
        KineticScrollFrameRuntime.register(front);
        KineticScrollFrameRuntime.exit(child);
        hits.clear();
        check(child.middleClick(2) && hits.equals(List.of("front")), "nested parent render does not leak into child: " + hits);
        hits.clear();
        check(parent.middleClick(2) && hits.equals(List.of("back")), "parent keeps its own participants: " + hits);

        KineticScrollFrameRuntime.register(front);
        check(true, "register outside any screen render is a no-op");

        System.out.println("PASS: " + checks + " scroll frame dispatch regressions");
    }

    private record Fake(String name, boolean hovered, List<String> hits) implements KineticScrollFrameRuntime.Participant {
        @Override
        public Component pendingHint() {
            return null;
        }

        @Override
        public boolean handleMiddleClick() {
            if (!hovered) return false;
            hits.add(name);
            return true;
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
        checks++;
    }
}
