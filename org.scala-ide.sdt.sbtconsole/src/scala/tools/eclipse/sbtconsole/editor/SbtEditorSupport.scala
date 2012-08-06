package scala.tools.eclipse.sbtconsole
package editor

import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.sbtconsole.properties.Preferences

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jdt.core.JavaCore
import org.eclipse.ui.IPartListener2
import org.eclipse.ui.IWorkbenchPartReference
import org.eclipse.ui.internal.EditorReference

/**
 * Support for editing SBT project build files by modifying the project classpath.
 */
class ClasspathModifier(project: IProject) extends HasLogger {

  val javaProject = JavaCore.create(project)
  val originalClasspath = javaProject.getRawClasspath

  /** Activates the modified classpath. */
  def activate() {
    val sbtClasspath = SbtUtils.getSbtClasspathFor(Preferences.sbtVersion(project))
    val sbtClasspathFiltered = sbtClasspath.filter(!originalClasspath.contains(_))

    javaProject.setRawClasspath(originalClasspath ++ sbtClasspathFiltered, true, ClasspathModifier.PROGRESS_MONITOR)
  }

  /** Deactivates the modified classpath. */
  def deactivate() {
    javaProject.setRawClasspath(originalClasspath, true, ClasspathModifier.PROGRESS_MONITOR)
  }
}
object ClasspathModifier {
  val PROGRESS_MONITOR = new NullProgressMonitor()
}

/**
 * Listens to part changes, activating the modified SBT build path
 * if a SBT build file is opened in an editor.
 */
class SbtEditorListener extends IPartListener2 with HasLogger {

  private var sbtEditorProjects = collection.immutable.Map[IProject, ClasspathModifier]()
  
  /**
   * If the given part reference is an editor reference,
   * activate the modified classpath or deactivate it
   * depending on whether the editor has a project file open.
   */
  def partActivated(partRef: IWorkbenchPartReference) {
    if (partRef.isInstanceOf[EditorReference]) {
      val view = partRef.asInstanceOf[EditorReference]
      if (view.getId == ScalaPlugin.plugin.editorId) {
        val file = view.getEditorInput.getAdapter(classOf[IFile]).asInstanceOf[IFile]
        val project = file.getProject

        if (file.getProjectRelativePath.toPortableString.startsWith(SbtUtils.SBT_PROJECT_FOLDER)) {
          if (!sbtEditorProjects.contains(project)) {
            val instance = new ClasspathModifier(project)
            sbtEditorProjects += project -> instance
            instance.activate()
          }
        } else if (sbtEditorProjects.contains(project)) {
          sbtEditorProjects(project).deactivate()
          sbtEditorProjects -= project
        }
      }
    }
  }

  /** 
   * Must be called when removing this listener from its parent, 
   * in order to properly deactivate all modified classpaths. 
   */
  def stop() {
    sbtEditorProjects.values.foreach(_.deactivate())     
  }

  def partDeactivated(partRef: IWorkbenchPartReference) {}
  def partBroughtToTop(partRef: IWorkbenchPartReference) {}
  def partClosed(partRef: IWorkbenchPartReference) {}
  def partOpened(partRef: IWorkbenchPartReference) {}
  def partHidden(partRef: IWorkbenchPartReference) {}
  def partVisible(partRef: IWorkbenchPartReference) {}
  def partInputChanged(partRef: IWorkbenchPartReference) {}
}