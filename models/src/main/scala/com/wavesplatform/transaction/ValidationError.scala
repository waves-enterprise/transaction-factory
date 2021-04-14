package com.wavesplatform.transaction

import com.google.common.base.Throwables
import com.wavesplatform.account.{Address, Alias, PublicKeyAccount}
import com.wavesplatform.acl.NonEmptyRole
import com.wavesplatform.crypto.internals.{CryptoError, GenericError => CryptoGenericError, InvalidAddress => CryptoInvalidAddress}
import com.wavesplatform.docker.ContractInfo
import com.wavesplatform.lang.ExprEvaluator.Log
import com.wavesplatform.privacy.PolicyDataHash
import com.wavesplatform.state.ByteStr
import com.wavesplatform.transaction.assets.exchange.Order
import com.wavesplatform.transaction.docker.{ExecutableTransaction, UpdateContractTransaction}

import scala.util.Either

trait ValidationError

object ValidationError {
  type Validation[T] = Either[ValidationError, T]

  case class InvalidPolicyDataHash(reason: String)             extends ValidationError
  case class InvalidAddress(reason: String)                    extends ValidationError
  case class NegativeAmount(amount: Long, of: String)          extends ValidationError
  case class NegativeMinFee(minFee: Long, of: String)          extends ValidationError
  case class InsufficientFee(msg: String = "insufficient fee") extends ValidationError
  case object TooBigArray                                      extends ValidationError
  case object InvalidName                                      extends ValidationError
  case object OverflowError                                    extends ValidationError
  case object ConstraintsOverflowError                         extends ValidationError
  case object ToSelf                                           extends ValidationError
  case object MissingSenderPrivateKey                          extends ValidationError
  case object UnsupportedTransactionType                       extends ValidationError
  case object InvalidRequestSignature                          extends ValidationError
  case class BlockFromFuture(rejectedBlockTs: Long, currentTimestamp: Long) extends ValidationError {
    override def toString: String =
      s"BlockFromFuture(rejected block timestamp is '$rejectedBlockTs', but current timestamp is '$currentTimestamp')"
  }
  case class ScriptParseError(message: String)               extends ValidationError
  case class AlreadyInProcessing(txId: ByteStr)              extends ValidationError
  case class AlreadyInTheState(txId: ByteStr, txHeight: Int) extends ValidationError
  case class AccountBalanceError(errs: Map[Address, String]) extends ValidationError
  case class AliasDoesNotExist(a: Alias)                     extends ValidationError
  case class AliasIsDisabled(a: Alias)                       extends ValidationError
  case class OrderValidationError(order: Order, err: String) extends ValidationError
  case class Mistiming(err: String)                          extends ValidationError

  case class ActivationError(err: String)                              extends ValidationError
  case class UnsupportedVersion(version: Int)                          extends ValidationError
  case class GenericError(err: String)                                 extends ValidationError
  case class PermissionError(err: String)                              extends ValidationError
  case class WrongHandshake(err: String)                               extends ValidationError
  case class InvalidSender(err: String)                                extends ValidationError
  case class ParticipantNotRegistered(address: Address)                extends ValidationError
  case class PolicyDataTooBig(policySize: Long, maxSize: Long)         extends ValidationError
  case class AddressIsLastOfRole(address: Address, role: NonEmptyRole) extends ValidationError

  object GenericError {
    def apply(ex: Throwable): GenericError = new GenericError(Throwables.getStackTraceAsString(ex))
  }

  case class TransactionNotFound(txId: ByteStr) extends ValidationError {
    override def toString: String = s"Transaction with id '$txId' is not found in blockchain"
  }

  case class InvalidSignature(s: Signed, details: Option[InvalidSignature] = None) extends ValidationError {
    override def toString: String = s"InvalidSignature(${s.toString + " reason: " + details})"
  }

  trait HasScriptType extends ValidationError {
    def isTokenScript: Boolean
  }

  case class ScriptExecutionError(error: String, scriptSrc: String, log: Log, isTokenScript: Boolean) extends ValidationError with HasScriptType

