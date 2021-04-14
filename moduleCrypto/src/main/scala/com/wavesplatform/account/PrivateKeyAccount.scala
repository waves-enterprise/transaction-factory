package com.wavesplatform.account

import com.wavesplatform.crypto.{PublicKey, PrivateKey, KeyPair}

sealed trait PrivateKeyAccount extends PublicKeyAccount {
  def privateKey: PrivateKey
}

object PrivateKeyAccount {

  private case class PrivateKeyAccountImpl(privateKey: PrivateKey, publicKey: PublicKey) extends PrivateKeyAccount

  def apply(privateKey: PrivateKey, publicKey: PublicKey): PrivateKeyAccount = {
    PrivateKeyAccountImpl(privateKey, publicKey)
  }

  def apply(keyPair: KeyPair): PrivateKeyAccount = {
    PrivateKeyAccount(keyPair.getPrivate, keyPair.getPublic)
  }
}
