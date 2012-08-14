package scala.tools.eclipse.sbtconsole

import java.io.File
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import scala.Array.canBuildFrom
import scala.concurrent.ops
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.sbtconsole.console.SbtConsole
import scala.tools.eclipse.sbtconsole.properties.Preferences
import org.eclipse.swt.widgets.Display
import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.ui.console.IConsole
import org.eclipse.ui.dialogs.PreferencesUtil
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent
import scala.tools.eclipse.util.SWTUtils
import org.eclipse.core.resources.IProject
import org.eclipse.jface.dialogs.MessageDialog
import scala.tools.eclipse.sbtconsole.SbtRunner._
/**
 * Entry point to the SBT Console.
 *
 * Holds the SBT process, the associated project and the corresponding Console instance.
 */
class SbtBuilder(project: IProject) extends HasLogger {

  /** The console instance of this SbtBuilder. */
  lazy val console: SbtConsole = createConsole()

  /** Returns whether the console is currently displayed. */
  def consoleDisplayed: Boolean = consoleManager.getConsoles.exists(_.getName == consoleName)

  private val consoleName = "Sbt - %s".format(project.getName)

  private val consoleManager = ConsolePlugin.getDefault().getConsoleManager()

  /** Actor which manages the SBT process. */
  private lazy val sbtRunner = new SbtRunner().start()

  /** Create and return the SbtConsole for this SbtBuilder. */
  private def createConsole(): SbtConsole = {
    val console = new SbtConsole(consoleName, null, restartSbt, dispose)
    console.setConsoleWidth(140)
    console
  }

  /**
   * Starts the SBT console.
   * 
   * Shows the console and starts the SBT process.
   */
  def start() {
    getProjectDirectory() match {
      case s if s.isEmpty => eclipseLog.warn("SBT Console not started due to project directory being not set.")
      case projectDir =>
        showConsole()
        startSbt(projectDir)
    }
  }
  
  /**
   * Shows, activates and clears the console. 
   * Can be used multiple times; only one console will be shown to the user.
   */
  def showConsole() {
    consoleManager.addConsoles(Array[IConsole](console))
    consoleManager.showConsoleView(console)
    console.clearConsole()
  }

  /**
   * Starts the SBT process.
   *
   * Optionally gets the current project directory and other settings 
   * and uses them to start the SBT process.
   * 
   * Does nothing if SBT is already started.
   */
  def startSbt(projectDir: String = getProjectDirectory())  {
    import Preferences._

    if (!projectDir.isEmpty && !sbtProcessStarted) {
      val sbtConfig = SbtConfiguration(project, sbtPath(project), sbtJavaArgs(project), projectDir)
      val streams = ConsoleStreams(console.getInputStream(), () => console.newOutputStream())
      sbtRunner ! Start(sbtConfig, streams)
    }
  }

  /** Restarts the SBT process and reactivates the console. */
  def restartSbt() {
    if (sbtProcessStarted) {
      sbtRunner ! Restart(sendExitToSbt, showConsole)
    } else {
      start()
    }
  }

  /** Terminates the SBT process and the console. */
  def dispose() {
    val afterStopped = () => {
//      console.dispose()
//      consoleManager.removeConsoles(Array(console))
    }
    sbtRunner ! Stop(sendExitToSbt, afterStopped)
  }

  /** Try to cleanly close the SBT process by sending it an exit command. */
  private def sendExitToSbt() {
    SWTUtils asyncExec {
      console.getInputStream().appendData("\nexit\n")
    }
  }

  /** 
   * Returns the root directory for the current project 
   * from the project preferences, 
   * or prompts the user if it hasn't been set.
   */
  private def getProjectDirectory(): String = {
    import Preferences._
    
    projectDirectory(project) match {
      case s if s.isEmpty => fetchProjectDirectory(project)
      case s              => s 
    }
  }

  /**
   * Return the root directory for this project;
   * prompt the user if it hasn't been set.
   */
  private def fetchProjectDirectory(project: IProject) = {
    import Preferences._

    val projectLocation = project.getLocation.toFile
    val projectFiles = projectLocation.list
    val projectProperties = projectStore(project)

    // check if this is a SBT root directory      
    if (projectFiles.contains("project") || projectFiles.contains("build.sbt")) {

      // current directory is almost certainly a SBT project root
      val directory = projectLocation.getPath
      projectProperties.setValue(P_PROJECT_DIRECTORY, directory)
      projectProperties.save()

      directory

    } else {

      // ask the user to choose a project root
      val shell = ScalaPlugin.getShell
      val useCurrentDirectory = MessageDialog.openQuestion(
        shell,
        "Project root directory",
        "Would you like to use the current project directory as the root directory for SBT?\n\n" +
          "Select No if this is part of a multi-module project, and you would like to set the project root yourself."
      ) // blocking

      if (useCurrentDirectory) {
        val directory = projectLocation.getPath
        projectProperties.setValue(P_PROJECT_DIRECTORY, directory)
        projectProperties.save()
        directory
      } else {
        val dialog = PreferencesUtil.createPropertyDialogOn(shell, project, PAGE_ID, Array(PAGE_ID), null)

        dialog.open() // blocking

        projectDirectory(project)
      }
    }
  }

  def sbtProcessStarted = {
    (sbtRunner !? SbtRunner.IsStarted) match {
      case true => true
      case _    => false
    }
  }
}

object SbtBuilder {

  /** Starts the SBT console on the given project. */
  def apply(project: IProject) = new SbtBuilder(project).start()

}