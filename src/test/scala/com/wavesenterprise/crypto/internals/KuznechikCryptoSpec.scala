package com.wavesenterprise.crypto.internals
import com.wavesenterprise.crypto.internals.gost.{GostAlgorithms, KuznechikAlgorithm}

class KuznechikCryptoSpec extends GostCryptoSpec {
  override val gostCrypto: GostAlgorithms = new KuznechikAlgorithm
}
