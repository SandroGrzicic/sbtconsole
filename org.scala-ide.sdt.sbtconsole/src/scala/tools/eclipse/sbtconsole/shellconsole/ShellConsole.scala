package scala.tools.eclipse.sbtconsole.shellconsole

import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.part.IPageBookViewPage
import org.eclipse.ui.console.IConsoleView
import org.eclipse.ui.console.IOConsole
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.util.SWTUtils

/** 
 * An advanced console based on IOConsole with history support for tab completion. 
 */
class ShellConsole(
  name: String, 
  consoleType: String, 
  imageDescriptor: ImageDescriptor, 
  autoLifecycle: Boolean,
  onTermination: () => Unit)
    extends IOConsole(name, consoleType, imageDescriptor, autoLifecycle)
    with HasLogger {
  
  protected var page: ShellConsolePage = _
  
  def getPage = page
  
  def this(name: String, imageDescriptor: ImageDescriptor, autoLifecycle: Boolean, onTermination: () => Unit) { 
    this(name, null, imageDescriptor, autoLifecycle, onTermination) 
  }

  def this(name: String, imageDescriptor: ImageDescriptor, onTermination: () => Unit) { 
    this(name, imageDescriptor, true, onTermination) 
  }

  def this(name: String, onTermination: () => Unit) { this(name, null, onTermination) }

  override def createPage(view: IConsoleView): IPageBookViewPage = {
    page = new ShellConsolePage(ShellConsole.this, view, onTermination)
    page
  }
  
  override protected def dispose() {
  }

  /** Notifies this console that the specified text has been appended to it. */
  protected[shellconsole] def textAppended(text: String) {
//    eclipseLog.info("Appended: " + text)
    SWTUtils.asyncExec {
      page.getListener.moveCaretToEnd()
    }
  }
}
