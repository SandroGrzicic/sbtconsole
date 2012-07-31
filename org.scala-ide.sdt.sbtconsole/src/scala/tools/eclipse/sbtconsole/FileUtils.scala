package scala.tools.eclipse.sbtconsole

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import scala.tools.eclipse.ScalaPlugin

/**
 * Extension to the default ScalaIDE FileUtils.
 */
object FileUtils {
  import ScalaPlugin.plugin
  
  def createMarker(file: IFile, severity: Int, msg: String, line: Int): IMarker = {
    val mrk = file.createMarker(plugin.problemMarkerId)
    mrk.setAttribute(IMarker.SEVERITY, severity)

    // Marker attribute values are limited to <= 65535 bytes and setAttribute will assert if they
    // exceed this. To guard against this we trim to <= 21000 characters ... see
    // org.eclipse.core.internal.resources.MarkerInfo.checkValidAttribute for justification
    // of this arbitrary looking number
    val maxMarkerLen = 21000
    val trimmedMsg = msg.take(maxMarkerLen)

    val attrValue = trimmedMsg.map {
      case '\n' | '\r' => ' '
      case c           => c
    }

    mrk.setAttribute(IMarker.MESSAGE, attrValue)
    mrk.setAttribute(IMarker.LINE_NUMBER, line)
    mrk
  }

}