  case class TransactionNotAllowedByScript(log: Log, scriptSrc: String, isTokenScript: Boolean) extends ValidationError with HasScriptType

  sealed trait ContractError extends ValidationError

  case class UnexpectedTransactionError(tx: ExecutableTransaction) extends ContractError {
    override def toString: String = s"Executable transaction with id = ${tx.id()}, type = ${tx.builder.typeId} wasn't executed"
  }

  case class InvalidContractId(contractId: String) extends ContractError {
    override def toString: String = s"Contract id '$contractId' is invalid"
  }

  case class ContractNotFound(contractId: ByteStr) extends ContractError {
    override def toString: String = s"Contract '$contractId' is not found"
  }

  case class ContractIsDisabled(contractId: ByteStr) extends ContractError {
    override def toString: String = s"Contract '$contractId' has been disabled. You cannot call or update a disabled contract"
  }

  case class ContractAlreadyDisabled(contractId: ByteStr) extends ContractError {
    override def toString: String = s"Contract '$contractId' has been already disabled"
  }

  case class ContractTransactionTooBig(wrongSize: Long, rightSize: Long) extends ContractError {
    override def toString: String = s"Contract transaction too big, actual size $wrongSize bytes, expected size $rightSize bytes"
  }

  case class ContractVersionMatchError(ci: ContractInfo, calledVersion: Int) extends ContractError {
    override def toString: String =
      s"Called version '$calledVersion' of contract with id '${ci.contractId}' doesn't match actual contract version '${ci.version}'"
  }

  case class ContractUpdateSenderError(tx: UpdateContractTransaction, contractCreator: PublicKeyAccount) extends ContractError {
    override def toString: String = s"Contract update transaction sender '${tx.sender}' is not equal to contract creator '$contractCreator'"
  }

  sealed trait PrivacyError extends ValidationError

  case class PolicyDoesNotExist(policyId: ByteStr) extends PrivacyError {
    override def toString: String = s"Policy $policyId does not exist"
  }

  case class PolicyDataHashAlreadyExists(hash: PolicyDataHash) extends PrivacyError {
    override def toString: String = s"The specified dataset with hash $hash was added earlier"
  }

  case class SenderIsNotInPolicyRecipients(sender: String) extends PrivacyError {
    override def toString: String = s"Sender '$sender' is not in policy recipients"
  }

  case object EmptySenderPKInDataTxError extends ValidationError {
    override def toString: String = "Expected a non-empty senderPublicKey field, because author was not the same as sender"
  }

  case class InvalidValidationProofsCount(actual: Int, expected: Int, resultsHash: ByteStr) extends ContractError {
    override def toString: String = s"Invalid validation proofs count for results hash '$resultsHash'. Actual '$actual', expected '$expected'."
  }

  case class InvalidValidatorSignature(publicKeyAccount: PublicKeyAccount, signature: ByteStr) extends ContractError {
    override def toString: String = s"Invalid validator '$publicKeyAccount' signature '$signature'"
  }

  case class InvalidValidator(publicKeyAccount: PublicKeyAccount) extends ContractError {
    override def toString: String =
      s"Blockchain does not contain validator '$publicKeyAccount' with miner role"
  }

  case class InvalidResultsHash(actual: ByteStr, expected: ByteStr) extends ContractError {
    override def toString: String = s"Invalid results hash. Actual '$actual', expected '$expected'."
  }

  object ParticipantNotRegistered {
    val participantNotRegisteredClassName: String = """(\w*)[$]?$""".r.findAllIn(ParticipantNotRegistered.getClass.getSimpleName).group(1).toLowerCase
  }

  case object SnapshotPeriodIsInProgress extends ValidationError {
    override def toString: String = s"Snapshot period is in progress. Unable to put new tx into UTX"
  }

  def fromCryptoError(e: CryptoError): ValidationError = {
    e match {
      case CryptoInvalidAddress(message) => InvalidAddress(message)
      case CryptoGenericError(message)   => GenericError(message)
    }
  }
}
