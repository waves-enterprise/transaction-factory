package com.wavesplatform.docker

import com.google.common.io.ByteStreams.{newDataInput, newDataOutput}
import com.wavesplatform.account.PublicKeyAccount
import com.wavesplatform.crypto
import com.wavesplatform.state.ByteStr
import com.wavesplatform.transaction.docker.{CreateContractTransaction, UpdateContractTransaction}
import com.wavesplatform.utils.DatabaseUtils._
import monix.eval.Coeval
import play.api.libs.json._

import scala.util.hashing.MurmurHash3

case class ContractInfo(creator: Coeval[PublicKeyAccount], contractId: ByteStr, image: String, imageHash: String, version: Int, active: Boolean) {

  override def equals(that: Any): Boolean =
    that match {
      case that: ContractInfo =>
        eq(that) ||
          (creator() == that.creator() &&
            contractId == that.contractId &&
            image == that.image &&
            imageHash == that.imageHash &&
            version == that.version &&
            active == that.active)
      case _ => false
    }

  override def hashCode(): Int = MurmurHash3.orderedHash(Seq(creator(), contractId, image, imageHash, version, active))
}

//noinspection UnstableApiUsage
object ContractInfo {

  val FirstVersion: Int = 1

  implicit val PublicKeyReads: Reads[PublicKeyAccount] = {
    case JsString(s) => PublicKeyAccount.fromBase58String(s).fold(e => JsError(e.message), JsSuccess(_))
    case _           => JsError("Expected string value for public key value")
  }

  implicit val PublicKeyFormat: Format[PublicKeyAccount] = Format(PublicKeyReads, PublicKeyAccount.Writes)
  implicit val LazyPublicKeyFormat: Format[Coeval[PublicKeyAccount]] =
    Format.invariantFunctorFormat.inmap(PublicKeyFormat, Coeval.pure[PublicKeyAccount], _.apply())
  implicit val ContractInfoFormat: OFormat[ContractInfo] = Json.format

  def apply(tx: CreateContractTransaction): ContractInfo = {
    ContractInfo(Coeval.pure(tx.sender), tx.contractId, tx.image, tx.imageHash, FirstVersion, active = true)
  }

  def apply(tx: UpdateContractTransaction, contractInfo: ContractInfo): ContractInfo = {
    contractInfo.copy(image = tx.image, imageHash = tx.imageHash, version = contractInfo.version + 1)
  }

  def toBytes(contractInfo: ContractInfo): Array[Byte] = {
    import contractInfo._
    val ndo = newDataOutput()
    ndo.writePublicKey(creator())
    ndo.writeBytes(contractId.arr)
    ndo.writeString(image)
    ndo.writeString(imageHash)
    ndo.writeInt(version)
    ndo.writeBoolean(active)
    ndo.toByteArray
  }

  def fromBytes(bytes: Array[Byte]): ContractInfo = {
    val ndi          = newDataInput(bytes)
    val creatorBytes = ndi.readBytes(crypto.KeyLength)
    val creator      = Coeval.evalOnce(PublicKeyAccount(creatorBytes))
    val contractId   = ndi.readBytes()
    val image        = ndi.readString()
    val imageHash    = ndi.readString()
    val version      = ndi.readInt()
    val active       = ndi.readBoolean()
    ContractInfo(creator, ByteStr(contractId), image, imageHash, version, active)
  }
}
