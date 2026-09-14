package dev.xyat.kineticcore.internal.config;

import dev.xyat.kineticcore.internal.client.config.ServerConfigClientRuntime;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class ServerConfigClientDispatch {
    private ServerConfigClientDispatch() {
    }

    public static void handleSync(
            String pageId,
            boolean editable,
            boolean saveResponse,
            boolean success,
            String messageKey,
            byte[] payload
    ) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ServerConfigClientRuntime.handleSync(
                        pageId,
                        editable,
                        saveResponse,
                        success,
                        messageKey,
                        payload
                )
        );
    }
}
