package com.wavesenterprise.crypto.internals

import com.wavesenterprise.crypto.internals.gost.KuznechikStream
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
    length    <- Gen.choose(1024 * 32, 1 * 1024 * 1024)
    dataBytes <- Gen.containerOfN[Array, Byte](length, Arbitrary.arbitrary[Byte])
  } yield dataBytes

  val internalChunkSize = 1024

  val genChunkSize: () => Int = () => Gen.choose(512, 2048).sample.get
  val random                  = new Random()

  property("Encrypt and decrypt some data") {
    KuznechikStream.ensureCryptoInitialized()
    val encryptionKeyGenerator: KeyGenerator = KeyGenerator.getInstance(JCP.GOST_K_CIPHER_NAME)

    forAll(genSomeBytes) { data =>
      val dataStream = new ByteArrayInputStream(data)
      val key = encryptionKeyGenerator.generateKey()

      val encryptor = KuznechikStream.Encryptor.custom(key,internalChunkSize)
      val decryptor = KuznechikStream.Decryptor.custom(key, internalChunkSize)

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
        resultDecrypted ++= decryptor(chunk)
      }

      resultDecrypted ++= decryptor.doFinal()

      assertResult(data)(resultDecrypted.toArray)
    }
  }

}
