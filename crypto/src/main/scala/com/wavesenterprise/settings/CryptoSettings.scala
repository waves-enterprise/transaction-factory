package com.wavesenterprise.settings

import cats.Show
import cats.implicits._
import com.wavesenterprise.settings.PkiCryptoSettings.{DisabledPkiSettings, EnabledPkiSettings, TestPkiSettings}
import com.wavesenterprise.utils.ScorexLogging
import enumeratum.EnumEntry
import enumeratum.EnumEntry.Uppercase
import pureconfig.error.{CannotConvert, CannotParse, ConfigReaderFailures, ThrowableFailure}
import pureconfig.{ConfigObjectCursor, ConfigReader}

import scala.collection.immutable

sealed trait CryptoSettings

object CryptoSettings extends ScorexLogging {

  case class GostCryptoSettings(pkiSettings: PkiCryptoSettings) extends CryptoSettings

  object GostCryptoSettings {
    def apply(): GostCryptoSettings = GostCryptoSettings(DisabledPkiSettings)
  }

  case object WavesCryptoSettings extends CryptoSettings

  implicit val configReader: ConfigReader[CryptoSettings] = ConfigReader.fromCursor { cursor =>
    for {
      objectCursor <- cursor.asObjectCursor
      deprecatedCursor = objectCursor.atKeyOrUndefined("waves-crypto")
      cryptoSettings <- if (deprecatedCursor.isUndefined) {
        for {
          cryptoCursor <- objectCursor.atKey("crypto").flatMap(_.asObjectCursor)
          cryptoType   <- cryptoCursor.atKey("type").flatMap(CryptoType.configReader.from)
          settings <- cryptoType match {
            case CryptoType.WAVES => validatePkiConfigForWaves(cryptoCursor).map(_ => WavesCryptoSettings)
            case CryptoType.GOST  => parseGostCryptoSettings(cryptoCursor)
          }
        } yield settings
      } else {
        log.warn {
          s"Usage of 'node.waves-crypto = yes | no' is deprecated since v1.9.0, use 'node.crypto.type = WAVES | GOST' instead. Refer to the docs for additional info: https://docs.wavesenterprise.com/en/latest/changelog.html"
        }
        deprecatedCursor.asBoolean.flatMap { isWavesCrypto =>
          val maybeCryptoCursor = objectCursor.atKeyOrUndefined("crypto")
          if (isWavesCrypto) {
            if (maybeCryptoCursor.isUndefined) {
              Right(WavesCryptoSettings)
            } else {
              maybeCryptoCursor.asObjectCursor.flatMap(validatePkiConfigForWaves).map(_ => WavesCryptoSettings)
            }
          } else {
            if (maybeCryptoCursor.isUndefined) {
              Right(GostCryptoSettings(DisabledPkiSettings))
            } else {
              maybeCryptoCursor.asObjectCursor.flatMap(parseGostCryptoSettings)
            }
          }
        }
      }
    } yield cryptoSettings
  }

  implicit val toPrintable: Show[CryptoSettings] = {
    case WavesCryptoSettings => "type: WAVES"
    case GostCryptoSettings(pkiSettings) =>
      s"""
         |type: GOST
         |pki: 
         |  ${show"$pkiSettings".replace("\n", "\n--")}
         """.stripMargin
  }

  private def validatePkiConfigForWaves(cryptoCursor: ConfigObjectCursor) = {
    val maybePkiCursor = cryptoCursor.atKeyOrUndefined("pki")
    if (maybePkiCursor.isUndefined) {
      Right(())
    } else {
      for {
        pkiCursor <- maybePkiCursor.asObjectCursor
        pkiMode   <- pkiCursor.atKey("mode").flatMap(PkiMode.configReader.from)
        _ <- pkiMode match {
          case PkiMode.OFF => Right(())
          case PkiMode.ON | PkiMode.TEST =>
            Left(ConfigReaderFailures {
              ThrowableFailure(new IllegalStateException("Usage of 'node.crypto.pki = ON | TEST' is forbidden for 'node.crypto.type = WAVES'"), None)
            })
        }
      } yield ()
    }
  }

