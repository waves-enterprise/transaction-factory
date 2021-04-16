package com.wavesplatform.transaction

import com.wavesplatform.CryptoHelpers
import com.wavesplatform.account.PrivateKeyAccount
import org.scalacheck.Gen

trait CommonGen {

  def accountGen: Gen[PrivateKeyAccount] = Gen.Choose.chooseByte.map(_ => CryptoHelpers.generatePrivateKey)

  val atomicBadgeOptGen: Gen[Option[AtomicBadge]] =
    for {
      trustedAddress <- Gen.option(accountGen.map(_.toAddress))
      atomicBadge    <- Gen.option(Gen.const(AtomicBadge(trustedAddress)))
    } yield atomicBadge

  val atomicBadgeGen: Gen[AtomicBadge] =
    for {
      trustedAddress <- accountGen.map(_.toAddress)
      atomicBadge    <- Gen.const(AtomicBadge(Some(trustedAddress)))
    } yield atomicBadge
}
