package com.wavesenterprise.crypto.internals

import com.wavesenterprise.utils.EitherUtils.EitherExt
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.io.ByteArrayInputStream
import javax.crypto.AEADBadTagException
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class AesStreamSpec extends PropSpec with ScalaCheckPropertyChecks with Matchers {

  val genSomeBytes: Gen[Array[Byte]] = for {
    length    <- Gen.choose(1024 * 32, 1 * 1024 * 1024)
    dataBytes <- Gen.containerOfN[Array, Byte](length, Arbitrary.arbitrary[Byte])
  } yield dataBytes

  val intenalChunkSize = 1024

  val genChunkSize: () => Int = () => Gen.choose(512, 2048).sample.get
  val random                  = new Random()

  property("Encrypt and decrypt some data") {

    forAll(genSomeBytes) { data =>
      val sender    = WavesAlgorithms.generateKeyPair()
      val recipient = WavesAlgorithms.generateKeyPair()

      val (encryptedKey, encryptor) = WavesAlgorithms.buildEncryptor(sender.getPrivate, recipient.getPublic, data.length).explicitGet()
      val dataStream                = new ByteArrayInputStream(data)

      val encryptedChunks = ArrayBuffer[Byte]()

      while (dataStream.available() != 0) {
        val chunk = dataStream.readNBytes(Math.min(genChunkSize(), dataStream.available()))
        encryptedChunks ++= encryptor(chunk)
      }
      encryptedChunks ++= encryptor.doFinal()

      val finalEncrypted = EncryptedForSingle(encryptedChunks.toArray, encryptedKey)

      val decryptor           = WavesAlgorithms.buildDecryptor(finalEncrypted.wrappedStructure, recipient.getPrivate, sender.getPublic).explicitGet()
      val encryptedDataStream = new ByteArrayInputStream(finalEncrypted.encryptedData)
      val resultDecrypted     = ArrayBuffer[Byte]()

      while (encryptedDataStream.available() != 0) {
        val chunk = encryptedDataStream.readNBytes(Math.min(genChunkSize(), encryptedDataStream.available()))
        resultDecrypted ++= decryptor(chunk)
      }

      resultDecrypted ++= decryptor.doFinal()

      assertResult(data)(resultDecrypted.toArray)
    }
  }

  property("Decryptor falls on changing encrypted data") {
    forAll(genSomeBytes) { data =>
      val sender    = WavesAlgorithms.generateKeyPair()
      val recipient = WavesAlgorithms.generateKeyPair()

      val (encryptedKey, encryptor) = WavesAlgorithms.buildEncryptor(sender.getPrivate, recipient.getPublic, data.length).explicitGet()
      val encrypted                 = encryptor(data) ++ encryptor.doFinal()
      val changedByteIdx            = random.nextInt(encrypted.length)

      encrypted(changedByteIdx) = (encrypted(changedByteIdx) + 1).toByte

      val decryptor = WavesAlgorithms.buildDecryptor(encryptedKey, recipient.getPrivate, sender.getPublic).explicitGet()

      assertThrows[AEADBadTagException] {
        decryptor(encrypted) ++ decryptor.doFinal()
      }
    }

  }

  property("Decryptor falls on lost of data chunks") {
    forAll(genSomeBytes) { data =>
      val key = new Array[Byte](16)
      random.nextBytes(key)

      val encryptor = AesStream.Encryptor.custom(key, data.length, intenalChunkSize)
      val decryptor = AesStream.Decryptor.custom(key, intenalChunkSize)

      val encrypted         = encryptor(data) ++ encryptor.doFinal()
      val encryptedWithLost = encrypted.take(intenalChunkSize)

      val ex = intercept[RuntimeException] {
        decryptor(encryptedWithLost) ++ decryptor.doFinal()
      }

      assertResult("Wrong chunk count, data integrity is violated")(ex.getMessage)
    }
  }

  property("Decryptor falls on data chunks reordering") {
    forAll(genSomeBytes) { data =>
      val key = new Array[Byte](16)
      random.nextBytes(key)

      val encryptor = AesStream.Encryptor.custom(key, data.length, intenalChunkSize)
      val decryptor = AesStream.Decryptor.custom(key, intenalChunkSize)

      val encrypted          = encryptor(data) ++ encryptor.doFinal()
      val reorderedEncrypted = encrypted.slice(intenalChunkSize, intenalChunkSize * 2) ++ encrypted.take(intenalChunkSize)

      val ex = intercept[RuntimeException] {
        decryptor(reorderedEncrypted) ++ decryptor.doFinal()
      }

      assertResult("Invalid chunk index, chunk processing order must be the same as in encryption")(ex.getMessage)
    }
  }

}
