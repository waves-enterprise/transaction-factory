package com.wavesenterprise.crypto.internals.gost

import cats.syntax.either._
import com.wavesenterprise.crypto.internals._
import ru.CryptoPro.Crypto.CryptoProvider
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCP.params.{CryptDhAllowedSpec, CryptParamsSpec}
import ru.CryptoPro.JCSP.JCSP
import ru.CryptoPro.reprov.RevCheck

import java.nio.ByteBuffer
import java.security.{PublicKey => _, _}
import javax.crypto.spec.IvParameterSpec
import javax.crypto.{Cipher, KeyAgreement, KeyGenerator, SecretKey}
import scala.util.Try

class GostAlgorithms extends CryptoAlgorithms[GostKeyPair] {

  Security.addProvider(new JCSP())
  Security.addProvider(new RevCheck())
  Security.addProvider(new CryptoProvider())

  private[gost] lazy val CryptoAlgorithm = CryptoProvider.GOST_CIPHER_NAME
  private val ProviderName               = JCSP.PROVIDER_NAME

  override val DigestSize: Int                  = messageDigestInstance().getDigestLength
  override val SignatureLength: Int             = 64
  override val KeyLength: Int                   = GostPublicKey.length
  override val SessionKeyLength: Int            = 104
  private val IVLength                          = 8
  private[gost] val WrappedKeyLength            = 42
  override lazy val WrappedStructureLength: Int = WrappedKeyLength + 3 * IVLength

  private[gost] val DataCipherAlgorithm    = CryptoAlgorithm + "/CFB/NoPadding"
  private[gost] val WrapperCipherAlgorithm = CryptoAlgorithm + "/SIMPLE_EXPORT/NoPadding"
  private val secureRandom                 = SecureRandom.getInstance(JCP.CP_RANDOM, ProviderName)

  /**
    * Used to generate user keys (requires graphical interface and user interaction)
    * Gost keys must be generated ONLY by AccountsGeneratorApp
    */
  private val userKeyGenerator: KeyPairGenerator = {
    val keyGen = KeyPairGenerator.getInstance(JCP.GOST_DH_2012_256_NAME, ProviderName)
    keyGen.initialize(new CryptDhAllowedSpec())
    keyGen
  }

  /**
    * Used to generate session keys
    */
  private val sessionKeyGenerator = {
    val keyGen = KeyPairGenerator.getInstance(JCP.GOST_EPH_DH_2012_256_NAME, ProviderName)
    keyGen.initialize(new CryptDhAllowedSpec())
    keyGen
  }

  /**
    * Generates symmetric data encryption key
    */
  private[gost] val encryptionKeyGenerator: KeyGenerator = {
    val kg = KeyGenerator.getInstance(CryptoAlgorithm, ProviderName)
    kg.init(CryptParamsSpec.getInstance(CryptParamsSpec.Rosstandart_TC26_Z))
    kg
  }

  /**
    * Generates a secret from Diffie-Hellman key agreement for two keys
    *   - agreementIV - initialization vector, should be passed or created externally
    */
  private[gost] def generateAgreementSecret(senderPrivateKey: GostPrivateKey,
                                            recipientPublicKey: AbstractGostPublicKey,
                                            agreementIV: IvParameterSpec): SecretKey = {
    val agreement = KeyAgreement.getInstance(JCP.GOST_DH_2012_256_NAME, ProviderName)
    agreement.init(senderPrivateKey.internal, agreementIV, null)
    agreement.doPhase(recipientPublicKey.internal, true)
    agreement.generateSecret(CryptoAlgorithm)
  }

  /**
    * Generates random bytes to use as initialization vector (IV)
    */
  private[gost] def generateIVBytes(size: Int = IVLength): Array[Byte] = {
    val IvBytes = new Array[Byte](size)
    secureRandom.nextBytes(IvBytes)
    IvBytes
  }

  override def generateKeyPair(): GostKeyPair = {
    val kp = userKeyGenerator.generateKeyPair()
    GostKeyPair(kp)
  }

  def generateSessionKey(): GostKeyPair = {
    val keyPair = sessionKeyGenerator.generateKeyPair()
    GostSessionKeyPair(keyPair)
  }

