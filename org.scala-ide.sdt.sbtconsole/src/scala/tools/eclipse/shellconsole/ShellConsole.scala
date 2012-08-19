package scala.tools.eclipse.shellconsole

import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.part.IPageBookViewPage
import org.eclipse.ui.console.IConsoleView
import org.eclipse.ui.console.IOConsole
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.util.SWTUtils
import org.eclipse.swt.widgets.Display
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.InputStream
import java.io.IOException
import scala.annotation.tailrec
import org.eclipse.ui.console.IOConsoleOutputStream

/**
 * An advanced console based on IOConsole with history support for tab completion.
 */
class ShellConsole(
  name: String,
  consoleType: String,
  imageDescriptor: ImageDescriptor,
  autoLifecycle: Boolean) extends IOConsole(name, consoleType, imageDescriptor, autoLifecycle)
    with HasLogger {

  protected var charset = ShellConsole.DEFAULT_CHARSET
  protected var page: ShellConsolePage = _

  def getPage = page

  def this(name: String, imageDescriptor: ImageDescriptor, autoLifecycle: Boolean) {
    this(name, null, imageDescriptor, autoLifecycle)
  }

  def this(name: String, imageDescriptor: ImageDescriptor) {
    this(name, imageDescriptor, true)
  }

  def this(name: String) { this(name, null) }

  override def createPage(view: IConsoleView): IPageBookViewPage = {
    page = new ShellConsolePage(ShellConsole.this, view)
    page
  }

  /** Called when the Terminate action is executed. */
  def onTerminate() {
    dispose()
  }

  /** Called when the Restart action is executed. */
  def onRestart() {}

  /** Notifies this console that the specified text has been appended to it. */
  protected[shellconsole] def textAppended(text: String) {
    if (page != null) {
      page.getListener.moveCaretToEndAsync()
    }
  }

  // Console -> Process
  /** InputStream to connect to the process to which writing is possible using `processWriter`. */
  val processInput = new PipedInputStream()
  /** Writer for writing to the process. */
  val processWriter = new BufferedWriter(new OutputStreamWriter(new PipedOutputStream(processInput), charset))

  // Process -> Console
  /** OutputStream to connect to the process from which reading is possible. */
  val processOutput = new PipedOutputStream()
  /** InputStream for reading from the process. */
  protected[shellconsole] val shellInput = new PipedInputStream(processOutput)

  /**
   * Creates, starts and returns a new thread which transfers data
   * from the process to this console.
   */
  protected[shellconsole] def newProcessToConsoleTransferThread() = {
    val thread = new BufferedTransferThread(shellInput, newOutputStream(), charset)
    thread.start()
    thread
  }
  
}

object ShellConsole {
  val DEFAULT_CHARSET = "UTF-8"
}
