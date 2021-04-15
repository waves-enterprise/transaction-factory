package com.wavesplatform.crypto.internals
import com.wavesplatform.crypto.internals.gost.{GostAlgorithms, KuznechikAlgorithm}

class KuznechikCryptoSpec extends GostCryptoSpec {
  override val gostCrypto: GostAlgorithms = new KuznechikAlgorithm
}
