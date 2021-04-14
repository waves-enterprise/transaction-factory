package com.wavesplatform

import com.wavesplatform.utils.Constants.base58Length

package object transaction {

  type AssetId = com.wavesplatform.state.ByteStr
  val AssetIdLength: Int       = com.wavesplatform.crypto.DigestSize
  val AssetIdStringLength: Int = base58Length(AssetIdLength)
  type DiscardedTransactions = Seq[Transaction]
  type AuthorizedTransaction = Authorized with Transaction
}