  private def parseGostCryptoSettings(cryptoCursor: ConfigObjectCursor) = {
    for {
      pkiCursor <- cryptoCursor.atKey("pki").flatMap(_.asObjectCursor)
      pkiMode   <- pkiCursor.atKey("mode").flatMap(PkiMode.configReader.from)
      pkiSettings <- pkiMode match {
        case PkiMode.OFF => Right(DisabledPkiSettings)
        case PkiMode.ON  => parseRequiredOIds(pkiCursor).map(EnabledPkiSettings)
        case PkiMode.TEST =>
          parseRequiredOIds(pkiCursor).map { requiredOIds =>
            log.warn("WARNING: 'node.crypto.pki.mode' is set to 'TEST'. PKI functionality is running in a testing mode.")
            TestPkiSettings(requiredOIds)
          }
      }
    } yield GostCryptoSettings(pkiSettings)
  }

  private def parseRequiredOIds(cursor: ConfigObjectCursor) = {
    val oidReg = """^\d+(?:\.\d+)*$"""
    cursor
      .atKey("required-oids")
      .flatMap { requiredOIdsCursor =>
        requiredOIdsCursor.asList.flatMap { listCursor =>
          listCursor
            .traverse(_.asString.flatMap { oid =>
              Either.cond(
                oid matches oidReg,
                oid,
                ConfigReaderFailures(CannotParse(s"Wrong OID format in configuration field 'crypto.pki.required-oids': $oid", None))
              )
            })
            .map(_.toSet)
        }
      }
  }

}

sealed trait PkiCryptoSettings

object PkiCryptoSettings {
  case object DisabledPkiSettings                          extends PkiCryptoSettings
  case class EnabledPkiSettings(requiredOIds: Set[String]) extends PkiCryptoSettings
  case class TestPkiSettings(requiredOIds: Set[String])    extends PkiCryptoSettings

  implicit val toPrintable: Show[PkiCryptoSettings] = {
    case DisabledPkiSettings => "mode: OFF"
    case EnabledPkiSettings(requiredOIds) =>
      s"""
         |mode: ON
         |requiredOIds: [${requiredOIds.mkString(", ")}]
       """.stripMargin
    case TestPkiSettings(requiredOIds) =>
      s"""
         |mode: TEST
         |requiredOIds: [${requiredOIds.mkString(", ")}]
       """.stripMargin
  }
}

sealed trait CryptoType extends EnumEntry with Uppercase

object CryptoType extends enumeratum.Enum[CryptoType] {

  case object WAVES extends CryptoType
  case object GOST  extends CryptoType

  override val values: immutable.IndexedSeq[CryptoType] = findValues

  val configReader: ConfigReader[CryptoType] = ConfigReader.fromString { str =>
    fromStr(str).toRight(CannotConvert(str, classOf[CryptoType].getSimpleName, s"possible values are: [${values.mkString(",")}]"))
  }

  def fromStr(str: String): Option[CryptoType] = withNameInsensitiveOption(str)
}

sealed trait PkiMode extends EnumEntry with Uppercase

object PkiMode extends enumeratum.Enum[PkiMode] {
  case object OFF  extends PkiMode
  case object ON   extends PkiMode
  case object TEST extends PkiMode

  override val values: immutable.IndexedSeq[PkiMode] = findValues

  val configReader: ConfigReader[PkiMode] = ConfigReader.fromString { str =>
    fromStr(str).toRight(CannotConvert(str, classOf[PkiMode].getSimpleName, s"possible values are: [${values.mkString(",")}]"))
  }

  def fromStr(str: String): Option[PkiMode] = withNameInsensitiveOption(str)
}
