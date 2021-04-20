package com.wavesenterprise.crypto.internals.gost

import cats.syntax.either._
import com.wavesenterprise.crypto.internals.{CryptoContext, CryptoError, GenericError, KeyStore}
import org.slf4j.{Logger, LoggerFactory}
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCP.KeyStore.JCPPrivateKeyEntry
import ru.CryptoPro.JCPRequest.GostCertificateRequest
import ru.CryptoPro.JCSP.{JCSP, Key}
import scorex.util.ScorexLogging

import java.io.{ByteArrayInputStream, File}
import java.security.cert.{Certificate, CertificateFactory}
import java.security.{KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStore => KeyStoreJ}
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

private[gost] abstract class JcspKeyStore(algorithms: GostAlgorithms, generatorKeyStoreFileOpt: Option[File], password: Array[Char])
    extends KeyStore[GostKeyPair](None, password) {

  private val ProviderName = JCSP.PROVIDER_NAME

  def toAlias(keyPair: GostKeyPair): String

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  private val store: KeyStoreJ = {
    val ks = KeyStoreJ.getInstance("HDIMAGE", ProviderName)
    ks.load(null, password)
    ks
  }

  override def generateAndStore(pwd: Option[Array[Char]]): Option[GostKeyPair#PublicKey0] = Option {
    val pair = algorithms.generateKeyPair()
    storeKey(pair, pwd)
    pair.getPublic
  }

  private def genSelfCert(pair: GostKeyPair, dname: String): Certificate = {
    val gr  = new GostCertificateRequest(ProviderName)
    val enc = gr.getEncodedSelfCert(pair.internal, dname)
    val cf  = CertificateFactory.getInstance(JCP.CERTIFICATE_FACTORY_NAME)
    cf.generateCertificate(new ByteArrayInputStream(enc))
  }

  override def aliases(): Seq[String] =
    store.aliases().asScala.toSeq

  def containsAlias(alias: String): Either[CryptoError, Boolean] = {
    Either
      .catchNonFatal(store.containsAlias(alias))
      .leftMap {
        case _: KeyStoreException => GenericError("Keystore not initialized")
      }
  }

  /**
    * Stores a keypair to JCSP KeyStore
    * Must be called only for AccountsGeneratorApp
    */
  private def storeKey(keyPair: GostKeyPair, pwd: Option[Array[Char]]): Unit = {
    val certificates     = Array(genSelfCert(keyPair, "CN=Waves_2012_256, O=Waves, C=RU"))
    val entry            = new JCPPrivateKeyEntry(keyPair.getPrivate.internal, certificates)
    val passwordNullable = pwd.filter(_.nonEmpty).orNull
    val protection       = new KeyStoreJ.PasswordProtection(passwordNullable)
    // Used setEntry instead of setKeyEntry, because setKeyEntry with CSP shows a graphic window with password input.
    store.setEntry(toAlias(keyPair), entry, protection)
  }

  def getCertificate(alias: String): Either[CryptoError, Certificate] = {
    Either
      .catchNonFatal(store.getCertificate(alias))
      .leftMap {
        case keyStoreEx: KeyStoreException =>
          log.error("Keystore not initialized", keyStoreEx)
          GenericError("Keystore not initialized")
        case ex =>
          log.error(s"Failed to get certificate for $alias", ex)
          GenericError(s"Can't get certificate for alias $alias")
      }
      .filterOrElse(nullableCert => Option(nullableCert).isDefined,
                    zero = GenericError(s"Given alias $alias doesn't exist or doesn't contain a certificate"))
  }

  def getCertificateChain(alias: String): Either[CryptoError, Array[Certificate]] = {
    Option(store.getCertificateChain(alias))
      .toRight(GenericError(s"Alias $alias doesn't exist or doesn't contain a certificate chain in the keystore"))
  }

  private def getKeyInternal(alias: String, passwordOpt: Option[Array[Char]]): Either[CryptoError, Key.GostPrivateKey] = {
    Either
      .catchNonFatal {
        val protection = new KeyStoreJ.PasswordProtection(passwordOpt.getOrElse(Array.empty[Char]))
        // Used getEntry instead of getKeyEntry, because setKeyEntry with CSP shows a graphic window with password input.
        val entry = store.getEntry(alias, protection).asInstanceOf[JCPPrivateKeyEntry]
        entry.getPrivateKey.asInstanceOf[ru.CryptoPro.JCSP.Key.GostPrivateKey]
      }
      .leftMap {
        case algorithmException: NoSuchAlgorithmException =>
          log.error("JCSP not installed correctly", algorithmException)
          GenericError("JCSP not installed correctly")

        case keyStoreEx: KeyStoreException =>
          log.error("Keystore not initialized", keyStoreEx)
          GenericError("Keystore not initialized")

        case _: UnrecoverableKeyException =>
          GenericError("Key cannot be recovered, probably invalid password")

        case NonFatal(ex) =>
          log.error("Unexpected exception", ex)
          GenericError("Unexpected exception occurred")
      }
      .filterOrElse(nullableKey => Option(nullableKey).isDefined, zero = GenericError(s"Key doesn't exist"))
      .leftMap(error => error.copy(message = s"Can't get key for alias $alias: " + error.message))
  }

  override def getKey(alias: String, pwdOpt: Option[Array[Char]]): Either[CryptoError, GostPrivateKey] = {
    getKeyInternal(alias, pwdOpt).map(GostPrivateKey)
  }

  override def getPublicKey(alias: String): Either[CryptoError, GostPublicKey] = {
    getCertificate(alias)
      .map(cert => new GostPublicKey(cert.getPublicKey))
  }

  override def getKeyPair(alias: String, pwdOpt: Option[Array[Char]]): Either[CryptoError, GostKeyPair] = {
    for {
      cert       <- getCertificate(alias)
      privateKey <- getKeyInternal(alias, pwdOpt)
    } yield GostKeyPair(new java.security.KeyPair(cert.getPublicKey, privateKey))
  }
}

abstract class GostCryptoContext() extends CryptoContext with ScorexLogging { self =>

  override type KeyPair0 = GostKeyPair
  override val isGost           = true
  override val algorithms       = new GostAlgorithms
  override val modernAlgorithms = new KuznechikAlgorithm

  def toAlias(keyPair: GostKeyPair): String

  override def keyStore(file: Option[File], password: Array[Char]): KeyStore[GostKeyPair] = {
    file.foreach(
      _ =>
        log.warn(
          "Gost cryptography tools do not support custom keystore path, keystore should be stored in /var/opt/cprocsp/keys/{username} directory"))

    new JcspKeyStore(algorithms, file, password) {
      override def toAlias(keyPair: GostKeyPair): String = self.toAlias(keyPair)
    }
  }
}
