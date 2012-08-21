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
import org.eclipse.swt.SWTException
import scala.sys.process.BasicIO

/**
 * ShellConsole KeyListener.
 *
 * Listens to key and traverse events in order to support tab completion and console history.
 *
 */
class ShellConsoleKeyListener(console: ShellConsole, page: ShellConsolePage)
    extends TraverseListener
    with KeyListener
    with HasLogger {

  /** The symbol which represents a new line in the process output. */
  var newlineSymbol = '\r'
    
  /** Holds user input (history). */
  var history = IndexedSeq[String]()
  /** Current line (position) in history. */
  var historyLine = 0

  private val currentLine = StringBuilder.newBuilder

  private def getCurrentLineInfo: IRegion =
    document.getLineInformation(document.getNumberOfLines - 1)

  private var transferThread = console.newProcessToConsoleTransferThread()

  private lazy val document = page.getViewer.getDocument
  private lazy val textWidget = page.getViewer.getTextWidget

  /** Move the caret to the end of the console. */
  def moveCaretToEnd() {
    if (textWidget != null && !textWidget.isDisposed()) {
      textWidget.setCaretOffset(Int.MaxValue)
    }
  }

  /** Move the caret to the end of the console after `after` milliseconds. */
  def moveCaretToEndAsync(after: Long = 200) {
    ThreadUtils.asyncExec(after) {
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

      case SWT.ARROW_LEFT =>
        val lineInfo = getCurrentLineInfo
        if (textWidget.getCaretOffset < lineInfo.getOffset + 3) {
          // disallow moving before the location of the command line prompt
          disableDefaultAction(e)
        }
      case SWT.ARROW_RIGHT =>
        val lineInfo = getCurrentLineInfo
        if (textWidget.getCaretOffset < lineInfo.getOffset + 2) {
          // caret is behind the location of the command line prompt
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
        disableDefaultAction(e)
        executeCurrentCommand()
        moveCaretToEnd()

      case _ => // ignored
    }
  }

  /** Executes the command in the current line, adds it to history and clears the current line. */
  def executeCurrentCommand() {
    
    val line = currentLine.mkString

    console.processWriter.write(line + "\n")
    console.processWriter.flush()
    
    replaceCurrentLineWith("")
    
    addLineToHistory(line)
    
    currentLine.clear()
  }

  /** Autocompletes the current input. */
  def completeCurrentInput() {
    val lineInfo = getCurrentLineInfo
    val line = currentLine.mkString

    try {
      // start buffering process output
      transferThread.writeTarget = BufferedTransferThread.Buffer

      // send the current line to the process for autocompletion
      console.processWriter.write(line + "\t")
      console.processWriter.flush()

      // wait for the process to print out the completion
      // because it's not possible to find out the end of the completion
      Thread.sleep(200)
      // make sure we actually get some output from the process
      ThreadUtils.sleepWhile(timeout = 1500, sleepSegments = 5) {
        transferThread.contentBuffer.length == 0
      }
      if (transferThread.contentBuffer.length == 0) {
        eclipseLog.warn("SBT Console: SBT took a very long time to autocomplete input; please try again.")
        transferThread.writeTarget = BufferedTransferThread.Output
        return
      }
      
      val contentBuffer = transferThread.contentBuffer.toString
      
      if (contentBuffer.count(newlineSymbol ==) == 1) {
        // single completion - complete current line
        val completion = contentBuffer.dropWhile(newlineSymbol !=).drop(3)

        // erase completion from process shell
        val backspaces = Array.fill(completion.length)('\b')
        console.processWriter.write(backspaces)
        console.processWriter.flush()
        replaceCurrentLineWith(completion)
        
        // clear content buffer and resume outputting process output to console
        transferThread.contentBuffer.setLength(0)
        transferThread.writeTarget = BufferedTransferThread.Output
        
        moveCaretToEnd()
        
      } else {
        // multiple completions - transfer them to the console

        // erase completion from process shell
        val backspaces = Array.fill(line.length)('\b')
        console.processWriter.write(backspaces)
        console.processWriter.flush()
        replaceCurrentLineWith("")
        
        // remove last line from output by using the copyToOutput limit
        val contentBufferCopyLimit = contentBuffer.lastIndexOf(newlineSymbol) + 4
        // copy the current content buffer to the console and clear it
        transferThread.copyToOutput(contentBufferCopyLimit, clearBuffer = true)
        // resume outputting process output to console
        transferThread.writeTarget = BufferedTransferThread.Output

        // wait until the console has been properly filled with the output
        ThreadUtils.asyncExec(200) {
          // set the current line to be the previously typed line
          replaceCurrentLineWith(line)
          moveCaretToEnd()
        }
      }
    } catch {
      case e: Throwable =>
        logger.warn("Exception while attempting autocomplete of " + line + ": e.getMessage", e)
    }
  }

  def addLineToHistory(currentLine: String) {
    history :+= currentLine
    historyLine = history.length
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

    try {
      document.replace(lineInfo.getOffset + 2, lineInfo.getLength - 2, contents)
    } catch {
      case e: Throwable =>
        logger.warn("Exception while replacing current line with " + contents + " - " + e.getMessage, e)
    }
    currentLine.replace(0, currentLine.length, contents)
    moveCaretToEnd()
  }

  def keyPressed(e: KeyEvent) {
    if (e.keyCode == SWT.BS) {
      if (currentLine.length > 0) {
        currentLine.length -= 1
      }
    } else if (e.keyCode < 65535) {
      currentLine += e.character
    }
  }

  def keyReleased(e: KeyEvent) {}

}