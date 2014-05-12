package com.giltgroupe.util.time

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.util
import scala.collection.JavaConverters._
import util.concurrent.atomic.{AtomicLong, AtomicInteger}


/**
 * Confirms that a bunch of threads hitting the MonotonicClock concurrently never see the same value.
 *
 * @author Eric Bowman
 * @since 11/20/12 10:03 PM
 */
class MonoticClockTest extends FunSuite with ShouldMatchers {

  test("Clock never returns the same value twice") {
    val seen = new util.concurrent.CopyOnWriteArraySet[Long]().asScala
    (1 to 10000).par.foreach { _ =>
      val now = MonotonicClock.currentTimeNanos()
      seen should not contain(now)
      seen.add(now)
    }
  }

  test("still increases even with leap seconds") {
    val clock = new MonotonicClock {
      val incr = new AtomicInteger(0)
      val clock = new AtomicLong(0l)
      override def millisecondClock(): Long = {
        if ((incr.incrementAndGet() % 5) == 0) {
          clock.get() - 1
        } else {
          clock.incrementAndGet()
        }
      }
    }
    var prev = clock.currentTimeNanos()
    (1 to 100000) foreach { _ =>
      val now = clock.currentTimeNanos()
      assert(now > prev)
      prev = now
    }
  }

  test("Clock blows exception if it cannot move forward") {
    val clock = new MonotonicClock {
      override def millisecondClock(): Long = 0
    }
    (1 to 999999) foreach { _ => clock.currentTimeNanos() }
    evaluating {
      clock.currentTimeNanos()
    } should produce[AssertionError]
  }
}