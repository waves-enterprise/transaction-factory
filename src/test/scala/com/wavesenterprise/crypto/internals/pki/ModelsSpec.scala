package com.wavesenterprise.crypto.internals.pki

import com.wavesenterprise.crypto.internals.pki.Models.{CertRequestContent, CustomExtendedKeyUsage, ExtendedKeyUsage}
import org.scalatest.{FreeSpec, Matchers}
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

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
        |            extended-key-usage = ["serverAuth", "1.2.3.4"]
        |            subject-alternative-name = "DNS:welocal.dev,DNS:localhost,IP:51.210.211.61,IP:127.0.0.1",
        |        }
        |    }
        |""".stripMargin

    val conf = ConfigSource.string(confString).loadOrThrow[CertRequestContent]

    conf.extensions.extendedKeyUsage should contain theSameElementsAs List(ExtendedKeyUsage.ServerAuth, CustomExtendedKeyUsage(Array(1, 2, 3, 4)))
    conf.extensions.subjectAlternativeName.value.size shouldBe 4
  }

  "CertRequestContent should fail with empty mandatory fields" in {
    val confString =
      """
        |   {
        |        CN = ""
        |        O = "Waves Enterprise"
        |        OU = "IT Business"
        |        C = "RU"
        |        S = "Moscow"
        |        L = "Moscow"
        |        extensions {
        |            key-usage = ["digitalSignature"]
        |            extended-key-usage = ["serverAuth", "1.2.3.4"]
        |            subject-alternative-name = "DNS:welocal.dev,DNS:localhost,IP:51.210.211.61,IP:127.0.0.1",
        |        }
        |    }
        |""".stripMargin

    (the[ConfigReaderException[_]] thrownBy ConfigSource
      .string(confString)
      .loadOrThrow[CertRequestContent]).getMessage should include("Mandatory fields 'CN', 'OU', 'O', 'C', 'L', 'S' must be non-empty")
  }

}
