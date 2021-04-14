package com.wavesplatform.transaction.docker

import com.google.common.io.ByteArrayDataOutput
import com.wavesplatform.account.PublicKeyAccount
import com.wavesplatform.crypto.{KeyLength, SignatureLength}
import com.wavesplatform.state.ByteStr
import play.api.libs.json.{Json, Writes}

case class ValidationProof(validatorPublicKey: PublicKeyAccount, signature: ByteStr)

object ValidationProof {
  implicit val writes: Writes[ValidationProof] = Json.writes[ValidationProof]

  def writeBytes(value: ValidationProof, output: ByteArrayDataOutput): Unit = {
    output.write(value.validatorPublicKey.publicKey.getEncoded)
    output.write(value.signature.arr)
  }

  def fromBytes(bytes: Array[Byte], offset: Int): (ValidationProof, Int) = {
    val (publicKey, keyEnd) = PublicKeyAccount(bytes.slice(offset, offset + KeyLength)) -> (offset + KeyLength)
    val (signature, end)    = ByteStr(bytes.slice(keyEnd, keyEnd + SignatureLength))    -> (keyEnd + SignatureLength)

    ValidationProof(publicKey, signature) -> end
  }
}
