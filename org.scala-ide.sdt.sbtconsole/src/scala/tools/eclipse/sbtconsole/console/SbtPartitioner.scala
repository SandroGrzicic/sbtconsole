package scala.tools.eclipse.sbtconsole.console

import org.eclipse.ui.internal.console.IOConsolePartitioner
import org.eclipse.swt.widgets.Display
import org.eclipse.ui.internal.console.IOConsolePartition
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.SWT
import org.eclipse.jface.text.BadLocationException
import scala.util.matching.Regex
import org.eclipse.swt.graphics.Color
import scala.tools.eclipse.sbtconsole.shellconsole.ShellConsolePartitioner

class SbtPartitioner(console: SbtConsole) extends ShellConsolePartitioner(console) {
  
  lazy final val fgColor    = Display.getDefault.getSystemColor(SWT.COLOR_BLACK)
  lazy final val bgColor    = Display.getDefault.getSystemColor(SWT.COLOR_WHITE)
  lazy final val infoColor  = Display.getDefault.getSystemColor(SWT.COLOR_DARK_MAGENTA)
  lazy final val errorColor = Display.getDefault.getSystemColor(SWT.COLOR_RED)
  lazy final val successColor = Display.getDefault.getSystemColor(SWT.COLOR_DARK_GREEN)
  
  lazy final val defaultStyle = (fgColor, bgColor)

  import SWT._
  /**
   *  Map ANSI control numbers to SWT color codes. It maps the
   *  foreground numer, add 10 for background
   *  
   *  @see http://www.termsys.demon.co.uk/vtansi.htm
   */
  lazy val ansiColors = Map(
      30 -> COLOR_BLACK,
      31 -> COLOR_RED,
      32 -> COLOR_GREEN,
      33 -> COLOR_YELLOW,
      34 -> COLOR_BLUE,
      35 -> COLOR_MAGENTA,
      36 -> COLOR_CYAN,
      37 -> COLOR_WHITE).withDefaultValue(fgColor)
  
  def getStyles(text: String): List[(Color, Color)] = {
    var fg = fgColor
    var bg = bgColor
    var start = 0
    var length = 0
    
//    var pattern = """[(\d\d?)(;\d\d?)*m""".r
//    
//    for (m <- pattern.findAllIn(text)) match {
//      case Groups(attr1, rest:_*) =>
//        length = 
//        attr1.toInt 
//    }
//    
    Nil
  }

  override def getStyleRanges(off: Int, length: Int): Array[StyleRange] = try {
    val part: IOConsolePartition = getPartition(off).asInstanceOf[IOConsolePartition];
    val offset = part.getOffset()
    val text = getDocument().get(offset, part.getLength)

    def stylePattern(regex: Regex, color: Color): List[StyleRange] = 
      for (m <- regex.findAllIn(text).matchData.toList) yield
        new StyleRange(offset + m.start, m.matched.length, color, bgColor)

    if (text ne null) {
      val superStyles = super.getStyleRanges(off, length)
      val styles = 
        stylePattern("""\[error\]""".r, errorColor) ++ 
        stylePattern("""\[info\]""".r, infoColor) ++
        stylePattern("""\[success\]""".r, successColor)
  
      superStyles ++ styles.sortBy(_.start)
    } else new Array[StyleRange](0)
  } catch {
    case e: BadLocationException => 
      // the user has cleared the console
      super.getStyleRanges(off, length)
  }
  
}