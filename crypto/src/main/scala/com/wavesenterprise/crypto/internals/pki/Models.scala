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
import ru.CryptoPro.JCPRequest
import ru.CryptoPro.reprov.x509._
import pureconfig.module.enumeratum._

import scala.collection.immutable

object Models {

  case class CertRequestContent(
      commonName: String, //CN
      organizationalUnit: String, //OU
      organization: String, //O
      country: String, //C
      stateOrProvince: String, //S
      locality: String, //L
      extensions: Extensions //X.509 v3 Extensions
  ) {
    def toX500Name: X500Name = new X500Name(commonName, organizationalUnit, organization, locality, stateOrProvince, country)
  }

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

    implicit val configReader: ConfigReader[CertRequestContent] = new ConfigReader[CertRequestContent] {
      def withValidation(conf: CertRequestContent): Result[CertRequestContent] = {
        val mustBeNonEmptyStrs = Seq(conf.stateOrProvince, conf.country, conf.locality, conf.commonName, conf.organization, conf.organizationalUnit)

        if (mustBeNonEmptyStrs.forall(!_.isBlank)) {
          Right(conf)
        } else {
          Left(ConfigReaderFailures(pureconfig.error.CannotParse("Mandatory fields 'CN', 'OU', 'O', 'C', 'L', 'S' must be non-empty", None)))
        }
      }

      override def from(cur: ConfigCursor): Result[CertRequestContent] = {
        baseConfigReader.from(cur).flatMap(withValidation)
      }
    }
  }

  case class Extensions(
      keyUsage: List[KeyUsage],
      extendedKeyUsage: List[ExtendedKeyUsage] = List.empty,
      subjectAlternativeName: SubjectAlternativeName = SubjectAlternativeName.empty
  )

  sealed abstract class KeyUsage(val jcspValue: Int) extends EnumEntry with Uncapitalised

  object KeyUsage extends Enum[KeyUsage] {
    import pureconfig.generic.auto._

    case object DigitalSignature  extends KeyUsage(JCPRequest.KeyUsage.DIGITAL_SIGNATURE)
    case object ContentCommitment extends KeyUsage(JCPRequest.KeyUsage.NON_REPUDIATION)
    case object KeyEncipherment   extends KeyUsage(JCPRequest.KeyUsage.KEY_ENCIPHERMENT)
    case object DataEncipherment  extends KeyUsage(JCPRequest.KeyUsage.DATA_ENCIPHERMENT)
    case object KeyAgreement      extends KeyUsage(JCPRequest.KeyUsage.KEY_AGREEMENT)
    case object KeyCertSign       extends KeyUsage(JCPRequest.KeyUsage.KEY_CERT_SIGN)
    case object CRLSign           extends KeyUsage(JCPRequest.KeyUsage.CRL_SIGN)
    case object EncipherOnly      extends KeyUsage(JCPRequest.KeyUsage.ENCIPHER_ONLY)
    case object DecipherOnly      extends KeyUsage(JCPRequest.KeyUsage.DECIPHER_ONLY)

    val values: immutable.IndexedSeq[KeyUsage] = findValues

    implicit val configReader: ConfigReader[KeyUsage] = deriveReader
  }

  sealed abstract class ExtendedKeyUsage(val jcspValue: Array[Int]) extends EnumEntry with Uncapitalised {
    private val oidStr: String = jcspValue.mkString(".")

    def strRepr: String = ExtendedKeyUsage.oidToValue.get(oidStr).fold(oidStr)(_.entryName)

    override def equals(other: Any): Boolean = other match {
      case that: ExtendedKeyUsage => oidStr == that.oidStr
      case _                      => false
    }

    override def hashCode(): Int = oidStr.hashCode
  }

  case class CustomExtendedKeyUsage(oid: Array[Int]) extends ExtendedKeyUsage(oid)

  object ExtendedKeyUsage extends Enum[ExtendedKeyUsage] {
    case object ServerAuth      extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_SERVER_AUTH)
    case object ClientAuth      extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_CLIENT_AUTH)
    case object CodeSigning     extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_CODE_SIGNING)
    case object EmailProtection extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_EMAIL_PROTECTION)
    case object TimeStamping    extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_TIME_STAMPING)
    case object OCSPSigning     extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_OCSP_SIGNING)

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

  sealed abstract class SubjectAlternativeNameItemType(jscpMapper: String => GeneralNameInterface) extends EnumEntry with Uppercase {
    def toJcspGeneralName(value: String): GeneralName = new GeneralName(jscpMapper(value))
  }

  object SubjectAlternativeNameItemType extends Enum[SubjectAlternativeNameItemType] {
    case object Dns extends SubjectAlternativeNameItemType(new DNSName(_))
    case object Ip  extends SubjectAlternativeNameItemType(new IPAddressName(_))

    override val values: scala.collection.immutable.IndexedSeq[SubjectAlternativeNameItemType] = findValues
  }

  case class SubjectAlternativeNameItem(itemType: SubjectAlternativeNameItemType, value: String) {
    def toJscpGeneralName: GeneralName = itemType.toJcspGeneralName(value)
  }

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
