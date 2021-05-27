package com.wavesenterprise.crypto.internals.gost

import com.wavesenterprise.crypto.internals.KeyPair

sealed abstract class GostKeyPair(val internal: java.security.KeyPair) extends KeyPair {
  override type PrivateKey0 = GostPrivateKey
  override type PublicKey0  = AbstractGostPublicKey

  val getPrivate: GostPrivateKey = GostPrivateKey(internal.getPrivate)
  val getPublic: PublicKey0
}

object GostKeyPair {
  def apply(publicKey: GostPublicKey, privateKey: GostPrivateKey): GostKeyPair = {
    GostKeyPairPrimary(new java.security.KeyPair(publicKey.internal, privateKey.internal))
  }

  def apply(internal: java.security.KeyPair): GostKeyPair = {
    GostKeyPairPrimary(internal)
  }
}

case class GostKeyPairPrimary(override val internal: java.security.KeyPair) extends GostKeyPair(internal) {
  override val getPublic: GostPublicKey = GostPublicKey(internal.getPublic)
}

object GostKeyPairPrimary {
  def apply(publicKey: GostPublicKey, privateKey: GostPrivateKey): GostKeyPair = {
    GostKeyPairPrimary(new java.security.KeyPair(publicKey.internal, privateKey.internal))
  }
}

case class GostSessionKeyPair(override val internal: java.security.KeyPair) extends GostKeyPair(internal) {
  override val getPublic = GostSessionPublicKey(internal.getPublic)
}
