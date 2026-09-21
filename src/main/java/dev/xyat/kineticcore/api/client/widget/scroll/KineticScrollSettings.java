package dev.xyat.kineticcore.api.client.widget.scroll;

import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;

import java.util.function.DoubleSupplier;

/** Global client-side scrolling preferences shared by Kinetic controls and screens. */
public final class KineticScrollSettings {
    private static final double DEFAULT_WHEEL_ITEMS_PER_NOTCH = 1.0D;
    private static volatile DoubleSupplier wheelItemsProvider = () -> DEFAULT_WHEEL_ITEMS_PER_NOTCH;

    private KineticScrollSettings() {
    }

    /**
     * Returns the configured logical content count moved by one wheel notch.
     *
     * <p>Before the client configuration is registered this method deliberately
     * returns the loader-independent default. This keeps public scroll state usable
     * in headless verification without pulling Forge configuration classes into the
     * widget-state layer.</p>
     */
    public static double wheelItemsPerNotch() {
        double value = wheelItemsProvider.getAsDouble();
        return isValidWheelAmount(value) ? value : DEFAULT_WHEEL_ITEMS_PER_NOTCH;
    }

    /**
     * Returns the client configuration specification backing the global scroll settings.
     * Accessing the specification installs the persisted value provider; normal headless
     * widget-state use never initializes this holder.
     */
    public static KTClientConfigSpec configSpec() {
        ConfigHolder.bind();
        return ConfigHolder.SPEC;
    }

    /** Saves the current scrolling preferences. */
    public static void save() {
        ConfigHolder.SPEC.save();
    }

    private static boolean isValidWheelAmount(Object value) {
        if (!(value instanceof Number number)) return false;
        double amount = number.doubleValue();
        return Double.isFinite(amount) && amount > 0D;
    }

    /** Lazily owns the loader-backed specification so pure scroll state stays loader independent. */
    private static final class ConfigHolder {
        private static final KTClientConfigSpec SPEC;
        private static final KTClientConfigSpec.DoubleValue WHEEL_ITEMS_PER_NOTCH;

        static {
            KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
            builder.comment(
                    "Kinetic 界面滚轮设置。",
                    "Kinetic UI mouse-wheel settings."
            ).translation("cfg.kineticcore.ui.title").push("Ui");

            WHEEL_ITEMS_PER_NOTCH = builder.comment(
                    "鼠标滚轮每滚动一次时移动的内容数量。允许小数并保持平滑滚动。",
                    "Number of content items moved per mouse-wheel notch. Fractional values keep smooth scrolling."
            ).translation("cfg.kineticcore.ui.wheel_items_per_notch")
                    .defineDoubleValidated(
                            "wheelItemsPerNotch",
                            DEFAULT_WHEEL_ITEMS_PER_NOTCH,
                            KineticScrollSettings::isValidWheelAmount
                    );

            builder.pop();
            SPEC = builder.build();
        }

        private ConfigHolder() {
        }

        private static void bind() {
            wheelItemsProvider = WHEEL_ITEMS_PER_NOTCH::get;
        }
    }
}
