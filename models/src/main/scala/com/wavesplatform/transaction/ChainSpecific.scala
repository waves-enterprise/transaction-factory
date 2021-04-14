package com.wavesplatform.transaction

trait ChainSpecific {
  def chainId: Byte
}
