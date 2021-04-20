package com.wavesenterprise.crypto.internals.gost

import cats.implicits.catsSyntaxEither
import com.wavesenterprise.crypto.internals._
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCP.params.Kexp15ParamsSpec

import java.nio.ByteBuffer
import javax.crypto.spec.IvParameterSpec
import javax.crypto.{Cipher, KeyAgreement, KeyGenerator}
import scala.util.Try

class KuznechikAlgorithm extends GostAlgorithms {
  override private[gost] lazy val CryptoAlgorithm = JCP.GOST_K_CIPHER_NAME
  private val KeyAlgorithm                        = JCP.GOST_DH_2012_256_NAME

  private val UKMLength                         = 32
  private val ExpUKMLength                      = 8
  private val ExtendedExpUKMLength              = 8
  private val AgreementIVBytes                  = 16
  private val CipherIVLength                    = 16
  override private[gost] val WrappedKeyLength   = 48
  override lazy val WrappedStructureLength: Int = ExpUKMLength + ExtendedExpUKMLength + AgreementIVBytes + CipherIVLength + WrappedKeyLength

  override private[gost] val WrapperCipherAlgorithm = CryptoAlgorithm + "/KEXP_2015_K_EXPORT/NoPadding"

  /**
    * Generates symmetric data encryption key
    */
  override private[gost] val encryptionKeyGenerator = KeyGenerator.getInstance(CryptoAlgorithm)

  /**
    * Generates a secret from Diffie-Hellman key agreement for two keys
    *   - agreementIV - initialization vector, should be passed or created externally
    */
  override private[gost] def generateAgreementSecret(senderPrivateKey: GostPrivateKey,
                                                     recipientPublicKey: AbstractGostPublicKey,
                                                     agreementIV: IvParameterSpec) = {
    val keyAgreement = KeyAgreement.getInstance(KeyAlgorithm)
    keyAgreement.init(senderPrivateKey.internal, agreementIV)
    keyAgreement.doPhase(recipientPublicKey.internal, true)
    keyAgreement.generateSecret(CryptoAlgorithm)
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
    * Encryption for a single recipient with GOST28147 and DH agreement key
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
    * Encryption for many recipients
    * Data is encrypted with GOST28147, then each recipient gets his own key, wrapped via DH agreement algorithm
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
    * Decrypt data, encrypted with GOST28147, given encryption key wrapped via DH agreement algorithm
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
}
