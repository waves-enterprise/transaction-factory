package com.wavesplatform.transaction

import com.wavesplatform.crypto
import com.wavesplatform.state.ByteStr
import monix.eval.Coeval

trait FastHashId extends ProvenTransaction {

  val id: Coeval[ByteStr] = Coeval.evalOnce(ByteStr(crypto.fastHash(proofSourceBytes)))
}
