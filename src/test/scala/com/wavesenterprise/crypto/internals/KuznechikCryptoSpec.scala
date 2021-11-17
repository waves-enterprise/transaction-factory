package com.wavesenterprise.crypto.internals

import com.wavesenterprise.crypto.internals.gost.{GostAlgorithms, KuznechikAlgorithm}
import com.wavesenterprise.utils.EitherUtils.EitherExt
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.io.ByteArrayInputStream
import scala.collection.mutable.ArrayBuffer

class KuznechikCryptoSpec extends GostCryptoSpec with ScalaCheckPropertyChecks {
  override val gostCrypto: GostAlgorithms = new KuznechikAlgorithm

  "Stream encrypt and decrypt" in {
    val genSomeBytes: Gen[Array[Byte]] = for {
      length    <- Gen.choose(32 * 1024, 1 * 1024 * 1024)
      dataBytes <- Gen.containerOfN[Array, Byte](length, Arbitrary.arbitrary[Byte])
    } yield dataBytes
    val genChunkSize: () => Int = () => Gen.choose(512, 2048).sample.get

    forAll(genSomeBytes) { data =>
      val sender    = gostCrypto.generateSessionKey()
      val recipient = gostCrypto.generateSessionKey()

      val (encryptedKey, encryptor) = gostCrypto.buildEncryptor(sender.getPrivate, recipient.getPublic).explicitGet()
      val dataStream                = new ByteArrayInputStream(data)

      val encryptedChunks = ArrayBuffer[Byte]()

      while (dataStream.available() != 0) {
        val chunk = dataStream.readNBytes(Math.min(genChunkSize(), dataStream.available()))
        encryptedChunks ++= encryptor(chunk)
      }
      encryptedChunks ++= encryptor.doFinal()

      val finalEncrypted = EncryptedForSingle(encryptedChunks.toArray, encryptedKey)

      val decryptor           = gostCrypto.buildDecryptor(finalEncrypted.wrappedStructure, recipient.getPrivate, sender.getPublic).explicitGet()
      val encryptedDataStream = new ByteArrayInputStream(finalEncrypted.encryptedData)
      val resultDecrypted     = ArrayBuffer[Byte]()

      while (encryptedDataStream.available() != 0) {
        val chunk = encryptedDataStream.readNBytes(Math.min(genChunkSize(), encryptedDataStream.available()))
        resultDecrypted ++= decryptor(chunk).explicitGet()
      }

      resultDecrypted ++= decryptor.doFinal().explicitGet()

      assertResult(data)(resultDecrypted.toArray)
    }
  }
}
