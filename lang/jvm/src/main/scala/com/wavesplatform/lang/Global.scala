package com.wavesplatform.lang

import com.wavesplatform.crypto.internals.{WavesAlgorithms, WavesPrivateKey, WavesPublicKey}
import com.wavesplatform.lang.v1.BaseGlobal
import com.wavesplatform.utils.{Base58, Base64}
import scorex.crypto.hash.{Blake2b256, Keccak256, Sha256}
import scorex.crypto.signatures.{Curve25519, PublicKey, Signature}
import com.wavesplatform.crypto.internals.gost.{GostAlgorithms, GostPrivateKey, GostPublicKey}

import scala.util.Try

trait CommonGlobal extends BaseGlobal {
  def base58Encode(input: Array[Byte]): Either[String, String] =
    if (input.length > MaxBase58Bytes) Left(s"base58Encode input exceeds $MaxBase58Bytes")
    else Right(Base58.encode(input))

  def base58Decode(input: String, limit: Int): Either[String, Array[Byte]] =
    if (input.length > limit) Left(s"base58Decode input exceeds $limit")
    else Base58.decode(input).toEither.left.map(_ => "can't parse Base58 string")

  def base64Encode(input: Array[Byte]): Either[String, String] =
    Either.cond(input.length <= MaxBase64Bytes, Base64.encode(input), s"base64Encode input exceeds $MaxBase64Bytes")

  def base64Decode(input: String, limit: Int): Either[String, Array[Byte]] =
    for {
      _      <- Either.cond(input.length <= limit, (), s"base64Decode input exceeds $limit")
      result <- Base64.decode(input).toEither.left.map(_ => "can't parse Base64 string")
    } yield result

  def sha256(message: Array[Byte]): Array[Byte] = Sha256.hash(message)
}

object Global extends CommonGlobal {
  def curve25519verify(message: Array[Byte], sig: Array[Byte], pub: Array[Byte]): Boolean = Curve25519.verify(Signature(sig), message, PublicKey(pub))

  def keccak256(message: Array[Byte]): Array[Byte]  = Keccak256.hash(message)
  def blake2b256(message: Array[Byte]): Array[Byte] = Blake2b256.hash(message)

  def secureHash(a: Array[Byte]): Array[Byte] = keccak256(blake2b256(a))

  def sign(privateKey: WavesPrivateKey, message: Array[Byte]): Array[Byte] = {
    WavesAlgorithms.sign(privateKey, message)
  }

  def verify(signature: Array[Byte], message: Array[Byte], publicKey: WavesPublicKey): Boolean = {
    Try(WavesAlgorithms.verify(signature, message, publicKey)).getOrElse(false)
  }

}

object GostGlobal extends CommonGlobal {

  val algorithms = new GostAlgorithms

  def curve25519verify(message: Array[Byte], sig: Array[Byte], pub: Array[Byte]): Boolean = {
    Try(algorithms.verify(sig, message, pub)).getOrElse(false)
  }

  def sign(pk: GostPrivateKey, message: Array[Byte]): Array[Byte] = {
    algorithms.sign(pk, message)
  }

  def verify(signature: Array[Byte], message: Array[Byte], publicKey: GostPublicKey): Boolean = {
    Try(algorithms.verify(signature, message, publicKey)).getOrElse(false)
  }

  def keccak256(message: Array[Byte]): Array[Byte]  = algorithms.fastHash(message)
  def blake2b256(message: Array[Byte]): Array[Byte] = algorithms.fastHash(message)
  def secureHash(a: Array[Byte]): Array[Byte]       = algorithms.fastHash(a)
}
