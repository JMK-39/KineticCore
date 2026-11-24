package dev.xyat.kineticcore.internal.registry;

/** Native cache and tracking invalidation provided by the core's attribute-instance invoker. */
public interface RuntimeAttributeInstanceAccess {
    void kineticcore$invalidate();
}
