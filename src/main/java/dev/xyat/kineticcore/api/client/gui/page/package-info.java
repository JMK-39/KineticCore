/**
 * Page base classes. {@link KineticPage} is the base of every Kinetic interface: subclasses create controls in
 * {@code build(KineticUi)} and draw in the render hooks, while KineticCore hosts the page in an internal screen and
 * handles scaling, focus, tooltips, drafts and back navigation. {@link KineticContainerPage} adds vanilla container
 * slots, and {@link PageLayout} describes the page coordinate system.
 */
package dev.xyat.kineticcore.api.client.gui.page;
