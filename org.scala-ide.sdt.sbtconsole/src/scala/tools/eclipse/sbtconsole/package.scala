package scala.tools.eclipse

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
package object sbtconsole {

  object SWTUtils2 {
    import org.eclipse.swt.widgets.Display

    implicit def function2runnable0(f: => Unit) = new Runnable {
      def run() { f }
    }
    implicit def function2runnable1(f: () => Unit) = new Runnable {
      def run() { f }
    }
    
    /** Run `f` on the UI thread after `after` milliseconds.  */
    def asyncExec(after: Long)(f: => Unit) {
      Executors.newSingleThreadScheduledExecutor().schedule({
        Display.getDefault.asyncExec(f)
      }, after, TimeUnit.MILLISECONDS)
    }
    
    
  }
}