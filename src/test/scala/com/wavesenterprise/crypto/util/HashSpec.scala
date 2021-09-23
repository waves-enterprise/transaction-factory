package com.wavesenterprise.crypto.util

import org.bouncycastle.util.encoders.Hex
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FreeSpec, Matchers}

class HashSpec extends FreeSpec with Matchers {

  val mb: Int = 1024 * 1024
  val dataGen: Gen[Array[Byte]] = for {
    dataLength <- Gen.choose(mb, 2 * mb)
    data <- Gen.containerOfN[Array, Byte](dataLength, Arbitrary.arbitrary[Byte])
  } yield data

  "A Sha256Hash" - {

    "must produce correct hash" in {
      val data = (1 to 100000).map(_.toByte).toArray
      val expectedHash = "dca13b0d568eac892b8b33e3fc6871ce9074d4091702056bc078c10afd7a62a9"

      val hasher = Sha256Hash()
      hasher.update(data)
      val hash = hasher.result()

      assertResult(expectedHash)(Hex.toHexString(hash))
    }

    "must produce the same hash when data processing by chunk and entirely" in {
      val data = dataGen.sample.get

      val hasher = Sha256Hash()

      hasher.update(data)
      val hash = hasher.result()

      data.grouped(1024).foreach(hasher.update)
      val chunkHash = hasher.result()

      hash shouldEqual chunkHash
    }
  }

}
