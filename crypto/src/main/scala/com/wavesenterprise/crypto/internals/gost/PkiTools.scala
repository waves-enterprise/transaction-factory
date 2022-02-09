package com.wavesenterprise.crypto.internals.gost

import cats.implicits._
import com.objsys.asn1j.runtime.{Asn1BerDecodeBuffer, Asn1BitString, Asn1DerEncodeBuffer}
import com.wavesenterprise.crypto.internals.pki.Models.{
  CertRequestContent,
  ExtendedKeyUsage,
  Extensions,
  KeyUsage,
  SubjectAlternativeName,
  SubjectAlternativeNameItem,
  SubjectAlternativeNameItemType
}
import ru.CryptoPro.Crypto.CryptoProvider
import ru.CryptoPro.JCP.ASN.CertificateExtensions.{ALL_CertificateExtensionsValues, _extKeyUsage_ExtnType}
import ru.CryptoPro.JCP.ASN.PKIX1Explicit88.{Extension, RDNSequence}
import ru.CryptoPro.JCP.ASN.PKIXCMP.CertificationRequest
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCP.KeyStore.JCPPrivateKeyEntry
import ru.CryptoPro.JCP.params.CryptDhAllowedSpec
import ru.CryptoPro.JCPRequest.GostCertificateRequest
import ru.CryptoPro.JCSP.JCSP
import ru.CryptoPro.JCSP.Key.GostPublicKey
import ru.CryptoPro.reprov.RevCheck
import ru.CryptoPro.reprov.array.{DerInputStream, DerOutputStream}
import ru.CryptoPro.reprov.x509.{GeneralNames, IPAddressName, X500Name, X509CertImpl}

import java.io._
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.security._
import java.security.cert.{Certificate, CertificateFactory}
import java.util
import scala.util.Success
import scala.util.Using

object PkiTools {

  val HdImageType = "HDIMAGE"

  private val ProviderName           = JCSP.PROVIDER_NAME
  private val Algorithm              = JCP.GOST_DH_2012_256_NAME
  private val SignAlgorithm          = JCP.GOST_SIGN_2012_256_NAME
  private val CertificateFactoryName = JCP.CERTIFICATE_FACTORY_NAME
  private val DefaultCertReqConfig   = CertRequestContent("Waves Enterprise CA", "IT Business", "Waves Enterprise", "RU", None)

  case class PrivateKeyEntry(privateKey: PrivateKey, certificate: Certificate) {
    def asJcsp = new JCPPrivateKeyEntry(privateKey, Array(certificate))
  }

  object PrivateKeyEntry {
    def fromJcsp(jcpPrivKeyEntry: JCPPrivateKeyEntry): PrivateKeyEntry =
      PrivateKeyEntry(jcpPrivKeyEntry.getPrivateKey, jcpPrivKeyEntry.getCertificate)
  }

  def init(): Unit = {
    Security.addProvider(new JCSP())
    Security.addProvider(new RevCheck())
    Security.addProvider(new CryptoProvider())
    java.awt.Toolkit.getDefaultToolkit
  }

  def generateKeyPair(): KeyPair = {
    val keyPairGen = KeyPairGenerator.getInstance(Algorithm, ProviderName)
    keyPairGen.initialize(new CryptDhAllowedSpec())
    keyPairGen.generateKeyPair()
  }

  def generatePrivateKeyEntry(certificateConfig: CertRequestContent): PrivateKeyEntry = {
    val keyPair   = generateKeyPair()
    val certBytes = generateAndEncodeCertificate(keyPair, certificateConfig)
    val cert      = certificateFromBytes(certBytes)

    PrivateKeyEntry(keyPair.getPrivate, cert)
  }

  def generateCertificateRequest(publicKey: PublicKey, subjectName: X500Name, maybeExtensions: Option[Extensions]): GostCertificateRequest = {
    val request = new GostCertificateRequest(ProviderName)

    request.setPublicKeyInfo(publicKey)

    maybeExtensions.foreach { extensions =>
      extensions.keyUsage.toNel.foreach { keyUsages =>
        val keyUsageComposition = keyUsages.map(_.jcspValue).reduceLeft(_ | _)
        request.setKeyUsage(keyUsageComposition)
      }

      extensions.extendedKeyUsage.foreach { extendedKeyUsage =>
        request.addExtKeyUsage(extendedKeyUsage.jcspValue)
      }

      addSubjectAlternativeName(request, extensions.subjectAlternativeName)
    }

    request.setSubjectInfo(subjectName)

    request
  }

