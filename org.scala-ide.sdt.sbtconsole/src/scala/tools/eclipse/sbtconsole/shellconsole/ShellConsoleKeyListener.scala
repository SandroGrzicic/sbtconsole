package scala.tools.eclipse.sbtconsole.shellconsole

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

  /** Keeps user input (history). */
  var history = IndexedSeq[String]()
  /** Current line (position) in history. */
  var historyLine = 0

  def document = page.getViewer.getDocument

  def getCurrentLine = {
    val doc = page.getViewer.getDocument
    doc.getLineInformation(doc.getNumberOfLines - 1)
  }

  def moveCaretToEnd() {
    page.getViewer.getTextWidget.setCaretOffset(Int.MaxValue)
//    eclipseLog.info("moving caret from " + page.getViewer.getTextWidget.getCaretOffset + " to " + page.getViewer.getBottomIndexEndOffset + " - docsize: " + page.getViewer.getDocument.getLength)
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
        val lineInfo = document.getLineInformation(document.getNumberOfLines - 1)
        if (page.getViewer.getTextWidget.getCaretOffset < lineInfo.getOffset + 3) {
          disableDefaultAction(e)
        }

      case SWT.ARROW_UP =>
        disableDefaultAction(e)
        showPrevHistoryInput()

      case SWT.ARROW_DOWN =>
        disableDefaultAction(e)
        showNextHistoryInput()

      case SWT.CR | SWT.KEYPAD_CR =>
        addCurrentLineToHistory()
        moveCaretToEnd()
//      case SWT.BS
      case _ => // ignored
    }
  }

  def completeCurrentInput() {
    val lineInfo = getCurrentLine
    val typedText = document.get(lineInfo.getOffset + 2, lineInfo.getLength - 2)

    if (lineInfo != null) {
      document.replace(lineInfo.getOffset + 2, lineInfo.getLength - 2, "")
      console.getInputStream.appendData(typedText + "\t")
      moveCaretToEnd()
    }
  }

  def addCurrentLineToHistory() {
    val lineInfo = getCurrentLine
    try {
      val userInput = document.get(lineInfo.getOffset + 2, lineInfo.getLength - 2)
      history :+= userInput
      historyLine = history.length
    } catch {
      case e: BadLocationException => //eclipseLog.info("Bad location while fetching current line", e)
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
    val lineInfo = getCurrentLine
    val userInput = document.get(lineInfo.getOffset + 2, lineInfo.getLength - 2)

    document.replace(lineInfo.getOffset + 2, lineInfo.getLength - 2, contents)
    moveCaretToEnd()
  }

  def keyReleased(e: KeyEvent) {
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