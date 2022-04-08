package com.wavesenterprise.crypto.internals

import com.wavesenterprise.NoShrink
import com.wavesenterprise.account.Address
import com.wavesenterprise.crypto.GostKeystoreSpec
import com.wavesenterprise.crypto.internals.gost._
import com.wavesenterprise.crypto.internals.pki.Models.{CustomExtendedKeyUsage, ExtendedKeyUsage}
import com.wavesenterprise.pki.CertChain
import com.wavesenterprise.utils.EitherUtils.EitherExt
import monix.eval.Coeval
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.CryptoPro.JCP.JCP
import ru.CryptoPro.JCPRequest
import ru.CryptoPro.JCPRequest.GostCertificateRequest
import ru.CryptoPro.JCSP.JCSP

import java.io.ByteArrayInputStream
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.{KeyPair => JavaKeyPair, KeyStore => JavaKeyStore, PublicKey => JavaPublicKey}
import scala.collection.mutable.ArrayBuffer

class GostAlgorithmsSpec extends FreeSpec with Matchers with NoShrink with ScalaCheckPropertyChecks {

  val pkiRequiredOid: ExtendedKeyUsage = CustomExtendedKeyUsage(Array(1, 2, 3, 4, 5, 6, 7))

  def buildGostCrypto(pkiRequiredOids: Set[ExtendedKeyUsage] = Set.empty, maybeTrustKeyStoreProvider: Option[Coeval[JavaKeyStore]] = None) =
    new GostAlgorithms(pkiRequiredOids, crlCheckIsEnabled = false, maybeTrustKeyStoreProvider)

  val oneKilobyteInBytes: Int = 1024
  val oneMegabyteInBytes: Int = 1024 * oneKilobyteInBytes

  val sampleBytesGenerator: Gen[Array[Byte]] = for {
    length    <- Gen.choose(oneKilobyteInBytes, oneMegabyteInBytes)
    dataBytes <- Gen.containerOfN[Array, Byte](length, Arbitrary.arbitrary[Byte])
  } yield dataBytes

  val aliceRealKeypair: GostKeyPair =
    GostKeystoreSpec.keyStore.getKeyPair(GostKeystoreSpec.addressWithoutPassword, pwd = None).explicitGet()
  val aliceCert: X509Certificate =
    GostKeystoreSpec.keyStore.getCertificate(GostKeystoreSpec.addressWithoutPassword).explicitGet().asInstanceOf[X509Certificate]
  val bobRealKeypair: GostKeyPair =
    GostKeystoreSpec.keyStore.getKeyPair(GostKeystoreSpec.secondAddressWithoutPassword, pwd = None).explicitGet()
  val charlesRealKeypair: GostKeyPair =
    GostKeystoreSpec.keyStore.getKeyPair(GostKeystoreSpec.addressWithPassword, pwd = Some(GostKeystoreSpec.keyPassword.toCharArray)).explicitGet()

