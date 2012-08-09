package scala.tools.eclipse.shellconsole

import org.eclipse.jface.action.Action
import org.eclipse.ui.internal.console.ConsoleMessages
import org.eclipse.ui.internal.console.ConsolePluginImages
import org.eclipse.ui.internal.console.IInternalConsoleConstants
import org.eclipse.ui.internal.console.IOConsolePage
import scala.tools.eclipse.ScalaImages

/**
 * Adds a terminate button. By default, disposes the console.
 */
class TerminateAction(console: ShellConsole) extends Action("Terminate") {
  setToolTipText(ConsoleMessages.CloseConsoleAction_0)
  
  private val icon = ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_ELCL_CLOSE)
  setHoverImageDescriptor(icon)
  setDisabledImageDescriptor(icon)
  setImageDescriptor(icon)

  setEnabled(true)

  override def run() {
    console.onTerminate()
  }

}

/** 
 * Adds a Restart button. By default, does nothing. 
 */
class RestartAction(console: ShellConsole) extends Action("Restart") {
  setToolTipText("Restart Console")

  private val icon = ScalaImages.REFRESH_REPL_TOOLBAR
  setHoverImageDescriptor(icon)
  setDisabledImageDescriptor(icon)
  setImageDescriptor(icon)

  setEnabled(true)

  override def run() {
    console.onRestart()
  }
  
}