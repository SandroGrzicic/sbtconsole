package scala.tools.eclipse.shellconsole

import org.eclipse.swt.events.TraverseListener
import org.eclipse.swt.events.TraverseEvent
import org.eclipse.swt.SWT
import scala.tools.eclipse.logging.HasLogger
import org.eclipse.ui.console.IOConsole
import org.eclipse.ui.internal.console.IOConsolePage
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.KeyEvent
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.TextEvent
import org.eclipse.jface.text.ITextListener
import org.eclipse.jface.text.IRegion
import scala.tools.eclipse.sbtconsole.SWTUtils2
import org.eclipse.swt.SWTException

/**
 * ShellConsole KeyListener.
 *
 * Listens to traverse events in order to support tab completion and console history.
 *
 */
class ShellConsoleKeyListener(console: IOConsole, page: IOConsolePage)
    extends TraverseListener
    with KeyListener
    with HasLogger {

  /** Holds user input (history). */
  var history = IndexedSeq[String]()
  /** Current line (position) in history. */
  var historyLine = 0

  private lazy val document = page.getViewer.getDocument
  private lazy val textWidget = page.getViewer.getTextWidget

  private def getCurrentLineInfo: IRegion =
    document.getLineInformation(document.getNumberOfLines - 1)

  private def getCurrentLineText: String =
    getCurrentLineText(getCurrentLineInfo)

  private def getCurrentLineText(lineInfo: IRegion): String =
    document.get(lineInfo.getOffset + 2, lineInfo.getLength - 2)

  /** Move the caret to the end of the console. */
  def moveCaretToEnd() {
    if (textWidget != null && !textWidget.isDisposed()) {
      textWidget.setCaretOffset(Int.MaxValue)
    }
  }

  /** Move the caret to the end of the console after `after` milliseconds. */
  def moveCaretToEndAsync(after: Long = 200) {
    SWTUtils2.asyncExec(after) {
      if (page != null) {
        moveCaretToEnd()
      }
    }
  }

  def keyTraversed(e: TraverseEvent) {

    def disableDefaultAction(e: TraverseEvent) {
      e.detail = SWT.TRAVERSE_NONE
      e.doit = true
    }

    e.keyCode match {
      case SWT.TAB =>
        disableDefaultAction(e)
        completeCurrentInput()

      case SWT.ARROW_LEFT => // disallow moving before the location of the command line prompt
        val lineInfo = getCurrentLineInfo
        if (textWidget.getCaretOffset < lineInfo.getOffset + 3) {
          disableDefaultAction(e)
        }
      case SWT.ARROW_RIGHT => // in case caret is behind the location of the command line prompt
        val lineInfo = getCurrentLineInfo
        if (textWidget.getCaretOffset < lineInfo.getOffset + 2) {
          disableDefaultAction(e)
          textWidget.setCaretOffset(lineInfo.getOffset + 2)
        }

      case SWT.ARROW_UP =>
        disableDefaultAction(e)
        showPrevHistoryInput()

      case SWT.ARROW_DOWN =>
        disableDefaultAction(e)
        showNextHistoryInput()

      case SWT.CR | SWT.KEYPAD_CR =>
        // intercepted for cleaner input; only accept characters at the current line 
        // instead of anywhere inside the console
        disableDefaultAction(e)

        executeCurrentCommand()
        moveCaretToEnd()
        
      case _ => // ignored
    }
  }

  /** Executes the command in the current line and adds it to history. */
  def executeCurrentCommand() {
    val currentLine = getCurrentLineText
    try {
      replaceCurrentLineWith("")
      console.getInputStream.appendData(currentLine + "\n")
    } catch {
      case _ =>
//        lastCompletedLine match {
//          case Some(line) if line.length > 0 && line.length > currentLine.length =>
//            console.getInputStream.appendData(currentLine.substring(line.length) + "\n")
//          case _ =>
        console.getInputStream.appendData("\n")
//        }
//        lastCompletedLine = None
    }

    addLineToHistory(currentLine)
  }

  var lastCompletedLine: Option[String] = None

  /** Autocompletes the current input. */
  def completeCurrentInput() {
    val lineInfo = getCurrentLineInfo
    val typedText = getCurrentLineText(lineInfo)

    //    eclipseLog.info(typedText)
    try {
      //      eclipseLog.info(document.get())
      document.replace(lineInfo.getOffset + 2, lineInfo.getLength - 2, "")
      console.getInputStream.appendData(typedText + "\t")
      lastCompletedLine = Some(typedText)
    } catch {
//      case e @ (_: NullPointerException | _: BadLocationException) =>
//        eclipseLog.info(typedText + " - (" + lineInfo.getOffset + ", " + lineInfo.getLength + ") - " + e.getMessage(), e)
//        lastCompletedLine match {
//          case Some(line) if line.length > 0 && line.length > typedText.length =>
//            var typedTextDifference = typedText.substring(line.length)
//            console.getInputStream.appendData(typedTextDifference + "\t")
//            lastCompletedLine = null
//          case _ =>
//        }
      case _ =>
//        try {
//          eclipseLog.info(getCurrentLineText(lineInfo))
//          document.replace(lineInfo.getOffset + 2, lineInfo.getLength - 2, "")
//        } catch {
//          case e =>
//            eclipseLog.info(typedText + " - (" + lineInfo.getOffset + ", " + lineInfo.getLength + ") - " + e.getMessage(), e)
//            eclipseLog.info(document.getLineLength(document.getNumberOfLines() - 1) + " - " + document.getLineLength(document.getNumberOfLines() - 2))
//        }
        console.getInputStream.appendData("\t")
    }
    moveCaretToEndAsync()
  }

  def addLineToHistory(currentLine: String = getCurrentLineText) {
    try {
      history :+= currentLine
      historyLine = history.length
    } catch {
      case e: BadLocationException => logger.error("Bad location while fetching current line", e)
    }
  }

  def showNextHistoryInput() {
    if (historyLine < history.length) {
      historyLine += 1
      if (historyLine == history.length) {
        replaceCurrentLineWith("")
      } else {
        replaceCurrentLineWith(history(historyLine))
      }
    }
  }

  def showPrevHistoryInput() {
    if (historyLine > 0) {
      historyLine -= 1
      replaceCurrentLineWith(history(historyLine))
    }
  }

  def replaceCurrentLineWith(contents: String) {
    val lineInfo = getCurrentLineInfo

    document.replace(lineInfo.getOffset + 2, lineInfo.getLength - 2, contents)
    moveCaretToEnd()
  }

  def keyReleased(e: KeyEvent) {
    // TODO: proper backspace support for autocomplete

    //    e.keyCode match {
    //      case SWT.CR | SWT.KEYPAD_CR =>
    //        moveCaretToEnd()
    //      case SWT.BS =>
    //        val lineInfo = getCurrentLine
    //        try {
    //          val userInput = document.get(lineInfo.getOffset + 2, lineInfo.getLength - 3)
    //          if (lineInfo.getOffset >= 0) {
    //            document.replace(lineInfo.getOffset + 2, lineInfo.getLength - 2, userInput)
    //          }
    //        } catch {
    //          case e: BadLocationException            => eclipseLog.info("Bad location while backspacing", e)
    //          case e: StringIndexOutOfBoundsException => eclipseLog.info("Bad index while backspacing", e)  
    //        }
    //      case _ => // ignored
    //    }
  }

  def keyPressed(e: KeyEvent) {}

}