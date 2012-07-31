package scala.tools.eclipse.sbtconsole.console

import org.eclipse.ui.internal.console.IOConsolePage
import org.eclipse.ui.console.IConsoleView
import org.eclipse.ui.console.TextConsole
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.events.TraverseListener
import org.eclipse.swt.events.TraverseEvent
import scala.tools.eclipse.logging.HasLogger
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyListener
import org.eclipse.ui.console.IOConsole
import org.eclipse.swt.events.KeyEvent
import org.eclipse.jface.text.ITextInputListener
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextListener
import org.eclipse.jface.text.TextEvent

/**
 * An IOConsolePage with support for a custom TraverseListener.
 */
class ShellConsolePage(console: IOConsole, view: IConsoleView)
    extends IOConsolePage(console, view)
    with HasLogger {

  override def createControl(parent: Composite) {
    super.createControl(parent)
    val control = getControl
    val listener = new SeriousKeyListener(console, ShellConsolePage.this)
    control.addTraverseListener(listener)
    control.addKeyListener(listener)
  }
  
}