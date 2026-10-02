package dev.xyat.kineticcore.internal.config;

import dev.xyat.kineticcore.internal.client.config.ServerConfigClientRuntime;
import dev.xyat.kineticcore.internal.runtime.KineticEnvironmentRuntime;

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
        KineticEnvironmentRuntime.runOnClient(() -> () ->
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