  /**
    * Generate a session key, based on parameters of partner's session key
    */
  def generateSessionKey(partnerKey: GostSessionPublicKey): GostKeyPair = {
    val kg = KeyPairGenerator.getInstance(JCP.GOST_EPH_DH_2012_256_NAME, ProviderName)

    val dataEncryptionOid = partnerKey.getOID

    kg.initialize(CryptParamsSpec.getInstance(dataEncryptionOid))
    val sessionKey = kg.generateKeyPair()
    GostSessionKeyPair(sessionKey)
  }

  override def sessionKeyFromBytes(bytes: Array[Byte]): PublicKey0 = {
    GostSessionPublicKey.fromBytes(bytes)
  }

  override def publicKeyFromBytes(bytes: Array[Byte]): GostPublicKey = GostPublicKey.withAsn1(bytes)

  private def messageDigestInstance() = MessageDigest.getInstance(JCP.GOST_DIGEST_2012_256_NAME, ProviderName)
  private def cspSignatureInstance()  = Signature.getInstance(JCP.GOST_SIGN_2012_256_NAME, ProviderName)

  private def hash(input: Array[Byte]): Array[Byte] = {
    val messageDigest = messageDigestInstance()
    messageDigest.digest(input)
  }

  override def fastHash(input: Array[Byte]): Array[Byte] = hash(input)

  override def secureHash(input: Array[Byte]): Array[Byte] = hash(input)

  override def sign(privateKey: GostPrivateKey, message: Array[Byte]): Array[Byte] = {
    val signInstance = privateKey.internal match {
      case _: ru.CryptoPro.JCSP.Key.GostPrivateKey =>
        cspSignatureInstance()
      case unknown =>
        throw new IllegalArgumentException(s"Unexpected private key type: '${unknown.getClass.getSimpleName}'")
    }
    signInstance.initSign(privateKey.internal)
    signInstance.update(message)
    try {
      signInstance.sign()
    } catch {
      case ex: AccessControlException if ex.getMessage.contains("0x80090010") =>
        throw new SignatureException("Access to the key is denied, probably because key expired", ex)
      case ex: IllegalArgumentException if ex.getMessage.contains("0x65b") =>
        throw new SignatureException("CryptoPro CSP license is expired or not yet valid", ex)
    }
  }

  override def verify(signature: Array[Byte], message: Array[Byte], publicKey: PublicKey0): Boolean =
    Try {
      val sig = cspSignatureInstance()
      sig.initVerify(publicKey.internal)
      sig.update(message)
      sig.verify(signature)
    }.getOrElse(false)

  /**
    * Encryption for a single recipient with GOST28147 and DH agreement key
    */
  def encrypt(data: Array[Byte],
              senderPrivateKey: GostPrivateKey,
              recipientPublicKey: AbstractGostPublicKey): Either[CryptoError, EncryptedForSingle] =
    Try {
      log.warn("GOST-28147 encryption algorithm is deprecated and will be removed in WE Node v1.6.0")

      val agreementIVBytes = generateIVBytes()
      val agreementIV      = new IvParameterSpec(agreementIVBytes)

      // sender's agreement secret with agreementIV for Bob's public key
      val senderSecret = generateAgreementSecret(senderPrivateKey, recipientPublicKey, agreementIV)

      // generate symmetric encryption key
      val encryptionKey = encryptionKeyGenerator.generateKey()

      // create cipher for encryption key
      val cipher = Cipher.getInstance(DataCipherAlgorithm, ProviderName)
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
      val cipherIVBytes = cipher.getIV

      // encrypt data
      val encryptedData = cipher.doFinal(data, 0, data.length)

      // Wrap encryption key using sender's secret
      val wrapCipher = Cipher.getInstance(WrapperCipherAlgorithm, ProviderName)
      wrapCipher.init(Cipher.WRAP_MODE, senderSecret)
      val wrapIVBytes = wrapCipher.getIV
      val wrappedKey  = wrapCipher.wrap(encryptionKey)

      val wrappedStructure = ByteBuffer
        .allocate(WrappedStructureLength)
        .put(agreementIVBytes)
        .put(cipherIVBytes)
        .put(wrapIVBytes)
        .put(wrappedKey)

      EncryptedForSingle(encryptedData, wrappedStructure.array())
    }.toEither
      .leftMap { ex =>
        log.error("Error in encrypt", ex)
        GenericError("Error in encrypt")
      }

