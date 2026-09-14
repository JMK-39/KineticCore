package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.internal.client.config.ServerConfigClientRuntime;

import java.util.List;
import java.util.Map;

public final class KTServerConfigClient {
    private KTServerConfigClient() {
    }

    public static boolean isLoaded(String pageId) {
        return ServerConfigClientRuntime.isLoaded(pageId);
    }

    public static boolean canEdit(String pageId) {
        return ServerConfigClientRuntime.canEdit(pageId);
    }

    public static long revision(String pageId) {
        return ServerConfigClientRuntime.revision(pageId);
    }

    public static String loadFailureKey(String pageId) {
        return ServerConfigClientRuntime.loadFailureKey(pageId);
    }

    public static void request(KTConfigPage page) {
        ServerConfigClientRuntime.request(page);
    }

    public static void request(String pageId) {
        ServerConfigClientRuntime.request(pageId);
    }

    public static boolean save(KTConfigPage page, Map<String, Object> changedValues) {
        return ServerConfigClientRuntime.save(page, changedValues);
    }

    public static boolean savePartial(String pageId, Map<String, Object> changedValues) {
        return ServerConfigClientRuntime.savePartial(pageId, changedValues);
    }


    public static boolean getBoolean(String pageId, String entryId, boolean fallback) {
        return ServerConfigClientRuntime.getBoolean(pageId, entryId, fallback);
    }

    public static int getInt(String pageId, String entryId, int fallback) {
        return ServerConfigClientRuntime.getInt(pageId, entryId, fallback);
    }

    public static long getLong(String pageId, String entryId, long fallback) {
        return ServerConfigClientRuntime.getLong(pageId, entryId, fallback);
    }

    public static double getDouble(String pageId, String entryId, double fallback) {
        return ServerConfigClientRuntime.getDouble(pageId, entryId, fallback);
    }

    public static String getString(String pageId, String entryId, String fallback) {
        return ServerConfigClientRuntime.getString(pageId, entryId, fallback);
    }

    public static List<String> getStringList(String pageId, String entryId, List<String> fallback) {
        return ServerConfigClientRuntime.getStringList(pageId, entryId, fallback);
    }

    public static List<Integer> getIntegerList(String pageId, String entryId, List<Integer> fallback) {
        return ServerConfigClientRuntime.getIntegerList(pageId, entryId, fallback);
    }

}
