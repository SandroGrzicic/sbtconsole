package scala.tools.eclipse.shellconsole

import java.io.InputStream
import org.eclipse.ui.console.IOConsoleOutputStream
import scala.annotation.tailrec
import java.io.IOException

/**
 * Transfers data from the input to the output;
 * holds data in a temporary buffer if a flag is toggled.
 */
class BufferedTransferThread(in: InputStream, out: IOConsoleOutputStream, charset: String) extends Thread {
  import scala.sys.process.BasicIO
  import BufferedTransferThread._

  /** Buffer holding the current content. Thread-safe. */
  val contentBuffer = new StringBuffer()
  /** How much of the content buffer to use when copying it to the output stream. */
  var contentBufferLimit = -1

  /**
   * Whether to write process output to the output stream
   * or to the `contentBuffer`.
   */
  @volatile var writeTarget: Target = Output

  /** Copy the currently buffered input characters to the output stream up to `contentBufferLimit`. */
  def copyToOutput() {
    val limit = if (contentBufferLimit == -1) contentBuffer.length else contentBufferLimit
    out.write(contentBuffer.toString.substring(0, limit))
    contentBufferLimit = -1
    try { out.flush() } catch { case _: IOException => }
  }

  override def run() {
    val streamBuffer = new Array[Byte](BUFFER_SIZE)
    var byteCount = 0

    @tailrec def loop() {
      byteCount = in.read(streamBuffer)
      if (byteCount > 0) {
        if (writeTarget == Output) {
          out.write(streamBuffer, 0, byteCount)
          // flush() will throw an exception once the process has terminated
          val available = try { out.flush(); true } catch { case _: IOException => false }
          if (available) loop()
        } else {
          contentBuffer.append(new String(streamBuffer, 0, byteCount, charset))
          loop()
        }
      }
    }
    loop()
    out.close()
  }
}

object BufferedTransferThread {
  val BUFFER_SIZE = 8192

  /** Transfer target. */
  sealed trait Target
  
  /** Target is the output stream. */
  case object Output extends Target
  /** Target is the temporary content buffer. */
  case object Buffer extends Target
  
}