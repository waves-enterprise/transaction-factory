package com.wavesenterprise.crypto.internals.pki

import com.wavesenterprise.crypto.internals.pki.Models.CertRequestContent
import org.scalatest.{FreeSpec, Matchers}
import pureconfig.ConfigSource

class ModelsSpec extends FreeSpec with Matchers {

  "CertRequestContent must be properly deserialized from config string" in {
    val confString =
      """
        |   {
        |        CN = "WE IT-tests"
        |        O = "Waves Enterprise"
        |        OU = "IT Business"
        |        C = "RU"
        |        S = "Moscow"
        |        L = "Moscow"
        |        extensions {
        |            key-usage = ["digitalSignature"]
        |            extended-key-usage = ["serverAuth"]
        |            subject-alternative-name = "DNS:welocal.dev,DNS:localhost,IP:51.210.211.61,IP:127.0.0.1",
        |        }
        |    }
        |""".stripMargin

    val conf = ConfigSource.string(confString).loadOrThrow[CertRequestContent]

    assertResult(1)(conf.extensions.extendedKeyUsage.size)
    assertResult(1)(conf.extensions.extendedKeyUsage.size)
    assertResult(4)(conf.extensions.subjectAlternativeName.value.size)

  }

}
