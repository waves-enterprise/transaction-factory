package com.wavesenterprise.crypto.internals.gost

import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCP.params.OmacParamsSpec
import ru.CryptoPro.JCSP.JCSP

import java.nio.ByteBuffer
import java.security.Security
import javax.crypto.{Cipher, SecretKey}
import scala.collection.mutable.ArrayBuffer

/**
  * Encryptor and Decryptor allows to encrypt and decrypt large amount of data using buffer of specific size
  * (8mb by default)
  *
  * Structure of data chunk:
  * 1. iv - initialization vector for kuznechik-omac cipher, 16 bytes
  * 2. chunkStatus - byte which indicates is current chunk final(=0x01) or not(=0x00)
  * 3. chunkIndex - index of the chunk to check if chunks was reordered by someone
  * 4. encryptedData - encrypted data, n bytes
  * 5. chunkCount - overall count of data chunks to check if some data was lost, 16 bytes, appended to the end of chunk
  *
  */
object KuznechikStream {
  ensureCryptoInitialized()

  private val CipherName = JCP.GOST_K_CIPHER_NAME + "/OMAC_CTR/NoPadding"

  private val DefaultChunkSize = 8 * 1024 * 1024
  private val MacLength = 16
  private val IvLength = 16

  private val NonFinalChunkByte: Byte = 0x00
  private val FinalChunkByte: Byte    = 0x01

  def ensureCryptoInitialized(): Unit = {
    if (Option(Security.getProvider("JCSP")).isEmpty) {
      Security.addProvider(new JCSP())
      Security.addProvider(new JCSP())
    }
  }

  class Encryptor private(key: SecretKey, val chunkSize: Int = DefaultChunkSize) {
    private val plainTextChunkSize   = chunkSize - MacLength
    private val bufferWithoutDataPos = IvLength + 1 + 4 // 1 byte for final byte indication, 4 bytes for chunk idx

    private var chunkIndex = 0
    private val buffer     = ByteBuffer.allocate(plainTextChunkSize)

    private val cipher = Cipher.getInstance(CipherName)

    init(NonFinalChunkByte)

    def init(firstByte: Byte): Unit = {
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val iv = cipher.getIV
      buffer.position(0)
      buffer.put(iv)
      buffer.put(firstByte)
      buffer.putInt(chunkIndex)
      chunkIndex += 1
    }

    def encrypt(plainText: Array[Byte]): Array[Byte] = {
      val encrypted = cipher.doFinal(plainText)
      val macParams = cipher.getParameters.getParameterSpec(classOf[OmacParamsSpec])
      val mac       = macParams.getOmacValue
      encrypted ++ mac
    }

    def apply(data: Array[Byte]): Array[Byte] = {
      val result = ArrayBuffer[Byte]()

      var dataPosition = 0

      while (dataPosition != data.length) {
        val bytesProcessed = Math.min(data.length - dataPosition, buffer.remaining())
        buffer.put(data.slice(dataPosition, dataPosition + bytesProcessed))
        dataPosition += bytesProcessed

        if (!buffer.hasRemaining) {
          val (iv, plainText) = buffer.array().splitAt(IvLength)
          val encrypted       = encrypt(plainText)
          result ++= (iv ++ encrypted)
          init(NonFinalChunkByte)
        }
      }

      result.toArray
    }

    def doFinal(): Array[Byte] = {
      val chunkCount = if (buffer.remaining() >= 4) chunkIndex else chunkIndex + 1

      val result = if (buffer.remaining() >= 4) {
        buffer.put(16, FinalChunkByte)
        apply(ByteBuffer.allocate(4).putInt(chunkCount).array())
      } else {
        val encrypted = apply(ByteBuffer.allocate(4).putInt(chunkCount).array())
        buffer.put(16, FinalChunkByte)
        encrypted
      }

      if (buffer.position() > bufferWithoutDataPos) {
        val (iv, plainText) = buffer.array().dropRight(buffer.remaining()).splitAt(IvLength)
        result ++ iv ++ encrypt(plainText)
      } else {
        result
      }
    }
  }

  object Encryptor {
    def apply(key: SecretKey): Encryptor = {
      new Encryptor(key)
    }

