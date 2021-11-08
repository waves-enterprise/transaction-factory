package com.wavesenterprise.crypto.internals

import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import scala.collection.mutable.ArrayBuffer

/**
  * AesStream allows to encrypt large amount of data using buffer of specific size (8mb by default)
  *
  * Structure of data chunk:
  * 1. iv - initialization vector for AES-GCM, 16 bytes
  * 2. chunkCount - overall count of data chunks to check if some data was lost, 16 bytes, defined only for first chunk
  * 3. encryptedData - encrypted data, n bytes
  * 4. chunkIndex - index of the chunk to check if chunks was reordered by someone
  *
  */
object AesStream {
  private val defaultChunkSize = 8 * 1024 * 1024 // 8mb
  private val ivLength         = 16
  private val keySize          = 16 // 256 bit
  private val cipherName       = "AES/GCM/NoPadding"

  private val random = WavesAlgorithms.createSecureRandomInstance()

  private def keyToSpec(key: Array[Byte]): SecretKeySpec = {
    var keyBytes           = key
    val sha: MessageDigest = MessageDigest.getInstance("MD5")
    keyBytes = sha.digest(keyBytes)
    keyBytes = java.util.Arrays.copyOf(keyBytes, keySize)
    new SecretKeySpec(keyBytes, "AES")
  }

  class Encryptor private (key: Array[Byte], dataLength: Long, val chunkSize: Int = defaultChunkSize) {

    private val plainTextChunkSize            = chunkSize - 16 // 16 bytes for auth tag
    private val chunkSizeWithoutIdxAndAuthTag = chunkSize - 4 - 16

    private var chunkIndex = 0
    private val buffer     = ByteBuffer.allocate(plainTextChunkSize)

    private val cipher = Cipher.getInstance(cipherName)

    init()

    def init(): Unit = {
      def chunkCount: Int = {
        val dataBytesInChunk      = chunkSizeWithoutIdxAndAuthTag - ivLength
        val dataBytesInFirstChunk = dataBytesInChunk - 4
        if (dataLength > dataBytesInFirstChunk) {
          val tailDataLength = dataLength - dataBytesInFirstChunk
          (1 + ((tailDataLength - 1) / dataBytesInChunk) + 1).toInt
        } else {
          1
        }
      }

      val iv = genIv()
      buffer.put(iv)
      buffer.putInt(chunkCount)
      cipher.init(Cipher.ENCRYPT_MODE, keyToSpec(key), new GCMParameterSpec(128, iv))
    }

    def genIv(length: Int = ivLength): Array[Byte] = {
      val iv = new Array[Byte](length)
      random.nextBytes(iv)
      iv
    }

    def nextChunk(): Unit = {
      chunkIndex += 1
      buffer.position(0)

      val iv = genIv()
      buffer.put(iv)
      cipher.init(Cipher.ENCRYPT_MODE, keyToSpec(key), new GCMParameterSpec(128, iv)) // reset cipher state
    }

    def apply(data: Array[Byte], isFinalChunk: Boolean = false): Array[Byte] = {
      val result = ArrayBuffer[Byte]()

      var dataPosition = 0

      while (dataPosition != data.length) {
        val bytesProcessed = Math.min(data.length - dataPosition, buffer.remaining() - 4)
        buffer.put(data.slice(dataPosition, dataPosition + bytesProcessed))
        dataPosition += bytesProcessed

        if (buffer.position() == chunkSizeWithoutIdxAndAuthTag) {
          buffer.putInt(chunkIndex)
          val (iv, plainText) = buffer.array().splitAt(ivLength)
          result ++= (iv ++ cipher.doFinal(plainText))
          nextChunk()
        }
      }

      result.toArray
    }

    def doFinal(): Array[Byte] = {
      if (buffer.position() > 16) {
        buffer.putInt(chunkIndex)
        val (iv, plainText) = buffer.array().dropRight(buffer.limit() - buffer.position()).splitAt(ivLength)
        iv ++ cipher.doFinal(plainText)
      } else {
        Array[Byte]()
      }
    }
  }