  "Basic key operations" - {
    "sign & verify signature" - {
      "with real keys (using testing gost_keystore)" in {
        val algorithms = buildGostCrypto()
        val data       = sampleBytesGenerator.sample.get
        val signature  = algorithms.sign(aliceRealKeypair.getPrivate, data)

        algorithms.verify(signature, data, aliceRealKeypair.getPublic) shouldBe true
      }
    }

    "sign & verify signature with cert chain" - {

      "successful sign & verify complex chain" in {
        val (caKeyPair, caCert, trustedCertsKeyStoreProvider) = buildCaSetup()

        val algorithms = buildGostCrypto(Set(pkiRequiredOid), Some(trustedCertsKeyStoreProvider))

        val intermediateKeyPair = charlesRealKeypair.internal
        val intermediateCert = buildCert(
          issuerKeyPair = caKeyPair,
          issuer = caCert.getSubjectX500Principal.toString,
          publicKey = charlesRealKeypair.internal.getPublic,
          subject = "CN=charles",
          keyUsages = Seq(JCPRequest.KeyUsage.KEY_CERT_SIGN)
        )

        val userKeyPair        = bobRealKeypair.internal
        val userGostPrivateKey = bobRealKeypair.getPrivate

        val userCert = buildCert(
          issuerKeyPair = intermediateKeyPair,
          issuer = intermediateCert.getSubjectX500Principal.toString,
          publicKey = userKeyPair.getPublic,
          subject = "CN=bob",
          keyUsages = Seq(JCPRequest.KeyUsage.DIGITAL_SIGNATURE),
          extKeyUsages = Seq(pkiRequiredOid)
        )

        val data      = sampleBytesGenerator.sample.get
        val signature = algorithms.sign(userGostPrivateKey, data)
        val certChain = CertChain(caCert, Seq(intermediateCert), userCert)

        algorithms.verify(signature, data, certChain, System.currentTimeMillis()) shouldBe 'right
      }

      "verification error when DIGITAL_SIGNATURE key usage is not specified" in {
        val (caKeyPair, caCert, trustedCertsKeyStoreProvider) = buildCaSetup()

        val algorithms = buildGostCrypto(Set(pkiRequiredOid), Some(trustedCertsKeyStoreProvider))

        val userKeyPair        = bobRealKeypair.internal
        val userGostPrivateKey = bobRealKeypair.getPrivate
        val userSubject        = "CN=bob"
        val data               = sampleBytesGenerator.sample.get
        val signature          = algorithms.sign(userGostPrivateKey, data)
        val userCert = buildCert(
          issuerKeyPair = caKeyPair,
          issuer = caCert.getSubjectX500Principal.toString,
          publicKey = userKeyPair.getPublic,
          subject = userSubject,
          extKeyUsages = Seq(pkiRequiredOid)
        )

        val certChain = CertChain(caCert, Seq.empty, userCert)

        algorithms.verify(signature, data, certChain, System.currentTimeMillis()) shouldBe
          Left {
            PKIError(
              s"Signature validation failed: 'digitalSignature' is not present in 'keyUsage' " +
                s"extension of signer's certificate (DN=$userSubject)")
          }
      }

      "verification error when required oid is not specified" in {
        val (caKeyPair, caCert, trustedCertsKeyStoreProvider) = buildCaSetup()

        val algorithms = buildGostCrypto(Set(pkiRequiredOid), Some(trustedCertsKeyStoreProvider))

        val userKeyPair        = bobRealKeypair.internal
        val userGostPrivateKey = bobRealKeypair.getPrivate
        val userSubject        = "CN=bob"

        val data      = sampleBytesGenerator.sample.get
        val signature = algorithms.sign(userGostPrivateKey, data)

        val userCert = buildCert(
          issuerKeyPair = caKeyPair,
          issuer = caCert.getSubjectX500Principal.toString,
          publicKey = userKeyPair.getPublic,
          subject = userSubject,
          keyUsages = Seq(JCPRequest.KeyUsage.DIGITAL_SIGNATURE)
        )

        val certChain = CertChain(caCert, Seq.empty, userCert)

        algorithms.verify(signature, data, certChain, System.currentTimeMillis()) shouldBe
          Left(PKIError(s"Missing the following required EKUs for the certificate '$userSubject': [${pkiRequiredOid.strRepr}]"))
      }

      "verification error when root cert is not trusted" in {
        val (caKeyPair, caCert, _) = buildCaSetup()

        val algorithms = buildGostCrypto(Set(pkiRequiredOid), None)

        val userKeyPair        = bobRealKeypair.internal
        val userGostPrivateKey = bobRealKeypair.getPrivate
        val userSubject        = "CN=bob"

        val data      = sampleBytesGenerator.sample.get
        val signature = algorithms.sign(userGostPrivateKey, data)

        val userCert = buildCert(
          issuerKeyPair = caKeyPair,
          issuer = caCert.getSubjectX500Principal.toString,
          publicKey = userKeyPair.getPublic,
          subject = userSubject,
          keyUsages = Seq(JCPRequest.KeyUsage.DIGITAL_SIGNATURE),
          extKeyUsages = Seq(pkiRequiredOid)
        )

        val certChain = CertChain(caCert, Seq.empty, userCert)

        algorithms.verify(signature, data, certChain, System.currentTimeMillis()) shouldBe
          Left(PKIError(s"Root certificate (DN=${aliceCert.getSubjectX500Principal}) is not trusted"))

      }

      "verification error when invalid cert chain path" in {
        val (caKeyPair, caCert, trustedCertsKeyStoreProvider) = buildCaSetup()

        val algorithms = buildGostCrypto(Set(pkiRequiredOid), Some(trustedCertsKeyStoreProvider))

        val userKeyPair        = bobRealKeypair.internal
        val userGostPrivateKey = bobRealKeypair.getPrivate
        val userSubject        = "CN=bob"

        val data      = sampleBytesGenerator.sample.get
        val signature = algorithms.sign(userGostPrivateKey, data)

        val userCert = buildCert(
          issuerKeyPair = caKeyPair,
          issuer = "CN=InvalidIssuer",
          publicKey = userKeyPair.getPublic,
          subject = userSubject,
          keyUsages = Seq(JCPRequest.KeyUsage.DIGITAL_SIGNATURE),
          extKeyUsages = Seq(pkiRequiredOid)
        )

        val certChain = CertChain(caCert, Seq.empty, userCert)
        val result    = algorithms.verify(signature, data, certChain, System.currentTimeMillis())
        result shouldBe 'left
        result.left.get.toString should include("unable to find valid certification path to requested target")
      }
    }
  }

