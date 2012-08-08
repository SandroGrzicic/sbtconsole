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

object SbtConsolePlugin {
  @volatile var plugin: SbtConsolePlugin = _
}

class SbtConsolePlugin extends AbstractUIPlugin with IStartup with HasLogger {

  lazy val sbtEditorListener = new SbtEditorListener()

  override def start(context: BundleContext) {
    super.start(context)
    SbtConsolePlugin.plugin = this
    
    if (Preferences.sbtPath(null).isEmpty) {
      val sbtPath = SbtUtils.getSbtPath()
      sbtPath match {
        case Some(path) => 
          Preferences.workspaceStore.setValue(Preferences.P_SBT_PATH, path)
        case None => 
          eclipseLog.error("Path to SBT could not be determined. Please set the path manually (Preferences -> Scala -> SBT Console).")
      }
    }
    
  }

  /** Adds a PartListener to the active workbench window. */
  def earlyStartup() {
    PlatformUI.getWorkbench().getDisplay.asyncExec(new Runnable() {
      def run() {
        activePage foreach { page =>
          page.addPartListener(sbtEditorListener)
        }
      }
    })
  }

  override def stop(context: BundleContext) {
    super.stop(context)
    activePage foreach { page =>
      sbtEditorListener.stop()
      page.removePartListener(sbtEditorListener)
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