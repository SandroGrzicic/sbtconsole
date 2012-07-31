package scala.tools.eclipse.sbtconsole.actions

import scala.tools.eclipse.ScalaPlugin
import org.eclipse.core.resources._
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.{IStructuredSelection, ISelection}
import org.eclipse.ui.console._
import org.eclipse.ui._
import scala.tools.eclipse.util.EclipseUtils._
import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.sbtconsole.SbtBuilder

/** Show the Sbt console for the current project.
 */
class ShowSbtConsoleAction extends IWorkbenchWindowActionDelegate with HasLogger {

  def performAction(project: IProject, action: Option[IAction]) {
    val scalaProject = ScalaPlugin.plugin.getScalaProject(project)
    
    import SbtConsoleHolder._
    sbtConsoles.get(scalaProject) match {
      case Some(console) =>
        if (console.visible) {
          console.dispose()
        }
        action.foreach { _.setChecked(false) }
        sbtConsoles -= scalaProject
      case None =>
        val console = new SbtBuilder(scalaProject)
        sbtConsoles += scalaProject -> console 
        console.showConsole()
        action.foreach { _.setChecked(true) }
    }
  }
  
  def performAction(project: IProject, action: IAction) {
    performAction(project, Option(action))
  }

  private var parentWindow: IWorkbenchWindow = null

  override def init(window: IWorkbenchWindow) {
    parentWindow = window
  }

  def dispose = {}

  def run(action: IAction) {
    if (currentProject.isDefined)
      performAction(currentProject.get, action)
    else
      eclipseLog.warn("Couldn't figure out current project.")
  }
  
  def editedProject: Option[IProject] = for {
      w <- Option(parentWindow)
      page <- Option(w.getActivePage)
      editor <- Option(page.getActiveEditor)
      input <- Option(editor.getEditorInput)
      res <- input.adaptToSafe[IResource]
      proj <- Option(res.getProject())
    } yield proj;

  var currentProject: Option[IProject] = None

  /** Enable the menu item only for open projects. */
  def selectionChanged(action: IAction, selection: ISelection) {
    def setProject(p: Option[IProject]) = {
      currentProject = p orElse editedProject
      action.setEnabled(currentProject.isDefined && currentProject.get.isOpen)
    }
    
    selection match {
      case structuredSel: IStructuredSelection =>
        structuredSel.getFirstElement() match {
          case el: IProject          => setProject(Some(el))
          case adaptable: IAdaptable => setProject(adaptable.adaptToSafe[IProject])
          case _                     => setProject(None)
        }
      case _ => setProject(None)
    }
  }
}

object SbtConsoleHolder {
  /** Holder of SbtConsoles. */
  var sbtConsoles = collection.immutable.Map[ScalaProject, SbtBuilder]()
}