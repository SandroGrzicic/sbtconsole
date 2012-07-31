package scala.tools.eclipse.sbtconsole.editor

import org.eclipse.ui.IPartListener2
import org.eclipse.ui.IWorkbenchPartReference
import scala.tools.eclipse.logging.HasLogger

/**
 * Listens to part changes, activating the SBT build path if a SBT build file is opened.  
 */
class SbtEditorListener extends IPartListener2 with HasLogger {

  def partActivated(partRef: IWorkbenchPartReference): Unit = {
//    eclipseLog.info("part activated: " + partRef)
//    partRef.getPage.
  }

  def partBroughtToTop(partRef: IWorkbenchPartReference): Unit = {}

  def partClosed(partRef: IWorkbenchPartReference): Unit = {}

  def partDeactivated(partRef: IWorkbenchPartReference): Unit = {}

  def partOpened(partRef: IWorkbenchPartReference): Unit = {}

  def partHidden(partRef: IWorkbenchPartReference): Unit = {}

  def partVisible(partRef: IWorkbenchPartReference): Unit = {}

  def partInputChanged(partRef: IWorkbenchPartReference): Unit = {}

}