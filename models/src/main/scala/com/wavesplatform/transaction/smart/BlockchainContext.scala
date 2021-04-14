package com.wavesplatform.transaction.smart

import com.wavesplatform.crypto
import com.wavesplatform.lang.{CommonGlobal, Global, GostGlobal}
import com.wavesplatform.transaction.Transaction
import com.wavesplatform.transaction.assets.exchange.Order
import shapeless.{:+:, CNil}

object BlockchainContext {
  type In = Transaction :+: Order :+: CNil
  val baseGlobal: CommonGlobal = if (crypto.isGost) GostGlobal else Global
}
