package com.wavesplatform.transaction.generator.base

sealed abstract class BinaryHeaderType(val scalaTrait: String)

object BinaryHeaderType {
  case object Legacy extends BinaryHeaderType("TransactionParser.OneVersion")
  case object Modern extends BinaryHeaderType("TransactionParser.MultipleVersions")
}

