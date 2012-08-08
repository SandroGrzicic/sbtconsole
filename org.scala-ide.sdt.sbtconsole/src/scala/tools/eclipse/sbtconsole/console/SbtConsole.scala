package scala.tools.eclipse.sbtconsole.console

import java.util.regex.Pattern
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.sbtconsole.FileUtils
import scala.util.matching.Regex.Groups
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.console.IHyperlink
import org.eclipse.ui.console.IPatternMatchListener
import org.eclipse.ui.console.IPatternMatchListenerDelegate
import org.eclipse.ui.console.PatternMatchEvent
import org.eclipse.ui.console.TextConsole
import org.eclipse.ui.ide.IDE
import scala.tools.eclipse.sbtconsole.shellconsole.ShellConsole

class SbtConsole(name: String, imgDescriptor: ImageDescriptor = null, onTermination: () => Unit)
    extends ShellConsole(name, imgDescriptor, onTermination)
    with HasLogger {

  val partitioner = new SbtPartitioner(this)
  partitioner.connect(getDocument());

  val sourceLocationPattern = """^\[error\]\w*(.*):(\d+):(.*)$"""

  addPatternMatchListener(PatMatchListener(sourceLocationPattern, (text, offset) =>
    sourceLocationPattern.r.findFirstMatchIn(text) match {
      case Some(m @ Groups(path, lineNr, msg)) =>
//        logger.info("error found at %s:%d:%s".format(path, lineNr.toInt, msg))
        for (file <- ResourcesPlugin.getWorkspace.getRoot().findFilesForLocation(new Path(path.trim))) {
//          logger.info("added hyperlink for %s".format(file))
          addHyperlink(ErrorHyperlink(file, lineNr.toInt, msg), offset + m.start(1), path.length)
        }

      case _ => logger.error("Unable to match a source location pattern.")
    }))

  override def getPartitioner() = partitioner

  override def dispose() = {
    super.dispose()
  }

}

/** A Pattern match listener that calls `fun' when the given regular expression matches. */
case class PatMatchListener(regex: String, fun: (String, Int) => Unit)
    extends IPatternMatchListener
    with IPatternMatchListenerDelegate
    with HasLogger {

  var console: Option[TextConsole] = None

  def getLineQualifier() = null

  def getCompilerFlags() = Pattern.MULTILINE

  def getPattern() = regex

  def connect(console: TextConsole) {
    this.console = Some(console)
  }

  def disconnect() {
    console = None
  }

  def matchFound(event: PatternMatchEvent) {
    console match {
      case Some(c) => fun(c.getDocument().get(event.getOffset(), event.getLength()), event.getOffset)
      case None    => eclipseLog.error("Invalid match, no text console found.")
    }
  }
}

/**
 * A hyperlink for errors in SBT output. When clicked, it opens the corresponding
 *  editor and selects the affected line.
 *
 *  Error markers are not persisted (the hyperlink deletes it after the editor is open)
 */
case class ErrorHyperlink(file: IFile, lineNr: Int, msg: String) extends IHyperlink with HasLogger {
  def linkActivated() {
    val marker = FileUtils.createMarker(file, IMarker.SEVERITY_ERROR, msg, lineNr)
    try {
      val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
      IDE.openEditor(page, marker, true)
    } catch {
      case e: Exception =>
        eclipseLog.error("Exception while opening editor.")
    } finally marker.delete()
  }

  def linkExited() {}
  def linkEntered() {}
}
