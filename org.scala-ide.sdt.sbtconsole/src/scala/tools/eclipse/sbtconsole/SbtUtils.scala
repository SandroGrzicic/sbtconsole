package scala.tools.eclipse.sbtconsole

import java.util.jar.JarFile
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.runtime.Path
import scala.collection.mutable.MutableList
import org.eclipse.jdt.core.IClasspathEntry
import java.io.File
import java.io.FilenameFilter

/** Useful SBT utility methods. */
object SbtUtils {

  val SEPARATOR = sys.props("file.separator")
  val SBT_LIBRARY_LOCATION_PREFIX = ".sbt" + SEPARATOR + "boot" + SEPARATOR
  val SBT_LIBRARY_LOCATION_SUFFIX = "org.scala-sbt" + SEPARATOR + "sbt" + SEPARATOR
  val USER_HOME = System.getProperty("user.home") + SEPARATOR
  val SBT_PROJECT_FOLDER = "project/"
    
  private var sbtClasspaths = Map.empty[String, List[IClasspathEntry]]

  /** Fetch the SBT version from the manifest file of the specified sbt-launch.jar. */
  def getSbtVersion(sbtPath: String) = {
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
  def sbtToScalaVersion(sbtVersion: String) = sbtVersion match {
    case "0.11.2" => "2.9.1"
    case "0.11.3" => "2.9.2"
    case _        => "2.9.2"
  }
  
  def getSbtClasspathFor(sbtVersion: String) = {
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

}