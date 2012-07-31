package scala.tools.eclipse.sbtconsole.console

import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.part.IPageBookViewPage
import org.eclipse.ui.console.AbstractConsole
import org.eclipse.ui.console.IConsoleView
import org.eclipse.ui.console.IOConsole
import org.eclipse.ui.internal.console.IOConsolePage
import org.eclipse.swt.events.TraverseListener
import org.eclipse.swt.events.TraverseEvent
import scala.tools.eclipse.logging.HasLogger
import org.eclipse.swt.SWT

class ShellConsole(
  name: String, 
  consoleType: String, 
  imageDescriptor: ImageDescriptor, 
  autoLifecycle: Boolean)
    extends IOConsole(name, consoleType, imageDescriptor, autoLifecycle)
    with HasLogger {

  def this(name: String, imageDescriptor: ImageDescriptor, autoLifecycle: Boolean) { this(name, null, imageDescriptor, autoLifecycle) }

  def this(name: String, imageDescriptor: ImageDescriptor) { this(name, imageDescriptor, true) }

  def this(name: String) { this(name, null) }

  override def createPage(view: IConsoleView): IPageBookViewPage = {
    new ShellConsolePage(ShellConsole.this, view)
  }

}
