package com.wavesenterprise.settings

import pureconfig.ConfigReader

sealed trait CryptoSettings

object CryptoSettings {

  case object GostCryptoSettings extends CryptoSettings

  case object WavesCryptoSettings extends CryptoSettings

  implicit val configReader: ConfigReader[CryptoSettings] = ConfigReader.fromCursor { cursor =>
    for {
      objectCursor <- cursor.asObjectCursor
      typeCursor   <- objectCursor.atKey("waves-crypto")
      isWaves      <- typeCursor.asBoolean
    } yield {
      if (isWaves)
        WavesCryptoSettings
      else
        GostCryptoSettings
    }
  }
}
