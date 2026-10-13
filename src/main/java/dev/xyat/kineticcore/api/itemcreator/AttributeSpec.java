package dev.xyat.kineticcore.api.itemcreator;

import java.util.Objects;

/**
 * One registered attribute edit. MULTIPLY_TOTAL stores the additive multiplier (1 means twice).
 * @param id exact registry ID
 * @param amount finite amount; SET changes only the base value
 * @param operation edit operation
 */
public record AttributeSpec(String id, double amount, Operation operation) {
    /** The supported spawn-time attribute operations. */
    public enum Operation { ADDITION, MULTIPLY_TOTAL, SET }
    public AttributeSpec {
        ItemCreatorValidation.validateId(id);
        Objects.requireNonNull(operation, "operation");
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Non-finite attribute amount");
        if (operation == Operation.MULTIPLY_TOTAL && amount <= -1) throw new IllegalArgumentException("Multiplier must be positive");
    }
}
