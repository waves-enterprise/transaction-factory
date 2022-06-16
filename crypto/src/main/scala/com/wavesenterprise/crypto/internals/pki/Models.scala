package com.wavesenterprise.crypto.internals.pki

import cats.data.{NonEmptyList, Validated}
import cats.implicits._
import com.wavesenterprise.crypto.internals.{CryptoError, PKIError}
import enumeratum.EnumEntry.{Uncapitalised, Uppercase}
import enumeratum.{Enum, EnumEntry}
import pureconfig.ConfigReader.Result
import pureconfig._
import pureconfig.error.{CannotConvert, ConfigReaderFailures}
import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader

import scala.collection.immutable

object Models {

  case class CertRequestContent(
      commonName: String, // CN
      organizationalUnit: String, // OU
      organization: String, // O
      country: String, // C
      stateOrProvince: String, // S
      locality: String, // L
      extensions: Extensions // X.509 v3 Extensions
  )

  object CertRequestContent {
    import pureconfig.generic.auto._

    implicit val productHint: ProductHint[CertRequestContent] = ProductHint[CertRequestContent] {
      case "commonName"         => "CN"
      case "organizationalUnit" => "OU"
      case "organization"       => "O"
      case "country"            => "C"
      case "locality"           => "L"
      case "stateOrProvince"    => "S"
      case "extensions"         => "extensions"
    }

    val baseConfigReader: ConfigReader[CertRequestContent] = deriveReader

    implicit val configReader: ConfigReader[CertRequestContent] = ConfigReader.fromCursor { cur =>
      def withValidation(conf: CertRequestContent): Result[CertRequestContent] = {
        val mustBeNonEmptyStrs = Seq(conf.stateOrProvince, conf.country, conf.locality, conf.commonName, conf.organization, conf.organizationalUnit)

        if (mustBeNonEmptyStrs.forall(!_.isBlank)) {
          Right(conf)
        } else {
          Left(ConfigReaderFailures(pureconfig.error.CannotParse("Mandatory fields 'CN', 'OU', 'O', 'C', 'L', 'S' must be non-empty", None)))
        }
      }

      baseConfigReader.from(cur).flatMap(withValidation)
    }
  }

  case class Extensions(
      keyUsage: List[KeyUsage],
      extendedKeyUsage: List[ExtendedKeyUsage] = List.empty,
      subjectAlternativeName: SubjectAlternativeName = SubjectAlternativeName.empty
  )

  sealed abstract class KeyUsage(val id: Int) extends EnumEntry with Uncapitalised

  object KeyUsage extends Enum[KeyUsage] {
    case object DigitalSignature  extends KeyUsage(1)
    case object ContentCommitment extends KeyUsage(2)
    case object KeyEncipherment   extends KeyUsage(4)
    case object DataEncipherment  extends KeyUsage(8)
    case object KeyAgreement      extends KeyUsage(16)
    case object KeyCertSign       extends KeyUsage(32)
    case object CRLSign           extends KeyUsage(64)
    case object EncipherOnly      extends KeyUsage(128)
    case object DecipherOnly      extends KeyUsage(256)

    val values: immutable.IndexedSeq[KeyUsage] = findValues

    private val valueByName = values.map(e => e.entryName -> e).toMap

    implicit val configReader: ConfigReader[KeyUsage] = ConfigReader.fromStringOpt(valueByName.get)
  }

  sealed abstract class ExtendedKeyUsage(val ids: Array[Int]) extends EnumEntry with Uncapitalised {
    private val oidStr: String = ids.mkString(".")

    def strRepr: String = ExtendedKeyUsage.oidToValue.get(oidStr).fold(oidStr)(_.entryName)

    override def equals(other: Any): Boolean = other match {
      case that: ExtendedKeyUsage => oidStr == that.oidStr
      case _                      => false
    }

    override def hashCode(): Int = oidStr.hashCode
  }

  case class CustomExtendedKeyUsage(oid: Array[Int]) extends ExtendedKeyUsage(oid)

