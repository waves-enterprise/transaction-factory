package com.wavesenterprise.settings

import com.wavesenterprise.settings.TestFees.{additionalFees, fees}

case class TestFeeSettings(base: Map[Byte, WestAmount], additional: Map[Byte, WestAmount]) {
  def forTxType(typeId: Byte): Long           = base(typeId).units
  def forTxTypeAdditional(typeId: Byte): Long = additional(typeId).units
}

object TestFeeSettings {
  val defaultFees: TestFeeSettings = TestFeeSettings(fees, additionalFees)
}
