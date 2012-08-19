package scala.tools.eclipse.sbtconsole

import org.eclipse.ui.plugin.AbstractUIPlugin
import org.eclipse.ui.IStartup
import org.osgi.framework.BundleContext
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.interpreter.EclipseRepl
import scala.tools.eclipse.interpreter.EclipseRepl._
import scala.tools.eclipse.interpreter.EclipseRepl.Client
import scala.tools.nsc.interpreter.Results.Result
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.IPartListener2
import scala.tools.eclipse.sbtconsole.editor.SbtEditorListener
import org.eclipse.ui.IWorkbenchPage
import scala.tools.eclipse.sbtconsole.properties.Preferences
import scala.tools.eclipse.sbtconsole.actions.ShowSbtConsoleAction
import org.eclipse.ui.internal.Workbench
import scala.tools.eclipse.util.SWTUtils

object SbtConsolePlugin {
  @volatile var plugin: SbtConsolePlugin = _
}

class SbtConsolePlugin extends AbstractUIPlugin with IStartup with HasLogger {

  lazy val sbtEditorListener = new SbtEditorListener()

  override def start(context: BundleContext) {
    super.start(context)
    SbtConsolePlugin.plugin = this

    if (Preferences.sbtPath().isEmpty) {
      val sbtPath = SbtUtils.getSbtPath()
      sbtPath match {
        case Some(path) =>
          Preferences.workspaceStore.setValue(Preferences.P_SBT_PATH, path)
        case None =>
          eclipseLog.error("Path to SBT could not be determined. Please set the path manually (Preferences -> Scala -> SBT Console).")
      }
    } else {
      if (Preferences.sbtAutostart) {
        SWTUtils.asyncExec {
          for (window <- Workbench.getInstance.getWorkbenchWindows()) {
            val action = new ShowSbtConsoleAction()
            action.init(window)
            for (project <- action.editedProject) {
              action.performAction(project)
            }
          }
        }
      }
    }

  }

  def earlyStartup() {
    toggleSbtEditorSupport(true)
  }

  override def stop(context: BundleContext) {
    super.stop(context)

    toggleSbtEditorSupport(false)

    SbtConsolePlugin.plugin = null
  }
  
  /** Switch SBT Scala project file editing support on or off. */
  def toggleSbtEditorSupport(status: Boolean) {
    if (Preferences.sbtEditorSupport) {
      if (status) {
        SWTUtils.asyncExec {
          activePage foreach { page =>
            page.addPartListener(sbtEditorListener)
          }
        }
      } else {
        activePage foreach { page =>
          sbtEditorListener.stop()
          page.removePartListener(sbtEditorListener)
        }
      }
    }
  }


  def activePage: Option[IWorkbenchPage] = {
    PlatformUI.getWorkbench() match {
      case null => None
      case workbench => workbench.getActiveWorkbenchWindow match {
        case null   => None
        case window => Some(window.getActivePage)
      }
    }
  }

  def pluginId = "org.scala-ide.sdt.sbtconsole"
}