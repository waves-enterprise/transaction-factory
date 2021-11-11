package com.wavesenterprise.crypto.internals

import com.wavesenterprise.NoShrink
import com.wavesenterprise.account.Address
import com.wavesenterprise.crypto.GostKeystoreSpec
import com.wavesenterprise.crypto.internals.gost.{GostAlgorithms, GostKeyPair, GostPrivateKey}
import com.wavesenterprise.utils.EitherUtils.EitherExt
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FreeSpec, Matchers}

class GostCryptoSpec extends FreeSpec with Matchers with NoShrink {
  val gostCrypto = new GostAlgorithms

  val oneKilobyteInBytes: Int = 1024
  val oneMegabyteInBytes: Int = 1024 * oneKilobyteInBytes

  val sampleBytesGenerator: Gen[Array[Byte]] = for {
    length    <- Gen.choose(oneKilobyteInBytes, oneMegabyteInBytes)
    dataBytes <- Gen.containerOfN[Array, Byte](length, Arbitrary.arbitrary[Byte])
  } yield dataBytes

  val aliceRealKeypair: GostKeyPair =
    GostKeystoreSpec.keyStore.getKeyPair(GostKeystoreSpec.addressWithoutPassword, pwd = None).explicitGet()
  val bobRealKeypair: GostKeyPair =
    GostKeystoreSpec.keyStore.getKeyPair(GostKeystoreSpec.secondAddressWithoutPassword, pwd = None).explicitGet()
  val charlesRealKeypair: GostKeyPair =
    GostKeystoreSpec.keyStore.getKeyPair(GostKeystoreSpec.addressWithPassword, pwd = Some(GostKeystoreSpec.keyPassword.toCharArray)).explicitGet()

  "Basic key operations" - {
    "sign & verify signature" - {
      "with real keys (using testing gost_keystore)" in {
        val data = sampleBytesGenerator.sample.get

        val signature = gostCrypto.sign(aliceRealKeypair.getPrivate, data)

        gostCrypto.verify(signature, data, aliceRealKeypair.getPublic) shouldBe true
      }
    }
  }

  "Crypto operations" - {
    "encrypt & decrypt for single recipient" - {
      "with session (ephemeral) keys" in {
        val bytes = sampleBytesGenerator.sample.get

        val alice = gostCrypto.generateSessionKey()
        val bob   = gostCrypto.generateSessionKey()

        val encryptedDataWithWrappedKey = gostCrypto.encrypt(bytes, alice.getPrivate, bob.getPublic).explicitGet()
        val decryptedResult             = gostCrypto.decrypt(encryptedDataWithWrappedKey, bob.getPrivate, alice.getPublic).explicitGet()

        decryptedResult should contain theSameElementsAs bytes
      }
      "with real keys (using testing gost_keystore)" in {
        val bytes = sampleBytesGenerator.sample.get

        val encryptedDataWithWrappedKey = gostCrypto.encrypt(bytes, aliceRealKeypair.getPrivate, bobRealKeypair.getPublic).explicitGet()
        val decryptedResult             = gostCrypto.decrypt(encryptedDataWithWrappedKey, bobRealKeypair.getPrivate, aliceRealKeypair.getPublic).explicitGet()

        decryptedResult should contain theSameElementsAs bytes
      }
    }
    "encrypt & decrypt for self" - {
      "with session (ephemeral) keys" in {
        val senderKeyPair = gostCrypto.generateSessionKey()
        val data          = sampleBytesGenerator.sample.get

        val encryptedStuff  = gostCrypto.encrypt(data, senderKeyPair.getPrivate, senderKeyPair.getPublic).explicitGet()
        val decryptedResult = gostCrypto.decrypt(encryptedStuff, senderKeyPair.getPrivate, senderKeyPair.getPublic).explicitGet()

        decryptedResult should contain theSameElementsAs data
      }
      "with real keys (using testing gost_keystore)" in {
        val data = sampleBytesGenerator.sample.get

        val encryptedStuff  = gostCrypto.encrypt(data, aliceRealKeypair.getPrivate, aliceRealKeypair.getPublic).explicitGet()
        val decryptedResult = gostCrypto.decrypt(encryptedStuff, aliceRealKeypair.getPrivate, aliceRealKeypair.getPublic).explicitGet()

        decryptedResult should contain theSameElementsAs data
      }
    }
    "encryptForMany & decrypt each" - {
      "with session (ephemeral) keys" in {
        val recipientPublicToPrivate = List.fill(3)(gostCrypto.generateSessionKey()).map(keyPair => keyPair.getPublic -> keyPair.getPrivate).toMap
        val recipientAddressToPrivate: Map[Address, GostPrivateKey] = recipientPublicToPrivate.map {
          case (publicKey, privateKey) => Address.fromPublicKey(publicKey.getEncoded) -> privateKey
        }
        val senderDummyKeyPair = gostCrypto.generateSessionKey()
        val data               = sampleBytesGenerator.sample.get

        val encrypted = gostCrypto.encryptForMany(data, senderDummyKeyPair.getPrivate, recipientPublicToPrivate.keys.toSeq).explicitGet()

        encrypted.recipientPubKeyToWrappedKey.foreach {
          case (publicKey, wrappedStructure) =>
            val recipientPrivateKey = recipientAddressToPrivate(Address.fromPublicKey(publicKey.getEncoded))
            val decryptedData = gostCrypto
              .decrypt(EncryptedForSingle(encrypted.encryptedData, wrappedStructure), recipientPrivateKey, senderDummyKeyPair.getPublic)
              .explicitGet()

            decryptedData should contain theSameElementsAs data
        }
      }
      "with real keys (using testing gost_keystore)" in {
        val recipientPublicToPrivate = List(aliceRealKeypair, bobRealKeypair, charlesRealKeypair)
          .map(keyPair => keyPair.getPublic -> keyPair.getPrivate)
          .toMap

        val recipientAddressToPrivate: Map[Address, GostPrivateKey] = recipientPublicToPrivate.map {
          case (publicKey, privateKey) => Address.fromPublicKey(publicKey.getEncoded) -> privateKey
        }
        val data = sampleBytesGenerator.sample.get

        val encrypted = gostCrypto.encryptForMany(data, aliceRealKeypair.getPrivate, recipientPublicToPrivate.keys.toSeq).explicitGet()

        encrypted.recipientPubKeyToWrappedKey.foreach {
          case (publicKey, wrappedStructure) =>
            val recipientPrivateKey = recipientAddressToPrivate(Address.fromPublicKey(publicKey.getEncoded))
            val decryptedData = gostCrypto
              .decrypt(EncryptedForSingle(encrypted.encryptedData, wrappedStructure), recipientPrivateKey, aliceRealKeypair.getPublic)
              .explicitGet()

            decryptedData should contain theSameElementsAs data
        }
      }
    }
  }
}
