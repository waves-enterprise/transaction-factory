package com.wavesenterprise.transaction

import com.wavesenterprise.CoreTransactionGen
import com.wavesenterprise.account.PublicKeyAccount
import com.wavesenterprise.state._
import com.wavesenterprise.transaction.transfer._
import com.wavesenterprise.utils.Base58
import com.wavesenterprise.utils.EitherUtils.EitherExt
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json

class TransferTransactionV2Specification
    extends PropSpec
    with ScalaCheckPropertyChecks
    with Matchers
    with CoreTransactionGen
    with WithSenderAndRecipient {

  property("VersionedTransferTransactionSpecification serialization roundtrip") {
    forAll(transferV2Gen) { tx: TransferTransactionV2 =>
      val recovered = TransferTransactionV2.parseBytes(tx.bytes()).get
      assertTxs(recovered, tx)
    }
  }

  property("TransferTransaction proto serialization roundtrip") {
    forAll(transferV2Gen) { tx =>
      val recovered = TransferTransactionV2.fromProto(tx.toInnerProto).explicitGet()
      recovered shouldEqual tx
    }
  }

  property("VersionedTransferTransactionSpecification serialization from TypedTransaction") {
    forAll(transferV2Gen) { tx: TransferTransactionV2 =>
      val recovered = TransactionParsers.parseBytes(tx.bytes()).get
      assertTxs(recovered.asInstanceOf[TransferTransactionV2], tx)
    }
  }

  property("VersionedTransferTransactionSpecification id doesn't depend on proof") {
    forAll(accountGen, proofsGen, proofsGen, bytes32gen) {
      case (acc, proofs1, proofs2, attachment) =>
        val tx1 = TransferTransactionV2.create(acc, None, None, 1, 1, 1, acc.toAddress, attachment, proofs1).explicitGet()
        val tx2 = TransferTransactionV2.create(acc, None, None, 1, 1, 1, acc.toAddress, attachment, proofs2).explicitGet()
        tx1.id() shouldBe tx2.id()
    }
  }

  private def assertTxs(first: TransferTransactionV2, second: TransferTransactionV2): Unit = {
    first.sender.address shouldEqual second.sender.address
    first.timestamp shouldEqual second.timestamp
    first.fee shouldEqual second.fee
    first.amount shouldEqual second.amount
    first.recipient shouldEqual second.recipient
    first.version shouldEqual second.version
    first.assetId shouldEqual second.assetId
    first.feeAssetId shouldEqual second.feeAssetId
    first.proofs shouldEqual second.proofs
    first.bytes() shouldEqual second.bytes()
  }

  property("JSON format validation") {
    val tx = TransferTransactionV2
      .create(
        PublicKeyAccount(senderAccount.publicKey),
        None,
        None,
        1526641218066L,
        100000000,
        100000000,
        recipientAccount.toAddress,
        Base58.decode("4t2Xazb2SX").get,
        Proofs(Seq(ByteStr.decodeBase58("4bfDaqBcnK3hT8ywFEFndxtS1DTSYfncUqd4s5Vyaa66PZHawtC73rDswUur6QZu5RpqM7L9NFgBHT1vhCoox4vi").get))
      )
      .right
      .get

    val js = Json.parse(s"""{
                       "type": 4,
                       "id": "${tx.id()}",
                       "sender": "${senderAccount.address}",
                       "senderPublicKey": "$senderPkBase58",
                       "fee": 100000000,
                       "timestamp": 1526641218066,
                       "proofs": [
                       "4bfDaqBcnK3hT8ywFEFndxtS1DTSYfncUqd4s5Vyaa66PZHawtC73rDswUur6QZu5RpqM7L9NFgBHT1vhCoox4vi"
                       ],
                       "version": 2,
                       "recipient": "${recipientAccount.address}",
                       "assetId": null,
                       "feeAssetId": null,
                       "amount": 100000000,
                       "attachment": "4t2Xazb2SX"}
    """)

    tx.json() shouldEqual js
  }
}
