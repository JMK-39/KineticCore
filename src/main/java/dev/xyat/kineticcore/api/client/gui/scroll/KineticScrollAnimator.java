package dev.xyat.kineticcore.api.client.gui.scroll;

import dev.xyat.kineticcore.internal.client.gui.widget.scroll.KineticScroll;

/**
 * 与 Kinetic 滚动手感一致的平滑数值动画（像素级滚动面板等）。
 * Smooth value animation matching Kinetic scrolling (e.g. pixel-scrolled panels).
 */
public final class KineticScrollAnimator {
    private final KineticScroll.State state = new KineticScroll.State();

    /** 创建动画器 / Creates an animator. */
    public KineticScrollAnimator() {
    }

    /** 设置目标与上限并推进动画，immediate 为真时直接到位 / Sets target and maximum, advancing the animation. */
    public double update(double target, double max, boolean immediate) {
        return state.update(target, max, immediate);
    }

    /** 跟随逻辑目标 / Follows a logical target. */
    public double follow(double logicalTarget, double max) {
        return state.follow(logicalTarget, max);
    }

    /** 按滚轮增量推进目标并返回新目标 / Applies a wheel delta and returns the new target. */
    public double wheel(double logicalTarget, double delta, double step, double max) {
        return state.wheel(logicalTarget, delta, step, max);
    }

    /** 立即跳到值 / Snaps to a value. */
    public void snap(double value, double max) {
        state.snap(value, max);
    }

    /** 当前动画值 / Current animated value. */
    public double current() {
        return state.current();
    }

    /** 目标值 / Target value. */
    public double target() {
        return state.target();
    }
}