  "Crypto operations" - {
    "encrypt & decrypt for single recipient" - {
      "with session (ephemeral) keys" in {
        val algorithms = buildGostCrypto()
        val bytes      = sampleBytesGenerator.sample.get

        val alice = algorithms.generateSessionKey()
        val bob   = algorithms.generateSessionKey()

        val encryptedDataWithWrappedKey = algorithms.encrypt(bytes, alice.getPrivate, bob.getPublic).explicitGet()
        val decryptedResult             = algorithms.decrypt(encryptedDataWithWrappedKey, bob.getPrivate, alice.getPublic).explicitGet()

        decryptedResult should contain theSameElementsAs bytes
      }
      "with real keys (using testing gost_keystore)" in {
        val algorithms = buildGostCrypto()
        val bytes      = sampleBytesGenerator.sample.get

        val encryptedDataWithWrappedKey = algorithms.encrypt(bytes, aliceRealKeypair.getPrivate, bobRealKeypair.getPublic).explicitGet()
        val decryptedResult             = algorithms.decrypt(encryptedDataWithWrappedKey, bobRealKeypair.getPrivate, aliceRealKeypair.getPublic).explicitGet()

        decryptedResult should contain theSameElementsAs bytes
      }
    }
    "encrypt & decrypt for self" - {
      "with session (ephemeral) keys" in {
        val algorithms    = buildGostCrypto()
        val senderKeyPair = algorithms.generateSessionKey()
        val data          = sampleBytesGenerator.sample.get

        val encryptedStuff  = algorithms.encrypt(data, senderKeyPair.getPrivate, senderKeyPair.getPublic).explicitGet()
        val decryptedResult = algorithms.decrypt(encryptedStuff, senderKeyPair.getPrivate, senderKeyPair.getPublic).explicitGet()

        decryptedResult should contain theSameElementsAs data
      }
      "with real keys (using testing gost_keystore)" in {
        val algorithms = buildGostCrypto()
        val data       = sampleBytesGenerator.sample.get

        val encryptedStuff  = algorithms.encrypt(data, aliceRealKeypair.getPrivate, aliceRealKeypair.getPublic).explicitGet()
        val decryptedResult = algorithms.decrypt(encryptedStuff, aliceRealKeypair.getPrivate, aliceRealKeypair.getPublic).explicitGet()

        decryptedResult should contain theSameElementsAs data
      }
    }
    "encryptForMany & decrypt each" - {
      "with session (ephemeral) keys" in {
        val algorithms               = buildGostCrypto()
        val recipientPublicToPrivate = List.fill(3)(algorithms.generateSessionKey()).map(keyPair => keyPair.getPublic -> keyPair.getPrivate).toMap
        val recipientAddressToPrivate: Map[Address, GostPrivateKey] = recipientPublicToPrivate.map {
          case (publicKey, privateKey) => Address.fromPublicKey(publicKey.getEncoded) -> privateKey
        }
        val senderDummyKeyPair = algorithms.generateSessionKey()
        val data               = sampleBytesGenerator.sample.get

        val encrypted = algorithms.encryptForMany(data, senderDummyKeyPair.getPrivate, recipientPublicToPrivate.keys.toSeq).explicitGet()

        encrypted.recipientPubKeyToWrappedKey.foreach {
          case (publicKey, wrappedStructure) =>
            val recipientPrivateKey = recipientAddressToPrivate(Address.fromPublicKey(publicKey.getEncoded))
            val decryptedData = algorithms
              .decrypt(EncryptedForSingle(encrypted.encryptedData, wrappedStructure), recipientPrivateKey, senderDummyKeyPair.getPublic)
              .explicitGet()

            decryptedData should contain theSameElementsAs data
        }
      }
      "with real keys (using testing gost_keystore)" in {
        val algorithms = buildGostCrypto()
        val recipientPublicToPrivate = List(aliceRealKeypair, bobRealKeypair, charlesRealKeypair)
          .map(keyPair => keyPair.getPublic -> keyPair.getPrivate)
          .toMap

        val recipientAddressToPrivate: Map[Address, GostPrivateKey] = recipientPublicToPrivate.map {
          case (publicKey, privateKey) => Address.fromPublicKey(publicKey.getEncoded) -> privateKey
        }
        val data = sampleBytesGenerator.sample.get

        val encrypted = algorithms.encryptForMany(data, aliceRealKeypair.getPrivate, recipientPublicToPrivate.keys.toSeq).explicitGet()

        encrypted.recipientPubKeyToWrappedKey.foreach {
          case (publicKey, wrappedStructure) =>
            val recipientPrivateKey = recipientAddressToPrivate(Address.fromPublicKey(publicKey.getEncoded))
            val decryptedData = algorithms
              .decrypt(EncryptedForSingle(encrypted.encryptedData, wrappedStructure), recipientPrivateKey, aliceRealKeypair.getPublic)
              .explicitGet()

            decryptedData should contain theSameElementsAs data
        }
      }
    }
  }

