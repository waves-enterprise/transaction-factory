package com.wavesplatform.transaction.validation

import com.wavesplatform.acl.PermissionOp
import com.wavesplatform.transaction.ValidationError

object PermitValidation {

  def validatePermissionOp(txTimestamp: Long, permissionOp: PermissionOp): Either[ValidationError, Unit] =
    Either.cond(
      permissionOp.timestamp == txTimestamp,
      (),
      ValidationError.GenericError("Invalid PermitTransaction: timestamp for tx must match timestamp from PermissionOp")
    )
}
