package com.wavesplatform.transaction

import com.wavesplatform.TransactionGen
import com.wavesplatform.account.PublicKeyAccount
import com.wavesplatform.state.ByteStr
import com.wavesplatform.utils.EitherUtils.EitherExt
import com.wavesplatform.transaction.lease.{LeaseCancelTransaction, LeaseCancelTransactionV2}
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json

class LeaseCancelTransactionSpecification
    extends PropSpec
    with ScalaCheckPropertyChecks
    with Matchers
    with TransactionGen
    with WithSenderAndRecipient {

  property("Lease cancel serialization roundtrip") {
    forAll(leaseCancelGen) { tx: LeaseCancelTransaction =>
      val recovered = tx.builder.parseBytes(tx.bytes()).get.asInstanceOf[LeaseCancelTransaction]
      assertTxs(recovered, tx)
    }
  }

  property("Lease cancel proto serialization roundtrip") {
    forAll(leaseCancelGen) { tx =>
      val recovered = LeaseCancelTransactionV2.fromProto(tx.toInnerProto).explicitGet()
      tx shouldEqual recovered
    }
  }

  property("Lease cancel serialization from TypedTransaction") {
    forAll(leaseCancelGen) { tx: LeaseCancelTransaction =>
      val recovered = TransactionParsers.parseBytes(tx.bytes()).get
      assertTxs(recovered.asInstanceOf[LeaseCancelTransaction], tx)
    }
  }

  private def assertTxs(first: LeaseCancelTransaction, second: LeaseCancelTransaction): Unit = {
    first.leaseId shouldEqual second.leaseId
    first.fee shouldEqual second.fee
    first.proofs shouldEqual second.proofs
    first.bytes() shouldEqual second.bytes()
  }

  property("JSON format validation for LeaseCancelTransactionV2") {
    val tx = LeaseCancelTransactionV2
      .create(
        'T',
        PublicKeyAccount(senderAccount.publicKey),
        1000000,
        1526646300260L,
        ByteStr.decodeBase58("DJWkQxRyJNqWhq9qSQpK2D4tsrct6eZbjSv3AH4PSha6").get,
        Proofs(Seq(ByteStr.decodeBase58("3h5SQLbCzaLoTHUeoCjXUHB6qhNUfHZjQQVsWTRAgTGMEdK5aeULMVUfDq63J56kkHJiviYTDT92bLGc8ELrUgvi").get))
      )
      .right
      .get

    val js = Json.parse(s"""{
                        "type": 9,
                        "id": "${tx.id()}",
                        "sender": "${senderAccount.address}",
                        "senderPublicKey": "$senderPkBase58",
                        "fee": 1000000,
                        "timestamp": 1526646300260,
                        "proofs": [
                        "3h5SQLbCzaLoTHUeoCjXUHB6qhNUfHZjQQVsWTRAgTGMEdK5aeULMVUfDq63J56kkHJiviYTDT92bLGc8ELrUgvi"
                        ],
                        "version": 2,
                        "leaseId": "DJWkQxRyJNqWhq9qSQpK2D4tsrct6eZbjSv3AH4PSha6",
                        "chainId": 84
                       }
    """)

    js shouldEqual tx.json()
  }

}
