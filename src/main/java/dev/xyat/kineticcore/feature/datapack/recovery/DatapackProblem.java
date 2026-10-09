package dev.xyat.kineticcore.feature.datapack.recovery;

/** A real loader failure, without guessing its owner from a referenced namespace. */
public record DatapackProblem(String packId, String resourceId, String reason, DatapackFiles.Location location) {
    public DatapackProblem withLocation(DatapackFiles.Location value) {
        return new DatapackProblem(packId, resourceId, reason, value);
    }
}
