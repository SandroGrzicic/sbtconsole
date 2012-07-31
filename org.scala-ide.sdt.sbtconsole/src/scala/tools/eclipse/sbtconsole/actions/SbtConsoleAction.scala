package scala.tools.eclipse.sbtconsole
package actions

import org.eclipse.jface.action.IAction
import org.eclipse.ui.IObjectActionDelegate
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.widgets.Shell
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jdt.core._

/**
 * Actions which runs the SBT Console. 
 */
class SbtConsoleAction extends IObjectActionDelegate {
  var target : Option[IJavaElement] = None
  val SBT_CONSOLE_LAUNCH_ID = "scala.sbtconsole"
  override def setActivePart(action : IAction, targetpart : IWorkbenchPart) = {}
  
  override def run(action: IAction) = {
    if(target.isEmpty) {
      val shell = new Shell
      MessageDialog.openInformation(shell, "Scala Development Tools", "SBT Console could not be created")
    }
    
    val project = target.get.getJavaProject.getProject
    
    Factory.openConsoleInProjectFromTarget(project, target)
  }
  
  override def selectionChanged(action: IAction, select: ISelection) = {
    if (select.isInstanceOf[IStructuredSelection]) {
      (select.asInstanceOf[IStructuredSelection]).getFirstElement match {
        case item : IJavaProject =>
          this.target = Some(item)
          action.setText("Show/hide a SBT console for " + item.getElementName)
        case item : IPackageFragment =>
          this.target = Some(item)
          action.setText("Show/hide a SBT console for " + item.getElementName)
        case _ =>
          this.target = null
          action.setText("Show/hide a SBT console")
      }
    }
  }
}
