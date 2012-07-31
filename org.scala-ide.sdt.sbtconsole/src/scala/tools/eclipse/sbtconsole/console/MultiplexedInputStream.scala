package scala.tools.eclipse.sbtconsole.console

import java.io.InputStream
import scala.annotation.tailrec
import scala.tools.eclipse.logging.HasLogger

class MultiplexedInputStream(inputStreams: InputStream*)
    extends InputStream with HasLogger {

// SbtBuilder
//      {
//        new Thread() { override def run() { BasicIO.transferFully(sbtProcessIn, in) } }.start()
//        new Thread() { override def run() { BasicIO.transferFully(console.getInputStream, in) } }.start()
//      },

  @tailrec 
  final def read(): Int = {
    
    eclipseLog.info("read")
    def firstAvailableInputStream: Int = {
      var totalFinished = 0
      inputStreams foreach { is =>
        eclipseLog.info("foreach...")
        if (is.available > 0) {
          eclipseLog.info("available: " + is.available)
          return is.read()
        } else if (is.available == -1) {
          eclipseLog.info("finished")
          totalFinished += 1
        } else {
          eclipseLog.info("Wtf: " + is.available)
        }
      }
      if (totalFinished == inputStreams.size)
        return -1 
      
      -2
    }
    
    firstAvailableInputStream match {
      case -1 => -1 // all streams are EOF
      case -2 => Thread.sleep(100); read()
      case n  => n

    } 
  }    

//    import scala.actors.Futures._

//    val futures = inputStreams map { is =>
//      future {
//        is.read()
//      }
//    }
//    
//      
//    awaitAll(Long.MaxValue, futures: _*).foldLeft(0) {
//      (sum, f) => sum + f.get.asInstanceOf[Int] 
//    } 
//  }

}