    /**
      * Use only for tests!!!
      */
    def custom(key: SecretKey, chunkSize: Int): Encryptor = {
      new Encryptor(key, chunkSize)
    }
  }

  class Decryptor private(val key: SecretKey, val chunkSize: Int = DefaultChunkSize) {
    private val cipher = Cipher.getInstance(CipherName)
    private val buffer = ByteBuffer.allocate(chunkSize)

    private var chunkIndex                     = 0
    private val last4PlainDataBytesOfPrevChunk = ByteBuffer.allocate(4) // just added to not skip 'chunkCount' filed while decrypting
    private var isLastDecryptedFinal           = false

    def nextChunk(): Unit = {
      chunkIndex += 1
      buffer.position(0)
    }

    def decryptBuffer(): Array[Byte] = {
      val data      = buffer.array().dropRight(buffer.remaining())
      val iv        = data.take(IvLength)
      val mac       = data.takeRight(MacLength)
      val encrypted = data.slice(IvLength, data.length - MacLength)
      cipher.init(Cipher.DECRYPT_MODE, key, new OmacParamsSpec(mac, iv))
      val decrypted = cipher.doFinal(encrypted)
      isLastDecryptedFinal = decrypted.head == FinalChunkByte
      decrypted
    }

    def saveLast4PlainTextBytes(decrypted: Array[Byte]): Unit = {
      val plainTextLength = decrypted.length - 4 - 1
      last4PlainDataBytesOfPrevChunk.position(0)
      last4PlainDataBytesOfPrevChunk.put(decrypted.takeRight(Math.min(4, plainTextLength)))
    }

    def apply(data: Array[Byte]): Array[Byte] = {
      def validateChunk(decrypted: Array[Byte]): Unit = {
        if (ByteBuffer.wrap(decrypted.slice(1, 5)).getInt() != chunkIndex) {
          throw new RuntimeException("Invalid chunk index, chunk processing order must be the same as in encryption")
        }
      }

      val result       = ArrayBuffer[Byte]()
      var dataPosition = 0

      while (dataPosition != data.length) {
        if (!buffer.hasRemaining) {
          nextChunk()
        }

        val bytesProcessed = Math.min(data.length - dataPosition, buffer.remaining())
        buffer.put(data.slice(dataPosition, dataPosition + bytesProcessed))
        dataPosition += bytesProcessed

        if (!buffer.hasRemaining) {
          val decrypted = decryptBuffer()
          validateChunk(decrypted)

          result ++= last4PlainDataBytesOfPrevChunk.array().take(last4PlainDataBytesOfPrevChunk.position()) ++
            decrypted.drop(5).dropRight(4)

          saveLast4PlainTextBytes(decrypted)
        }
      }

      result.toArray
    }

    def doFinal(): Array[Byte] = {

      def validate(bytes: Array[Byte]): Unit = {
        def expectedChunkCount() = {
          val chunkCountBytes = (last4PlainDataBytesOfPrevChunk.array() ++ bytes.drop(5)).takeRight(4)
          ByteBuffer
            .wrap(chunkCountBytes)
            .position(0)
            .getInt()
        }

        val actualChunkCount = chunkIndex + 1

        if ((bytes.nonEmpty && ByteBuffer.wrap(bytes.slice(1, 5)).getInt() != chunkIndex) ||
          !isLastDecryptedFinal ||
          actualChunkCount != expectedChunkCount()) {
          throw new RuntimeException("Decryption failed! Probably some data chunks was reordered or lost")
        }
      }

      if (buffer.position() != 0 && buffer.hasRemaining) {
        val decryptedData = decryptBuffer()
        validate(decryptedData)
        (last4PlainDataBytesOfPrevChunk.array() ++ decryptedData.drop(5)).dropRight(4)
      } else {
        val result = Array[Byte]()
        validate(result)
        result
      }
    }

  }

  object Decryptor {
    def apply(key: SecretKey): Decryptor = {
      new Decryptor(key)
    }

    /**
      * Use only for tests!!!
      * (Decryption with chunk size which not equal to used in encryption won't give sane result)
      */
    def custom(key: SecretKey, chunkSize: Int): Decryptor = {
      new Decryptor(key, chunkSize)
    }
  }

}
