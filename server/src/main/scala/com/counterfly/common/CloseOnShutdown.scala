package com.counterfly.common

import com.twitter.app.App
import com.twitter.conversions.DurationOps._
import com.twitter.util.Await
import com.twitter.util.Closable
import com.twitter.util.Future
import com.twitter.util.Return
import com.twitter.util.Throw

trait CloseOnShutdown { self: App =>
  val shutdownSleepDuration = 2.seconds
  @volatile private[this] var closedCalled = false
  runOnExit(() => {
    closedCalled = true
  })

  {
    val closable = Closable.make { t =>
      // If close has already been called, do nothing, otherwise sleep requested amount of time before actually closing
      val sleep =
        if (closedCalled) Future.Done
        else Future.sleep(shutdownSleepDuration)(self.shutdownTimer)
      sleep before self.close(t)
    }

    // Shutdown
    sys.addShutdownHook {
      val closeGracePeriod = defaultCloseGracePeriod

      // TODO: convert to logger
      println(
        s"[info] Shutdown hook invoked. Closing $name with grace period of $closeGracePeriod",
      )

      // NOTE: this uses `.liftToTry.map` instead of `.respond` to ensure that the side-effect is evaluated before the future satisfied.
      val closing = closable
        .close(closeGracePeriod)
        .liftToTry
        .map {
          case Return(_) =>
            // TODO: convert to logger
            println(s"[info] $name was closed successfully")
          case Throw(t) =>
            // TODO: convert to logger
            println(
              s"[warn] $name threw an error during shutdown. Some resources may not be managed correctly. $t",
            )
            throw t
        }

      Await.result(closing, closeGracePeriod)
    }
  }
}
