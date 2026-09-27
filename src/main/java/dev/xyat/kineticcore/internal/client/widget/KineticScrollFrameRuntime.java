package dev.xyat.kineticcore.internal.client.widget;

import dev.xyat.kineticcore.internal.client.input.KineticMouseButtonRuntime;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 每个 Kinetic Screen 持有一个 Frame，收集本帧渲染过、且已绑定选中项的滚动条，
 * 由 Screen 统一派发“悬停提示”和“中键跳转”，业务代码无需手写这两段逻辑。
 * Per-screen frame collector for bound scrollbars; the owning screen dispatches the
 * hover hint and middle-click jump so addons never hand-wire them.
 */
public final class KineticScrollFrameRuntime {
    /** 由 GridScrollController 私有实现 / Implemented privately by GridScrollController. */
    public interface Participant {
        /** 当前应显示的提示，未到 0.5 秒或不可用时返回 null / Hint to show now, or null. */
        Component pendingHint();

        /** 若鼠标在上一帧悬停于滑块，则执行跳转并返回 true / Jumps when the thumb was hovered last frame. */
        boolean handleMiddleClick();
    }

    /** 单个 Screen 的帧收集器 / One screen's frame collector. */
    public static final class Frame {
        private final List<Participant> building = new ArrayList<>();
        private List<Participant> rendered = List.of();

        public void begin() {
            building.clear();
        }

        public void end() {
            rendered = List.copyOf(building);
            building.clear();
        }

        /** 本帧后渲染者优先（更靠上层）/ Later-rendered participants win (top-most). */
        public Component hint() {
            for (int index = building.size() - 1; index >= 0; index--) {
                Component hint = building.get(index).pendingHint();
                if (hint != null) return hint;
            }
            return null;
        }

        /** 使用上一完整帧的悬停状态派发中键 / Dispatches using the last completed frame. */
        public boolean middleClick(int button) {
            if (!KineticMouseButtonRuntime.isMiddle(button)) return false;
            for (int index = rendered.size() - 1; index >= 0; index--) {
                if (rendered.get(index).handleMiddleClick()) return true;
            }
            return false;
        }

        private void add(Participant participant) {
            if (!building.contains(participant)) building.add(participant);
        }
    }

    private static final Deque<Frame> ACTIVE = new ArrayDeque<>();

    private KineticScrollFrameRuntime() {
    }

    /** Screen.render 开始时调用 / Called when a screen render starts. */
    public static void enter(Frame frame) {
        frame.begin();
        ACTIVE.push(frame);
    }

    /** Screen.render 结束时（finally）调用 / Called from the render finally block. */
    public static void exit(Frame frame) {
        if (!ACTIVE.isEmpty() && ACTIVE.peek() == frame) {
            ACTIVE.pop();
        } else {
            ACTIVE.remove(frame);
        }
        frame.end();
    }

    /** 滚动条渲染时登记；不在 Kinetic Screen 渲染期间则忽略 / No-op outside a Kinetic screen render. */
    public static void register(Participant participant) {
        Frame frame = ACTIVE.peek();
        if (frame != null && participant != null) frame.add(participant);
    }
}