  "Stream encrypt and decrypt" in {
    val algorithms = buildGostCrypto()
    val genSomeBytes: Gen[(Array[Byte], Int)] = for {
      length    <- Gen.choose(32 * 1024, 1 * 1024 * 1024)
      chunkSize <- Gen.choose(1, length)
      dataBytes <- Gen.containerOfN[Array, Byte](length, Arbitrary.arbitrary[Byte])
    } yield dataBytes -> chunkSize
    val genChunkSize: () => Int = () => Gen.choose(512, 2048).sample.get

    forAll(genSomeBytes) {
      case (data, chunkSize) =>
        val sender    = algorithms.generateSessionKey()
        val recipient = algorithms.generateSessionKey()

        val (encryptedKey, encryptor) = algorithms.buildEncryptor(sender.getPrivate, recipient.getPublic, chunkSize).explicitGet()
        val dataStream                = new ByteArrayInputStream(data)

        val encryptedChunks = ArrayBuffer[Byte]()

        while (dataStream.available() != 0) {
          val chunk = dataStream.readNBytes(Math.min(genChunkSize(), dataStream.available()))
          encryptedChunks ++= encryptor(chunk)
        }
        encryptedChunks ++= encryptor.doFinal()

        val finalEncrypted = EncryptedForSingle(encryptedChunks.toArray, encryptedKey)

        val decryptor           = algorithms.buildDecryptor(finalEncrypted.wrappedStructure, recipient.getPrivate, sender.getPublic, chunkSize).explicitGet()
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

  private def buildCert(issuerKeyPair: JavaKeyPair,
                        issuer: String,
                        publicKey: JavaPublicKey,
                        subject: String,
                        keyUsages: Seq[Int] = Seq(JCPRequest.KeyUsage.NOT_SET),
                        extKeyUsages: Seq[ExtendedKeyUsage] = Seq.empty) = {
    val certRequest = generateCertificateRequest(
      keyUsages = keyUsages,
      extKeyUsages = extKeyUsages
    )

    val certBytes = certRequest.generateCert(issuerKeyPair.getPrivate, publicKey, subject, issuer)

    CertificateFactory
      .getInstance(JCP.CERTIFICATE_FACTORY_NAME)
      .generateCertificate(new ByteArrayInputStream(certBytes))
      .asInstanceOf[X509Certificate]
  }

  private def generateCertificateRequest(keyUsages: Seq[Int], extKeyUsages: Seq[ExtendedKeyUsage]): GostCertificateRequest = {
    val request = new GostCertificateRequest(JCSP.PROVIDER_NAME)

    if (keyUsages.nonEmpty) {
      val keyUsageComposition = keyUsages.reduceLeft(_ | _)
      request.setKeyUsage(keyUsageComposition)
    }

    extKeyUsages.foreach { extendedKeyUsage =>
      request.addExtKeyUsage(extendedKeyUsage.jcspValue)
    }

    request
  }

  private def buildCaSetup(): (JavaKeyPair, X509Certificate, Coeval[JavaKeyStore]) = {
    val caKeyPair = aliceRealKeypair.internal

    val caCert = buildCert(
      issuerKeyPair = caKeyPair,
      issuer = aliceCert.getSubjectX500Principal.toString,
      publicKey = caKeyPair.getPublic,
      subject = aliceCert.getSubjectX500Principal.toString,
      keyUsages = Seq(JCPRequest.KeyUsage.KEY_CERT_SIGN),
    )

    val trustedCertsKeyStoreProvider: Coeval[JavaKeyStore] = Coeval.evalOnce {
      val store = JavaKeyStore.getInstance("PKCS12")
      store.load(null, "qwerty".toCharArray)
      val entry = new JavaKeyStore.TrustedCertificateEntry(caCert)
      store.setEntry("cert-entry", entry, None.orNull)
      store
    }

    (caKeyPair, caCert, trustedCertsKeyStoreProvider)
  }

}
