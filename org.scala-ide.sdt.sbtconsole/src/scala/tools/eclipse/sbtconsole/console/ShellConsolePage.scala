package scala.tools.eclipse.sbtconsole.console

import scala.tools.eclipse.logging.HasLogger

import org.eclipse.jface.action.IToolBarManager
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.console.IConsoleConstants
import org.eclipse.ui.console.IConsoleView
import org.eclipse.ui.console.IOConsole
import org.eclipse.ui.internal.console.IOConsolePage

/**
 * An IOConsolePage with 
 */
class ShellConsolePage(console: IOConsole, view: IConsoleView, onTermination: () => Unit)
    extends IOConsolePage(console, view)
    with HasLogger {

  private var terminateAction: TerminateAction = _
    
  override def createControl(parent: Composite) {
    super.createControl(parent)
    val control = getControl
    val listener = new ShellConsoleKeyListener(console, ShellConsolePage.this)
    control.addTraverseListener(listener)
    control.addKeyListener(listener)
  }

  override protected def createActions() {
    super.createActions()
    terminateAction = new TerminateAction(this)
    
  }
  override protected def configureToolBar(mgr: IToolBarManager) {
    mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, terminateAction)
    super.configureToolBar(mgr)
  }
  
  override def dispose() {
    if (terminateAction != null) {
      terminateAction = null
    }
    super.dispose()
    onTermination()
  }

}