package com.wavesenterprise.crypto.internals.gost

import enumeratum.values.{IntEnum, IntEnumEntry}
import ru.CryptoPro.CAdES.CAdESParameters

sealed abstract class SignatureType(val value: Int, val jcpTypeId: Int) extends IntEnumEntry

/**
  * Supported algorithms, according to [[https://confluence.wavesplatform.com/pages/viewpage.action?pageId=1640862119]]:
  *   CAdES-BES
  *   CAdES-X Long Type 1
  *   CAdES-T
  */
object SignatureType extends IntEnum[SignatureType] {

  val values = findValues

  case object `CAdES-BES`           extends SignatureType(1, CAdESParameters.CAdES_BES)
  case object `CAdES-X Long Type 1` extends SignatureType(2, CAdESParameters.CAdES_X_Long_Type_1)
  case object `CAdES-T`             extends SignatureType(3, CAdESParameters.CAdES_T)

  /**
    * Currently not supported by JCP 2.0.39014
    */
  /*
  case object `CAdES-C`             extends SignatureType(4, CAdESParameters.CAdES_C)
  case object `PKCS7`               extends SignatureType(5, CAdESParameters.PKCS7)
 */
}
