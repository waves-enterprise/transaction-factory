package com.wavesplatform
import com.wavesplatform.utils.NTP
import org.scalatest.{BeforeAndAfterAll, Suite}

trait NTPTime extends BeforeAndAfterAll { _: Suite =>
  protected val ntpTime = new NTP(Seq("pool.ntp.org"))(monix.execution.Scheduler.global)

  override protected def afterAll(): Unit = {
    super.afterAll()
    ntpTime.close()
  }
}
