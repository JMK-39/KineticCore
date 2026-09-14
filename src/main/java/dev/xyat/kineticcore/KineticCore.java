package dev.xyat.kineticcore;

import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.bootstrap.KineticCoreBootstrap;
import dev.xyat.kineticcore.bootstrap.client.KineticCoreClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(KineticCore.MODID)
public class KineticCore {
    public static final String MODID = KineticRuntime.MOD_ID;
    public static final org.slf4j.Logger LOGGER = KineticRuntime.logger();

    public KineticCore() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> KineticCoreClientBootstrap::registerInfrastructure);

        KineticCoreBootstrap.register();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> KineticCoreClientBootstrap::registerModules);
    }
}
