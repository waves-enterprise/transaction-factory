package com.wavesplatform.transaction

import com.wavesplatform.account.PrivateKeyAccount
import com.wavesplatform.state.diffs.ProduceError.produce
import com.wavesplatform.transaction.docker.{
  ContractTransactionGen,
  ExecutableTransaction,
  ExecutedContractTransaction,
  ExecutedContractTransactionV1
}
import com.wavesplatform.utils.EitherUtils.EitherExt
import com.wavesplatform.{TransactionGen, crypto}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tools.GenHelper.ExtendedGen

class AtomicTransactionV1Specification extends PropSpec with ScalaCheckPropertyChecks with Matchers with TransactionGen with ContractTransactionGen {

  private val signer = accountGen.generateSample()
  private val badge  = AtomicBadge(Some(signer.toAddress))

  property("Atomic serialization roundtrip") {
    forAll(atomicTxV1Gen(signer, atomicSingleInnerTxGen(Some(badge)))) { tx =>
      val recovered = tx.builder.parseBytes(tx.bytes()).get
      recovered.bytes() shouldEqual tx.bytes()
    }
  }

  property("Atomic proto serialization roundtrip") {
    forAll(atomicTxV1Gen(signer, atomicSingleInnerTxGen(Some(badge)))) { tx =>
      val recovered = AtomicTransactionV1.fromProto(tx.toInnerProto).explicitGet()
      recovered shouldEqual tx
    }
  }

  property("The proofs of mined tx transaction are valid and matches the init atomic transaction") {
    forAll(accountGen, atomicInnerTxsGen(Some(badge), 5), positiveLongGen) { (minerAcc, innerTxs, ts) =>
      val tx      = AtomicTransactionV1.selfSigned(signer, None, innerTxs, ts).explicitGet()
      val minedTx = enrichAtomicTx(signer, minerAcc, innerTxs, tx, ts)

      val minerProofSourceBytes = {
        val parts = Array(minedTx.proofSourceBytes) ++ minedTx.transactions.collect { case t: ExecutedContractTransaction => t.id().arr }
        Array.concat(parts: _*)
      }

      crypto.verify(tx.proofs.proofs.head.arr, minedTx.proofSourceBytes, minedTx.sender.publicKey.getEncoded) shouldBe true
      crypto.verify(minedTx.proofs.proofs(1).arr, minerProofSourceBytes, minedTx.miner.get.publicKey.getEncoded) shouldBe true
    }
  }

  property("Transaction inside atomic container must have atomic badge") {
    forAll(atomicInnerTxsGen(None, 5), positiveLongGen) { (innerTxs, ts) =>
      AtomicTransactionV1.selfSigned(signer, None, innerTxs, ts) should produce("transaction must have atomic badge")
    }
  }

  property("There must be more than 1 transaction inside atomic container") {
    forAll(atomicSingleInnerTxGen(Some(badge)), positiveLongGen) { (singleInnerTx, ts) =>
      AtomicTransactionV1.selfSigned(signer, None, List(singleInnerTx), ts) should produce("Atomic transaction must contain more than 1 transaction")
    }
  }

  property("Atomic badge without trusted address not valid for another sender") {
    forAll(atomicInnerTxsGen(atomicBadge = Some(AtomicBadge()), 5), positiveLongGen) { (innerTxs, ts) =>
      AtomicTransactionV1.selfSigned(signer, None, innerTxs, ts) should (produce("sender address") and produce("does not match the atomic sender"))
    }
  }

  property("Atomic badge with trusted address not valid for another sender") {
    val invalidAtomicBadge = Some(AtomicBadge(Some(accountGen.generateSample().toAddress)))
    forAll(atomicInnerTxsGen(invalidAtomicBadge, 5), positiveLongGen) { (innerTxs, ts) =>
      AtomicTransactionV1.selfSigned(signer, None, innerTxs, ts) should (produce("trusted address") and produce("does not match the atomic sender"))
    }
  }

  private def enrichAtomicTx[T <: AtomicInnerTransaction](senderAcc: PrivateKeyAccount,
                                                          minerAcc: PrivateKeyAccount,
                                                          innerTxs: List[T],
                                                          tx: AtomicTransaction,
                                                          timestamp: Long) = {
    val innerTxsWithExecuted = innerTxs.map {
      case executableTx: ExecutableTransaction =>
        ExecutedContractTransactionV1.selfSigned(senderAcc, executableTx, List.empty, timestamp).explicitGet()
      case tx => tx
    }

    val unsignedMinedTx = tx match {
      case v1: AtomicTransactionV1 => v1.copy(miner = Some(minerAcc), transactions = innerTxsWithExecuted)
    }

    AtomicUtils.addMinerProof(unsignedMinedTx, minerAcc).explicitGet()
  }

}
