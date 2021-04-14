package com.wavesplatform.transaction.docker

import com.wavesplatform.state.{ByteStr, DataEntry}
import com.wavesplatform.transaction.{FastHashId, VersionedTransaction}
import play.api.libs.json.Writes

trait ExecutableTransaction extends FastHashId with VersionedTransaction {

  def contractId: ByteStr

  def txType: Long

  def params: List[DataEntry[_]] = List.empty

  def fee: Long
}

object ExecutableTransaction {

  val MaxBytes: Int = 125 * 1024 // 125KB

  implicit val writes: Writes[ExecutableTransaction] = tx => tx.json()
}
