package com.wavesenterprise.crypto.internals.pki

import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.{Uncapitalised, Uppercase}
import pureconfig._
import pureconfig.ConfigReader
import pureconfig.generic.auto._
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
    implicit val productHint: ProductHint[CertRequestContent] = ProductHint[CertRequestContent] {
      case "commonName"         => "CN"
      case "organizationalUnit" => "OU"
      case "organization"       => "O"
      case "country"            => "C"
      case "locality"           => "L"
      case "stateOrProvince"    => "S"
      case "extensions"         => "extensions"
    }

    implicit val configReader: ConfigReader[CertRequestContent] = deriveReader
  }

  case class Extensions(
      keyUsage: List[KeyUsage],
      extendedKeyUsage: List[ExtendedKeyUsage] = List.empty,
      subjectAlternativeName: SubjectAlternativeName = SubjectAlternativeName.empty
  )

  sealed abstract class KeyUsage(val jcspValue: Int) extends EnumEntry with Uncapitalised

  object KeyUsage extends Enum[KeyUsage] {
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

  }

  sealed abstract class ExtendedKeyUsage(val jcspValue: Array[Int]) extends EnumEntry with Uncapitalised

  object ExtendedKeyUsage extends Enum[ExtendedKeyUsage] {
    case object ServerAuth      extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_SERVER_AUTH)
    case object ClientAuth      extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_CLIENT_AUTH)
    case object CodeSigning     extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_CODE_SIGNING)
    case object EmailProtection extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_EMAIL_PROTECTION)
    case object TimeStamping    extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_TIME_STAMPING)
    case object OCSPSigning     extends ExtendedKeyUsage(JCPRequest.KeyUsage.INTS_PKIX_OCSP_SIGNING)

    override val values: scala.collection.immutable.IndexedSeq[ExtendedKeyUsage] = findValues

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
