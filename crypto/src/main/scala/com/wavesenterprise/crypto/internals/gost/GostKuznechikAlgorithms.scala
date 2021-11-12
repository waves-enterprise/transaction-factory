package com.wavesenterprise.crypto.internals.gost

import cats.syntax.either._
import com.wavesenterprise.crypto.internals._
import ru.CryptoPro.Crypto.CryptoProvider
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCP.params.{CryptDhAllowedSpec, CryptParamsSpec, Kexp15ParamsSpec}
import ru.CryptoPro.JCSP.JCSP
import ru.CryptoPro.reprov.RevCheck
import ru.CryptoPro.ssl.Provider

import java.nio.ByteBuffer
import java.security.{PublicKey => _, _}
import javax.crypto.spec.IvParameterSpec
import javax.crypto.{Cipher, KeyAgreement, KeyGenerator, SecretKey}
import scala.util.Try

class GostKuznechikAlgorithms extends CryptoAlgorithms[GostKeyPair] {

  Security.addProvider(new JCSP())
  Security.addProvider(new RevCheck())
  Security.addProvider(new CryptoProvider())
  val provider = new Provider()
  Security.addProvider(provider)

  private[gost] lazy val CryptoAlgorithm = JCP.GOST_K_CIPHER_NAME
  private val KeyAlgorithm               = JCP.GOST_DH_2012_256_NAME
  private val ProviderName               = JCSP.PROVIDER_NAME

  private[gost] val DataCipherAlgorithm    = CryptoAlgorithm + "/CFB/NoPadding"
  private[gost] val WrapperCipherAlgorithm = CryptoAlgorithm + "/KEXP_2015_K_EXPORT/NoPadding"

  override val DigestSize: Int       = messageDigestInstance().getDigestLength
  override val SignatureLength: Int  = 64
  override val KeyLength: Int        = GostPublicKey.length
  override val SessionKeyLength: Int = 104

  private val UKMLength                = 32
  private val ExpUKMLength             = 8
  private val ExtendedExpUKMLength     = 8
  private val AgreementIVBytes         = 16
  private val CipherIVLength           = 16
  private[gost] val WrappedKeyLength   = 48
  lazy val WrappedStructureLength: Int = ExpUKMLength + ExtendedExpUKMLength + AgreementIVBytes + CipherIVLength + WrappedKeyLength

  private val secureRandom = SecureRandom.getInstance(JCP.CP_RANDOM, ProviderName)

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
  private[gost] val encryptionKeyGenerator = KeyGenerator.getInstance(CryptoAlgorithm)

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
    * Generates a secret from Diffie-Hellman key agreement for two keys
    *   - agreementIV - initialization vector, should be passed or created externally
    */
  private[gost] def generateAgreementSecret(senderPrivateKey: GostPrivateKey,
                                            recipientPublicKey: AbstractGostPublicKey,
                                            agreementIV: IvParameterSpec) = {
    val keyAgreement = KeyAgreement.getInstance(KeyAlgorithm)
    keyAgreement.init(senderPrivateKey.internal, agreementIV)
    keyAgreement.doPhase(recipientPublicKey.internal, true)
    keyAgreement.generateSecret(CryptoAlgorithm)
  }

  /**
    * Generates random bytes to use as initialization vector (IV)
    */
  private[gost] def generateIVBytes(size: Int): Array[Byte] = {
    val IvBytes = new Array[Byte](size)
    secureRandom.nextBytes(IvBytes)
    IvBytes
  }

  private def generateIV: (IvParameterSpec, Kexp15ParamsSpec, Array[Byte], Array[Byte]) = {
    val UKM                          = generateIVBytes(UKMLength)
    val bUKM                         = UKM.take(AgreementIVBytes).reverse
    val agreementIV: IvParameterSpec = new IvParameterSpec(bUKM)
    val expUKM: Array[Byte]          = UKM.takeRight(ExpUKMLength)
    val extendedUKM: Array[Byte]     = UKM.slice(16, 16 + ExpUKMLength)
    val kExpSpec: Kexp15ParamsSpec   = new Kexp15ParamsSpec(expUKM, extendedUKM)
    (agreementIV, kExpSpec, expUKM, extendedUKM)
  }

