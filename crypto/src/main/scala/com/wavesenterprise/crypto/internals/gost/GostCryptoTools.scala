package com.wavesenterprise.crypto.internals.gost

import cats.syntax.either._
import com.wavesenterprise.crypto.internals.{CryptoError, GenericError, KeyStoreProvider, KeyPair => KeyPairAbstract}
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.util.CollectionStore
import org.slf4j.{Logger, LoggerFactory}
import ru.CryptoPro.AdES.exception.IAdESException
import ru.CryptoPro.CAdES.CAdESSignature
import ru.CryptoPro.CAdES.exception.CAdESException
import ru.CryptoPro.JCSP.JCSP

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.security.cert.Certificate
import java.security.{PrivateKey => JPrivateKey}
import scala.collection.JavaConverters._

class GostCryptoTools(keyStoreProvider: KeyStoreProvider[GostKeyPair], tspServerUrl: String) {
  import GostCryptoTools._

  /**
    * Pki magical properties to enable OCSP checks
    */
  System.setProperty("ru.CryptoPro.reprov.enableCRLDP", "true")
  System.setProperty("com.sun.security.enableCRLDP", "true")
  System.setProperty("com.ibm.security.enableCRLDP", "true")

  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  def signCAdES(keyAlias: String,
                keyPasswordOpt: Option[String],
                dataToSign: Array[Byte],
                signatureType: SignatureType): Either[CadesError, Array[Byte]] = {
    for {
      _ <- keyStoreProvider.useKeyStore(
        _.containsAlias(keyAlias)
          .filterOrElse(identity, GenericError(s"Keystore doesn't contain key with alias '$keyAlias'"))
          .leftMap(cryptoErrorToCadesError))

      privateKey <- keyStoreProvider.useKeyStore(
        _.getKey(keyAlias, keyPasswordOpt.map(_.toCharArray))
          .map(_.internal)
          .leftMap(cryptoErrorToCadesError))

      certChain <- keyStoreProvider.useKeyStore(
        _.getCertificateChain(keyAlias)
          .leftMap(cryptoErrorToCadesError))

      certCollection <- mkCertCollection(certChain, keyAlias)
        .leftMap(cryptoErrorToCadesError)

      //It's empty, because we're using OCSP server to check certificate status
      crlCollection = new CollectionStore(java.util.Collections.EMPTY_LIST)

      /*_*/
      cades <- prepareSignature(certCollection, certChain, crlCollection, privateKey, signatureType)
      /*_*/
      signatureBytes <- performSignInternal(cades, dataToSign)
    } yield signatureBytes
  }

  def verifyCAdES(dataBytes: Array[Byte],
                  signatureBytes: Array[Byte],
                  signatureType: SignatureType,
                  extendedKeyUsageToCheck: List[String]): Either[CadesError, Unit] = {
    val dataStream      = new ByteArrayInputStream(dataBytes)
    val signatureStream = new ByteArrayInputStream(signatureBytes)

    /*_*/
    for {
      cades <- Either
        .catchNonFatal {
          log.trace(s"Attempting to check signature type ${signatureType.toString}")
          val cadesSignature = new CAdESSignature(signatureStream, dataStream, signatureType.jcpTypeId)
          log.trace(s"Successfully created CAdESSignature for ${signatureType.toString}")
          cadesSignature
        }
        .leftMap(cadesSignatureNPEToCadesError.orElse(exceptionFromCadesToCadesError))
      verifiedCades <- Either
        .catchNonFatal {
          cades.verify(null, null) //if signature check fails, it throws exception
          log.trace("CAdES signature verification success")
          cades
        }
        .leftMap(exceptionFromCadesToCadesError)

      signerCertHolder <- Either
        .catchNonFatal {
          log.trace("Extracting signer certificate from CertificateStore of CAdES signature")
          verifiedCades.getCertificateStore
            .getMatches(null)
            .iterator()
            .next()
            .asInstanceOf[X509CertificateHolder]
        }
        .leftMap(exceptionFromCadesToCadesError)

      _ <- checkExtendedKeyUsage(signerCertHolder, extendedKeyUsageToCheck)
    } yield ()
    /*_*/
  }

