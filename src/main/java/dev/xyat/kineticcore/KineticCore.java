package dev.xyat.kineticcore;

import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.bootstrap.KineticCoreBootstrap;
import dev.xyat.kineticcore.bootstrap.client.KineticCoreClientBootstrap;
import net.minecraftforge.fml.common.Mod;

@Mod(KineticCore.MODID)
public class KineticCore {
    public static final String MODID = "kineticcore";

    public KineticCore() {
        KineticPlatform.runOnClient(() -> KineticCoreClientBootstrap::registerInfrastructure);

        KineticCoreBootstrap.register();

        KineticPlatform.runOnClient(() -> KineticCoreClientBootstrap::registerModules);
    }
}
