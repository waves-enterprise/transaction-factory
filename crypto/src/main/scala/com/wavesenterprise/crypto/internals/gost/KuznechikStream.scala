package com.wavesenterprise.crypto.internals.gost

import com.wavesenterprise.crypto.internals.StreamCipher
import com.wavesenterprise.crypto.internals.StreamCipher.{AbstractDecryptor, AbstractEncryptor}
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCP.params.OmacParamsSpec
import ru.CryptoPro.JCSP.JCSP

import java.security.{Key, Security}
import javax.crypto.Cipher

object KuznechikStream {
  ensureCryptoInitialized()

  private val CipherName = JCP.GOST_K_CIPHER_NAME + "/OMAC_CTR/NoPadding"

  def ensureCryptoInitialized(): Unit = {
    if (Option(Security.getProvider("JCSP")).isEmpty) {
      Security.addProvider(new JCSP())
    }
  }

  class Encryptor private (key: Key, chunkSize: Int) extends AbstractEncryptor(chunkSize) {
    override protected lazy val ivLength: Int = 16

    override protected lazy val macLength: Int = 16

    override protected lazy val cipher: Cipher = Cipher.getInstance(CipherName)

    override protected def resetCipher(): Unit = {
      cipher.init(Cipher.ENCRYPT_MODE, key)
    }

    override protected def getIv: Array[Byte] = {
      cipher.getIV
    }

    override protected def encrypt(plainText: Array[Byte]): Array[Byte] = {
      val encrypted = cipher.doFinal(plainText)
      val macParams = cipher.getParameters.getParameterSpec(classOf[OmacParamsSpec])
      val mac       = macParams.getOmacValue
      encrypted ++ mac
    }

  }

  object Encryptor {

    /**
      * Warning: decryption with chunk size which not equal to used in encryption won't give same result.
      */
    def apply(key: Key, chunkSize: Int): Encryptor = new Encryptor(key, chunkSize)
  }

  class Decryptor private (key: Key, chunkSize: Int) extends AbstractDecryptor(chunkSize) {
    override protected lazy val ivLength: Int = 16

    override protected lazy val macLength: Int = 16

    override protected val cipher: Cipher = Cipher.getInstance(CipherName)

    override def decrypt(): Array[Byte] = {
      val data      = buffer.array().dropRight(buffer.remaining())
      val iv        = data.take(ivLength)
      val mac       = data.takeRight(macLength)
      val encrypted = data.slice(ivLength, data.length - macLength)
      cipher.init(Cipher.DECRYPT_MODE, key, new OmacParamsSpec(mac, iv))
      val decrypted = cipher.doFinal(encrypted)
      isLastDecryptedFinal = decrypted.head == StreamCipher.FinalChunkByte
      decrypted
    }
  }

  object Decryptor {

    /**
      * Warning: decryption with chunk size which not equal to used in encryption won't give same result.
      */
    def apply(key: Key, chunkSize: Int): Decryptor = new Decryptor(key, chunkSize)
  }

}