package com.wavesplatform

import com.wavesplatform.account.PrivateKeyAccount

object CryptoHelpers {

  /**
    * Use me only for unit tests!
    */
  def generatePrivateKey: PrivateKeyAccount = {
    PrivateKeyAccount(crypto.generateKeyPair())
  }
}
