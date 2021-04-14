package com.wavesplatform.crypto

import cats.syntax.either._
import com.wavesplatform.account.Address
import com.wavesplatform.crypto.internals._
import com.wavesplatform.state.ByteStr
import com.wavesplatform.utils.JsonFileStorage
import play.api.libs.json.{Format, Json}

import java.io.File
import java.security.cert.Certificate
import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal

class WavesKeyStore(storageFolder: Option[File], password: Array[Char], chainId: Byte) extends KeyStore[WavesKeyPair](storageFolder, password) {

  import WavesKeyStore._

  def toAlias(publicKey: WavesPublicKey): String = {
    Address.fromPublicKey(publicKey.getEncoded, chainId, WavesAlgorithms).address
  }

  override def getKey(alias: String, pwd: Option[Array[Char]]): Either[CryptoError, WavesPrivateKey] = {
    getKeyPair(alias, pwd).map(_.getPrivate)
  }

  override def getPublicKey(alias: String): Either[CryptoError, WavesPublicKey] = {
    accountsCache
      .get(alias)
      .toRight(GenericError(s"Public key for $alias wasn't found"))
      .map(_.publicKey)
  }

  override def getKeyPair(alias: String, pwd: Option[Array[Char]]): Either[CryptoError, WavesKeyPair] = {
    accountsCache
      .get(alias)
      .toRight(GenericError(s"Key wasn't found"))
      .flatMap {
        case encrypted: CachedEncryptedEntry =>
          for {
            pass <- pwd.toRight(GenericError(s"Expected password"))
            pk <- encrypted
              .privateKey(pass)
              .leftMap(_ => GenericError(s"Wrong password"))
          } yield WavesKeyPair(pk, encrypted.publicKey)

        case plain: CachedPlainEntry =>
          Right(WavesKeyPair(plain.privateKey, plain.publicKey))
      }
      .leftMap(error => error.copy(message = s"Failed to get key for $alias: ${error.message}"))
  }

  def getCertificate(alias: String): Either[CryptoError, Certificate] = {
    Either.left(GenericError("IllegalState: getCertificate called for Waves crypto. There are no certificates for Curve25519."))
  }

  def getCertificateChain(alias: String): Either[CryptoError, Array[Certificate]] = {
    Either.left(GenericError("IllegalState: getCertificateChain called for Waves crypto. There are no certificates for Curve25519."))
  }

  override def aliases(): Seq[String] = {
    accountsCache.keys.toSeq
  }

  def containsAlias(alias: String): Either[CryptoError, Boolean] = {
    Either.right(accountsCache.keys.toSeq.contains(alias))
  }

  private val key = JsonFileStorage.prepareKey(new String(password))

  private def loadOrImport(f: File): Option[WalletFileData] =
    try {
      Some(JsonFileStorage.load[WalletFileData](f.getCanonicalPath, Some(key)))
    } catch {
      case NonFatal(_) => None
    }

  private var walletData: WalletFileData = {
    if (storageFolder.isEmpty)
      WalletFileData(Seq.empty)
    else {
      val file = storageFolder.get
      if (file.exists() && file.length() > 0) {
        val wd = loadOrImport(file)
        if (wd.isDefined) wd.get
        else {
          throw new IllegalStateException(s"Failed to open existing wallet file '${storageFolder.get}' maybe provided password is incorrect")
        }
      } else WalletFileData(Seq.empty)
    }
  }

  private val l = new Object

  private def lock[T](f: => T): T = l.synchronized(f)

  private val accountsCache = {
    val entries = walletData.entries.map { entry =>
      val publicKey = WavesPublicKey(entry.publicKey.arr)
      toAlias(publicKey) -> CachedEntry(entry)
    }
    TrieMap(entries: _*)
  }

  private def save(): Unit =
    storageFolder.foreach(f => JsonFileStorage.save(walletData, f.getCanonicalPath, Some(key)))

  private def generateNewAccountWithoutSave(pwd: Option[Array[Char]]): Option[CachedEntry] = lock {
    val kp    = WavesAlgorithms.generateKeyPair()
    val alias = toAlias(kp.getPublic)

    if (!accountsCache.contains(alias)) {
      ByteStr.decodeBase58(kp.getPrivate.base58).toOption.flatMap { privateKey =>
        val pk        = pwd.map(pass => ByteStr(aes.encrypt(pass, privateKey.arr))).getOrElse(privateKey)
        val fileEntry = FileEntry(publicKey = ByteStr(kp.getPublic.getEncoded), privateKey = pk)
        val cached    = CachedEntry(fileEntry)
        accountsCache += (alias -> cached)
        walletData = walletData.copy(entries = walletData.entries :+ fileEntry)
        Some(cached)
      }
    } else None
  }

  override def generateAndStore(pwd: Option[Array[Char]]): Option[WavesKeyPair#PublicKey0] = lock {
    generateNewAccountWithoutSave(pwd).map { acc =>
      save()
      acc.publicKey
    }
  }
}

object WavesKeyStore {
  private case class WalletFileData(entries: Seq[FileEntry])
  private case class FileEntry(publicKey: ByteStr, privateKey: ByteStr)

  private implicit val entryFormat: Format[FileEntry]               = Json.format[FileEntry]
  private implicit val walletFileDataFormat: Format[WalletFileData] = Json.format[WalletFileData]

  private val aes = new AesEncryption

  private sealed trait CachedEntry {
    def entry: FileEntry
    def publicKey: WavesPublicKey = WavesPublicKey(entry.publicKey.arr)
  }

  private object CachedEntry {
    def apply(fileEntry: FileEntry): CachedEntry =
      if (fileEntry.privateKey.arr.length == WavesAlgorithms.KeyLength) CachedPlainEntry(fileEntry)
      else CachedEncryptedEntry(fileEntry)
  }

  private case class CachedEncryptedEntry(entry: FileEntry) extends CachedEntry {
    def privateKey(password: Array[Char]): Either[CryptoError, WavesPrivateKey] = {
      aes.decrypt(password, entry.privateKey.arr).map(WavesPrivateKey)
    }
  }

  private case class CachedPlainEntry(entry: FileEntry) extends CachedEntry {
    def privateKey: WavesPrivateKey = WavesPrivateKey(entry.privateKey.arr)
  }
}
