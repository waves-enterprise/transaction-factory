package com.wavesplatform.transaction

import com.wavesplatform.account.PublicKeyAccount

trait Authorized {
  def sender: PublicKeyAccount
}