  /** Warning: in-place operation
    */
  def signCertificateRequest(privateKey: PrivateKey, certRequest: GostCertificateRequest): Unit = {
    certRequest.encodeAndSign(privateKey, SignAlgorithm)
  }

  def generateCertificate(keyPair: KeyPair, certificateConfig: CertRequestContent = DefaultCertReqConfig): Certificate = {
    val gr  = generateCertificateRequest(keyPair.getPublic, certificateConfig.toX500Name, certificateConfig.extensions)
    val enc = gr.getEncodedSelfCert(keyPair, certificateConfig.toX500Name, None.orNull)
    val cf  = CertificateFactory.getInstance(CertificateFactoryName)
    cf.generateCertificate(new ByteArrayInputStream(enc))
  }

  def generateAndEncodeCertificate(keyPair: KeyPair, certificateConfig: CertRequestContent): Array[Byte] = {
    generateCertificateRequest(keyPair.getPublic, certificateConfig.toX500Name, certificateConfig.extensions)
      .getEncodedSelfCert(keyPair, certificateConfig.toX500Name, None.orNull)
  }

  def addSubjectAlternativeName(request: GostCertificateRequest, subjectAlternativeName: SubjectAlternativeName): Unit = {
    val generalNames = new GeneralNames()

    subjectAlternativeName.value.foreach { item =>
      generalNames.add(item.toJscpGeneralName)
    }

    val extensionBytes = Using.resource(new DerOutputStream()) { stream =>
      generalNames.encode(stream)
      stream.toByteArray
    }

    val subjectAltNameOid = ALL_CertificateExtensionsValues.id_ce_subjectAltName
    val sanExtension      = new Extension(subjectAltNameOid, false, extensionBytes)
    request.addExtension(sanExtension)
  }

  def saveKeyEntry(id: String, entry: PrivateKeyEntry, keyStore: KeyStore, password: Array[Char]): Unit = {
    val protection = new KeyStore.PasswordProtection(password)
    keyStore.setEntry(id, entry.asJcsp, protection)
  }

  def getKeyStore(keyStorePassword: Array[Char]): KeyStore = {
    val keyStore = KeyStore.getInstance(HdImageType, ProviderName)
    keyStore.load(null, keyStorePassword)
    keyStore
  }

  def getEntryFromKeyStore(keyStore: KeyStore, id: String, password: Array[Char]): PrivateKeyEntry = {
    val protection = new KeyStore.PasswordProtection(password)
    PrivateKeyEntry.fromJcsp(keyStore.getEntry(id, protection).asInstanceOf[JCPPrivateKeyEntry])
  }

  def getIssuer(cert: Certificate): X500Name = {
    val x509Cert = new X509CertImpl(cert.getEncoded)
    val x        = x509Cert.getIssuerX500Principal.toString
    new X500Name(x)
  }

  def certificateFromBytes(certBytes: Array[Byte]): Certificate = {
    CertificateFactory
      .getInstance(JCP.CERTIFICATE_FACTORY_NAME)
      .generateCertificate(new ByteArrayInputStream(certBytes))
  }

