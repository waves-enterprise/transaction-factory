package com.wavesplatform

import cats.syntax.either._
import com.wavesplatform.crypto.internals.{CryptoError, GenericError}
import com.wavesplatform.utils.ScorexLogging
import ru.CryptoPro.JCP.tools.JarChecker
import ru.CryptoPro.JCSP.{JCSP, JCSPLicense}

object CheckEnvironment extends ScorexLogging {
  def checkCryptoEnv(expectedGostCrypto: Boolean): Either[CryptoError, Unit] = {
    (if (expectedGostCrypto) {
       for {
         _ <- checkJcspExistence
         _ <- checkJcspVersion
         _ <- checkJcspLicenseValid
         _ <- checkJcspCryptoInstalled
       } yield ()
     } else {
       Right(())
     }).leftMap(error => GenericError(s"Environment check failed: ${error.message}"))
  }

  private def checkJcspExistence: Either[CryptoError, Unit] = {
    Either
      .catchNonFatal(Class.forName("ru.CryptoPro.JCSP.JCSP"))
      .leftMap(_ => GenericError("CryptoPro JCSP is not found in classpath"))
      .map(_ => ())
  }

  private def checkJcspCryptoInstalled: Either[CryptoError, Unit] = {
    Either
      .catchNonFatal(Class.forName("ru.CryptoPro.Crypto.CryptoProvider"))
      .leftMap(_ => GenericError("CryptoPro JCryptoP (crypto module) is not installed"))
      .map(_ => ())
  }

  def checkJcspLicenseValid: Either[CryptoError, Unit] = {
    Either.catchNonFatal(new JCSPLicense().verifyLicense()) match {
      case Right(0) | Right(1) => // valid client or server license
        Right(())
      case Right(errorCode) =>
        Left(GenericError(s"CryptoPro JCSP License is missing or expired, JCSP error code $errorCode"))
      case Left(e) =>
        log.warn("Can't check CryptoPro JCSP License", e)
        Left(GenericError("CryptoPro JCSP License is missing or expired"))
    }
  }

  /**
    * Could be returned in case when JCSP is not installed on machine
    */
  private val EmptyVersion: String = "0.0.0"

  private def checkJcspVersion: Either[CryptoError, Unit] =
    Either
      .catchNonFatal(JarChecker.getFromManifest(classOf[JCSP], "Release-Version", EmptyVersion))
      .leftMap(_ =>
        GenericError(s"CryptoPro JCSP '${CryptoVersion.supportedJcspVersion}' or '${CryptoVersion.supportedExperimentalJcspVersion}' is not found"))
      .flatMap {
        case CryptoVersion.supportedJcspVersion | CryptoVersion.supportedExperimentalJcspVersion =>
          Right(())

        case EmptyVersion =>
          Left(GenericError {
            s"CryptoPro JCSP '${CryptoVersion.supportedJcspVersion}' or '${CryptoVersion.supportedExperimentalJcspVersion}' is not found"
          })

        case envJcspVersion =>
          Left(GenericError {
            s"Supported JCSP versions are ['${CryptoVersion.supportedJcspVersion}', '${CryptoVersion.supportedExperimentalJcspVersion}'], actual is $envJcspVersion"
          })
      }
}
