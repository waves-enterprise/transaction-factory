import ru.CryptoPro.JCSP.JCSPLicense

import scala.util.Try

object CheckJCSP {

  private val EmptyVersion = "0.0.0"

  def run(): Unit = {
    (for {
      jcspClass <- checkJcspExistence
      _         <- checkJcspVersion(jcspClass)
      _         <- checkJcspLicenseValid
      _         <- checkJcspCryptoInstalled
    } yield ()).fold(ex => throw ex, identity)
  }

  private def checkJcspExistence: Either[RuntimeException, Class[_]] =
    Try {
      Class.forName("ru.CryptoPro.JCSP.JCSP")
    }.toEither.left.map { ex =>
      new CheckJCSPException(s"CryptoPro JCSP is not found in classpath. Caused by '$ex'")
    }

  private def checkJcspCryptoInstalled: Either[RuntimeException, Class[_]] =
    Try {
      Class.forName("ru.CryptoPro.Crypto.CryptoProvider")
    }.toEither.left.map { ex =>
      new CheckJCSPException(s"CryptoPro JCryptoP (crypto module) is not installed. Caused by '$ex'")
    }

  private def checkJcspVersion(jcspClass: Class[_]): Either[RuntimeException, Unit] =
    Try {
      val method = Class
        .forName("ru.CryptoPro.JCP.tools.JarChecker")
        .getMethod("getFromManifest", classOf[Class[_]], classOf[String], classOf[String])
      method.invoke(null, jcspClass, "Release-Version", EmptyVersion).toString
    }.toEither.left.map {
      new RuntimeException(s"CryptoPro JCSP version check failed", _)
    } flatMap {
      case Dependencies.supportedJcspVersion => Right(())
      case EmptyVersion =>
        Left(
          new CheckJCSPException(
            s"CryptoPro JCSP '${Dependencies.supportedJcspVersion}' is not found"))
      case envJcspVersion =>
        Left(new CheckJCSPException(
          s"Supported JCSP versions is '${Dependencies.supportedJcspVersion}',  actual is '$envJcspVersion'"))
    }

  private def checkJcspLicenseValid: Either[RuntimeException, Unit] = {
    Try(new JCSPLicense().verifyLicense()).toEither match {
      case Right(0) | Right(1) => // valid client or server license
        Right(())
      case Right(errorCode) =>
        Left(new CheckJCSPException(s"CryptoPro JCSP License is missing or expired, JCSP error code $errorCode"))
      case Left(_) =>
        Left(new CheckJCSPException("CryptoPro JCSP License is missing or expired"))
    }
  }
}

class CheckJCSPException(message: String, cause: Throwable) extends RuntimeException(message, cause, false, false) {
  def this(message: String) {
    this(message, null)
  }
}
