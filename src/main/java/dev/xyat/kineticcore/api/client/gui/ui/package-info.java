/**
 * Control factory and builders. A page receives a {@link KineticUi} in {@code build}; every factory method returns
 * a builder, and {@code build()} creates the control and registers it with the page. Builders set text, state and
 * callbacks; sizes, borders, hover and disabled styles are decided by the API. Numeric builders expose explicit
 * range, sign and validator options and never add limits on their own.
 */
package dev.xyat.kineticcore.api.client.gui.ui;
