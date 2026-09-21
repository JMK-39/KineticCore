package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.internal.client.config.ServerConfigClientRuntime;

import java.util.List;
import java.util.Map;

/** Public API type for kt server config client. */
public final class KTServerConfigClient {
    private KTServerConfigClient() {
    }

    /**
     * Returns whether loaded.
     */
    public static boolean isLoaded(String pageId) {
        return ServerConfigClientRuntime.isLoaded(pageId);
    }

    /**
     * Returns whether edit.
     */
    public static boolean canEdit(String pageId) {
        return ServerConfigClientRuntime.canEdit(pageId);
    }

    /**
     * Returns the revision.
     */
    public static long revision(String pageId) {
        return ServerConfigClientRuntime.revision(pageId);
    }

    /**
     * Loads failure key.
     */
    public static String loadFailureKey(String pageId) {
        return ServerConfigClientRuntime.loadFailureKey(pageId);
    }

    /**
     * Performs the request API operation.
     */
    public static void request(KTConfigPage page) {
        ServerConfigClientRuntime.request(page);
    }

    /**
     * Requests page id.
     */
    public static void requestPageId(String pageId) {
        ServerConfigClientRuntime.request(pageId);
    }

    /**
     * Saves the current values.
     */
    public static boolean save(KTConfigPage page, Map<String, Object> changedValues) {
        return ServerConfigClientRuntime.save(page, changedValues);
    }

    /**
     * Performs the save partial API operation.
     */
    public static boolean savePartial(String pageId, Map<String, Object> changedValues) {
        return ServerConfigClientRuntime.savePartial(pageId, changedValues);
    }


    /** Applies the latest cached authoritative values to the supplied config page. */
    public static void applyCached(KTConfigPage page) {
        ServerConfigClientRuntime.applyCached(page);
    }

    /**
     * Returns boolean.
     */
    public static boolean getBoolean(String pageId, String entryId, boolean fallback) {
        return ServerConfigClientRuntime.getBoolean(pageId, entryId, fallback);
    }

    /**
     * Returns int.
     */
    public static int getInt(String pageId, String entryId, int fallback) {
        return ServerConfigClientRuntime.getInt(pageId, entryId, fallback);
    }

    /**
     * Returns long.
     */
    public static long getLong(String pageId, String entryId, long fallback) {
        return ServerConfigClientRuntime.getLong(pageId, entryId, fallback);
    }

    /**
     * Returns double.
     */
    public static double getDouble(String pageId, String entryId, double fallback) {
        return ServerConfigClientRuntime.getDouble(pageId, entryId, fallback);
    }

    /**
     * Returns string.
     */
    public static String getString(String pageId, String entryId, String fallback) {
        return ServerConfigClientRuntime.getString(pageId, entryId, fallback);
    }

    /**
     * Returns string list.
     */
    public static List<String> getStringList(String pageId, String entryId, List<String> fallback) {
        return ServerConfigClientRuntime.getStringList(pageId, entryId, fallback);
    }

    /**
     * Returns integer list.
     */
    public static List<Integer> getIntegerList(String pageId, String entryId, List<Integer> fallback) {
        return ServerConfigClientRuntime.getIntegerList(pageId, entryId, fallback);
    }

}
