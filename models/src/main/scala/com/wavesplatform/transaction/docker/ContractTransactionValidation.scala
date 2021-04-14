package com.wavesplatform.transaction.docker

import cats.implicits._
import com.wavesplatform.state.DataEntry
import com.wavesplatform.transaction.ValidationError.GenericError
import com.wavesplatform.transaction.{Transaction, ValidationError}

/**
  * Common validations for contract transactions
  */
trait ContractTransactionValidation {

  val ImageMinLength: Int                = 1 // in symbols
  val ImageMaxLength: Int                = 200 // in symbols
  val ContractNameMaxLength              = 200 // in symbols
  val MaxExecutedTransactionBytes: Int   = 300 * 1024 // 300KB
  val RequiredValidationProofsCount: Int = 2

  private val Sha256HexLength = 64
  private val Sha256HexDigits = (('0' to '9') ++ ('a' to 'f') ++ ('A' to 'F')).toSet

  def validateImage(image: String): Either[ValidationError, Unit] = {
    Either.cond(
      image.length >= ImageMinLength && image.length <= ImageMaxLength,
      (),
      GenericError(s"Incorrect image length: ${image.length}. Length must be between $ImageMinLength and $ImageMaxLength")
    )
  }

  def validateImageHash(imageHash: String): Either[ValidationError, Unit] = {
    Either.cond(
      imageHash.length == Sha256HexLength && imageHash.forall(Sha256HexDigits.contains),
      (),
      GenericError(s"Image hash string $imageHash is not valid SHA-256 hex string")
    )
  }

  def validateContractName(contractName: String): Either[GenericError, Unit] = {
    Either.cond(
      contractName.nonEmpty && contractName.length <= ContractNameMaxLength,
      (),
      GenericError(
        s"Incorrect contractName length: ${contractName.length}. It must be non-empty and its length must be less than $ContractNameMaxLength")
    )
  }

  def validateParams(params: List[DataEntry[_]]): Either[ValidationError, Unit] = {
    for {
      _ <- Either.cond(params.forall(_.key.nonEmpty), (), GenericError("Param with empty key was found"))
      _ <- Either.cond(params.map(_.key).distinct.length == params.size, (), GenericError("Params with duplicate keys were found"))
      _ <- params.map(ContractTransactionEntryOps.validate).find(_.isLeft).getOrElse(Right(()))
    } yield ()
  }

  def validateResults(results: List[DataEntry[_]]): Either[ValidationError, Unit] = {
    for {
      _ <- Either.cond(results.forall(_.key.nonEmpty), (), GenericError("Result with empty key was found"))
      _ <- Either.cond(results.map(_.key).distinct.length == results.size, (), GenericError("Results with duplicate keys were found"))
      _ <- results.traverse(ContractTransactionEntryOps.validate)
    } yield ()
  }

  def validateSize(tx: ExecutableTransaction): Either[ValidationError, Unit] = validateSize(tx, ExecutableTransaction.MaxBytes)

  def validateSize(tx: ExecutedContractTransaction): Either[ValidationError, Unit] = validateSize(tx, MaxExecutedTransactionBytes)

  private[this] def validateSize(tx: Transaction, limit: Int): Either[ValidationError.ContractTransactionTooBig, Unit] = {
    val size = tx.bytes().length
    Either.cond(size <= limit, (), ValidationError.ContractTransactionTooBig(size, limit))
  }

}
