package scala.tools.eclipse.shellconsole

import org.eclipse.ui.internal.console.IOConsolePartitioner
import org.eclipse.ui.console.IOConsoleInputStream
import org.eclipse.ui.console.IOConsoleOutputStream

/** 
 * ShellConsole Partitioner.
 * 
 * Based on IOConsolePartitioner, overrides streamAppended() in order to notify the console
 * about an OutputStream change.  
 * 
 */
class ShellConsolePartitioner(inputStream: IOConsoleInputStream, console: ShellConsole) 
    extends IOConsolePartitioner(inputStream, console) {

  def this(inputStream: IOConsoleInputStream) { this(inputStream, null) }
  
  def this(console: ShellConsole) { this(console.getInputStream(), console) }
  
  override def streamAppended(stream: IOConsoleOutputStream, string: String) {
    super.streamAppended(stream, string)
    console.textAppended(string)
  }

}