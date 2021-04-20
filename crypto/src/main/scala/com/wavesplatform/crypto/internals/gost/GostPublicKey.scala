package com.wavesplatform.crypto.internals.gost

import com.objsys.asn1j.runtime.Asn1DerDecodeBuffer
import com.wavesplatform.crypto.internals.PublicKey
import ru.CryptoPro.JCP.ASN.PKIX1Explicit88.SubjectPublicKeyInfo
import ru.CryptoPro.JCP.params.{AlgIdSpec, OID}
import scorex.util.encode.Base58

import java.security.{PublicKey => PublicKeyJ}

sealed abstract class AbstractGostPublicKey(protected[gost] val internal: PublicKeyJ) extends PublicKey {
  def getOID: OID
}

case class GostPublicKey(override val internal: PublicKeyJ) extends AbstractGostPublicKey(internal) {
  override def getEncoded: Array[Byte] = {
    val encodedAsn1 = internal.getEncoded
    encodedAsn1.drop(encodedAsn1.length - GostPublicKey.length)
  }
  def getEncodedAsn1: Array[Byte] = internal.getEncoded

  def getOID: OID = {
    val publicKeyInfo = new SubjectPublicKeyInfo()
    val buffer        = new Asn1DerDecodeBuffer(this.getEncodedAsn1)

    publicKeyInfo.decode(buffer)

    val algIdSpec         = new AlgIdSpec(publicKeyInfo.algorithm)
    val dataEncryptionOid = algIdSpec.getCryptParams.getOID
    dataEncryptionOid
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[GostPublicKey]

  override def equals(other: Any): Boolean = other match {
    case that: GostPublicKey =>
      (that canEqual this) &&
        internal == that.internal
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(internal)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object GostPublicKey {
  private[crypto] val asn1Gost2012PrefixBytes: Array[Byte] = Base58.decode("3QUrn8yR6ZeqJ46HsAhktxU1dUuZsZKZULs5swPH6JjwU9ie5daE8d5").get
  val length                                               = 64

  def withAsn1(publicKey: Array[Byte]): GostPublicKey =
    new GostPublicKey(new ru.CryptoPro.JCSP.Key.GostPublicKey(asn1Gost2012PrefixBytes ++ publicKey, true))
}

case class GostSessionPublicKey(override val internal: PublicKeyJ) extends AbstractGostPublicKey(internal) {
  override def getEncoded: Array[Byte] = internal.getEncoded

  def getOID: OID = {
    val publicKeyInfo = new SubjectPublicKeyInfo()
    val buffer        = new Asn1DerDecodeBuffer(getEncoded)

    publicKeyInfo.decode(buffer)

    val algIdSpec         = new AlgIdSpec(publicKeyInfo.algorithm)
    val dataEncryptionOid = algIdSpec.getCryptParams.getOID
    dataEncryptionOid
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[GostSessionPublicKey]

  override def equals(other: Any): Boolean = other match {
    case that: GostPublicKey =>
      (that canEqual this) &&
        internal == that.internal
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(internal)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object GostSessionPublicKey {
  def fromBytes(publicKey: Array[Byte]): GostSessionPublicKey = {
    GostSessionPublicKey(new ru.CryptoPro.JCSP.Key.GostPublicKey(publicKey, true))
  }
}
