package com.wavesenterprise.cryptojs

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("CryptoGostAPI")
object GostCrypto extends js.Object {
  def hash(data: js.Array[Byte]): js.Array[Byte]                                                  = js.native
  def sign(privateKey: js.Array[Byte], data: js.Array[Byte]): js.Array[Byte]                      = js.native
  def verify(publicKey: js.Array[Byte], data: js.Array[Byte], signature: js.Array[Byte]): Boolean = js.native
}
