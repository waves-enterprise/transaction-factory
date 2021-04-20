package com.wavesplatform.crypto

import java.io.File

import com.wavesplatform.account.AddressScheme
import com.wavesplatform.crypto.internals.{CryptoAlgorithms, CryptoContext, KeyStore, WavesAlgorithms, WavesKeyPair}

private[crypto] class WavesCryptoContext extends CryptoContext {
  override type KeyPair0 = WavesKeyPair
  override val isGost                                           = false
  override val algorithms: CryptoAlgorithms[WavesKeyPair]       = WavesAlgorithms
  override val modernAlgorithms: CryptoAlgorithms[WavesKeyPair] = algorithms
  override def keyStore(file: Option[File], password: Array[Char]): KeyStore[WavesKeyPair] =
    new WavesKeyStore(file, password, AddressScheme.getAddressSchema.chainId)
}