  object Encryptor {
    def apply(key: Array[Byte], dataLength: Long): Encryptor = {
      new Encryptor(key, dataLength)
    }

    /**
      * Use only for tests!!!
      * (Decryption with chunk size which not equal to used in encryption won't give sane result)
      */
    def custom(key: Array[Byte], dataLength: Long, chunkSize: Int): Encryptor = {
      new Encryptor(key, dataLength, chunkSize)
    }
  }

  class Decryptor private (val key: Array[Byte], val chunkSize: Int = defaultChunkSize) {
    private var chunkCount = -1 // will be encoded in first chunk
    private var chunkIndex = 0

    private val buffer = ByteBuffer.allocate(chunkSize)
    private val cipher = Cipher.getInstance(cipherName)

    def init(iv: Array[Byte]): Unit = {
      cipher.init(Cipher.DECRYPT_MODE, keyToSpec(key), new GCMParameterSpec(128, iv)) // reset cipher state
    }

    def nextChunk(): Unit = {
      chunkIndex += 1
      buffer.position(0)
    }

    def apply(data: Array[Byte], isFinalChunk: Boolean = false): Array[Byte] = {
      def validateChunk(decrypted: Array[Byte]): Unit = {
        if (ByteBuffer.wrap(decrypted.takeRight(4)).getInt() != chunkIndex) {
          throw new RuntimeException("Invalid chunk index, chunk processing order must be the same as in encryption")
        }
      }

      val result       = ArrayBuffer[Byte]()
      var dataPosition = 0

      while (dataPosition != data.length) {

        val bytesProcessed = Math.min(data.length - dataPosition, buffer.remaining())
        buffer.put(data.slice(dataPosition, dataPosition + bytesProcessed))
        dataPosition += bytesProcessed

        if (!buffer.hasRemaining) {
          val (iv, plainText) = buffer.array().splitAt(ivLength)
          init(iv)
          val decrypted = cipher.doFinal(plainText)
          validateChunk(decrypted)

          val isFirstChunk = chunkCount == -1
          if (isFirstChunk) {
            chunkCount = ByteBuffer.wrap(decrypted.take(4)).getInt
            result ++= decrypted.drop(4).dropRight(4)
          } else {
            result ++= decrypted.dropRight(4)
          }

          nextChunk()
        }
      }

      result.toArray
    }

    def doFinal(): Array[Byte] = {

      def validate(decrypted: Array[Byte]): Unit = {
        if (buffer.position() == 0 && chunkCount != chunkIndex || chunkCount != chunkIndex + 1) {
          throw new RuntimeException("Wrong chunk count, data integrity is violated")
        }
        if (decrypted.nonEmpty && ByteBuffer.wrap(decrypted.takeRight(4)).getInt() != chunkIndex) {
          throw new RuntimeException("Invalid chunk index, chunk processing order must be the same as in encryption")
        }
      }

      if (buffer.position() != 0) {
        init(buffer.array().take(ivLength))
        val decrypted = cipher.doFinal(buffer.array().slice(ivLength, buffer.position()))

        val isFirstChunk = chunkCount == -1

        if (isFirstChunk) {
          chunkCount = ByteBuffer.wrap(decrypted.take(4)).getInt
          validate(decrypted)
          decrypted.drop(4).dropRight(4)
        } else {
          validate(decrypted)
          decrypted.dropRight(4)
        }

      } else {
        val result = Array[Byte]()
        validate(result)
        result
      }
    }

  }

  object Decryptor {
    def apply(key: Array[Byte]): Decryptor = {
      new Decryptor(key)
    }

    /**
      * Use only for tests!!!
      * (Decryption with chunk size which not equal to used in encryption won't give sane result)
      */
    def custom(key: Array[Byte], chunkSize: Int): Decryptor = {
      new Decryptor(key, chunkSize)
    }
  }

}
