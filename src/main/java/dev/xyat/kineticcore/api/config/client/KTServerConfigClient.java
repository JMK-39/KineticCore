package dev.xyat.kineticcore.api.config.client;

import dev.xyat.kineticcore.internal.client.config.ServerConfigClientRuntime;

import java.util.List;
import java.util.Map;

/**
 * Client-side access to server-authoritative config pages: requesting values from the server, sending edits back
 * and reading the last values the server sent.
 *
 * <p>Values are cached per page id after the server answers a {@link #request(KTConfigPage) request}. The getters
 * never block; they return the fallback until values have arrived. Call these methods on the client thread.
 */
public final class KTServerConfigClient {
    private KTServerConfigClient() {
    }

    /** Returns whether the server has sent values for the page since the last request. */
    public static boolean isLoaded(String pageId) {
        return ServerConfigClientRuntime.isLoaded(pageId);
    }

    /** Returns whether the server reported that the local player may edit the page (operator level 2 or higher). */
    public static boolean canEdit(String pageId) {
        return ServerConfigClientRuntime.canEdit(pageId);
    }

    /**
     * Returns a counter that changes whenever the cached state of the page changes (loading, loaded or failed).
     * Screens compare it to decide when to refresh.
     *
     * @return the latest revision, or {@code 0} when the page was never requested
     */
    public static long revision(String pageId) {
        return ServerConfigClientRuntime.revision(pageId);
    }

    /**
     * Returns the language key describing why the last load failed.
     *
     * @return the failure key, or an empty string when there is no failure
     */
    public static String loadFailureKey(String pageId) {
        return ServerConfigClientRuntime.loadFailureKey(pageId);
    }

    /**
     * Asks the server for the page's current values. The answer arrives asynchronously and updates
     * {@link #isLoaded(String)}, {@link #canEdit(String)} and {@link #revision(String)}.
     *
     * <p>Does nothing for pages that are not server managed or while not connected to a world.
     *
     * @param page page to request
     * @throws NullPointerException if {@code page} is {@code null}
     */
    public static void request(KTConfigPage page) {
        ServerConfigClientRuntime.request(page);
    }

    /**
     * Same as {@link #request(KTConfigPage)} for a page registered with {@code KTConfigApi}; does nothing for
     * unknown ids.
     */
    public static void requestPageId(String pageId) {
        ServerConfigClientRuntime.request(pageId);
    }

    /**
     * Sends changed values to the server. The server validates and applies them as one transaction.
     *
     * @param page server-managed page being saved
     * @param changedValues changed values keyed by entry id
     * @return {@code true} if the request was sent; {@code false} if the player may not edit the page, the client
     *   is not connected, or sending failed (a toast is shown in that case)
     * @throws NullPointerException if {@code page} is {@code null}
     */
    public static boolean save(KTConfigPage page, Map<String, Object> changedValues) {
        return ServerConfigClientRuntime.save(page, changedValues);
    }

    /**
     * Same as {@link #save(KTConfigPage, Map)} for a page registered with {@code KTConfigApi}.
     *
     * @return {@code false} when the page id is unknown or the request was not sent
     */
    public static boolean savePartial(String pageId, Map<String, Object> changedValues) {
        return ServerConfigClientRuntime.savePartial(pageId, changedValues);
    }


    /** Applies the latest cached authoritative values to the supplied config page. */
    public static void applyCached(KTConfigPage page) {
        ServerConfigClientRuntime.applyCached(page);
    }

    /**
     * Returns the cached server boolean, or {@code fallback} until the page is loaded or when the entry is missing
     * or not a boolean.
     */
    public static boolean getBoolean(String pageId, String entryId, boolean fallback) {
        return ServerConfigClientRuntime.getBoolean(pageId, entryId, fallback);
    }

    /**
     * Returns the cached server integer, or {@code fallback} when it is missing, fractional or outside the
     * {@code int} range.
     */
    public static int getInt(String pageId, String entryId, int fallback) {
        return ServerConfigClientRuntime.getInt(pageId, entryId, fallback);
    }

    /**
     * Returns the cached server long, or {@code fallback} when it is missing, fractional or outside the
     * {@code long} range.
     */
    public static long getLong(String pageId, String entryId, long fallback) {
        return ServerConfigClientRuntime.getLong(pageId, entryId, fallback);
    }

    /** Returns the cached server decimal, or {@code fallback} when it is missing or not finite. */
    public static double getDouble(String pageId, String entryId, double fallback) {
        return ServerConfigClientRuntime.getDouble(pageId, entryId, fallback);
    }

    /** Returns the cached server string, or {@code fallback} when it is missing or not a string. */
    public static String getString(String pageId, String entryId, String fallback) {
        return ServerConfigClientRuntime.getString(pageId, entryId, fallback);
    }

    /**
     * Returns an unmodifiable copy of the cached server string list, or of {@code fallback} when it is missing or
     * malformed.
     */
    public static List<String> getStringList(String pageId, String entryId, List<String> fallback) {
        return ServerConfigClientRuntime.getStringList(pageId, entryId, fallback);
    }

    /**
     * Returns an unmodifiable copy of the cached server integer list, or of {@code fallback} when it is missing or
     * malformed.
     */
    public static List<Integer> getIntegerList(String pageId, String entryId, List<Integer> fallback) {
        return ServerConfigClientRuntime.getIntegerList(pageId, entryId, fallback);
    }

}
