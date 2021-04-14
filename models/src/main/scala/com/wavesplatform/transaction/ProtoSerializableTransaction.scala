package com.wavesplatform.transaction

import com.wavesplatform.transaction.protobuf.{Transaction => PbTransaction}

trait ProtoSerializableTransaction extends Transaction {
  def toProto: PbTransaction
}
