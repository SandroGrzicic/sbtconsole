package scala.tools.eclipse.sbtconsole

import java.util.jar.JarFile
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.runtime.Path
import scala.collection.mutable.MutableList
import org.eclipse.jdt.core.IClasspathEntry
import java.io.File
import java.io.FilenameFilter

case class SbtInfo(path: String, version: String, scalaVersion: String)

/** Useful SBT utility methods. */
object SbtUtils {

  val SEPARATOR = sys.props("file.separator")
  val SBT_LIBRARY_LOCATION_PREFIX = ".sbt" + SEPARATOR + "boot" + SEPARATOR
  val SBT_LIBRARY_LOCATION_SUFFIX = "org.scala-sbt" + SEPARATOR + "sbt" + SEPARATOR
  val USER_HOME = System.getProperty("user.home") + SEPARATOR
  val SBT_PROJECT_FOLDER = "project/"
  val SBT_LAUNCH_JAR = "sbt-launch.jar"
    
  private var sbtClasspaths = Map.empty[String, List[IClasspathEntry]]

  /**
   * Returns the SBT path, SBT version and SBT Scala version. 
   * Uses empty strings in case of unknown information or exceptions.
   * Use the specific methods in order to get specific information.
   */
  def getSbtInfo(): SbtInfo = {
    val path = getSbtPath()
    val version = path map getSbtVersion getOrElse Left() fold(_ => "", identity)
    val scalaVersion = sbtToScalaVersion(version)
    
    SbtInfo(path getOrElse "", version, scalaVersion)
  }

  /** Fetch the SBT version from the manifest file of the specified sbt-launch.jar. */
  def getSbtVersion(sbtPath: String): Either[Throwable, String] = {
    var jar: JarFile = null
    try {
      jar = new java.util.jar.JarFile(sbtPath)
      val manifest = jar.getManifest
      Right(manifest.getMainAttributes.getValue("Implementation-Version"))
    } catch {
      case e => Left(e)
    } finally {
      if (jar != null)
        jar.close()
    }
  }
  
  /** Maps SBT versions to Scala versions. */
  def sbtToScalaVersion(sbtVersion: String): String = sbtVersion match {
    case "0.11.2"       => "2.9.1"
    case "0.11.3"       => "2.9.2"
    case s if s.isEmpty => ""
    case _        => "2.9.2"
  }
  
  def getSbtClasspathFor(sbtVersion: String): List[IClasspathEntry] = {
    if (!sbtClasspaths.contains(sbtVersion)) {
      
      val scalaPath = "scala-" + sbtToScalaVersion(sbtVersion) 
      val sbtClasspath =
        USER_HOME +
        SBT_LIBRARY_LOCATION_PREFIX + scalaPath + SEPARATOR +
        SBT_LIBRARY_LOCATION_SUFFIX + sbtVersion + SEPARATOR
      
      val classpath = MutableList.empty[IClasspathEntry]
      
      val dir = new File(sbtClasspath)
      val jars = dir.list(new FilenameFilter() {
        def accept(dir: File, name: String) = name.endsWith(".jar")
      })
      jars foreach { jar =>
        classpath += JavaCore.newLibraryEntry(new Path(sbtClasspath + jar), null, null)        
      } 
      
      sbtClasspaths += sbtVersion -> classpath.toList
    }
    
    sbtClasspaths(sbtVersion)
  }
 
  /** Tries to find the SBT location based on the user's OS. */
  def getSbtPath(): Option[String] = {
    sys.props("os.name") match {
      case os: String =>
        var possibleLocations = List.empty[File]
        if (os.toLowerCase.contains("windows")) {
          // windows
          if (sys.env.contains("ProgramFiles"))
            possibleLocations :+= new File(sys.env("ProgramFiles") + SEPARATOR + "sbt" + SEPARATOR + SBT_LAUNCH_JAR)
          if (sys.env.contains("ProgramFiles(x86)"))
            possibleLocations :+= new File(sys.env("ProgramFiles(x86)") + SEPARATOR + "sbt" + SEPARATOR + SBT_LAUNCH_JAR)
        } else {
          // linux / mac, most likely
          possibleLocations :+= new File("~" + SEPARATOR + "bin" + SEPARATOR + SBT_LAUNCH_JAR)
          possibleLocations :+= new File("~" + SEPARATOR + "bin" + SEPARATOR + "sbt" + SEPARATOR + SBT_LAUNCH_JAR)
        }
        
        possibleLocations foreach { file =>
          if (file.exists())
            return Some(file.toString)
        }
        
      case _ =>
    }
    
    None
  }
}