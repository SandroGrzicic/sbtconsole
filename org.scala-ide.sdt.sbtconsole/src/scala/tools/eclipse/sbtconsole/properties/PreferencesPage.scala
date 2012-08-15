package scala.tools.eclipse.sbtconsole
package properties

import scala.collection.Set
import scala.collection.mutable
import scala.tools.eclipse.logging.HasLogger
import scala.tools.eclipse.properties.PropertyStore
import scala.tools.eclipse.sbtconsole.SbtConsolePlugin
import scala.tools.eclipse.sbtconsole.SbtUtils
import scala.tools.eclipse.util.SWTUtils.noArgFnToSelectionAdapter
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages
import org.eclipse.jface.preference.DirectoryFieldEditor
import org.eclipse.jface.preference.FieldEditor
import org.eclipse.jface.preference.FileFieldEditor
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.preference.StringFieldEditor
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Link
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.ui.dialogs.PreferencesUtil
import org.eclipse.ui.dialogs.PropertyPage
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swt.MigLayout
import org.eclipse.jface.preference.BooleanFieldEditor

/**
 * The PropertyPage / PreferencePage for SBT Console.
 *
 * Shows a page with project properties or global workbench preferences.
 */
class PreferencesPage extends PropertyPage with IWorkbenchPreferencePage with HasLogger {
  import Preferences._

  private var isGlobalPrefs = false

  private var projectSpecificButton: Button = _

  /** Controls which are present in both the global and project specific properties. */
  private var allSharedControls: Set[Control] = Set()

  private val fieldEditors = mutable.ListBuffer[FieldEditor]()

  /** Called if we are initialized from the global Preferences. */
  override def init(workbench: IWorkbench) {
    isGlobalPrefs = true
  }

  private def getProject = getElement match {
    case project: IProject     => project
    case project: IJavaProject => project.getProject
    case _                     => null
  }

  private def initUnderlyingPreferenceStore() {
    setPreferenceStore(getElement match {
      case project: IProject     => projectStore(project)
      case project: IJavaProject => projectStore(project.getProject)
      case _                     => workspaceStore
    })
  }

  def addNewFieldEditorWrappedInComposite[T <: FieldEditor](parent: Composite)(f: Composite => T): T = {
    val composite = new Composite(parent, SWT.NONE)

    val fieldEditor = f(composite)

    fieldEditor.setPreferenceStore(getPreferenceStore)
    fieldEditor.load

    if (isGlobalPrefs) {
      composite.setLayoutData(new CC().grow.wrap)
    } else {
      composite.setLayoutData(new CC().spanX(2).grow.wrap)
    }

    fieldEditor
  }

  def createContents(parent: Composite): Control = {

    initUnderlyingPreferenceStore()

    val control = new Composite(parent, SWT.NONE)
    control.setLayout(new MigLayout(new LC().insetsAll("0").fillX, new AC(), new AC()))

    def horizontalLine() {
      val horizontalLine = new Label(control, SWT.SEPARATOR | SWT.HORIZONTAL)
      horizontalLine.setLayoutData(new CC().spanX(2).grow.wrap)
    }

    if (!isGlobalPrefs) {
      // project-specific properties

      fieldEditors += addNewFieldEditorWrappedInComposite(parent = control) { parent =>
        new DirectoryFieldEditor(P_PROJECT_DIRECTORY, "Project root", parent) {
          if (getProject != null) {
            setFilterPath(getProject.getLocation.toFile)
          }
        }
      }

      horizontalLine()

      projectSpecificButton = new Button(control, SWT.CHECK | SWT.WRAP)
      projectSpecificButton.setText("Enable project specific settings")
      projectSpecificButton.setSelection(getPreferenceStore.getBoolean(P_USE_PROJECT_SPECIFIC_SETTINGS))
      projectSpecificButton.addSelectionListener { () =>
        val enabled = projectSpecificButton.getSelection
        getPreferenceStore.setValue(P_USE_PROJECT_SPECIFIC_SETTINGS, enabled)
        allSharedControls foreach { _.setEnabled(enabled) }
      }

      projectSpecificButton.setLayoutData(new CC)

      val linkToGlobalPreferences = new Link(control, SWT.NONE)
      linkToGlobalPreferences.setText("<a>" + PreferencesMessages.PropertyAndPreferencePage_useworkspacesettings_change + "</a>")
      linkToGlobalPreferences.addSelectionListener { () =>
        PreferencesUtil.createPreferenceDialogOn(getShell, PAGE_ID, Array(PAGE_ID), null).open()
      }
      linkToGlobalPreferences.setLayoutData(new CC().alignX("right").wrap)

      horizontalLine()
    }

    fieldEditors += addNewFieldEditorWrappedInComposite(parent = control) { parent =>
      new FileFieldEditor(P_SBT_PATH, "SBT path", parent) {
        setFileExtensions(Array("*.jar"))

        allSharedControls += getTextControl(parent)
        allSharedControls += getChangeControl(parent)
      }
    }

    fieldEditors += addNewFieldEditorWrappedInComposite(parent = control) { parent =>
      new StringFieldEditor(P_SBT_JAVA_ARGS, "SBT arguments", parent) {
        allSharedControls += getTextControl(parent)
      }
    }

    if (isGlobalPrefs) {
      
      fieldEditors += addNewFieldEditorWrappedInComposite(parent = control) { parent =>
        new BooleanFieldEditor(P_SBT_AUTOSTART, "Autostart SBT Console on Eclipse startup", parent) {
          allSharedControls += getChangeControl(parent)
        }
      }
      fieldEditors += addNewFieldEditorWrappedInComposite(parent = control) { parent =>
        new BooleanFieldEditor(P_SBT_EDITOR_SUPPORT, "Auto-add local SBT JARs for SBT project Scala build files", parent) {
          allSharedControls += getChangeControl(parent)
        }
      }
      
    }
    
    if (!isGlobalPrefs) {
      
      val enabled = getPreferenceStore.getBoolean(P_USE_PROJECT_SPECIFIC_SETTINGS)
      allSharedControls foreach { _.setEnabled(enabled) }
      
    }

    control
  }

