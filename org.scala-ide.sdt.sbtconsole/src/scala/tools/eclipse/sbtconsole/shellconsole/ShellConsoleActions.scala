package scala.tools.eclipse.sbtconsole.shellconsole

import org.eclipse.jface.action.Action
import org.eclipse.ui.internal.console.ConsoleMessages
import org.eclipse.ui.internal.console.ConsolePluginImages
import org.eclipse.ui.internal.console.IInternalConsoleConstants
import org.eclipse.ui.internal.console.IOConsolePage

/**
 * Adds a terminate button which disposes the console parent of the specified page.
 */
class TerminateAction(page: IOConsolePage) extends Action("Terminate") {

  setToolTipText(ConsoleMessages.CloseConsoleAction_0)
  setHoverImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_ELCL_CLOSE));
  setDisabledImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_DLCL_CLOSE));
  setImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_ELCL_CLOSE));

  setEnabled(true)

  override def run() {
    page.dispose()
  }

}