package com.wavesenterprise.crypto.settings

import com.wavesenterprise.settings.CryptoSettings
import com.wavesenterprise.settings.CryptoSettings.GostCryptoSettings
import com.wavesenterprise.settings.PkiCryptoSettings.{DisabledPkiSettings, EnabledPkiSettings}
import org.scalatest.{FreeSpec, Matchers}
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

class CryptoSettingsSpec extends FreeSpec with Matchers {
  "should read crypto config" in {
    val config = ConfigSource.string {
      """
        |crypto {
        |  type = GOST
        |  pki {
        |    mode = ON
        |    required-oids = ["1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0"]
        |  }
        |}
        |""".stripMargin
    }

    config.loadOrThrow[CryptoSettings] shouldBe GostCryptoSettings(EnabledPkiSettings(Set("1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0")))
  }

  "should ignore crypto.type when waves-crypto is used" in {
    val config = ConfigSource.string {
      """
        |waves-crypto = no
        |crypto {
        |  type = WAVES
        |  pki {
        |    mode = ON
        |    required-oids = ["1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0"]
        |  }
        |}
        |""".stripMargin
    }

    config.loadOrThrow[CryptoSettings] shouldBe GostCryptoSettings(EnabledPkiSettings(Set("1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0")))
  }

  "should ignore required-oids when pki is disabled" in {
    val config = ConfigSource.string {
      """
        |crypto {
        |  type = GOST
        |  pki {
        |    mode = OFF
        |    required-oids = ["1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0"]
        |  }
        |}
        |""".stripMargin
    }

    config.loadOrThrow[CryptoSettings] shouldBe GostCryptoSettings(DisabledPkiSettings)
  }

  "returns exception on enabled PKI with Waves crypto" in {
    val config = ConfigSource.string {
      """
        |crypto {
        |  type = WAVES
        |  pki {
        |    mode = ON
        |    required-oids = ["1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0"]
        |  }
        |}
        |""".stripMargin
    }

    (the[ConfigReaderException[_]] thrownBy {
      config.loadOrThrow[CryptoSettings]
    }).getMessage should include {
      "Usage of 'node.crypto.pki = ON | TEST' is forbidden for 'node.crypto.type = WAVES'"
    }

    val testConfig = ConfigSource.string {
      """
        |crypto {
        |  type = WAVES
        |  pki {
        |    mode = TEST
        |    required-oids = ["1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0"]
        |  }
        |}
        |""".stripMargin
    }

    (the[ConfigReaderException[_]] thrownBy {
      testConfig.loadOrThrow[CryptoSettings]
    }).getMessage should include {
      "Usage of 'node.crypto.pki = ON | TEST' is forbidden for 'node.crypto.type = WAVES'"
    }
  }

  "returns exception on unexpected 'pki.mode' value" in {
    val config = ConfigSource.string {
      """
        |crypto {
        |  type = GOST
        |  pki {
        |    mode = ENABLED
        |    required-oids = ["1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0"]
        |  }
        |}
        |""".stripMargin
    }

    (the[ConfigReaderException[_]] thrownBy {
      config.loadOrThrow[CryptoSettings]
    }).getMessage should include {
      "Cannot convert 'ENABLED' to PkiMode: possible values are: [OFF,ON,TEST]"
    }
  }

  "returns exception on unexpected 'crypto.type' value" in {
    val config = ConfigSource.string {
      """
        |crypto {
        |  type = AES
        |  pki {
        |    mode = ENABLED
        |    required-oids = ["1.2.3.4.5.6.7.8.9", "192.168.0.1.255.255.255.0"]
        |  }
        |}
        |""".stripMargin
    }

    (the[ConfigReaderException[_]] thrownBy {
      config.loadOrThrow[CryptoSettings]
    }).getMessage should include {
      "Cannot convert 'AES' to CryptoType: possible values are: [WAVES,GOST]"
    }
  }

  "returns exception on wrong OID format" in {
    val config = ConfigSource.string {
      """
        |crypto {
        |  type = GOST
        |  pki {
        |    mode = ON
        |    required-oids = ["1a", "2b"]
        |  }
        |}
        |""".stripMargin
    }

    (the[ConfigReaderException[_]] thrownBy {
      config.loadOrThrow[CryptoSettings]
    }).getMessage should include {
      "Unable to parse the configuration: Wrong OID format in configuration field 'crypto.pki.required-oids': 1a"
    }
  }
}
