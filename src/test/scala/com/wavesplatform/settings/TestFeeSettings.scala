package com.wavesplatform.settings

import com.wavesplatform.transaction._
import com.wavesplatform.transaction.acl.PermitTransaction
import com.wavesplatform.transaction.assets._
import com.wavesplatform.transaction.assets.exchange.ExchangeTransaction
import com.wavesplatform.transaction.docker._
import com.wavesplatform.transaction.lease.{LeaseCancelTransaction, LeaseTransaction}
import com.wavesplatform.transaction.smart.SetScriptTransaction
import com.wavesplatform.transaction.transfer.{MassTransferTransaction, TransferTransaction}
import com.wavesplatform.utils.NumberUtils.DoubleExt

case class TestFeeSettings(base: Map[Byte, WestAmount], additional: Map[Byte, WestAmount]) {
  def forTxType(typeId: Byte): Long = base(typeId).units
}

object TestFeeSettings {
  val defaultFees: TestFeeSettings = {
    val fees: Map[Byte, WestAmount] = Map(
      GenesisTransaction.typeId          -> 0.west,
      GenesisPermitTransaction.typeId    -> 0.west,
      IssueTransaction.typeId            -> 1.0.west,
      TransferTransaction.typeId         -> 0.01.west,
      ReissueTransaction.typeId          -> 1.0.west,
      BurnTransaction.typeId             -> 0.05.west,
      ExchangeTransaction.typeId         -> 0.005.west,
      LeaseTransaction.typeId            -> 0.01.west,
      LeaseCancelTransaction.typeId      -> 0.01.west,
      CreateAliasTransaction.typeId      -> 1.0.west,
      MassTransferTransaction.typeId     -> 0.05.west,
      DataTransaction.typeId             -> 0.05.west,
      SetScriptTransaction.typeId        -> 0.5.west,
      SponsorFeeTransactionV1.typeId     -> 1.0.west,
      SetAssetScriptTransactionV1.typeId -> 1.0.west,
      PermitTransaction.typeId           -> 0.01.west,
      CreateContractTransaction.typeId   -> 1.0.west,
      CallContractTransaction.typeId     -> 0.1.west,
      ExecutedContractTransaction.typeId -> 0.0.west,
      DisableContractTransaction.typeId  -> 0.01.west,
      UpdateContractTransaction.typeId   -> 0.0.west,
      RegisterNodeTransactionV1.typeId   -> 0.01.west,
      CreatePolicyTransaction.typeId     -> 1.0.west,
      UpdatePolicyTransaction.typeId     -> 0.5.west,
      PolicyDataHashTransaction.typeId   -> 0.05.west,
      AtomicTransaction.typeId           -> 0.west
    ).mapValues(WestAmount(_))

    val additionalFees: Map[Byte, WestAmount] = Map(
      MassTransferTransaction.typeId -> 0.01.west,
      DataTransaction.typeId         -> 0.01.west
    ).mapValues(WestAmount(_))

    TestFeeSettings(fees, additionalFees)
  }
}
