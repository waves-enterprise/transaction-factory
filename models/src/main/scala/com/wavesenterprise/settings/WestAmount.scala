package com.wavesenterprise.settings

import com.wavesenterprise.utils.NumberUtils.ValidBigDecimal
import play.api.libs.json._

case class WestAmount(units: Long) extends AnyVal {
  override def toString: String = units.toString
}

object WestAmount {
  private val tokenName      = "WEST"
  private val minDecimalUnit = BigDecimal(1) / Constants.UnitsInWest

  implicit val WestAmountFormat: Format[WestAmount] = new Format[WestAmount] {
    override def writes(o: WestAmount): JsValue = JsNumber(o.units)
    override def reads(json: JsValue): JsResult[WestAmount] = json match {
      case JsNumber(v) => JsSuccess(WestAmount(v.toLongExact))
      case _           => JsError("Expected JsNumber")
    }
  }

  def unapply(arg: String): Option[WestAmount] = {
    arg.trim.split(' ') match {
      case Array(ValidBigDecimal(decimal), `tokenName`) if decimal >= 0 && (decimal % 1 >= minDecimalUnit || decimal % 1 == 0) =>
        Some(WestAmount((decimal * Constants.UnitsInWest).toLongExact))
      case Array(ValidBigDecimal(units)) if units >= 0 && units % 1 == 0 =>
        Some(WestAmount(units.toLongExact))
      case _ =>
        None
    }
  }
}
