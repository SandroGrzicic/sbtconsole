package scala.tools.eclipse.shellconsole

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.eclipse.swt.widgets.Display

/** Useful utility methods. */
object ThreadUtils {
  import org.eclipse.swt.widgets.Display

  implicit def function2runnable0(f: => Unit) = new Runnable {
    def run() { f }
  }
  implicit def function2runnable1(f: () => Unit) = new Runnable {
    def run() { f }
  }

  /** Executor used with asyncExec. */
  lazy val executor = Executors.newSingleThreadScheduledExecutor() 
  
  /** Run `f` on the UI thread after `after` milliseconds.  */
  def asyncExec(after: Long)(f: => Unit) {
    executor.schedule({
      Display.getDefault.asyncExec(f)
    }, after, TimeUnit.MILLISECONDS)
  }

  /**
   * Sleeps the current thread while the condition is true
   * or the `timeout` (in ms) elapses.
   * `sleepSegments` controls how often will the condition be checked
   * (more segments = more checks).
   */
  def sleepWhile(timeout: Long, sleepSegments: Int = 20)(condition: => Boolean) {
    if (timeout < 0) {
      throw new IllegalArgumentException("Timeout must be nonnegative!")
    } else if (sleepSegments <= 0) {
      throw new IllegalArgumentException("SleepSegments must be positive!")
    }
    var steps = 0
    val sleepTime = timeout / sleepSegments
    val endTime = System.currentTimeMillis + timeout
    while (condition && steps < sleepSegments && System.currentTimeMillis < endTime) {
      Thread.sleep(sleepTime)
      steps += 1
    }
  }
}
