package com.wavesenterprise.transaction.docker

import com.wavesenterprise.account.PublicKeyAccount
import com.wavesenterprise.state.{BinaryDataEntry, BooleanDataEntry, ByteStr, IntegerDataEntry, StringDataEntry}
import com.wavesenterprise.transaction.{Proofs, TransactionParsers, ValidationError}
import com.wavesenterprise.utils.Base64
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import com.wavesenterprise.utils.EitherUtils.EitherExt

class CallContractTransactionV3Spec extends PropSpec with ScalaCheckPropertyChecks with Matchers with ContractTransactionGen {

  property("CallContractTransactionV3Spec serialization roundtrip") {
    forAll(callContractV3ParamGen) { tx: CallContractTransactionV3 =>
      val recovered = CallContractTransactionV3.parseBytes(tx.bytes()).get
      recovered shouldBe tx
    }
  }

  property("CallContractTransactionV3 proto serialization roundtrip") {
    forAll(callContractV3ParamGen) { tx =>
      val recovered = CallContractTransactionV3.fromProto(tx.toInnerProto).explicitGet()
      recovered shouldBe tx
    }
  }

  property("CallContractTransactionV3Spec serialization from TypedTransaction") {
    forAll(callContractV3ParamGen) { tx: CallContractTransactionV3 =>
      val recovered = TransactionParsers.parseBytes(tx.bytes()).get
      recovered shouldBe tx
    }
  }

  property("CallContractTransactionV3Spec negative validation cases") {
    forAll(callContractV3ParamGen) {
      case CallContractTransactionV3(sender, contractId, _, fee, timestamp, contractVersion, feeAssetId, proofs) =>
        val emptyKeyParams = List(IntegerDataEntry("", 2))
        val emptyKeyEither =
          CallContractTransactionV3.create(sender, contractId, emptyKeyParams, fee, timestamp, contractVersion, feeAssetId, proofs)
        emptyKeyEither shouldBe Left(ValidationError.GenericError("Param with empty key was found"))

        val duplicateKeysParams = List(IntegerDataEntry("key1", 2), StringDataEntry("key1", "value"))
        val duplicateKeysEither =
          CallContractTransactionV3.create(sender, contractId, duplicateKeysParams, fee, timestamp, contractVersion, feeAssetId, proofs)
        duplicateKeysEither shouldBe Left(ValidationError.GenericError("Params with duplicate keys were found"))

        val tooBigTxParams = List(BinaryDataEntry("key1", ByteStr(Array.fill(ExecutableTransaction.MaxBytes)(1: Byte))))
        val tooBigTxEither =
          CallContractTransactionV3.create(sender, contractId, tooBigTxParams, fee, timestamp, contractVersion, feeAssetId, proofs)
        tooBigTxEither.left.get shouldBe a[ValidationError.ContractTransactionTooBig]
    }
  }

  property("JSON format validation") {
    val params = List(
      IntegerDataEntry("int", 24),
      BooleanDataEntry("bool", value = true),
      BinaryDataEntry("blob", ByteStr(Base64.decode("YWxpY2U=").get))
    )
    val timestamp       = System.currentTimeMillis()
    val contractId      = "9ekQuYn92natMnMq8KqeGK3Nn7cpKd3BvPEGgD6fFyyz"
    val contractVersion = 1
    val feeAssetId      = "9ekQuYn92natMnMq8KqeGK3Nn7cpKd3BvPEGgD6fFyyz"
    val tx = CallContractTransactionV3
      .create(
        PublicKeyAccount(senderAccount.publicKey),
        ByteStr.decodeBase58(contractId).get,
        params,
        fee = 0,
        timestamp,
        contractVersion,
        Some(ByteStr.decodeBase58(feeAssetId).get),
        Proofs(Seq(ByteStr.decodeBase58("32mNYSefBTrkVngG5REkmmGAVv69ZvNhpbegmnqDReMTmXNyYqbECPgHgXrX2UwyKGLFS45j7xDFyPXjF8jcfw94").get))
      )
      .right
      .get

    val js = Json.parse(s"""{
                       "type": 104,
                       "id": "${tx.id()}",
                       "sender": "${senderAccount.address}",
                       "senderPublicKey": "$senderPkBase58",
                       "fee": 0,
                       "timestamp": $timestamp,
                       "proofs": [
                       "32mNYSefBTrkVngG5REkmmGAVv69ZvNhpbegmnqDReMTmXNyYqbECPgHgXrX2UwyKGLFS45j7xDFyPXjF8jcfw94"
                       ],
                       "version": 3,
                       "contractId": "$contractId",
                       "contractVersion": $contractVersion,
                       "feeAssetId": "$feeAssetId",
                       "params": [
                       {
                       "key": "int",
                       "type": "integer",
                       "value": 24
                       },
                       {
                       "key": "bool",
                       "type": "boolean",
                       "value": true
                       },
                       {
                       "key": "blob",
                       "type": "binary",
                       "value": "base64:YWxpY2U="
                       }
                       ]
                       }
  """)

    js shouldEqual tx.json()
  }

}