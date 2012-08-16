package scala.tools.eclipse.shellconsole

import java.io.InputStream
import org.eclipse.ui.console.IOConsoleOutputStream
import scala.annotation.tailrec
import java.io.IOException

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
 *  
 */
class BufferedTransferThread(in: InputStream, out: IOConsoleOutputStream, charset: String = "UTF-8") extends Thread {
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
   * Copy the currently buffered input characters to the output stream, 
   * optionally up to `contentBufferLimit`. 
   */
  def copyToOutput(contentBufferLimit: Int = contentBuffer.length) {
    out.write(contentBuffer.toString.substring(0, contentBufferLimit))
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