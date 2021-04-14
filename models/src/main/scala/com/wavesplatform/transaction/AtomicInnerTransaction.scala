package com.wavesplatform.transaction

import com.wavesplatform.account.PublicKeyAccount

trait AtomicInnerTransaction extends VersionedTransaction {
  def sender: PublicKeyAccount
  def atomicBadge: Option[AtomicBadge]
}
