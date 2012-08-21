package scala.tools.eclipse.shellconsole

import scala.tools.eclipse.logging.HasLogger
import org.eclipse.jface.action.IToolBarManager
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.console.IConsoleConstants
import org.eclipse.ui.console.IConsoleView
import org.eclipse.ui.console.IOConsole
import org.eclipse.ui.internal.console.IOConsolePage
import org.eclipse.jface.action.Action

/**
 * ShellConsole Page.
 * 
 * Based on IOConsolePage, adds a ShellConsoleKeyListener, a terminate button, 
 * and executes onTerminate on termination. 
 */
class ShellConsolePage(console: ShellConsole, view: IConsoleView)
    extends IOConsolePage(console, view)
    with HasLogger {

  private var actions = List.empty[Action]
    
  private var listener: ShellConsoleKeyListener = _
  
  def getListener: ShellConsoleKeyListener = listener
  
  override def createControl(parent: Composite) {
    super.createControl(parent)
    val control = getControl
    listener = new ShellConsoleKeyListener(console, ShellConsolePage.this)
    addListenerToControl()
  }

  override protected def createActions() {
    super.createActions()
    actions :+= new TerminateAction(console)
    actions :+= new RestartAction(console)
  }
  
  override protected def configureToolBar(mgr: IToolBarManager) {
    actions foreach { action =>
      mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, action)      
    }
    super.configureToolBar(mgr)
  }
  
  /** Whether this Page is editable. */
  def setEditable(editable: Boolean) {
    getViewer.setEditable(editable)
    editable match {
      case false => removeListenerFromControl()
      case true  => addListenerToControl()
    }
  }
  
  private def addListenerToControl() {
    getControl.addTraverseListener(listener)
    getControl.addKeyListener(listener)
  }
  private def removeListenerFromControl() {
    getControl.removeTraverseListener(listener)
    getControl.removeKeyListener(listener)
  }
  
  override def dispose() {
    actions = null

    if (getControl != null) {
      removeListenerFromControl()
    }
    
    super.dispose()
  }

}