  /**
    * Encryption for a single recipient with Kuznechik and DH agreement key
    */
  override def encrypt(data: Array[Byte],
                       senderPrivateKey: GostPrivateKey,
                       recipientPublicKey: AbstractGostPublicKey): Either[CryptoError, EncryptedForSingle] = {
    Try {
      val (agreementIV, kExpSpec, expUKM, extendedUKM) = generateIV
      val agreementSecretKey                           = generateAgreementSecret(senderPrivateKey, recipientPublicKey, agreementIV)
      val encryptionKey                                = encryptionKeyGenerator.generateKey()

      val cipher = Cipher.getInstance(DataCipherAlgorithm)
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
      val encryptedData = cipher.doFinal(data, 0, data.length)

      val wrapCipher = Cipher.getInstance(WrapperCipherAlgorithm)
      wrapCipher.init(Cipher.WRAP_MODE, agreementSecretKey, kExpSpec)
      val wrappedKey = wrapCipher.wrap(encryptionKey)

      val wrappedStructure = ByteBuffer
        .allocate(WrappedStructureLength)
        .put(agreementIV.getIV)
        .put(cipher.getIV)
        .put(expUKM)
        .put(extendedUKM)
        .put(wrappedKey)

      EncryptedForSingle(encryptedData, wrappedStructure.array())
    }.toEither
      .leftMap { ex =>
        log.error("Error in encrypt", ex)
        GenericError("Error in encrypt")
      }
  }

  /**
    *
    * @param senderPrivateKey
    * @param recipientPublicKey
    * @return (encrypted encryption key with some additional data, stream encryptor)
    */
  override def buildEncryptor(senderPrivateKey: GostPrivateKey,
                              recipientPublicKey: AbstractGostPublicKey): Either[CryptoError, (Array[Byte], KuznechikStream.Encryptor)] = {
    Try {
      val (agreementIV, kExpSpec, expUKM, extendedUKM) = generateIV
      val agreementSecretKey                           = generateAgreementSecret(senderPrivateKey, recipientPublicKey, agreementIV)
      val encryptionKey                                = encryptionKeyGenerator.generateKey()

      val wrapCipher = Cipher.getInstance(WrapperCipherAlgorithm)
      wrapCipher.init(Cipher.WRAP_MODE, agreementSecretKey, kExpSpec)
      val wrappedKey = wrapCipher.wrap(encryptionKey)

      val wrappedStructure = ByteBuffer
        .allocate(WrappedStructureLength)
        .put(agreementIV.getIV)
        .put(expUKM)
        .put(extendedUKM)
        .put(wrappedKey)

      (wrappedStructure.array(), KuznechikStream.Encryptor(encryptionKey))
    }.toEither
      .leftMap { ex =>
        log.error("Error in encryptor creating process", ex)
        GenericError("Error in encryptor creating process")
      }
  }