  case class ParseCertReqResult(subject: X500Name, publicKey: ru.CryptoPro.JCSP.Key.GostPublicKey, gostCertificateRequest: GostCertificateRequest)
  def importCertificateRequest(file: File): ParseCertReqResult = {
    val certificateRequest = Using.resource(new FileInputStream(file)) { fileStream =>
      val decodeBuffer = new Asn1BerDecodeBuffer(fileStream)
      val request      = new CertificationRequest()
      request.decode(decodeBuffer)
      request
    }

    val subjectName = {
      val encodeBuffer = new Asn1DerEncodeBuffer()
      certificateRequest.certificationRequestInfo.subject.getElement.asInstanceOf[RDNSequence].encode(encodeBuffer)
      new X500Name(encodeBuffer.getMsgCopy)
    }

    val publicKey = {
      val asnPrefix = Array[Byte](
        48, 102, 48, 31, 6, 8, 42, -123, 3, 7, 1, 1, 1, 1, 48, 19, 6, 7, 42, -123, 3, 2, 2, 36, 0, 6, 8, 42, -123, 3, 7, 1, 1, 2, 2, 3, 67, 0
      )

      new ru.CryptoPro.JCSP.Key.GostPublicKey(
        asnPrefix ++ certificateRequest.certificationRequestInfo.subjectPublicKeyInfo.subjectPublicKey.value,
        true
      )
    }

    val maybeExtensions = certificateRequest.certificationRequestInfo.attributes.elements.headOption.flatMap {
      _.values.elements.headOption.collect {
        case extensions: ru.CryptoPro.JCP.ASN.PKIX1Explicit88.Extensions =>
          extensions
      }
    }

    val maybeKeyUsages = maybeExtensions
      .flatMap { extensions =>
        extensions.elements.find(e => util.Arrays.equals(e.extnID.value, ALL_CertificateExtensionsValues.id_ce_keyUsage))
      }
      .map { keyUsages =>
        val decodeBuffer = new Asn1BerDecodeBuffer(keyUsages.extnValue.value)
        val bitString    = new Asn1BitString()
        bitString.decode(decodeBuffer)
        bitString.toBoolArray.zip(KeyUsage.values).collect { case (true, flag) => flag }
      }

    val maybeExtKeyUsages = maybeExtensions
      .flatMap { extensions =>
        extensions.elements.find(e => util.Arrays.equals(e.extnID.value, ALL_CertificateExtensionsValues.id_ce_extKeyUsage))
      }
      .map { extKeyUsages =>
        val decodeBuffer = new Asn1BerDecodeBuffer(extKeyUsages.extnValue.value)

        val internalExtKeyUsages = new _extKeyUsage_ExtnType()
        internalExtKeyUsages.decode(decodeBuffer)

        internalExtKeyUsages.elements.flatMap { elem =>
          ExtendedKeyUsage.values.find(i => util.Arrays.equals(i.jcspValue, elem.value))
        }
      }

    val maybeSubjectAltName = maybeExtensions
      .flatMap { extensions =>
        extensions.elements.find(e => util.Arrays.equals(e.extnID.value, ALL_CertificateExtensionsValues.id_ce_subjectAltName))
      }
      .map { subjectAltName =>
        val stream = new DerInputStream(subjectAltName.extnValue.value)

        val DnsTag = -128 | 2
        val IpTag  = -128 | 7

        stream.getSequence(1).map { derValue =>
          derValue.tag match {
            case DnsTag => SubjectAlternativeNameItem(SubjectAlternativeNameItemType.Dns, new String(derValue.data.toByteArray, "ASCII"))
            case IpTag  => SubjectAlternativeNameItem(SubjectAlternativeNameItemType.Ip, new IPAddressName(derValue.data.toByteArray).getName)
          }
        }
      }

    val maybeResultExtensions = maybeSubjectAltName.map { subjectAltName =>
      Extensions(
        maybeKeyUsages.toList.flatten,
        maybeExtKeyUsages.toList.flatten,
        SubjectAlternativeName(subjectAltName.toList)
      )
    }

    ParseCertReqResult(subjectName, publicKey, generateCertificateRequest(publicKey, subjectName, maybeResultExtensions))
  }

  def exportCertificate(certificate: Certificate, file: File): Unit =
    exportToFile(certificate.getEncoded, file)

  /** @param request
    *   must be encoded and signed in advance
    */
  def exportCertificateRequest(request: GostCertificateRequest, file: File): Unit =
    exportToFile(request.getEncoded, file)

  def exportToFile(bytes: Array[Byte], file: File): Unit =
    Using.resource(new FileOutputStream(file))(_.write(bytes))

  def generateCertificateFromRequest(
      request: GostCertificateRequest,
      caPrivateKeyEntry: PrivateKeyEntry,
      publicKey: PublicKey,
      subjectName: X500Name
  ): Array[Byte] = {
    val issuer = PkiTools.getIssuer(caPrivateKeyEntry.certificate)
    request.generateCert(caPrivateKeyEntry.privateKey, publicKey, subjectName, issuer, null)
  }

  def charsToBytes(chars: Array[Char], charset: String = "UTF-8"): Array[Byte] = {
    val charBuffer = CharBuffer.wrap(chars)
    val byteBuffer = Charset.forName(charset).encode(charBuffer)
    val bytes      = java.util.Arrays.copyOfRange(byteBuffer.array, byteBuffer.position: Int, byteBuffer.limit: Int)
    wipeMemory(byteBuffer.array)
    bytes
  }

  def wipeMemory[A](arrays: Array[A]*)(implicit numeric: Numeric[A]): Unit =
    arrays.foreach { arr =>
      for (i <- arr.indices) arr(i) = numeric.zero
    }
}