  /**
    * Encryption for many recipients
    * Data is encrypted with GOST28147, then each recipient gets his own key, wrapped via DH agreement algorithm
    */
  def encryptForMany(data: Array[Byte],
                     senderPrivateKey: GostPrivateKey,
                     recipientPublicKeys: Seq[AbstractGostPublicKey]): Either[CryptoError, EncryptedForMany] = {
    Either
      .cond(recipientPublicKeys.nonEmpty, (), GenericError("Cannot encrypt for an empty list of recipients"))
      .flatMap { _ =>
        Try {
          log.warn("GOST-28147 encryption algorithm is deprecated and will be removed in WE Node v1.6.0")

          // generate symmetric encryption key for GOST28147
          val encryptionKey = encryptionKeyGenerator.generateKey()

          // create cipher for encryption key
          val cipher = Cipher.getInstance(DataCipherAlgorithm)
          cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
          val cipherIVBytes = cipher.getIV

          // encrypt data
          val encryptedData = cipher.doFinal(data, 0, data.length)

          // make a wrapped key for every recipient
          val recipientPubKeyToWrappedStructure: Map[PublicKey, Array[Byte]] = recipientPublicKeys.map { recipientPublicKey =>
            // shared agreement for sender and current recipient
            val agreementIVBytes = generateIVBytes()
            val secret           = generateAgreementSecret(senderPrivateKey, recipientPublicKey, new IvParameterSpec(agreementIVBytes))

            // wrap encryption key using agreement secret
            val wrapCipher = Cipher.getInstance(WrapperCipherAlgorithm)
            wrapCipher.init(Cipher.WRAP_MODE, secret)
            val wrapIVBytes = wrapCipher.getIV
            val wrappedKey  = wrapCipher.wrap(encryptionKey)

            val wrappedStructure = ByteBuffer
              .allocate(WrappedStructureLength)
              .put(agreementIVBytes)
              .put(cipherIVBytes)
              .put(wrapIVBytes)
              .put(wrappedKey)

            recipientPublicKey -> wrappedStructure.array()
          }.toMap

          EncryptedForMany(encryptedData, recipientPubKeyToWrappedStructure)
        }.toEither
          .leftMap { ex =>
            log.error("Error in encrypt", ex)
            GenericError("Error in encrypt")
          }
      }
      .leftMap { cryptoError =>
        cryptoError.copy(message = s"EncryptForMany failed: ${cryptoError.message}")
      }
  }

  /**
    * Decrypt data, encrypted with GOST28147, given encryption key wrapped via DH agreement algorithm
    */
  def decrypt(encryptedWithWrappedKey: EncryptedForSingle,
              receiverPrivateKey: GostPrivateKey,
              senderPublicKey: PublicKey0): Either[CryptoError, Array[Byte]] =
    Try {
      log.warn("GOST-28147 encryption algorithm is deprecated and will be removed in WE Node v1.6.0")

      val EncryptedForSingle(encryptedData, wrappedStructure) = encryptedWithWrappedKey

      val cipherIVBytes    = new Array[Byte](IVLength)
      val agreementIVBytes = new Array[Byte](IVLength)
      val wrapIVBytes      = new Array[Byte](IVLength)

      val wrappedKey = new Array[Byte](WrappedKeyLength)

      ByteBuffer
        .wrap(wrappedStructure)
        .get(agreementIVBytes)
        .get(cipherIVBytes)
        .get(wrapIVBytes)
        .get(wrappedKey)

      // all necessary initialization vectors
      val agreementIV = new IvParameterSpec(agreementIVBytes)
      val cipherIV    = new IvParameterSpec(cipherIVBytes)
      val wrapIV      = new IvParameterSpec(wrapIVBytes)

      // agreement on receiver's side
      val recipientSecret = generateAgreementSecret(receiverPrivateKey, senderPublicKey, agreementIV)

      // unwrap the original symmetric encryption key
      val unwrapCipher = Cipher.getInstance(WrapperCipherAlgorithm)
      unwrapCipher.init(Cipher.UNWRAP_MODE, recipientSecret, wrapIV)
      val unwrappedKey = unwrapCipher.unwrap(wrappedKey, null, Cipher.SECRET_KEY).asInstanceOf[SecretKey]

      // decrypt the data
      val decipher = Cipher.getInstance(DataCipherAlgorithm)
      decipher.init(Cipher.DECRYPT_MODE, unwrappedKey, cipherIV, null)
      val decryptedData = decipher.doFinal(encryptedData, 0, encryptedData.length)

      decryptedData
    }.toEither
      .leftMap { ex =>
        log.error("Error in decrypt", ex)
        GenericError("Error in decrypt")
      }
}
