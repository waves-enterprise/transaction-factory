package com.wavesenterprise.features

case class BlockchainFeature private (id: Short, description: String)

object BlockchainFeature {
  implicit val blockchainFeaturesOrdering: Ordering[BlockchainFeature] =
    Ordering.by(_.id)
}

object BlockchainFeatures {

  val NG                                   = BlockchainFeature(2, "NG Protocol")
  val MassTransfer                         = BlockchainFeature(3, "Mass Transfer Transaction")
  val SmartAccounts                        = BlockchainFeature(4, "Smart Accounts")
  val DataTransaction                      = BlockchainFeature(5, "Data Transaction")
  val BurnAnyTokens                        = BlockchainFeature(6, "Burn Any Tokens")
  val FeeSwitch                            = BlockchainFeature(7, "Fee Switch")
  val SmartAssets                          = BlockchainFeature(9, "Smart Assets")
  val SmartAccountTrading                  = BlockchainFeature(10, "Smart Account Trading")
  val Ride4DApps                           = BlockchainFeature(11, "RIDE 4 DAPPS")
  val ConsensusFix                         = BlockchainFeature(100, "Updated PoS")
  val ContractsGrpcSupport                 = BlockchainFeature(101, "Support of gRPC for Docker contracts")
  val PoaOptimisationFix                   = BlockchainFeature(119, "Performance optimisation for PoA")
  val SponsoredFeesSupport                 = BlockchainFeature(120, "Sponsored fees support")
  val MinerBanHistoryOptimisationFix       = BlockchainFeature(130, "Performance optimisation for miner ban history")
  val AtomicTransactionSupport             = BlockchainFeature(140, "Support of atomic transaction")
  val ParallelLiquidBlockGenerationSupport = BlockchainFeature(160, "Support of parallel generation of liquid block with micro-block")
  val ContractValidationsSupport           = BlockchainFeature(162, "Support of Docker contracts validation")
  val MicroBlockInventoryV2Support         = BlockchainFeature(173, "Support of micro-block inventory v2")

  private val dict = Seq(
    NG,
    MassTransfer,
    SmartAccounts,
    DataTransaction,
    BurnAnyTokens,
    FeeSwitch,
    SmartAccountTrading,
    SmartAssets,
    ConsensusFix,
    ContractsGrpcSupport,
    PoaOptimisationFix,
    SponsoredFeesSupport,
    MinerBanHistoryOptimisationFix,
    AtomicTransactionSupport,
    ParallelLiquidBlockGenerationSupport,
    ContractValidationsSupport,
    MicroBlockInventoryV2Support
  ).map(f => f.id -> f).toMap

  val implemented: Set[Short] = dict.keySet

  def feature(id: Short): Option[BlockchainFeature] = dict.get(id)
}
