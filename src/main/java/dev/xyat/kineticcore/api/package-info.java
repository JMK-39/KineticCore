/**
 * Public KineticCore API for add-ons. Everything add-ons may depend on lives below this package; KineticCore's
 * {@code internal} and {@code feature} packages are implementation details and may change without notice.
 *
 * <h2>Conventions</h2>
 * <ul>
 * <li><b>Null values.</b> Parameters and return values are non-null unless the method documentation says a value
 * may be {@code null} or is optional. Values that may be absent are documented as nullable or returned as
 * {@link java.util.Optional}.</li>
 * <li><b>Threads and sides.</b> Packages named {@code client} are client only and must run on the client (render)
 * thread. Server-side methods run on the server thread. Each class documents any exception to this rule.</li>
 * <li><b>Callbacks.</b> Event handlers and hooks run in registration order within a priority. A handler that throws
 * does not stop the other handlers; the failure is reported after dispatch. Registration methods return a handle
 * whose {@code close()} unregisters the callback.</li>
 * <li><b>Player-visible text.</b> Text is built from language keys through {@code KineticI18n}; colors come from
 * the language files, never from Java code.</li>
 * <li><b>GUI.</b> Add-ons build interfaces from {@code KineticPage} and the {@code KineticUi} control factory,
 * which own sizing, 4K scaling, clipping, focus, tooltips and theme. Add-ons do not create vanilla widgets
 * directly.</li>
 * </ul>
 */
package dev.xyat.kineticcore.api;
