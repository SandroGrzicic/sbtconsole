package scala.tools.eclipse.shellconsole

import java.io.InputStream
import org.eclipse.ui.console.IOConsoleOutputStream
import scala.annotation.tailrec
import java.io.IOException
import scala.tools.eclipse.logging.HasLogger

/**
 * Transfers data from the input to the output;
 * holds data in a temporary buffer if a flag is toggled.
 * 
 * The data can be read by switching the `writeTarget` to `Buffer` and then
 * reading from the `contentBuffer` which should be cleared after reading.
 * After that, normal behavior can be resumed by setting the `writeTarget`
 * to `Output`.
 * 
 * The currently buffered output can be copied to the output stream by using
 * `copyToOutput`; the copied length can be limited using `contentBufferLimit`.
 * 
 *  The transfer terminates when the input stream has no more input, or the 
 *  output stream is not accepting any more input (throws an IOException).
 *  
 */
class BufferedTransferThread(
    in: InputStream, 
    out: IOConsoleOutputStream, 
    charset: String = "UTF-8"
) extends Thread with HasLogger {
  import scala.sys.process.BasicIO
  import BufferedTransferThread._

  /** Buffer holding the current content. Thread-safe. */
  val contentBuffer = new StringBuffer()

  /**
   * Whether to write process output to the output stream
   * or to the `contentBuffer`.
   */
  @volatile var writeTarget: Target = Output

  /** 
   * Copies the currently buffered input characters to the output stream, 
   * optionally up to `contentBufferLimit`.
   * Clears the buffer when it's done unless otherwise specified.
   */
  def copyToOutput(contentBufferLimit: Int = contentBuffer.length, clearBuffer: Boolean = true) {
    out.write(contentBuffer.toString.substring(0, contentBufferLimit))
    try { out.flush() } catch { case _: IOException => }
    
    if (clearBuffer) {
      contentBuffer.setLength(0)      
    }
  }

  override def run() {
    try {
      // an IOException will be thrown once the process has terminated
      transferLoop()
    } catch {
      case e: IOException =>
    }
  }

  /** Starts the transfer loop. */
  private def transferLoop() {
    val streamBuffer = new Array[Byte](BUFFER_SIZE)
    
    @tailrec def loop() {
      val byteCount = in.read(streamBuffer)
      if (byteCount > 0) {
        if (writeTarget == Output) {
          out.write(streamBuffer, 0, byteCount)
          out.flush()
        } else {
          contentBuffer.append(new String(streamBuffer, 0, byteCount, charset))
        }
        loop()
      }
    }
    loop()
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