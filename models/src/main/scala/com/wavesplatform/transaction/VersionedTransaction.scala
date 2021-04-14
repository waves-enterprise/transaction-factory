package com.wavesplatform.transaction

trait VersionedTransaction extends Transaction {
  def version: Byte
}
