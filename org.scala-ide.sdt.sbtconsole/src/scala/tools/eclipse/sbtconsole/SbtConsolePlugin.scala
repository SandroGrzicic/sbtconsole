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


object SbtConsolePlugin {
  @volatile var plugin: SbtConsolePlugin = _
}

class SbtConsolePlugin extends AbstractUIPlugin with IStartup with HasLogger {

  override def start(context: BundleContext) {
    super.start(context)
    SbtConsolePlugin.plugin = this
    logger.debug("SbtConsolePlugin startup") 
  }

  /** Adds a PartListener to the active workbench window. */
  def earlyStartup() {
    logger.debug("SbtConsolePlugin early startup") 
    val workbench = PlatformUI.getWorkbench()
    workbench.getDisplay.asyncExec(new Runnable() {
       def run() {
         val window = workbench.getActiveWorkbenchWindow
         if (window != null) {
           logger.debug("SbtConsolePlugin part listener added")
           window.getActivePage.addPartListener(new SbtEditorListener())
         }
       }
     });

  }

  def pluginId = "org.scala-ide.sdt.sbtconsole"
}