  def checkExtendedKeyUsage(certHolder: X509CertificateHolder, expectedEKUs: List[String]): Either[CadesError, Unit] = {
    val certificateOids  = certHolder.getExtensionOIDs.asInstanceOf[java.util.List[ASN1ObjectIdentifier]].asScala.map(_.getId)
    val (_, missingEKUs) = expectedEKUs.partition(certificateOids.contains)
    Either.cond(missingEKUs.isEmpty, (), ExtendedKeyUsageError(missingEKUs))
  }

  /**
    * Creates a bouncy-castle CollectionStore from sequence of Certificates, to use it in CAdES sign
    */
  private def mkCertCollection(certs: Seq[Certificate], keyAlias: String) = {
    Either
      .catchNonFatal(new CollectionStore(certs.toList.map(cert => new X509CertificateHolder(cert.getEncoded)).asJavaCollection))
      .leftMap { ex =>
        log.error(s"Failed to build certificate chain into collection store for alias $keyAlias", ex)
        GenericError(s"Failed to build certificate chain into collection store for alias $keyAlias")
      }
  }

  private def prepareSignature(certChainCollection: CollectionStore[X509CertificateHolder],
                               certChain: Seq[Certificate],
                               crlCollection: CollectionStore[_ <: Any],
                               signerPrivateKey: JPrivateKey,
                               signatureType: SignatureType): Either[CadesError, CAdESSignature] = {
    Either
      .catchNonFatal {
        val cades = new CAdESSignature(true)

        /*_*/
        cades.setCertificateStore(certChainCollection)
        cades.setCRLStore(crlCollection)
        cades.addSigner(
          JCSP.PROVIDER_NAME,
          signerPrivateKey,
          certChain.map(_.asInstanceOf[java.security.cert.X509Certificate]).asJava,
          signatureType.jcpTypeId,
          tspServerUrl,
          false,
          null,
          null
        )
        /*_*/

        cades
      }
      .leftMap(exceptionFromCadesToCadesError)
  }

  private def performSignInternal(cades: CAdESSignature, dataToSign: Array[Byte]): Either[CadesError, Array[Byte]] = {
    Either
      .catchNonFatal {
        val signatureStream = new ByteArrayOutputStream()
        cades.open(signatureStream)
        cades.update(dataToSign)
        cades.close()
        signatureStream.toByteArray
      }
      .leftMap(exceptionFromCadesToCadesError)
  }

  private val cadesSignatureNPEToCadesError: PartialFunction[Throwable, CadesError] = {
    case npe: NullPointerException =>
      log.error("Error in CAdES", npe)
      SignatureCreationError
  }

  private val exceptionFromCadesToCadesError: PartialFunction[Throwable, CadesError] = {
    case cadesEx: CAdESException =>
      log.error("Error in CAdES", cadesEx)
      val errorCode       = cadesEx.getErrorCode
      val detailedMessage = cadesEx.getMessage

      errorCode match {
        case IAdESException.ecBuilderRootIsAbsent | IAdESException.ecBuilderRootIsUntrusted =>
          CertificateSearchError(detailedMessage)
        case IAdESException.ecRevocationInvalidCRL =>
          CRLSearchError(detailedMessage)
        case _ =>
          UnrecognizedError(cadesEx)
      }

    case otherUnexpectedEx =>
      log.error("Error in CAdES", otherUnexpectedEx)
      UnrecognizedError(otherUnexpectedEx)
  }

  private val cryptoErrorToCadesError: PartialFunction[CryptoError, CadesError] = {
    case cryptoError => UnrecognizedError(new RuntimeException(cryptoError.message))
  }

}

object GostCryptoTools {
  sealed trait CadesError
  case class UnrecognizedError(ex: Throwable)                 extends CadesError
  case class ExtendedKeyUsageError(missingOIDs: List[String]) extends CadesError
  case class CertificateSearchError(detailedMessage: String)  extends CadesError
  case class CRLSearchError(detailedMessage: String)          extends CadesError
  case object SignatureCreationError                          extends CadesError

  def createAsOption[KeyPairI <: KeyPairAbstract](isGost: Boolean,
                                                  keyStoreProvider: KeyStoreProvider[KeyPairI],
                                                  tspServerUrl: String): Option[GostCryptoTools] = {
    if (isGost) {
      val gostKeyStoreProvider = keyStoreProvider.asInstanceOf[KeyStoreProvider[GostKeyPair]]
      Some(new GostCryptoTools(gostKeyStoreProvider, tspServerUrl))
    } else {
      None
    }
  }
}
