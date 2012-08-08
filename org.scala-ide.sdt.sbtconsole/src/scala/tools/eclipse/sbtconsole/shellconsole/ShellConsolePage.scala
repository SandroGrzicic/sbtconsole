package scala.tools.eclipse.sbtconsole.shellconsole

import scala.tools.eclipse.logging.HasLogger
import org.eclipse.jface.action.IToolBarManager
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.console.IConsoleConstants
import org.eclipse.ui.console.IConsoleView
import org.eclipse.ui.console.IOConsole
import org.eclipse.ui.internal.console.IOConsolePage

/**
 * ShellConsole Page.
 * 
 * Based on IOConsolePage, adds a ShellConsoleKeyListener, a terminate button, 
 * and executes onTerminate on termination. 
 */
class ShellConsolePage(console: IOConsole, view: IConsoleView, onTermination: () => Unit)
    extends IOConsolePage(console, view)
    with HasLogger {

  private var terminateAction: TerminateAction = _
    
  private var listener: ShellConsoleKeyListener = _
  
  def getListener = listener
  
  override def createControl(parent: Composite) {
    super.createControl(parent)
    val control = getControl
    listener = new ShellConsoleKeyListener(console, ShellConsolePage.this)
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
    if (getControl != null) {
      getControl.removeTraverseListener(listener)
      getControl.removeKeyListener(listener)
    }
    
    super.dispose()
    onTermination()
  }

}