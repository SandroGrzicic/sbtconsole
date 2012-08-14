package scala.tools.eclipse.sbtconsole

import scala.actors.Actor
import scala.tools.eclipse.logging.HasLogger
import org.eclipse.core.resources.IProject
import scala.tools.eclipse.util.SWTUtils
import java.io.OutputStream
import java.io.InputStream
import java.io.File
import scala.tools.eclipse.ScalaPlugin
import scala.concurrent.SyncVar

object SbtRunner {
  sealed trait SbtRunnerMessage
  
  case class Start(config: SbtConfiguration, consoleStreams: ConsoleStreams) extends SbtRunnerMessage
  case class Restart(stop: () => Unit, start: () => Unit) extends SbtRunnerMessage
  case class Stop(stop: () => Unit, afterStopped: () => Unit) extends SbtRunnerMessage
  
  case object IsStarted extends SbtRunnerMessage
  
  case class SbtConfiguration(project: IProject, pathToSbt: String, sbtJavaArgs: String, projectDir: String)
  case class ConsoleStreams(in: InputStream, newOut: () => OutputStream)
  
  /** Time the SBT process has to clean itself up after receiving the "exit" command. */
  val SBT_EXIT_CLEANUP_TIME = 3000 // ms
}

/**
 * Manages the lifecycle of the SBT process.
 */
class SbtRunner extends Actor with HasLogger {
  import SbtRunner._
  import scala.sys.process.Process
  
  @volatile private var sbtProcess: Option[Process] = None
  
  @volatile private var sbtProcessBusy = false
  
  @volatile private var sbtConfiguration: SbtConfiguration = _
  @volatile private var consoleStreams: ConsoleStreams = _
 
  def act() {
    loop {
      react {
        case s: Start   => launchSbt(s.config, s.consoleStreams)
        case r: Restart => restartSbt(r.stop, r.start)
        case s: Stop    => stopSbt(s.stop, s.afterStopped)
        case IsStarted  => reply(sbtProcess.isDefined)
      } 
    }
  }
  
  /**
   * Launch the SBT process and route input and output through the Console object.
   */
  def launchSbt(config: SbtConfiguration, console: ConsoleStreams) {
    import scala.sys.process.{Process, ProcessIO, BasicIO}

    if (sbtProcessBusy || sbtProcess.isDefined) return
    sbtProcessBusy = true
    
    sbtConfiguration = config
    consoleStreams = console
    
    val consoleIn = console.in
    val consoleOut = console.newOut()
    
    val pio = new ProcessIO(
      sbtIn  => BasicIO.transferFully(consoleIn, sbtIn),
      sbtOut => BasicIO.transferFully(sbtOut, consoleOut),
      sbtErr => BasicIO.transferFully(sbtErr, consoleOut),
      false
    )

    try {
      val javaCmd = "java" :: config.sbtJavaArgs.split(' ').map(_.trim).toList :::
        List("-jar", "-Dsbt.log.noformat=true", "-Djline.WindowsTerminal.directConsole=false", config.pathToSbt)
      logger.info("Starting SBT in %s (%s)".format(config.projectDir, javaCmd))
      val builder = Process(javaCmd.toArray, Some(new File(config.projectDir)))
      sbtProcess = Some(builder.run(pio))
      
      scala.concurrent.ops.spawn {
        sbtProcessBusy = false
        // wait until the process terminates
        val exitCode = sbtProcess.get.exitValue() // blocks
        // process has terminated
        sbtProcess = None
        consoleOut.close()
        logger.info("SBT finished with exit code: %d".format(exitCode))
        
        if (exitCode != 0) SWTUtils.asyncExec {
          import org.eclipse.jface.dialogs.MessageDialog
          MessageDialog.openInformation(ScalaPlugin.getShell, "SBT launch error", """Could not launch SBT.

Please check the path to sbt-launch.jar (currently %s) in SBT Console Preferences.""".format(config.pathToSbt))
        }
      }
    } catch {
      case e =>
        eclipseLog.error("Error launching SBT", e)
    }
  }
  
  /** 
   * Restarts SBT. Runs the Stop function first, restarts SBT 
   * and finally runs the Start function. 
   */
  def restartSbt(stop: () => Unit, start: () => Unit) {
    val afterStopped = () => {
      launchSbt(sbtConfiguration, consoleStreams)
      start()
    }
    stopSbt(stop, afterStopped)
  }
  
  /** 
   * Stops SBT. Runs the Stop function first, then completely stops the SBT process 
   * and finally calls the afterStopped function. 
   */
  def stopSbt(stop: () => Unit, afterStopped: () => Unit) {
    
    if (sbtProcessBusy || sbtProcess.isEmpty) return
    sbtProcessBusy = true
    
    sbtProcess match {
      case Some(sbt) =>
        stop()
        // wait until it takes its time to exit cleanly
        var timeWaiting = 0
        while (sbtProcess.isDefined && timeWaiting < SBT_EXIT_CLEANUP_TIME) {
          val delta = 100
          Thread.sleep(delta)
          timeWaiting += delta
        }
        if (sbtProcess.isDefined) {
          sbt.destroy()
          sbtProcess = None
        }
      case None =>
    }
    sbtProcessBusy = false
    afterStopped()
  }
 
}