  override def performDefaults() = {
    super.performDefaults()
    fieldEditors.foreach(_.loadDefault)
  }

  override def performOk() = {
    super.performOk()
    fieldEditors.foreach(_.store)

    val prefStore = preferenceStore(getProject)
    SbtUtils.getSbtVersion(prefStore.getString(P_SBT_PATH)) match {
      case Right(version) => prefStore.setValue(P_SBT_VERSION, version)
      case Left(e)        => eclipseLog.info("Exception while reading SBT version - check if the path to the sbt launcher jar is correct.", e)
    }

    SbtConsolePlugin.plugin.savePluginPreferences()
    true
  }

}

object Preferences {
  val PAGE_ID = "scala.tools.eclipse.sbtconsole.properties.PreferencesPage"
  val BASE = "scala.tools.eclipse.sbtconsole."
  val P_SBT_PATH = BASE + "sbtPath"
  val P_SBT_VERSION = BASE + "sbtVersion"
  val P_SBT_SCALA_VERSION = BASE + "sbtScalaVersion"
  val P_SBT_JAVA_ARGS = BASE + "sbtJavaArgs"
  val P_SBT_AUTOSTART = BASE + "sbtAutostart"
  val P_SBT_EDITOR_SUPPORT = BASE + "sbtEditorSupport"
  val P_PROJECT_DIRECTORY = BASE + "projectDir"
  val P_USE_PROJECT_SPECIFIC_SETTINGS = BASE + "useProjectSpecificSettings"

  def workspaceStore = SbtConsolePlugin.plugin.getPreferenceStore

  def projectStore(project: IProject) =
    new PropertyStore(project, workspaceStore, SbtConsolePlugin.plugin.pluginId)

  def preferenceStore = workspaceStore

  def preferenceStore(project: IProject): IPreferenceStore = {
    if (project == null)
      return workspaceStore

    val useProjectSettings = projectStore(project).getBoolean(P_USE_PROJECT_SPECIFIC_SETTINGS)

    if (useProjectSettings)
      projectStore(project)
    else
      workspaceStore
  }

  def sbtPath(project: IProject = null) = preferenceStore(project).getString(P_SBT_PATH)

  def sbtVersion(project: IProject = null) = preferenceStore(project).getString(P_SBT_VERSION)

  def sbtScalaVersion(project: IProject = null) = preferenceStore(project).getString(P_SBT_SCALA_VERSION)

  def sbtJavaArgs(project: IProject = null) = preferenceStore(project).getString(P_SBT_JAVA_ARGS)

  def sbtAutostart = workspaceStore.getBoolean(P_SBT_AUTOSTART)
  
  def sbtEditorSupport = workspaceStore.getBoolean(P_SBT_EDITOR_SUPPORT)

  def projectDirectory(project: IProject = null) = projectStore(project).getString(P_PROJECT_DIRECTORY)

}

class PreferenceInitializer extends AbstractPreferenceInitializer {

  import Preferences._

  override def initializeDefaultPreferences() {
    val store = SbtConsolePlugin.plugin.getPreferenceStore

    val sbtInfo = SbtUtils.getSbtInfo()

    store.setDefault(P_SBT_PATH, sbtInfo.path)
    store.setDefault(P_SBT_VERSION, sbtInfo.version)
    store.setDefault(P_SBT_SCALA_VERSION, sbtInfo.scalaVersion)
    store.setDefault(P_SBT_JAVA_ARGS, "-Xmx1000M")
    store.setDefault(P_SBT_AUTOSTART, false)
    store.setDefault(P_SBT_EDITOR_SUPPORT, false)
    store.setDefault(P_PROJECT_DIRECTORY, "")
  }

}