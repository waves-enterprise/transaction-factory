package com.wavesplatform.transaction.validation

import com.wavesplatform.utils.Constants.base58Length

object TransferValidation {
  val MaxTransferCount: Int        = 100
  val MaxAttachmentSize: Int       = 140
  val MaxAttachmentStringSize: Int = base58Length(MaxAttachmentSize)
}