  object ExtendedKeyUsage extends Enum[ExtendedKeyUsage] {
    case object ServerAuth      extends ExtendedKeyUsage(Array(1, 3, 6, 1, 5, 5, 7, 3, 1))
    case object ClientAuth      extends ExtendedKeyUsage(Array(1, 3, 6, 1, 5, 5, 7, 3, 2))
    case object CodeSigning     extends ExtendedKeyUsage(Array(1, 3, 6, 1, 5, 5, 7, 3, 3))
    case object EmailProtection extends ExtendedKeyUsage(Array(1, 3, 6, 1, 5, 5, 7, 3, 4))
    case object TimeStamping    extends ExtendedKeyUsage(Array(1, 3, 6, 1, 5, 5, 7, 3, 8))
    case object OCSPSigning     extends ExtendedKeyUsage(Array(1, 3, 6, 1, 5, 5, 7, 3, 9))

    override val values: immutable.IndexedSeq[ExtendedKeyUsage] = findValues

    private val nameToValue = values.map(e => e.entryName.toUpperCase -> e).toMap
    private val oidToValue  = values.map(e => e.oidStr                -> e).toMap

    private val extendedKeyUsagePattern = """^(\d+\.)*\d+$"""

    def parseString(str: String): Either[CryptoError, ExtendedKeyUsage] = {
      nameToValue.get(str.toUpperCase) match {
        case Some(eku) => Right(eku)
        case None =>
          Either
            .cond(str.matches(extendedKeyUsagePattern), str, PKIError(s"Extended key usage '$str' mismatch OID pattern"))
            .flatMap { validStr =>
              oidToValue.get(str) match {
                case Some(eku) => Right(eku)
                case None =>
                  Either
                    .catchOnly[NumberFormatException](validStr.split("""\.""").map(_.toInt))
                    .leftMap(err => PKIError(s"Unable to parse Integer from OID value: ${err.getMessage}"))
                    .map(CustomExtendedKeyUsage)
              }
            }
      }
    }

    def parseStrings(str: String*): Either[CryptoError, List[ExtendedKeyUsage]] = {
      str.toList
        .traverse { eku =>
          Validated.fromEither(parseString(eku).leftMap(_ => NonEmptyList.of(eku)))
        }
        .toEither
        .leftMap { invalidKeyUsages =>
          PKIError(s"The following extended key usages mismatch OID pattern: [${invalidKeyUsages.toList.mkString(", ")}]")
        }
    }

    implicit val configReader: ConfigReader[ExtendedKeyUsage] = ConfigReader.fromString { str =>
      parseString(str)
        .leftMap { err =>
          CannotConvert(str, ExtendedKeyUsage.getClass.getSimpleName, err.message)
        }
    }
  }

  sealed trait SubjectAlternativeNameItemType extends EnumEntry with Uppercase

  object SubjectAlternativeNameItemType extends Enum[SubjectAlternativeNameItemType] {
    case object Dns extends SubjectAlternativeNameItemType
    case object Ip  extends SubjectAlternativeNameItemType

    override val values: scala.collection.immutable.IndexedSeq[SubjectAlternativeNameItemType] = findValues
  }

  case class SubjectAlternativeNameItem(itemType: SubjectAlternativeNameItemType, value: String)

  case class SubjectAlternativeName(value: List[SubjectAlternativeNameItem])

  object SubjectAlternativeName {
    val empty: SubjectAlternativeName = SubjectAlternativeName(List())

    implicit val configReader: ConfigReader[SubjectAlternativeName] = ConfigReader.fromCursor { cursor =>
      for {
        alternativeNames <- cursor.asString
      } yield {
        val altNameItems = alternativeNames
          .split(",")
          .map(parseItem)
        SubjectAlternativeName(altNameItems.toList)
      }
    }

    private def parseItem(value: String): SubjectAlternativeNameItem =
      value.split(":") match {
        case Array(tpe, value) =>
          SubjectAlternativeNameItemType
            .withNameInsensitiveOption(tpe)
            .map { matchedType =>
              SubjectAlternativeNameItem(matchedType, value)
            }
            .getOrElse(
              throw new RuntimeException(s"Expected one of subject alternative name type: [${SubjectAlternativeNameItemType.values.mkString(",")}]"))
        case _ =>
          throw new RuntimeException(s"Invalid item format: '$value', expected 'TYPE:VALUE'")
      }
  }

}
