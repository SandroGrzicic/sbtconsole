package scala.tools.eclipse.sbtconsole

import java.io.File
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import scala.Array.canBuildFrom
import scala.concurrent.ops
import scala.sys.process.BasicIO
import scala.sys.process.Process
import scala.sys.process.ProcessIO
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.ScalaProject
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.sbtconsole.console.SbtConsole
import scala.tools.eclipse.sbtconsole.properties.Preferences
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.widgets.Display
import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.ui.console.IConsole
import org.eclipse.ui.dialogs.PreferencesUtil
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent


/**
 * Holds the SBT process, the associated project and the corresponding Console instance.
 */
class SbtBuilder(project: ScalaProject) extends HasLogger {

  def console: SbtConsole = getConsole()

  @volatile private var shuttingDown = false

  private var sbtProcess: Process = _

  private val consoleName = "Sbt - %s".format(project.underlying.getName)

  private val consoleManager = ConsolePlugin.getDefault().getConsoleManager()

  /**
   * Return the corresponding Sbt console for this builder.
   * If the current Console manager has a console registered with this name,
   * return it. Otherwise create and add a new console to the Console manager.
   */
  def getConsole(): SbtConsole = {
    consoleManager.getConsoles().find(_.getName == consoleName) match {
      case Some(c: SbtConsole) => c
      case None => createConsole()
    }
  }

  
  /**
   * Create a new SbtConsole. Install the pattern matcher that adds hyperlinks
   * for error messages.
   */
  private def createConsole(): SbtConsole = {

    val onConsoleTermination = () => dispose()
    val console = new SbtConsole(consoleName, null, onConsoleTermination)
    console.setConsoleWidth(140)
    
    consoleManager.addConsoles(Array[IConsole](console))
    console
  }

  var consoleOutputStream: OutputStream = _

  
  /**
   * Launch the SBT process and route input and output through the Console object.
   * Escape sequences are stripped from the output of SBT.
   */
  def launchSbt(pathToSbt: String, sbtJavaArgs: String, projectDir: String) {

    val pio = new ProcessIO(in => BasicIO.transferFully(console.getInputStream, in),
      os => BasicIO.transferFully(os, consoleOutputStream),
      es => BasicIO.transferFully(es, consoleOutputStream), 
      false
    )

    try {
      consoleOutputStream = console.newOutputStream()

      val javaCmd = "java" :: sbtJavaArgs.split(' ').map(_.trim).toList :::
        List("-jar", "-Dsbt.log.noformat=true", "-Djline.WindowsTerminal.directConsole=false", pathToSbt)
      logger.info("Starting SBT in %s (%s)".format(projectDir, javaCmd))
      shuttingDown = false
      val builder = Process(javaCmd.toArray, Some(new File(projectDir)))
      sbtProcess = builder.run(pio)
      ops.spawn {
        // wait until the process terminates, and close this console
        val exitCode = sbtProcess.exitValue() // blocks
        logger.info("SBT finished with exit code: %d".format(exitCode))
        if (exitCode != 0 && !shuttingDown) Display.getDefault.asyncExec(new Runnable {
          def run() {
            MessageDialog.openInformation(ScalaPlugin.getShell, "Sbt launch error", """Could not launch SBT.

Please check the path to sbt-launch.jar (currently %s) in SBT Console Preferences.""".format(pathToSbt))
          }
        })
        dispose()
      }
    } catch {
      case e =>
        eclipseLog.error("Error launching SBT", e)
    }
  }

  /** Returns the SBT Console settings from the user preferences. */
  private def loadSbtSettings() = {
    import Preferences._

    val p = project.underlying.getProject
    
    (sbtPath(p), sbtJavaArgs(p), projectDirectory(p))
  }

  /** Entry point. */ 
  def showConsole() {
    
    def fetchProjectDirectory() = {
      import Preferences._
      
      val projectDirectory = project.underlying.getLocation.toFile
      val projectFiles = projectDirectory.list
      val preferences = projectStore(project.underlying.getProject)

      // check if this is a SBT root directory      
      if (projectFiles.contains("project") || projectFiles.contains("build.sbt")) {
        
        // current directory is almost certainly a SBT project root
        val projectDir = projectDirectory.getPath
        preferences.setValue(P_PROJECT_DIRECTORY, projectDir)
        preferences.save()
        projectDir
      
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
           val projectDir = projectDirectory.getPath
           preferences.setValue(P_PROJECT_DIRECTORY, projectDir)
           preferences.save()
           projectDir
         } else {
           val dialog = PreferencesUtil.createPropertyDialogOn(shell, project.underlying, PAGE_ID, Array(PAGE_ID), null)

           dialog.open() // blocking
           
           val projectDir = loadSbtSettings()._3
           projectDir
         }
      }
    }
   
    val (pathToSbt, sbtJavaArgs, projectDirSetting) = loadSbtSettings()
          
    var projectDir: String = projectDirSetting
    if (projectDirSetting.isEmpty) {
      projectDir = fetchProjectDirectory()
    }
    
    if (!projectDir.isEmpty) {
      if (sbtProcess == null) 
        launchSbt(pathToSbt, sbtJavaArgs, projectDir)
      consoleManager.showConsoleView(console)
    }
  }

  def dispose() {
    if (visible) {
      shuttingDown = true
      sbtProcess.destroy
      sbtProcess = null
      console.dispose()
      consoleManager.removeConsoles(Array(console))
    }
  }
  
  def visible: Boolean = sbtProcess ne null
}
