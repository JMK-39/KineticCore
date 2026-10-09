package dev.xyat.kineticcoredatapackvalidation;

@net.minecraftforge.fml.common.Mod("kineticcore_datapack_validation")
public final class DatapackRecoveryValidationEntry {
    public DatapackRecoveryValidationEntry() {
        if (Boolean.getBoolean("kineticcore.datapackValidation")) {
            dev.xyat.kineticcore.api.runtime.KineticPlatform.runOnClient(() -> DatapackRecoveryValidation::install);
        }
    }
}
