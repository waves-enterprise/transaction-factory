package com.wavesenterprise.crypto

import java.io.File

import com.wavesenterprise.NoShrink
import com.wavesenterprise.account.Address
import com.wavesenterprise.crypto.internals.KeyStore
import com.wavesenterprise.crypto.internals.gost.{GostCryptoContext, GostKeyPair}
import org.scalatest.{FreeSpec, Matchers}

class GostKeystoreSpec extends FreeSpec with Matchers with NoShrink {
  import GostKeystoreSpec._

  "Testing Gost keystore" - {
    "keypair with password" - {
      "retrieves from keystore with correct password" in {
        val maybePrivateKey = keyStore.getKey(addressWithPassword, pwd = Some(keyPassword.toCharArray))
        maybePrivateKey shouldBe 'right
      }
      "doesn't retrieve from keystore on wrong password" in {
        val maybePrivateKey = keyStore.getKey(addressWithPassword, pwd = Some((keyPassword ++ "bad").toCharArray))
        maybePrivateKey shouldBe 'left
      }
    }
    "keypair without password" - {
      "retrieves from keystore with empty-array password" in {
        val maybePrivateKey = keyStore.getKey(addressWithoutPassword, pwd = Some(Array.emptyCharArray))
        maybePrivateKey shouldBe 'right
      }
      "retrieves from keystore with None password" in {
        val maybePrivateKey = keyStore.getKey(addressWithoutPassword, pwd = None)
        maybePrivateKey shouldBe 'right
      }
    }
  }
}

object GostKeystoreSpec {
  val keyStoreDirName      = "/gost_keystore"
  val keyStorePassword     = "test keystore password"
  val testingChainId: Byte = 'T'.toByte
  val gostContext: GostCryptoContext = new GostCryptoContext(Set.empty, false) {
    override def toAlias(keyPair: GostKeyPair): String =
      Address.fromPublicKey(keyPair.getPublic.getEncoded, chainId = testingChainId).address
  }
  val keyStoreFile: File = {
    val pathToResource = getClass.getResource(keyStoreDirName).getPath
    new File(pathToResource)
  }
  val keyStore: KeyStore[GostKeyPair] = gostContext.keyStore(file = Some(keyStoreFile), password = keyStorePassword.toCharArray)
  val keyStoreAliases: List[String]   = keyStore.aliases().toList

  val addressWithPassword          = "3MyumCrZgGyuqD99du3FUkav3F8YX1Vuhxo"
  val addressWithoutPassword       = "3MssCNhfZo1yJgBx2ZMid7XHBqTTjJVQXWC"
  val secondAddressWithPassword    = "3Mq9crNkTFf8oRPyisgtf4TjBvZxo4BL2ax"
  val secondAddressWithoutPassword = "3MzD9Q3UhebvjCyNacuSCGLoYqpdqCd77Fj"
  val keyPassword                  = "testkeypassword"
}
