package com.wavesenterprise.crypto.internals


import com.wavesenterprise.crypto.internals.gost.KuznechikStream
import com.wavesenterprise.utils.EitherUtils.EitherExt
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.PropSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.CryptoPro.JCP.JCP

import java.io.ByteArrayInputStream
import javax.crypto.KeyGenerator
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class KuznechikStreamSpec extends PropSpec with ScalaCheckDrivenPropertyChecks {

  val genSomeBytes: Gen[Array[Byte]] = for {
    length    <- Gen.choose(32 * 1024, 1 * 1024 * 1024)
    dataBytes <- Gen.containerOfN[Array, Byte](length, Arbitrary.arbitrary[Byte])
  } yield dataBytes

  val genChunkSize: () => Int = () => Gen.choose(512, 2048).sample.get
  val random                  = new Random()

  KuznechikStream.ensureCryptoInitialized()
  val encryptionKeyGenerator: KeyGenerator = KeyGenerator.getInstance(JCP.GOST_K_CIPHER_NAME)

  property("Encrypt and decrypt some data") {

    forAll(genSomeBytes) { data =>
      val chunkSize: Int = 128 * 1024

      val dataStream = new ByteArrayInputStream(data)
      val key        = encryptionKeyGenerator.generateKey()

      val encryptor = KuznechikStream.Encryptor.custom(key, chunkSize)
      val decryptor = KuznechikStream.Decryptor.custom(key, chunkSize)

      val encryptedChunks = ArrayBuffer[Byte]()

      while (dataStream.available() != 0) {
        val chunk = dataStream.readNBytes(Math.min(genChunkSize(), dataStream.available()))
        encryptedChunks ++= encryptor(chunk)
      }
      encryptedChunks ++= encryptor.doFinal()

      val encryptedDataStream = new ByteArrayInputStream(encryptedChunks.toArray)
      val resultDecrypted     = ArrayBuffer[Byte]()

      while (encryptedDataStream.available() != 0) {
        val chunk = encryptedDataStream.readNBytes(Math.min(genChunkSize(), encryptedDataStream.available()))
        resultDecrypted ++= decryptor(chunk).explicitGet()
      }

      resultDecrypted ++= decryptor.doFinal().explicitGet()

      assertResult(data)(resultDecrypted.toArray)
    }
  }

  property("Decryption fail on changing encrypted data") {
    forAll(genSomeBytes) { data =>
      val key = encryptionKeyGenerator.generateKey()

      val chunkSize      = 128 * 1024
      val encryptor      = KuznechikStream.Encryptor.custom(key, chunkSize)
      val decryptor      = KuznechikStream.Decryptor.custom(key, chunkSize)
      val encrypted      = encryptor(data) ++ encryptor.doFinal()
      val changedByteIdx = random.nextInt(encrypted.length)

      encrypted(changedByteIdx) = (encrypted(changedByteIdx) + 1).toByte

      val result = for {
        decrypted      <- decryptor(encrypted)
        finalDecrypted <- decryptor.doFinal()
      } yield decrypted ++ finalDecrypted

      assertResult("Cipher was not initiated for encryption or decryption operation.")(result.left.get.message)
    }

  }

  property("Decryption fail on lost of data chunks") {
    forAll(genSomeBytes) { data =>
      val key = encryptionKeyGenerator.generateKey()

      val chunkSize = 16 * 1024

      val encryptor = KuznechikStream.Encryptor.custom(key, chunkSize)
      val decryptor = KuznechikStream.Decryptor.custom(key, chunkSize)

      val encrypted         = encryptor(data) ++ encryptor.doFinal()
      val encryptedWithLost = encrypted.take(chunkSize)

      val result = for {
        decrypted      <- decryptor(encryptedWithLost)
        finalDecrypted <- decryptor.doFinal()
      } yield decrypted ++ finalDecrypted

      assertResult("Decryption failed! Probably some data chunks was reordered or lost")(result.left.get.message)
    }
  }

  property("Decryption fail on data chunks reordering") {
    forAll(genSomeBytes) { data =>
      val key = encryptionKeyGenerator.generateKey()

      val chunkSize = 16 * 1024

      val encryptor = KuznechikStream.Encryptor.custom(key, chunkSize)
      val decryptor = KuznechikStream.Decryptor.custom(key, chunkSize)

      val encrypted          = encryptor(data) ++ encryptor.doFinal()
      val reorderedEncrypted = encrypted.slice(chunkSize, chunkSize * 2) ++ encrypted.take(chunkSize)

      val result = for {
        decrypted      <- decryptor(reorderedEncrypted)
        finalDecrypted <- decryptor.doFinal()
      } yield decrypted ++ finalDecrypted

      assertResult("Invalid chunk index, chunk processing order must be the same as in encryption")(result.left.get.message)
    }
  }
}