  /**
    * Encryption for many recipients
    * Data is encrypted with Kuznechik, then each recipient gets his own key, wrapped via DH agreement algorithm
    */
  override def encryptForMany(data: Array[Byte],
                              senderPrivateKey: GostPrivateKey,
                              recipientPublicKeys: Seq[AbstractGostPublicKey]): Either[CryptoError, EncryptedForMany] = {
    Either
      .cond(recipientPublicKeys.nonEmpty, (), GenericError("Cannot encrypt for an empty list of recipients"))
      .flatMap { _ =>
        Try {
          val encryptionKey = encryptionKeyGenerator.generateKey()

          val cipher = Cipher.getInstance(DataCipherAlgorithm)
          cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
          val encryptedData = cipher.doFinal(data, 0, data.length)

          val recipientPubKeyToWrappedStructure: Map[PublicKey, Array[Byte]] = recipientPublicKeys.map { recipientPublicKey =>
            val (agreementIV, kExpSpec, expUKM, extendedUKM) = generateIV
            val agreementSecretKey                           = generateAgreementSecret(senderPrivateKey, recipientPublicKey, agreementIV)

            val wrapCipher = Cipher.getInstance(WrapperCipherAlgorithm)
            wrapCipher.init(Cipher.WRAP_MODE, agreementSecretKey, kExpSpec)
            val wrappedKey = wrapCipher.wrap(encryptionKey)

            val wrappedStructure = ByteBuffer
              .allocate(WrappedStructureLength)
              .put(agreementIV.getIV)
              .put(cipher.getIV)
              .put(expUKM)
              .put(extendedUKM)
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
    * Decrypt data, encrypted with Kuznechik, given encryption key wrapped via DH agreement algorithm
    */
  override def decrypt(encryptedWithWrappedKey: EncryptedForSingle,
                       receiverPrivateKey: GostPrivateKey,
                       senderPublicKey: PublicKey0): Either[CryptoError, Array[Byte]] = {
    Try {
      val EncryptedForSingle(encryptedData, wrappedStructure) = encryptedWithWrappedKey

      val cipherIVBytes    = new Array[Byte](CipherIVLength)
      val agreementIVBytes = new Array[Byte](AgreementIVBytes)
      val expUkmBytes      = new Array[Byte](ExpUKMLength)
      val extendedUkmBytes = new Array[Byte](ExtendedExpUKMLength)
      val wrappedKey       = new Array[Byte](WrappedKeyLength)

      ByteBuffer
        .wrap(wrappedStructure)
        .get(agreementIVBytes)
        .get(cipherIVBytes)
        .get(expUkmBytes)
        .get(extendedUkmBytes)
        .get(wrappedKey)

      val agreementIV = new IvParameterSpec(agreementIVBytes)
      val cipherIV    = new IvParameterSpec(cipherIVBytes)
      val kExpSpec    = new Kexp15ParamsSpec(expUkmBytes, extendedUkmBytes)

      val agreementSecretKey = generateAgreementSecret(receiverPrivateKey, senderPublicKey, agreementIV)

      val unwrapCipher = Cipher.getInstance(WrapperCipherAlgorithm)
      unwrapCipher.init(Cipher.UNWRAP_MODE, agreementSecretKey, kExpSpec)
      val encryptionKey = unwrapCipher.unwrap(wrappedKey, null, Cipher.SECRET_KEY)

      val cipher = Cipher.getInstance(DataCipherAlgorithm)
      cipher.init(Cipher.DECRYPT_MODE, encryptionKey, cipherIV, null)
      cipher.doFinal(encryptedData, 0, encryptedData.length)
    }.toEither
      .leftMap { ex =>
        log.error("Error in decrypt", ex)
        ex.printStackTrace()
        if (ex.getCause != null) ex.printStackTrace()
        GenericError("Error in decrypt")
      }
  }

  /**
    *
    * @param encryptedKeyInfo - encrypted encryption key with some additional data
    * @param recipientPrivateKey
    * @param senderPublicKey
    * @return decryptor object which can be used for stream data decryption
    */
  override def buildDecryptor(encryptedKeyInfo: Array[Byte],
                              recipientPrivateKey: GostPrivateKey,
                              senderPublicKey: AbstractGostPublicKey): Either[CryptoError, KuznechikStream.Decryptor] = {

    Try {

      val agreementIVBytes = new Array[Byte](AgreementIVBytes)
      val expUkmBytes      = new Array[Byte](ExpUKMLength)
      val extendedUkmBytes = new Array[Byte](ExtendedExpUKMLength)
      val wrappedKey       = new Array[Byte](WrappedKeyLength)

      ByteBuffer
        .wrap(encryptedKeyInfo)
        .get(agreementIVBytes)
        .get(expUkmBytes)
        .get(extendedUkmBytes)
        .get(wrappedKey)

      val agreementIV = new IvParameterSpec(agreementIVBytes)
      val kExpSpec    = new Kexp15ParamsSpec(expUkmBytes, extendedUkmBytes)

      val agreementSecretKey = generateAgreementSecret(recipientPrivateKey, senderPublicKey, agreementIV)

      val unwrapCipher = Cipher.getInstance(WrapperCipherAlgorithm)
      unwrapCipher.init(Cipher.UNWRAP_MODE, agreementSecretKey, kExpSpec)
      val encryptionKey = unwrapCipher.unwrap(wrappedKey, null, Cipher.SECRET_KEY)

      KuznechikStream.Decryptor(encryptionKey)
    }.toEither
      .leftMap { ex =>
        log.error("Error in decryptor creating process", ex)
        GenericError("Error in decryptor creating process")
      }
  }
}
