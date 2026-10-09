package dev.xyat.kineticcoreflightvalidation;

@net.minecraftforge.fml.common.Mod("kineticcore_flight_validation")
public final class FlightTutorialValidationEntry {
    public FlightTutorialValidationEntry() {
        if (Boolean.getBoolean("kineticcore.flightValidation")) {
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> FlightTutorialValidation::install);
        }
    }
}
