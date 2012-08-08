package scala.tools.eclipse.sbtconsole

import java.io.File
import org.eclipse.core.runtime.{ IProgressMonitor, NullProgressMonitor }
import org.eclipse.debug.core.{ ILaunch, ILaunchConfiguration }
import org.eclipse.jdt.launching.{ AbstractJavaLaunchConfigurationDelegate, ExecutionArguments, VMRunnerConfiguration }
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.sbtconsole.actions.ShowSbtConsoleAction

/**
 * A Launch Delegate which launches the ShowSbtConsoleAction with the current project.
 */
class SbtConsoleLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {  
  
  override def launch(configuration: ILaunchConfiguration, mode: String, launch: ILaunch, monitor: IProgressMonitor) {
    val project = verifyJavaProject(configuration)
    new ShowSbtConsoleAction().performAction(project.getProject, None)
  }
 
}